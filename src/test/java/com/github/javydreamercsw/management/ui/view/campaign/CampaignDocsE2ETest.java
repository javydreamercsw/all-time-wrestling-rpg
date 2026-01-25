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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CampaignDocsE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private TournamentService tournamentService;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository
      wrestlerAlignmentRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository
      backstageActionHistoryRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository
      campaignEncounterRepository;

  @BeforeEach
  void setup() {
    // Clean up
    wrestlerAlignmentRepository.deleteAllInBatch();
    campaignStateRepository.deleteAllInBatch();
    backstageActionHistoryRepository.deleteAllInBatch();
    campaignEncounterRepository.deleteAllInBatch();
    campaignRepository.deleteAllInBatch();
  }

  @Test
  void testCaptureTournamentView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    Campaign campaign = createCampaignInChapter(player, "tournament");

    // Initialize tournament data
    tournamentService.initializeTournament(campaign);

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Tournament Bracket");
    documentFeature(
        "Campaign",
        "Tournament Bracket",
        "The tournament chapter challenges you to climb the ranks in a single-elimination bracket."
            + " Win your matches to advance to the finals and claim the trophy.",
        "campaign-tournament-bracket");
  }

  @Test
  void testCaptureFightingChampionView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "fighting_champion");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: The Fighting Champion");
    documentFeature(
        "Campaign",
        "Fighting Champion",
        "As the Fighting Champion, you must defend your title against a series of challengers. Each"
            + " defense increases your prestige but tests your stamina.",
        "campaign-fighting-champion");
  }

  @Test
  void testCaptureGangWarfareView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "gang_warfare");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: Gang Warfare");
    documentFeature(
        "Campaign",
        "Gang Warfare",
        "In Gang Warfare, you must navigate the dangerous world of factions. Build alliances or"
            + " crush rivals as you fight for control of the locker room.",
        "campaign-gang-warfare");
  }

  private Wrestler getOrCreateWrestler(Account account) {
    return wrestlerRepository
        .findByAccount(account)
        .orElseGet(
            () -> {
              Wrestler w =
                  Wrestler.builder()
                      .name("Docs Wrestler")
                      .startingHealth(100)
                      .startingStamina(100)
                      .account(account)
                      .isPlayer(true)
                      .active(true)
                      .build();
              return wrestlerRepository.save(w);
            });
  }

  private Campaign createCampaignInChapter(Wrestler player, String chapterId) {
    if (campaignService.hasActiveCampaign(player)) {
      Campaign existing = campaignRepository.findActiveByWrestler(player).get();
      existing.getState().setCurrentChapterId(chapterId);
      return campaignRepository.save(existing);
    }
    Campaign c = campaignService.startCampaign(player);
    c.getState().setCurrentChapterId(chapterId);
    return campaignRepository.save(c);
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}
