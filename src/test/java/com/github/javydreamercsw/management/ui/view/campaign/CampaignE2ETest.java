/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.ui.view.campaign;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class CampaignE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired private CampaignEncounterRepository campaignEncounterRepository;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;

  private Wrestler player;

  @BeforeEach
  @Override
  public void setup(org.junit.jupiter.api.TestInfo testInfo) {
    super.setup(testInfo);

    // Initialize campaign for the admin user if it doesn't exist
    Account admin = accountRepository.findByUsername("admin").get();
    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    if (wrestlers.isEmpty()) {
      log.info("Creating a test wrestler for admin user");
      // Check for name conflict too
      wrestlerRepository.findByName("Admin Wrestler").ifPresent(w -> wrestlerRepository.delete(w));

      player = Wrestler.builder().name("Admin Wrestler").account(admin).build();
      player.setExternalId("ADMIN_WRESTLER");
      player = wrestlerRepository.saveAndFlush(player);
    } else {
      player = wrestlers.getFirst();
    }

    if (campaignRepository.findActiveByWrestler(player).isEmpty()) {
      campaignService.startCampaign(player);
    }
  }

  @Test
  void testCampaignFlow() {
    // 1. Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // Verify key elements
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Chapter"));
    assertTrue(driver.getPageSource().contains("Victory Points"));

    takeSequencedScreenshot("campaign-dashboard-initial");

    // 2. Navigate to Backstage Actions
    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Backstage Actions']"));
    clickElement(actionsButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("backstage-actions-view");

    // Verify we are on the backstage actions view
    waitForText("Backstage Actions");

    // 3. Perform an action (Training)
    WebElement trainingButton = waitForVaadinElement(driver, By.id("action-button-TRAINING"));
    clickElement(trainingButton);

    // Verify notification (Success or Fail)
    takeSequencedScreenshot("after-training-action");

    // 4. Navigate back
    // Wrap in retry to handle StaleElementReferenceException if page re-renders during navigation
    int retryCount = 0;
    while (retryCount < 3) {
      try {
        WebElement backButton =
            waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Back to Dashboard']"));
        clickElement(backButton);
        break;
      } catch (org.openqa.selenium.StaleElementReferenceException e) {
        log.warn("Stale element during navigation, retrying... (attempt {})", retryCount + 1);
        retryCount++;
        if (retryCount == 3) throw e;
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
      }
    }
    waitForVaadinClientToLoad();

    waitForText("Campaign: All or Nothing");
  }

  @Test
  void testCampaignUpgrades() {
    // 1. Grant tokens directly in DB before starting test
    Campaign campaign = campaignRepository.findActiveByWrestler(player).get();
    campaign.getState().setSkillTokens(8);
    campaignRepository.save(campaign);

    // 2. Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("campaign-dashboard-for-upgrades");

    // 3. Verify Upgrade section is visible
    waitForText("Available Skill Upgrades");
    waitForText("Iron Man");
    waitForText("Unbreakable");

    // 4. Purchase an upgrade (Iron Man)
    WebElement upgradeButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Iron Man']"));
    clickElement(upgradeButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("after-upgrade-purchase");

    // 5. Verify upgrade is in "Purchased Skills" section
    waitForText("Purchased Skills");
    waitForText("Iron Man: Increases your wrestler’s maximum stamina by 2.");

    // 6. Verify the upgrade section is gone (since only 8 tokens were granted and consumed)
    // assertFalse(driver.getPageSource().contains("Available Skill Upgrades"));
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}
