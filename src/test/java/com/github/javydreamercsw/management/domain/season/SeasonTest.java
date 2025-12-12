/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for Season entity. Tests the ATW RPG season management functionality. */
@DisplayName("Season Tests")
class SeasonTest {

  private Season season;

  @BeforeEach
  void setUp() {
    season = new Season();
    season.setName("Test Season");
    season.setShowsPerPpv(5);
    season.setIsActive(true);
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Season newSeason = new Season();

    assertThat(newSeason.getIsActive()).isFalse();
    assertThat(newSeason.getShowsPerPpv()).isEqualTo(5);
    assertThat(newSeason.getShows()).isEmpty();
  }

  @Test
  @DisplayName("Should check if season is current")
  void shouldCheckIfSeasonIsCurrent() {
    // Active season with no end date
    season.setIsActive(true);
    season.setEndDate(null);
    assertThat(season.isCurrentSeason()).isTrue();

    // Inactive season
    season.setIsActive(false);
    assertThat(season.isCurrentSeason()).isFalse();

    // Active but ended season
    season.setIsActive(true);
    season.setEndDate(Instant.now());
    assertThat(season.isCurrentSeason()).isFalse();
  }

  @Test
  @DisplayName("Should count total shows")
  void shouldCountTotalShows() {
    assertThat(season.getTotalShows()).isEqualTo(0);

    // Add shows
    Show show1 = createShow("Show 1", "regular");
    Show show2 = createShow("Show 2", "regular");
    season.addShow(show1);
    season.addShow(show2);

    assertThat(season.getTotalShows()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should calculate expected PPV count")
  void shouldCalculateExpectedPpvCount() {
    // No shows
    assertThat(season.getExpectedPpvCount()).isEqualTo(0);

    // Add 4 shows (not enough for PPV)
    for (int i = 1; i <= 4; i++) {
      season.addShow(createShow("Show " + i, "regular"));
    }
    assertThat(season.getExpectedPpvCount()).isEqualTo(0);

    // Add 5th show (should trigger PPV)
    season.addShow(createShow("Show 5", "regular"));
    assertThat(season.getExpectedPpvCount()).isEqualTo(1);

    // Add 10 shows total
    for (int i = 6; i <= 10; i++) {
      season.addShow(createShow("Show " + i, "regular"));
    }
    assertThat(season.getExpectedPpvCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should determine when it's time for PPV")
  void shouldDetermineWhenItsTimeForPpv() {
    // No shows - not time for PPV
    assertThat(season.isTimeForPpv()).isFalse();

    // Add 4 regular shows - not time yet
    for (int i = 1; i <= 4; i++) {
      season.addShow(createShow("Show " + i, "regular"));
    }
    assertThat(season.isTimeForPpv()).isFalse();

    // Add 5th regular show - time for PPV
    season.addShow(createShow("Show 5", "regular"));
    assertThat(season.isTimeForPpv()).isTrue();

    // Add PPV - should reset
    season.addShow(createShow("PPV 1", "ppv"));
    assertThat(season.isTimeForPpv()).isFalse();

    // Add 5 more regular shows - time for next PPV
    for (int i = 6; i <= 10; i++) {
      season.addShow(createShow("Show " + i, "regular"));
    }
    assertThat(season.isTimeForPpv()).isTrue();
  }

  @Test
  @DisplayName("Should end season properly")
  void shouldEndSeasonProperly() {
    season.setIsActive(true);
    season.setEndDate(null);

    Instant beforeEnd = Instant.now();
    season.endSeason();
    Instant afterEnd = Instant.now();

    assertThat(season.getIsActive()).isFalse();
    assertThat(season.getEndDate()).isNotNull();
    assertThat(season.getEndDate()).isBetween(beforeEnd, afterEnd);
  }

  @Test
  @DisplayName("Should add and remove shows")
  void shouldAddAndRemoveShows() {
    Show show1 = createShow("Show 1", "regular");
    Show show2 = createShow("Show 2", "regular");

    // Add shows
    season.addShow(show1);
    season.addShow(show2);

    assertThat(season.getShows()).hasSize(2);
    assertThat(season.getShows()).extracting(Show::getName).contains("Show 1", "Show 2");
    assertThat(show1.getSeason()).isEqualTo(season);
    assertThat(show2.getSeason()).isEqualTo(season);

    // Remove show
    season.removeShow(show1);

    assertThat(season.getShows()).hasSize(1);
    assertThat(season.getShows()).extracting(Show::getName).contains("Show 2");
    assertThat(season.getShows()).extracting(Show::getName).doesNotContain("Show 1");
    assertThat(show1.getSeason()).isNull();
  }

  @Test
  @DisplayName("Should create display name")
  void shouldCreateDisplayName() {
    season.setName("Championship Era");

    assertThat(season.getDisplayName()).isEqualTo("Championship Era");
  }

  @Test
  @DisplayName("Should handle different shows per PPV settings")
  void shouldHandleDifferentShowsPerPpvSettings() {
    season.setShowsPerPpv(3); // Every 3 shows = 1 PPV

    // Add 2 shows - not time for PPV
    season.addShow(createShow("Show 1", "regular"));
    season.addShow(createShow("Show 2", "regular"));
    assertThat(season.isTimeForPpv()).isFalse();

    // Add 3rd show - time for PPV
    season.addShow(createShow("Show 3", "regular"));
    assertThat(season.isTimeForPpv()).isTrue();
    assertThat(season.getExpectedPpvCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle mixed show types correctly")
  void shouldHandleMixedShowTypesCorrectly() {
    // Add regular shows and PPVs
    season.addShow(createShow("Show 1", "regular"));
    season.addShow(createShow("Show 2", "regular"));
    season.addShow(createShow("PPV 1", "ppv"));
    season.addShow(createShow("Show 3", "regular"));
    season.addShow(createShow("Show 4", "regular"));

    // Should have 4 regular shows and 1 PPV
    assertThat(season.getTotalShows()).isEqualTo(5);

    // PPV counting should be based on regular shows only
    long regularShows =
        season.getShows().stream()
            .filter(show -> !show.getType().getName().toLowerCase().contains("ppv"))
            .count();
    long ppvShows =
        season.getShows().stream()
            .filter(show -> show.getType().getName().toLowerCase().contains("ppv"))
            .count();

    assertThat(regularShows).isEqualTo(4);
    assertThat(ppvShows).isEqualTo(1);
  }

  private Show createShow(String name, String type) {
    Show show = new Show();
    show.setName(name);
    show.setDescription("Test show");

    ShowType showType = new ShowType();
    showType.setName(type);
    showType.setDescription("Test show type");
    show.setType(showType);

    return show;
  }
}
