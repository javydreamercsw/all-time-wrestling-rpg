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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class AbilityTreeDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private DataInitializer dataInitializer;

  private Wrestler player;

  @BeforeEach
  void setup() {
    campaignRepository.deleteAll();
    dataInitializer.init();

    Account admin = accountRepository.findByUsername("admin").get();
    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    player =
        wrestlers.isEmpty()
            ? wrestlerRepository.saveAndFlush(
                Wrestler.builder()
                    .name("Ability Tree Wrestler")
                    .startingHealth(100)
                    .startingStamina(100)
                    .account(admin)
                    .isPlayer(true)
                    .active(true)
                    .build())
            : wrestlers.getFirst();

    // Start a campaign and give the wrestler enough tokens to spend
    Campaign campaign = campaignService.startCampaign(player);
    CampaignState state = campaign.getState();
    state.setSkillTokens(20);
    campaignStateRepository.saveAndFlush(state);
  }

  @Test
  void testCaptureAbilityTreeScreenshot() {
    navigateTo("campaign/abilities");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//h2[contains(text(), 'Ability Tree')]"));

    documentFeature(
        "Campaign",
        "Ability Tree",
        "The Ability Tree lets players spend Skill Tokens earned through campaign progress"
            + " on permanent passive upgrades. Each ability card shows its name, effect,"
            + " and token cost. Tokens are scarce — choose upgrades that suit your"
            + " wrestler's style and the current campaign chapter.",
        "campaign-ability-tree");
  }

  @Test
  void testRecordAbilityTreeWalkthrough() {
    setVideoInfo("Campaign", "Skill Upgrades — Ability Tree", "campaign-ability-tree");

    navigateTo("campaign/abilities");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//h2[contains(text(), 'Ability Tree')]"));

    captureCaption(
        "Ability Tree — spend Skill Tokens earned through campaign wins and backstage"
            + " actions to unlock permanent passive upgrades. Each ability improves"
            + " a specific aspect of your wrestler's in-ring performance.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(1000);
    captureCaption(
        "Three core ability tracks are available: Iron Man (stamina efficiency),"
            + " High Flyer (agility bonus), and Hardcore Legend (weapon damage)."
            + " Each costs 8 tokens — enough budget for two abilities over a full campaign.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(800);

    // Click 'Unlock' on the first ability to show the purchase flow
    try {
      org.openqa.selenium.WebElement unlockButton =
          waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='Unlock']"));
      captureCaption(
          "Click Unlock to spend the tokens and activate the ability immediately."
              + " Purchased abilities persist for the rest of the campaign — they are"
              + " listed on the Campaign Dashboard under Purchased Skills.",
          4500);
      clickElement(unlockButton);
      waitForVaadinClientToLoad();
      sleep(800);
      captureCaption(
          "Ability unlocked — the token balance drops and the upgrade is active."
              + " The match engine and AI narration will reference this ability"
              + " when generating outcomes for future matches.",
          4000);
    } catch (Exception e) {
      // If unlock button isn't clickable (e.g. already purchased), just show the tree
      captureCaption(
          "Unlock buttons are disabled when you have insufficient tokens — the cost is"
              + " shown on each card so you can plan your upgrade path across the season.",
          4000);
    }

    sleep(1500);
  }
}
