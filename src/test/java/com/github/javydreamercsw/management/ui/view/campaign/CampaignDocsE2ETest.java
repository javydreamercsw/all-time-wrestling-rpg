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
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CampaignDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private TournamentService tournamentService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleService titleService;

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

  @Test
  void testCaptureBackstageActionsView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "fighting_champion"); // Any chapter works

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign/actions");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Backstage Area");
    documentFeature(
        "Campaign",
        "Backstage Actions",
        "Take daily actions to improve your stats, recover from injuries, or build hype for your"
            + " next match. Choose wisely, as you have limited time each day.",
        "campaign-backstage-actions");
  }

  @Test
  void testCaptureWrestlerProfileView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);

    // 2. Navigate
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + player.getId());
    takeSequencedScreenshot("before-profile-load");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Wrestler Profile");
    documentFeature(
        "Wrestler",
        "Wrestler Profile",
        "View detailed information about any wrestler, including their core stats (Drive,"
            + " Resilience, Charisma, Brawl), current alignment (FACE/HEEL), and championship"
            + " history.",
        "wrestler-profile");
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
                      .gender(Gender.MALE)
                      .tier(WrestlerTier.MIDCARDER)
                      .fans(5000L)
                      .drive(3)
                      .resilience(2)
                      .charisma(4)
                      .brawl(3)
                      .description(
                          "A rising star in the wrestling world, known for his technical prowess"
                              + " and charismatic promos. He is determined to climb the ranks and"
                              + " become a legend.")
                      .build();
              w = wrestlerRepository.save(w);

              // Assign a title if available or create one
              if (titleRepository.count() == 0) {
                Title title = new Title();
                title.setName("ATW TV Championship");
                title.setIsActive(true);
                title.setTier(WrestlerTier.MIDCARDER);
                title.setChampionshipType(
                    com.github.javydreamercsw.management.domain.title.ChampionshipType.SINGLE);
                title = titleRepository.save(title);
                titleService.awardTitleTo(title, List.of(w));
              } else {
                Title title = titleRepository.findAll().get(0);
                titleService.awardTitleTo(title, List.of(w));
              }

              return w;
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
