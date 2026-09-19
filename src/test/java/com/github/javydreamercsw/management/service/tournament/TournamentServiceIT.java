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
package com.github.javydreamercsw.management.service.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link TournamentService#findByIdWithDetails} — the initialization guard
 * for detached UI reads of the tournament graph. These failures cannot happen with mocked
 * repositories (no sessions to violate), so they need a live database.
 */
class TournamentServiceIT extends ManagementIntegrationTest {

  @Autowired private TournamentService tournamentService;
  @Autowired private TitleService titleService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;

  private Universe anyUniverse() {
    return universeRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No universe in test database"));
  }

  @Test
  @DisplayName("findByIdWithDetails initializes the linked title outside a session")
  void findByIdWithDetails_initializesLinkedTitle() {
    // Wizard flow: create a championship, link it to a new tournament.
    Title championship =
        titleService.createTitle(
            "IT Grand Title " + System.nanoTime(),
            "IT fixture",
            WrestlerTier.MIDCARDER,
            com.github.javydreamercsw.management.domain.title.ChampionshipType.SINGLE,
            anyUniverse().getId());
    Tournament tournament =
        tournamentService.createTournament(
            "IT Title Cup " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            championship.getUniverse(),
            championship,
            java.time.LocalDate.now(),
            List.of());

    // The detail view navigates to the new tournament and reads the championship's name
    // outside the transaction — a lazy proxy without initialization throws
    // LazyInitializationException here (regression: initializeGraph skipped linkedTitle).
    Tournament fetched = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(fetched.getLinkedTitle()).isNotNull();
    assertThat(fetched.getLinkedTitle().getName()).isEqualTo(championship.getName());
  }

  @Test
  @DisplayName("findByIdWithDetails initializes entries, rules, and round graph")
  void findByIdWithDetails_initializesGraphCollections() {
    SegmentRule rule = new SegmentRule();
    rule.setName("IT Cup Rule " + System.nanoTime());
    rule.setDescription("IT fixture");
    rule = segmentRuleRepository.save(rule);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Graph Cup " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            anyUniverse(),
            null,
            java.time.LocalDate.now(),
            List.of(rule));

    Tournament fetched = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(fetched.getEntries()).isEmpty();
    assertThat(fetched.getAllowedRules()).hasSize(1);
    assertThat(fetched.getAllowedRules().get(0).getName()).isEqualTo(rule.getName());
    assertThat(fetched.getRounds()).isEmpty();
  }
}
