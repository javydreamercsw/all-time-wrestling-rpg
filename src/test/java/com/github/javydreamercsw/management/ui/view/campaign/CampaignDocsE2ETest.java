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
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CampaignDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private TournamentService tournamentService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleService titleService;

  @Test
  @Order(1)
  void testCaptureBeginningView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "beginning");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: The Beginning");
    documentFeature(
        "Campaign",
        "The Beginning",
        "Your journey starts here. Establish your reputation through backstage encounters and"
            + " initial matches as you find your footing in All Time Wrestling.",
        "campaign-beginning");
  }

  @Test
  @Order(2)
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
  @Order(3)
  void testCaptureTagTeamView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "tag_team");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: Tag Team Redemption");
    documentFeature(
        "Campaign",
        "Tag Team Redemption",
        "Experience the power of partnership. Find a compatible partner and dominate the tag team"
            + " division while rebuilding your momentum.",
        "campaign-tag-team");
  }

  @Test
  @Order(4)
  void testCaptureBetrayalView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "betrayal");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: Betrayal");
    documentFeature(
        "Campaign",
        "Betrayal",
        "Trust is a luxury you can no longer afford. Face the consequences of a broken alliance"
            + " and seek retribution against your former partner.",
        "campaign-betrayal");
  }

  @Test
  @Order(5)
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
  @Order(6)
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
  @Order(7)
  void testCaptureCorporatePowerTripView() {
    // 1. Setup
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler player = getOrCreateWrestler(admin);
    createCampaignInChapter(player, "corporate_power_trip");

    // 2. Navigate
    driver.get("http://localhost:" + serverPort + getContextPath() + "/campaign");
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Chapter: Corporate Power Trip");
    documentFeature(
        "Campaign",
        "Corporate Power Trip",
        "The authorities are against you. Survive impossible odds and unfair stipulations as you"
            + " challenge the corrupt management's control.",
        "campaign-corporate-power-trip");
  }

  @Test
  @Order(8)
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
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    if (!wrestlers.isEmpty()) {
      return wrestlers.get(0);
    }

    Wrestler w =
        Wrestler.builder()
            .name("Docs Wrestler")
            .startingHealth(100)
            .startingStamina(100)
            .account(account)
            .isPlayer(true)
            .active(true)
            .gender(Gender.MALE)
            .build();
    return wrestlerRepository.saveAndFlush(w);
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
