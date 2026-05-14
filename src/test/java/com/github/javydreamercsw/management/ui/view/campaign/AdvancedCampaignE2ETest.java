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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    waitForText("The Fighting Champion");
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("The Fighting Champion"));
  }

  @Test
  void testGangWarfareTrigger() {
    // Start Campaign and force chapter directly (entry condition not evaluated)
    Campaign campaign = campaignService.startCampaign(player);
    campaign.getState().setCurrentChapterId("gang_warfare");
    campaignStateRepository.saveAndFlush(campaign.getState());

    // 3. Verify Dashboard shows correct chapter
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    waitForText("Gang Warfare");
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Gang Warfare"));
  }

  @Test
  void testCorporatePowerTripTrigger() {
    // 1. Start Campaign normally
    Campaign campaign = campaignService.startCampaign(player);

    // 2. Manually set high VP and force chapter
    CampaignState state = campaign.getState();
    state.setVictoryPoints(15);
    state.setCurrentChapterId("corporate_power_trip");
    campaignStateRepository.saveAndFlush(state);

    // 3. Verify Dashboard shows correct chapter
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    waitForText("Corporate Power Trip");
    assertTrue(Objects.requireNonNull(driver.getPageSource()).contains("Corporate Power Trip"));
  }

  private void waitForText(final String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}
