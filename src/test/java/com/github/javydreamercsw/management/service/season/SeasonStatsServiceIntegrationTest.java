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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SeasonStatsDTO;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class SeasonStatsServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private SeasonStatsService seasonStatsService;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional
  void testCalculateStats_withDetachedWrestler_shouldNotThrowLazyInitializationException() {
    // 1. Create and save a season
    Season season = new Season();
    season.setName("Test Season");
    season.setStartDate(Instant.now().minus(10, ChronoUnit.DAYS));
    season.setEndDate(Instant.now().plus(10, ChronoUnit.DAYS));
    season = seasonRepository.save(season);

    // 2. Create and save a wrestler
    Wrestler wrestler = createTestWrestler("Stats Wrestler");
    wrestler = wrestlerRepository.save(wrestler);

    // 3. Create and save a title and a reign for this wrestler
    Title title = new Title();
    title.setName("Integration Championship");
    title.setTier(WrestlerTier.MIDCARDER);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setCreationDate(Instant.now());
    title = titleRepository.save(title);

    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setChampions(List.of(wrestler));
    reign.setStartDate(Instant.now().minus(5, ChronoUnit.DAYS));
    reign = titleReignRepository.save(reign);

    // Flush and clear to ensure entities are detached and in the DB
    entityManager.flush();
    entityManager.clear();

    // 4. Reload wrestler and season from DB
    Wrestler detachedWrestler = wrestlerRepository.findById(wrestler.getId()).orElseThrow();
    Season detachedSeason = seasonRepository.findById(season.getId()).orElseThrow();

    // Force detachment again just to be absolutely sure we're testing the scenario
    entityManager.detach(detachedWrestler);
    entityManager.detach(detachedSeason);

    // 5. Call calculateStats and verify it doesn't throw LazyInitializationException when accessing
    // reigns
    final Wrestler finalWrestler = detachedWrestler;
    final Season finalSeason = detachedSeason;

    SeasonStatsDTO stats =
        assertDoesNotThrow(() -> seasonStatsService.calculateStats(finalWrestler, finalSeason));

    assertNotNull(stats);
    assertEquals("Test Season", stats.getSeasonName());
    assertEquals(1, stats.getAccolades().size());
    assertEquals("Integration Championship", stats.getAccolades().get(0));
  }
}
