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
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
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
  @Autowired private TournamentRepository tournamentRepository;
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

  @Test
  @DisplayName("Team eligibility is universe-scoped — other-universe teams never seed or count")
  void teamEligibility_isUniverseScoped() {
    Universe home = anyUniverse();
    Universe away =
        universeRepository.findAll().stream()
            .filter(u -> !u.getId().equals(home.getId()))
            .findFirst()
            .orElseGet(
                () -> {
                  Universe spare = new Universe();
                  spare.setName("IT Away Universe " + System.nanoTime());
                  return universeRepository.save(spare);
                });
    assertThat(away.getId()).isNotEqualTo(home.getId());

    Wrestler ace =
        wrestlerRepository.save(TestUtils.createWrestler("IT UAce " + System.nanoTime(), 0L, home));
    Wrestler bea =
        wrestlerRepository.save(TestUtils.createWrestler("IT UBea " + System.nanoTime(), 0L, home));
    Wrestler ed =
        wrestlerRepository.save(TestUtils.createWrestler("IT UEd " + System.nanoTime(), 0L, home));
    Wrestler fay =
        wrestlerRepository.save(TestUtils.createWrestler("IT UFay " + System.nanoTime(), 0L, home));
    Wrestler gil =
        wrestlerRepository.save(TestUtils.createWrestler("IT UGil " + System.nanoTime(), 0L, home));
    Wrestler hal =
        wrestlerRepository.save(TestUtils.createWrestler("IT UHal " + System.nanoTime(), 0L, home));
    Wrestler ivy =
        wrestlerRepository.save(TestUtils.createWrestler("IT UIvy " + System.nanoTime(), 0L, home));
    Wrestler jon =
        wrestlerRepository.save(TestUtils.createWrestler("IT UJon " + System.nanoTime(), 0L, home));
    Wrestler cy =
        wrestlerRepository.save(TestUtils.createWrestler("IT UCy " + System.nanoTime(), 0L, away));
    Wrestler di =
        wrestlerRepository.save(TestUtils.createWrestler("IT UDi " + System.nanoTime(), 0L, away));
    Wrestler kim =
        wrestlerRepository.save(TestUtils.createWrestler("IT UKim " + System.nanoTime(), 0L, away));
    Wrestler lou =
        wrestlerRepository.save(TestUtils.createWrestler("IT ULou " + System.nanoTime(), 0L, away));

    // Four home duos (satisfies the single-elimination format's 4-team minimum) and two away
    // duos with higher fans — scoping must keep the away teams out of both count and seed.
    Team homeDuoA = new Team();
    homeDuoA.setName("IT Home Duo A " + System.nanoTime());
    homeDuoA.setMembers(new ArrayList<>(List.of(ace, bea)));
    homeDuoA.setTeamSize(TeamSize.DUO);
    homeDuoA.setUniverse(home);
    homeDuoA.setFans(400L);
    homeDuoA = teamRepository.save(homeDuoA);
    Team homeDuoB = new Team();
    homeDuoB.setName("IT Home Duo B " + System.nanoTime());
    homeDuoB.setMembers(new ArrayList<>(List.of(ed, fay)));
    homeDuoB.setTeamSize(TeamSize.DUO);
    homeDuoB.setUniverse(home);
    homeDuoB.setFans(300L);
    homeDuoB = teamRepository.save(homeDuoB);
    Team homeDuoC = new Team();
    homeDuoC.setName("IT Home Duo C " + System.nanoTime());
    homeDuoC.setMembers(new ArrayList<>(List.of(gil, hal)));
    homeDuoC.setTeamSize(TeamSize.DUO);
    homeDuoC.setUniverse(home);
    homeDuoC.setFans(200L);
    homeDuoC = teamRepository.save(homeDuoC);
    Team homeDuoD = new Team();
    homeDuoD.setName("IT Home Duo D " + System.nanoTime());
    homeDuoD.setMembers(new ArrayList<>(List.of(ivy, jon)));
    homeDuoD.setTeamSize(TeamSize.DUO);
    homeDuoD.setUniverse(home);
    homeDuoD.setFans(100L);
    homeDuoD = teamRepository.save(homeDuoD);

    Team awayDuoA = new Team();
    awayDuoA.setName("IT Away Duo A " + System.nanoTime());
    awayDuoA.setMembers(new ArrayList<>(List.of(cy, di)));
    awayDuoA.setTeamSize(TeamSize.DUO);
    awayDuoA.setUniverse(away);
    awayDuoA.setFans(9000L);
    teamRepository.save(awayDuoA);
    Team awayDuoB = new Team();
    awayDuoB.setName("IT Away Duo B " + System.nanoTime());
    awayDuoB.setMembers(new ArrayList<>(List.of(kim, lou)));
    awayDuoB.setTeamSize(TeamSize.DUO);
    awayDuoB.setUniverse(away);
    awayDuoB.setFans(8000L);
    teamRepository.save(awayDuoB);

    // Count: only the home-universe teams count for a home-scoped probe. The teams.json seed
    // catalog now legitimately lands in the home universe too (member-agreement scoping, C1
    // fix) — the scoping contract under test is that the AWAY duos never appear.
    Tournament countProbe = new Tournament();
    countProbe.setTeamSize(TeamSize.DUO);
    countProbe.setUniverse(home);
    assertThat(tournamentService.findEligibleTeamsSortedByFans(countProbe))
        .extracting(Team::getName)
        .contains(homeDuoA.getName(), homeDuoB.getName(), homeDuoC.getName(), homeDuoD.getName())
        .doesNotContain(awayDuoA.getName(), awayDuoB.getName());

    // Seed: a home-universe tournament seeds the home team, never the away team.
    Tournament tournament =
        tournamentService.createTournament(
            "IT Universe Cup " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            home,
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

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 4, home.getId());
    // Seeded by team fans within the universe — the higher-fans away teams never appear.
    // (The seeded-catalog teams are also home-scoped since the C1 fix, so the bracket fills
    // with whichever 4 home teams hold the most fans — the contract under test is the away
    // exclusion, not the exact roster.)
    assertThat(entries).extracting(TournamentEntry::getTeam).doesNotContain(awayDuoA, awayDuoB);
  }

  @Test
  @DisplayName(
      "Teams created WITHOUT an explicit universe land in the current universe and become"
          + " eligible (C1 fix)")
  void teamsCreatedWithoutExplicitUniverse_areScopedAndEligible() {
    Universe home = anyUniverse();

    Wrestler ace =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT C1Ace " + System.nanoTime(), 0L, home));
    Wrestler bea =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT C1Bea " + System.nanoTime(), 0L, home));
    Wrestler ed =
        wrestlerRepository.save(TestUtils.createWrestler("IT C1Ed " + System.nanoTime(), 0L, home));
    Wrestler fay =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT C1Fay " + System.nanoTime(), 0L, home));

    // The production shape (registry dialog / wizard one-off): create through the service
    // without ever touching setUniverse — the blind spot that hid C1.
    Team created =
        teamService
            .createTeam(
                "IT C1 Duo " + System.nanoTime(),
                "service-created",
                List.of(ace.getId(), bea.getId()),
                TeamSize.DUO,
                null,
                null,
                com.github.javydreamercsw.management.domain.team.TeamOrigin.REGISTRY)
            .orElseThrow();
    assertThat(created.getUniverse()).as("createTeam must stamp the current universe").isNotNull();
    assertThat(created.getUniverse().getId()).isEqualTo(home.getId());

    // The wizard's eligibility pool finds the freshly created team.
    assertThat(tournamentService.countEligibleTeamEntrants(TeamSize.DUO, null, null, home.getId()))
        .isGreaterThanOrEqualTo(1);
    Tournament probe =
        tournamentService.createTournament(
            "IT C1 Probe " + System.nanoTime(),
            "SINGLE_ELIMINATION",
            home,
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
    assertThat(tournamentService.findEligibleTeamsSortedByFans(probe).stream().map(Team::getId))
        .contains(created.getId());
  }
}
