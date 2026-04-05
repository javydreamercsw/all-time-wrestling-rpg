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
package com.github.javydreamercsw.management.ui.view.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerDashboardDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentService segmentService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;

  @BeforeEach
  public void setupData() {
    // Clear data to ensure a clean state for documentation
    segmentRepository.deleteAll();
    showRepository.deleteAll();
    seasonRepository.deleteAll();
    titleReignRepository.deleteAll();
    titleRepository.deleteAll();
    wrestlerRepository.deleteAll();

    Account playerAccount = accountRepository.findByUsername("player").get();

    // 1. Create Player Wrestler
    Wrestler wrestler =
        Wrestler.builder()
            .name("Documentation Legend")
            .isPlayer(true)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MAIN_EVENTER)
            .account(playerAccount)
            .fans(5000L)
            .build();
    wrestlerRepository.save(wrestler);
    playerAccount.setActiveWrestlerId(wrestler.getId());
    accountRepository.save(playerAccount);

    // 2. Setup Types
    ShowType weeklyType =
        showTypeRepository
            .findByName("Weekly Show")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly Show");
                  st.setDescription("Standard Weekly Show");
                  return showTypeRepository.save(st);
                });

    SegmentType oneOnOne =
        segmentTypeRepository
            .findByName("One on One")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("One on One");
                  return segmentTypeRepository.save(st);
                });

    // 3. Create Title
    Title worldTitle =
        titleRepository
            .findByName("ATW World Heavyweight Championship")
            .orElseGet(
                () -> {
                  Title t = new Title();
                  t.setName("ATW World Heavyweight Championship");
                  t.setTier(WrestlerTier.ICON);
                  t.setChampionshipType(ChampionshipType.SINGLE);
                  t.setIsActive(true);
                  return titleRepository.save(t);
                });

    // 4. Setup Season 1 (Past)
    Season season1 = new Season();
    season1.setName("Season 2025: Genesis");
    season1.setStartDate(Instant.parse("2025-01-01T00:00:00Z"));
    season1.setEndDate(Instant.parse("2025-06-30T23:59:59Z"));
    season1.setIsActive(false);
    seasonRepository.save(season1);

    // Matches for Season 1: 3 wins, 1 loss
    for (int i = 1; i <= 4; i++) {
      Show show = new Show();
      show.setName("Show S1-" + i);
      show.setDescription("Show in Season 1");
      show.setShowDate(LocalDate.of(2025, i, 1));
      show.setType(weeklyType);
      show.setSeason(season1);
      showRepository.save(show);

      Segment segment =
          segmentService.createSegment(show, oneOnOne, Instant.now(), new HashSet<>());
      segment.addParticipant(wrestler);
      if (i < 4) {
        segment.setWinners(List.of(wrestler));
      } else {
        Wrestler jobber =
            wrestlerRepository
                .findByName("Jobber")
                .orElseGet(
                    () -> {
                      Wrestler w = Wrestler.builder().name("Jobber").build();
                      return wrestlerRepository.save(w);
                    });
        segment.addParticipant(jobber);
        segment.setWinners(List.of(jobber));
      }
      segmentService.updateSegment(segment);
    }

    // 5. Setup Season 2 (Active)
    Season season2 = new Season();
    season2.setName("Season 2026: New Era");
    season2.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));
    season2.setIsActive(true);
    seasonRepository.save(season2);

    // Matches for Season 2: 2 wins
    for (int i = 1; i <= 2; i++) {
      Show show = new Show();
      show.setName("Show S2-" + i);
      show.setDescription("Show in Season 2");
      show.setShowDate(LocalDate.of(2026, i, 1));
      show.setType(weeklyType);
      show.setSeason(season2);
      showRepository.save(show);

      Segment segment =
          segmentService.createSegment(show, oneOnOne, Instant.now(), new HashSet<>());
      segment.addParticipant(wrestler);
      segment.setWinners(List.of(wrestler));
      segmentService.updateSegment(segment);
    }

    // Add a Title Reign in Season 2
    TitleReign reign = new TitleReign();
    reign.setTitle(worldTitle);
    reign.setChampions(Arrays.asList(wrestler));
    reign.setStartDate(Instant.parse("2026-01-15T00:00:00Z"));
    titleReignRepository.save(reign);
  }

  @Test
  void testCaptureSeasonSummary() {
    login("player", "player123");

    // 1. Navigate to Player Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/player");
    waitForVaadinToLoad(driver);

    assertDoesNotThrow(
        () -> {
          // 2. Capture Active Season (Season 2)
          waitForVaadinElement(driver, By.id("season-summary-title"));

          documentFeature(
              "Player Dashboard",
              "Season Summary",
              "The Season Summary component provides a snapshot of your wrestler's performance"
                  + " during the currently active season, including their win-loss record, fan"
                  + " growth progress, and any championships or accolades earned.",
              "player-season-summary");

          // 3. Switch to Past Season (Season 1)
          WebElement comboBox = waitForVaadinElement(driver, By.id("season-selector"));
          scrollIntoView(comboBox);

          // Use JavaScript to open the combo box and select the item to avoid flakiness
          selectFromVaadinComboBox("season-selector", "Season 2025: Genesis");

          // Wait for the stats to update
          WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
          wait.until(
              ExpectedConditions.textToBePresentInElementLocated(
                  By.id("season-record"), "Record: 3-1-0"));

          waitForVaadinElement(driver, By.id("season-summary-title"));
          assertEquals(
              "Record: 3-1-0", waitForVaadinElement(driver, By.id("season-record")).getText());

          documentFeature(
              "Player Dashboard",
              "Historical Season Performance",
              "Review your past glory by switching between seasons. The dashboard stores historical"
                  + " data, allowing you to track your wrestler's career trajectory and milestones"
                  + " over time.",
              "player-season-history");
        });
  }
}
