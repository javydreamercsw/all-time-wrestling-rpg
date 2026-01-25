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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class CampaignE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignRepository
      campaignRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository
      campaignStateRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository
      backstageActionHistoryRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository
      campaignEncounterRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository
      wrestlerAlignmentRepository;

  @BeforeEach
  void setupCampaign() {
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();

    // Manually setup a wrestler for the admin account to avoid brittle UI steps
    Account admin = accountRepository.findByUsername("admin").get();

    Wrestler player =
        wrestlerRepository
            .findByAccount(admin)
            .orElseGet(
                () -> {
                  Wrestler w =
                      Wrestler.builder()
                          .name("Test E2E Wrestler")
                          .startingHealth(100)
                          .startingStamina(100)
                          .account(admin)
                          .isPlayer(true)
                          .active(true)
                          .build();
                  return wrestlerRepository.save(w);
                });

    if (!campaignService.hasActiveCampaign(player)) {
      campaignService.startCampaign(player);
    }
  }

  @Test
  void testCampaignFlow() {
    // 1. Navigate directly to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("campaign-dashboard-initial");
    takeDocScreenshot("campaign-dashboard");

    // Verify dashboard loaded
    waitForText("Campaign: All or Nothing");
    assertTrue(driver.getPageSource().contains("Chapter"));
    assertTrue(driver.getPageSource().contains("Victory Points"));

    // 2. Navigate to Backstage Actions
    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Backstage Actions']"));
    clickElement(actionsButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("backstage-actions-view");
    takeDocScreenshot("backstage-actions");

    // Verify we are on the backstage actions view
    waitForText("Backstage Actions");

    // 3. Perform an action (Training)
    WebElement trainingButton = waitForVaadinElement(driver, By.id("action-button-TRAINING"));
    clickElement(trainingButton);

    // Verify notification (Success or Fail)
    takeSequencedScreenshot("after-training-action");

    // 4. Navigate back
    WebElement backButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Back to Dashboard']"));
    clickElement(backButton);
    waitForVaadinClientToLoad();

    waitForText("Campaign: All or Nothing");
  }

  @Test
  void testCampaignUpgrades() {
    // 1. Grant tokens directly in DB before starting test
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = wrestlerRepository.findByAccount(admin).get();
    com.github.javydreamercsw.management.domain.campaign.Campaign campaign =
        campaignRepository.findActiveByWrestler(player).get();
    campaign.getState().setSkillTokens(8);
    campaignRepository.save(campaign);

    // 2. Navigate to Campaign Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("campaign-dashboard-for-upgrades");

    // 3. Verify Upgrade section is visible
    waitForText("Available Skill Upgrades");
    assertTrue(driver.getPageSource().contains("Iron Man"));
    assertTrue(driver.getPageSource().contains("Unbreakable"));

    // 4. Purchase an upgrade (Iron Man)
    WebElement upgradeButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Iron Man']"));
    clickElement(upgradeButton);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("after-upgrade-purchase");

    // 5. Verify upgrade is in "Purchased Skills" section
    waitForText("Purchased Skills");
    assertTrue(
        driver
            .getPageSource()
            .contains("Iron Man: Increases your wrestler’s maximum stamina by 2."));

    // 6. Verify the upgrade section is gone (since only 8 tokens were granted and consumed)
    assertTrue(!driver.getPageSource().contains("Available Skill Upgrades"));
  }

  private void waitForText(String text) {
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}
