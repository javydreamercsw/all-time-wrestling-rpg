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
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestoneRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class AdvancedCampaignE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired private CampaignEncounterRepository campaignEncounterRepository;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Autowired private CampaignStorylineRepository campaignStorylineRepository;
  @Autowired private StorylineMilestoneRepository storylineMilestoneRepository;
  @Autowired private DataInitializer dataInitializer;

  private Wrestler player;

  @BeforeEach
  void setup() {
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    storylineMilestoneRepository.deleteAllInBatch();
    campaignStorylineRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();

    dataInitializer.init();

    Account admin = accountRepository.findByUsername("admin").get();

    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(admin);
    player = wrestlers.isEmpty() ? null : wrestlers.getFirst();

    if (player == null) {
      Wrestler w =
          Wrestler.builder()
              .name("Test E2E Veteran")
              .startingHealth(100)
              .startingStamina(100)
              .account(admin)
              .isPlayer(true)
              .active(true)
              .build();
      player = wrestlerRepository.saveAndFlush(w);
    }
  }

  @Test
  void testFightingChampionTrigger() {
    setVideoInfo("Campaign", "The Fighting Champion Chapter", "campaign-fighting-champion");

    // 1. Give player a title
    Title title = titleRepository.findAll().getFirst();
    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setChampions(new java.util.LinkedHashSet<>(java.util.List.of(player)));
    reign.setStartDate(Instant.now());
    titleReignRepository.saveAndFlush(reign);

    // 2. Start Campaign and force chapter
    Campaign campaign = campaignService.startCampaign(player);
    campaign.getState().setCurrentChapterId("fighting_champion");
    campaignStateRepository.saveAndFlush(campaign.getState());

    // 3. Verify Dashboard shows correct chapter
    navigateTo("campaign");

    waitForText("The Fighting Champion");
    captureCaption(
        "The Fighting Champion chapter activates when the player holds a championship."
            + " The campaign tracks title reigns automatically and unlocks this chapter,"
            + " presenting unique storyline choices available only to a defending champion.",
        4500);

    // Scroll down to show alignment track and player card
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    waitForVaadinClientToLoad();
    sleep(1000);
    captureCaption(
        "The alignment track records whether the player is playing face or heel."
            + " In the Fighting Champion chapter, staying face earns crowd support"
            + " that can flip close matches in your favour.",
        4000);

    // Scroll further to show VP rules and actions panel
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    sleep(1000);
    captureCaption(
        "Scoring: +3 VP for any win, −3 VP for a loss, and a bonus +5 VP for every"
            + " successful title defense. The chapter ends in a Legendary Reign once you"
            + " reach 20 VP across at least 5 matches — or in Title Lost the moment"
            + " you drop the belt.",
        5000);

    // Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);
    captureCaption(
        "Use Backstage Actions before each show to train, cut promos, or attack rivals."
            + " Two actions are available per day — spend them wisely to enter each"
            + " title defense at peak condition.",
        4000);

    sleep(1500);
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("The Fighting Champion"));
  }

  @Test
  void testGangWarfareTrigger() {
    setVideoInfo("Campaign", "Gang Warfare Chapter", "campaign-gang-warfare");

    // Start Campaign and force chapter directly (entry condition not evaluated)
    Campaign campaign = campaignService.startCampaign(player);
    campaign.getState().setCurrentChapterId("gang_warfare");
    campaignStateRepository.saveAndFlush(campaign.getState());

    // 3. Verify Dashboard shows correct chapter
    navigateTo("campaign");

    waitForText("Gang Warfare");
    captureCaption(
        "Gang Warfare unlocks when factions collide for territory control — the chapter"
            + " sets up a faction turf war storyline across multiple shows, with backstage"
            + " actions that let the player recruit allies or sabotage rivals.",
        4500);

    // Scroll down to reveal alignment track and player card
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    waitForVaadinClientToLoad();
    sleep(1000);
    captureCaption(
        "The alignment track is especially important here — Heel players gain access"
            + " to sabotage-style backstage actions, while Face players can rally"
            + " the crowd to pressure rivals into mistakes.",
        4000);

    // Scroll further to show VP rules and actions section
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    sleep(1000);
    captureCaption(
        "Scoring: +2 VP per win, −1 VP per loss. A Faction Finale triggers at 10 VP."
            + " Win it and the chapter ends in Faction Dominance; lose and your"
            + " wrestler is Exiled — forced to rebuild from scratch.",
        5000);

    // Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);
    captureCaption(
        "Recruit, train, and prepare between shows using Backstage Actions."
            + " Two actions per day let you build faction strength before"
            + " the climactic showdown.",
        4000);

    sleep(1500);
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Gang Warfare"));
  }

  @Test
  void testCorporatePowerTripTrigger() {
    setVideoInfo("Campaign", "Corporate Power Trip Chapter", "campaign-corporate-power-trip");

    // 1. Start Campaign normally
    Campaign campaign = campaignService.startCampaign(player);

    // 2. Manually set high VP and force chapter
    CampaignState state = campaign.getState();
    state.setVictoryPoints(15);
    state.setCurrentChapterId("corporate_power_trip");
    campaignStateRepository.saveAndFlush(state);

    // 3. Verify Dashboard shows correct chapter
    navigateTo("campaign");

    waitForText("Corporate Power Trip");
    captureCaption(
        "Corporate Power Trip triggers at 15 Victory Points — the player has earned enough"
            + " momentum to challenge the establishment and climb to the top of the card."
            + " VP accumulates from match wins, backstage actions, and completed milestones.",
        4500);

    // Scroll down to reveal alignment track and player card
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    waitForVaadinClientToLoad();
    sleep(1000);
    captureCaption(
        "The alignment track shifts throughout the chapter as the corrupt GM stacks"
            + " the deck against you. Going Heel can unlock shortcuts; staying Face"
            + " builds the crowd heat that fuels the final takeover.",
        4000);

    // Scroll further to show VP rules and actions
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)");
    sleep(1000);
    captureCaption(
        "This is the hardest chapter — rated LEGENDARY. Matches are booked unfairly,"
            + " but the rewards reflect it: +5 VP for every win, and only −1 VP"
            + " for a loss. Survive 5 shows and win the Finale to achieve Takeover,"
            + " or get Buried and removed from the main event picture.",
        5500);

    // Scroll back to top
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1000);
    captureCaption(
        "Use every Backstage Action to counter the Authority's interference."
            + " Smart positioning before each show can be the difference"
            + " between a takeover and getting buried.",
        4000);

    sleep(1500);
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Corporate Power Trip"));
  }

  private void waitForText(final String text) {
    waitForVaadinElement(driver, org.openqa.selenium.By.xpath("//*[contains(., '" + text + "')]"));
  }
}
