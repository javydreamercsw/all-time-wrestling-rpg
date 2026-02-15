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
package com.github.javydreamercsw.management.service.season;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "admin", roles = "ADMIN")
class SeasonServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private SeasonService seasonService;
  @Autowired private GameSettingService gameSettingService;

  @BeforeEach
  void setUp() {
    // Clear all seasons for a clean test state
    seasonRepository.deleteAllInBatch();
    gameSettingService.saveCurrentGameDate(
        LocalDate.of(2026, 2, 10)); // Set a consistent in-game date
  }

  @Test
  void testGetActiveSeason_noActiveSeason() {
    Optional<Season> activeSeason = seasonService.getActiveSeason();
    assertFalse(activeSeason.isPresent());
  }

  @Test
  void testGetActiveSeason_activeSeasonWithEndDateInFuture() {
    Season season = new Season();
    season.setName("Future Season");
    season.setStartDate(LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setEndDate(LocalDate.of(2026, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setIsActive(true);
    seasonService.save(season);

    Optional<Season> foundSeason = seasonService.getActiveSeason();
    assertTrue(foundSeason.isPresent());
    assertEquals(season.getName(), foundSeason.get().getName());
  }

  @Test
  void testGetActiveSeason_activeSeasonWithEndDateInPast() {
    gameSettingService.saveCurrentGameDate(LocalDate.of(2026, 3, 1)); // Advance game date

    Season season = new Season();
    season.setName("Past Season");
    season.setStartDate(LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setEndDate(LocalDate.of(2026, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setIsActive(true);
    seasonService.save(season);

    Optional<Season> activeSeason = seasonService.getActiveSeason();
    assertFalse(activeSeason.isPresent());
  }

  @Test
  void testGetActiveSeason_activeSeasonNoEndDate() {
    Season season = new Season();
    season.setName("Ongoing Season");
    season.setStartDate(LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setEndDate(null); // No end date
    season.setIsActive(true);
    seasonService.save(season);

    Optional<Season> foundSeason = seasonService.getActiveSeason();
    assertTrue(foundSeason.isPresent());
    assertEquals(season.getName(), foundSeason.get().getName());
  }

  @Test
  void testGetActiveSeason_inactiveSeason() {
    Season season = new Season();
    season.setName("Inactive Season");
    season.setStartDate(LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setEndDate(LocalDate.of(2026, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
    season.setIsActive(false); // Inactive
    seasonService.save(season);

    Optional<Season> activeSeason = seasonService.getActiveSeason();
    assertFalse(activeSeason.isPresent());
  }

  @Test
  void testGetActiveSeason_multipleSeasonsOnlyOneActive() {
    // Inactive season
    Season inactiveSeason = new Season();
    inactiveSeason.setName("Inactive One");
    inactiveSeason.setStartDate(
        LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    inactiveSeason.setEndDate(
        LocalDate.of(2026, 1, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
    inactiveSeason.setIsActive(false);
    seasonService.save(inactiveSeason);

    // Active season
    Season activeSeason = new Season();
    activeSeason.setName("Active One");
    activeSeason.setStartDate(
        LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    activeSeason.setEndDate(
        LocalDate.of(2026, 2, 28).atStartOfDay(ZoneId.systemDefault()).toInstant());
    activeSeason.setIsActive(true);
    seasonService.save(activeSeason);

    // Another inactive season (date range in future)
    Season futureSeason = new Season();
    futureSeason.setName("Future One");
    futureSeason.setStartDate(
        LocalDate.of(2026, 3, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    futureSeason.setEndDate(
        LocalDate.of(2026, 3, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
    futureSeason.setIsActive(true); // Active, but date is in future
    seasonService.save(futureSeason);

    Optional<Season> foundSeason = seasonService.getActiveSeason();
    assertTrue(foundSeason.isPresent());
    assertEquals("Active One", foundSeason.get().getName());
  }

  @Test
  void testCreateSeason_defaultStartDateIsGameDate() {
    // Current game date is 2026-02-10 from @BeforeEach
    Season newSeason = seasonService.createSeason("New Default Season", "Desc", 5);
    LocalDate expectedStartDate = LocalDate.of(2026, 2, 10);
    assertEquals(
        expectedStartDate, newSeason.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate());
    assertTrue(newSeason.getIsActive());
    assertTrue(newSeason.getEndDate() == null);
  }
}
