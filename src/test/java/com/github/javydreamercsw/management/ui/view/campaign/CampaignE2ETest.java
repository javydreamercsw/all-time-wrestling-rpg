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
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Tag("video")
class CampaignE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;

  private Wrestler player;

  @BeforeEach
  @Override
  public void setup(final TestInfo testInfo) throws Exception {
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
    setVideoInfo("Campaign", "Campaign Flow and Backstage Actions", "campaign-backstage-actions");

    // 1. Navigate to Campaign Dashboard
    navigateTo("campaign");

    // Verify key elements
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Chapter"));
    assertTrue(driver.getPageSource().contains("Victory Points"));
    captureCaption(
        "Campaign Dashboard — tracks your current chapter, Victory Points, and available"
            + " actions. Each chapter has unique story beats and goals; completing them"
            + " unlocks the next chapter and new backstage options.",
        4500);

    // 2. Navigate to Backstage Actions
    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Backstage Actions']"));
    clickElement(actionsButton);
    waitForVaadinClientToLoad();

    // Verify we are on the backstage actions view
    waitForText("Backstage Actions");
    captureCaption(
        "Backstage Actions — spend your one weekly action on Training, Promos, Networking,"
            + " or other activities. Each option has different stat impacts and story"
            + " consequences; choose based on your current chapter goals.",
        4500);

    // 3. Perform an action (Training)
    WebElement trainingButton = waitForVaadinElement(driver, By.id("action-button-TRAINING"));
    clickElement(trainingButton);
    captureCaption(
        "Training increases your wrestler's core stats over time — the result is shown"
            + " immediately and logged to your campaign history. Repeated training compounds"
            + " gains and can eventually unlock new skills.",
        4000);

    // 4. Navigate back
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
        if (retryCount == 3) {
          throw e;
        }
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
      }
    }
    waitForVaadinClientToLoad();

    waitForText("Campaign: All or Nothing");
    captureCaption(
        "Back on the Campaign Dashboard — Victory Points accumulate from backstage actions"
            + " and match wins, driving chapter progression throughout the season. Reaching"
            + " each VP threshold automatically transitions the story to the next chapter.",
        4500);
  }

  @Test
  void testCampaignUpgrades() {
    setVideoInfo("Campaign", "Skill Upgrades", "campaign-skill-upgrades");

    // 1. Grant tokens directly in DB before starting test
    Campaign campaign = campaignRepository.findActiveByWrestler(player).get();
    campaign.getState().setSkillTokens(8);
    campaignRepository.save(campaign);

    // 2. Navigate to Campaign Dashboard
    navigateTo("campaign");

    // 3. Verify Upgrade section is visible
    waitForText("Available Skill Upgrades");
    waitForText("Iron Man");
    waitForText("Unbreakable");
    captureCaption(
        "Skill Upgrades appear on the Campaign Dashboard when you have enough Skill Tokens."
            + " Tokens are earned by completing backstage actions and winning matches —"
            + " the more consistent your performance, the faster you unlock new abilities.",
        4500);

    // 4. Purchase an upgrade (Iron Man)
    WebElement upgradeButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Iron Man']"));
    clickElement(upgradeButton);
    waitForVaadinClientToLoad();
    captureCaption(
        "Purchasing Iron Man — each upgrade permanently boosts a stat for the rest of"
            + " the campaign. Choose carefully; tokens are limited and upgrades cannot be"
            + " reversed once purchased.",
        4000);

    // 5. Verify upgrade is in "Purchased Skills" section
    waitForText("Purchased Skills");
    waitForText("Iron Man: Increases your wrestler's maximum stamina by 2.");
    captureCaption(
        "The upgrade moves to Purchased Skills and takes effect immediately — maximum"
            + " stamina increased by 2 for all future matches. The effective stats shown"
            + " on your Player Dashboard update to reflect the new total.",
        4000);
  }

  private void waitForText(final String text) {
    String escaped =
        text.contains("'")
            ? "concat('" + text.replace("'", "', \"'\", '") + "')"
            : "'" + text + "'";
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), " + escaped + ")]"));
  }
}
