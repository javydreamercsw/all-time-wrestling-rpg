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

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.LocalDate;
import java.util.ArrayList;
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
  @Autowired private TournamentEntryRepository entryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TeamRepository teamRepository;

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
            ChampionshipType.SINGLE,
            anyUniverse().getId());
    Tournament tournament =
        tournamentService.createTournament(
            "IT Title Cup " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            championship.getUniverse(),
            championship,
            LocalDate.now(),
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
            LocalDate.now(),
            List.of(rule));

    Tournament fetched = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(fetched.getEntries()).isEmpty();
    assertThat(fetched.getAllowedRules()).hasSize(1);
    assertThat(fetched.getAllowedRules().get(0).getName()).isEqualTo(rule.getName());
    assertThat(fetched.getRounds()).isEmpty();
  }

  @Test
  @DisplayName("A team-only entry persists — wrestler_id is nullable (V164/V133 relaxation)")
  void teamEntry_persistsWithNullWrestler() {
    // Regression for the V164 (H2) / V133 (MySQL) fix: the exactly-one CHECK constraint needs
    // wrestler_id NULL when team_id is set, but V127/V96's CREATE TABLE kept it NOT NULL — a
    // team-only INSERT violated it. This test fails on a schema without the relaxation.
    Universe universe = anyUniverse();
    Wrestler ace = wrestlerRepository.save(TestUtils.createWrestler("IT Ace", 0L, universe));
    Wrestler bea = wrestlerRepository.save(TestUtils.createWrestler("IT Bea", 0L, universe));
    Team duo = new Team();
    duo.setName("IT Duo " + System.nanoTime());
    duo.setMembers(new ArrayList<>(List.of(ace, bea)));
    duo.setTeamSize(TeamSize.DUO);
    duo = teamRepository.save(duo);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Team Cup " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            List.of(),
            null,
            null,
            null,
            null,
            false,
            false,
            TeamSize.DUO);

    TournamentEntry entry = tournamentService.addEntry(tournament, duo, 1);

    // Round-trip through the database: the row survives with a null wrestler and a set team.
    // Raw row read first — the wrestler column itself must be NULL in the table.
    TournamentEntry raw = entryRepository.findById(entry.getId()).orElseThrow();
    assertThat(raw.getWrestler()).isNull();
    assertThat(raw.getSeed()).isEqualTo(1);
    // Graph read initializes the lazy team proxy for the detached entrantName() assertion.
    Tournament fetched = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    TournamentEntry reloaded =
        fetched.getEntries().stream()
            .filter(e -> e.getId().equals(entry.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(reloaded.getTeam()).isNotNull();
    assertThat(reloaded.getTeam().getId()).isEqualTo(duo.getId());
    assertThat(reloaded.getWrestler()).isNull();
    assertThat(reloaded.entrantName()).isEqualTo(duo.getName());
  }
}
