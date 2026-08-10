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
package com.github.javydreamercsw.management.ui.view.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonAward;
import com.github.javydreamercsw.management.domain.season.SeasonAwardRepository;
import com.github.javydreamercsw.management.domain.season.SeasonAwardType;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class SeasonAwardsVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private SeasonRepository seasonRepository;
  @Autowired private SeasonAwardRepository awardRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  private Long seasonId;

  @BeforeEach
  void seedSeason() {
    Season season = new Season();
    season.setName("Showcase Season");
    season.setDescription("Season used for video walkthrough");
    season.setShowsPerPpv(5);
    season.setIsActive(false);
    season.setStartDate(Instant.now().minus(60, ChronoUnit.DAYS));
    season.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
    season = seasonRepository.saveAndFlush(season);
    seasonId = season.getId();

    var showType = showTypeRepository.findByName("Weekly").orElse(null);
    if (showType != null) {
      for (int i = 0; i < 12; i++) {
        Show show = new Show();
        show.setName("Showcase " + (i + 1));
        show.setDescription("Week " + (i + 1) + " of the Showcase Season");
        show.setType(showType);
        show.setSeason(season);
        show.setShowDate(LocalDate.now().minusDays(60 - (i * 5L)));
        showRepository.save(show);
      }
      showRepository.flush();
    }

    Wrestler champ =
        wrestlerRepository
            .findByName("Johnny All Time")
            .orElseGet(
                () -> {
                  Wrestler w = new Wrestler();
                  w.setName("Johnny All Time");
                  return wrestlerRepository.saveAndFlush(w);
                });

    Wrestler rising =
        wrestlerRepository
            .findByName("Tormenta")
            .orElseGet(
                () -> {
                  Wrestler w = new Wrestler();
                  w.setName("Tormenta");
                  return wrestlerRepository.saveAndFlush(w);
                });

    awardRepository.saveAllAndFlush(
        List.of(
            SeasonAward.builder()
                .season(season)
                .wrestler(champ)
                .awardType(SeasonAwardType.WRESTLER_OF_THE_YEAR)
                .awardValue("8 wins")
                .build(),
            SeasonAward.builder()
                .season(season)
                .wrestler(rising)
                .awardType(SeasonAwardType.MOST_IMPROVED)
                .awardValue("42,000 new fans")
                .build(),
            SeasonAward.builder()
                .season(season)
                .wrestler(champ)
                .awardType(SeasonAwardType.MOST_DECORATED)
                .awardValue("3 title reign(s)")
                .build()));
  }

  @Test
  void recordSeasonAwardsWorkflow() {
    setVideoInfo("Booker", "Season Awards Ceremony", "booker-season-awards-workflow");

    navigateTo("season-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "The Season List shows all seasons with their status, show count, and date range."
            + " Each row has a 'Details' link to open the full season dashboard.",
        2000);

    navigateTo("season-detail/" + seasonId);
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Season Overview')]"));
    captureCaption(
        "The Season Detail view shows a stat card summary — status, total shows, start/end"
            + " dates, and total duration at a glance.",
        2500);

    captureCaption(
        "The Awards Ceremony section lists each award winner: Wrestler of the Year (most wins),"
            + " Most Improved (highest fan growth), and Most Decorated (most title reigns)."
            + " Winners automatically receive fan bonuses when the season ends.",
        3000);
  }
}
