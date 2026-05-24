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
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
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
class CampaignNarrativeDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
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
                    .name("Narrative Wrestler")
                    .startingHealth(100)
                    .startingStamina(100)
                    .account(admin)
                    .isPlayer(true)
                    .active(true)
                    .build())
            : wrestlers.getFirst();

    campaignService.startCampaign(player);
  }

  @Test
  void testCaptureNarrativeScreenshot() {
    navigateTo("campaign/narrative");
    waitForVaadinClientToLoad();

    // Wait for either the narrative heading or the offline message
    waitForVaadinElement(
        driver, By.xpath("//h2[contains(., 'Story') or contains(., 'Director Offline')]"));

    documentFeature(
        "Campaign",
        "Story Narrative",
        "The Story Narrative view drives your wrestler's campaign journey through"
            + " AI-generated encounter text and branching choices. Each session presents a"
            + " new situation — a backstage confrontation, a contract negotiation, or a"
            + " pre-match promo challenge — and your decision shapes the storyline direction"
            + " and campaign VP outcome.",
        "campaign-narrative");
  }

  @Test
  void testRecordNarrativeWalkthrough() {
    setVideoInfo("Campaign", "Story Narrative — AI Encounters", "campaign-narrative-walkthrough");

    navigateTo("campaign/narrative");
    waitForVaadinClientToLoad();
    waitForVaadinElement(
        driver, By.xpath("//h2[contains(., 'Story') or contains(., 'Director Offline')]"));

    captureCaption(
        "Story Narrative — the heart of the campaign mode. Each time you open this view"
            + " the AI Story Director generates a new encounter based on your current"
            + " chapter, alignment track, and recent match history.",
        5000);

    boolean hasNarrative =
        !driver.findElements(By.xpath("//h2[contains(., 'Story Director Offline')]")).isEmpty()
                == false
            && driver.getPageSource().contains("Story Director Offline");

    if (!hasNarrative) {
      // AI is available — show the narrative flow
      ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 150)");
      sleep(800);
      captureCaption(
          "The encounter is displayed as a narrative paragraph describing the situation."
              + " Below it are two or three choice buttons — each represents a different"
              + " approach (aggressive, diplomatic, or cunning) with different VP outcomes.",
          5000);

      ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
      sleep(600);
      captureCaption(
          "Your choice is recorded and affects the storyline's direction. Choosing"
              + " aggressively raises your Heel alignment; diplomatic choices push you"
              + " toward Face. Some encounters trigger bonus matches or backstage events.",
          4500);
    } else {
      // AI offline — show the graceful degradation state
      captureCaption(
          "When no AI provider is configured the view shows a 'Story Director Offline'"
              + " message with a Retry button. Configure an AI provider (Claude, Gemini,"
              + " or OpenAI) in Admin Settings to enable generated narratives.",
          5000);
      captureCaption(
          "Campaign progress still works without AI — backstage actions, ability upgrades,"
              + " and manual match booking all function normally. The narrative view is an"
              + " optional enrichment layer on top of the core campaign loop.",
          4500);
    }

    sleep(1500);
  }
}
