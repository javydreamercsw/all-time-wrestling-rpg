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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for team entrants in tournaments (ATW-01g2): the {@code addEntry(Tournament, Team,
 * seed)} validation chain (team-size gate, per-member gender, wrestler-uniqueness across entry
 * shapes), team auto-seeding by team fans, and the creation-time exclusions (contender/briefcase
 * stay singles-only; tournament team size must match the linked TEAM title).
 */
@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private TournamentEntryRepository entryRepository;
  @Mock private TournamentRoundRepository roundRepository;
  @Mock private TournamentMatchRepository matchRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private ShowRepository showRepository;
  @Mock private ShowBookingService showBookingService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TournamentFormat format;

  private TournamentService service;

  @BeforeEach
  void setUp() {
    lenient().when(format.getFormatId()).thenReturn("SINGLE_ELIMINATION");
    lenient().when(format.getMinEntrants()).thenReturn(2);
    lenient().when(format.getMaxEntrants()).thenReturn(8);
    // Construct manually: Mockito cannot inject a plain List into the constructor.
    service =
        new TournamentService(
            tournamentRepository,
            entryRepository,
            roundRepository,
            matchRepository,
            wrestlerRepository,
            teamRepository,
            showRepository,
            showBookingService,
            reservationService,
            titleReignRepository,
            segmentRuleRepository,
            titleOpportunityService,
            List.of(format));
  }

  @Test
  @DisplayName("Team entry persists with team and seed into a team tournament")
  void addsTeamEntryToTeamTournament() {
    Tournament tournament = tournament(TeamSize.DUO);
    Team duo = duoTeam("High Flyers", 10L, 500L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of());
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TournamentEntry entry = service.addEntry(tournament, duo, 3);

    assertThat(entry.getTeam()).isSameAs(duo);
    assertThat(entry.getWrestler()).isNull();
    assertThat(entry.getTournament()).isSameAs(tournament);
    assertThat(entry.getSeed()).isEqualTo(3);
    // Central name resolution: a team entry resolves through the team side.
    assertThat(entry.entrantName()).isEqualTo("High Flyers");
    assertThat(entry.entrantId()).isEqualTo(10L);
    verify(entryRepository).save(any(TournamentEntry.class));
  }

  @Test
  @DisplayName("A team cannot enter a singles tournament (teamSize null)")
  void rejectsTeamEntryInSinglesTournament() {
    Tournament tournament = tournament(null);
    Team duo = duoTeam("High Flyers", 10L, 500L, wrestler("Ace", 1L), wrestler("Bea", 2L));

    assertThatThrownBy(() -> service.addEntry(tournament, duo, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("singles");
  }

  @Test
  @DisplayName("A team cannot enter a team tournament of a different size")
  void rejectsTeamWhenSizeMismatchesTournament() {
    Tournament tournament = tournament(TeamSize.DUO);
    Team trio =
        trioTeam(
            "Three Amigos",
            11L,
            500L,
            wrestler("Ace", 1L),
            wrestler("Bea", 2L),
            wrestler("Cy", 3L));

    assertThatThrownBy(() -> service.addEntry(tournament, trio, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TRIO team");
  }

  @Test
  @DisplayName("Every member must satisfy the tournament gender restriction")
  void rejectsTeamWhenAnyMemberFailsGender() {
    Tournament tournament = tournament(TeamSize.DUO);
    tournament.setGender(Gender.FEMALE);
    Team duo =
        duoTeam("Mixed Pair", 12L, 500L, wrestler("Bea", 2L, Gender.FEMALE), wrestler("Ace", 1L));

    assertThatThrownBy(() -> service.addEntry(tournament, duo, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("gender");
  }

  @Test
  @DisplayName("A wrestler already entered via a team cannot be added again — via team or solo")
  void rejectsWrestlerAlreadyEnteredViaTeam() {
    Tournament tournament = tournament(TeamSize.DUO);
    Wrestler ace = wrestler("Ace", 1L);
    Wrestler bea = wrestler("Bea", 2L);
    Wrestler cy = wrestler("Cy", 3L);
    Team holders = duoTeam("Title Holders", 10L, 500L, ace, bea);
    TournamentEntry existing =
        TournamentEntry.builder().tournament(tournament).team(holders).seed(1).build();
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of(existing));

    // Via another team that shares member Ace.
    Team challengers = duoTeam("Challengers", 11L, 400L, ace, cy);
    assertThatThrownBy(() -> service.addEntry(tournament, challengers, 2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already entered");

    // Individually — a team tournament takes team entries, so a solo add is rejected outright.
    assertThatThrownBy(() -> service.addEntry(tournament, ace, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("team");
  }

  @Test
  @DisplayName("Team tournaments auto-seed from eligible REGISTRY teams by team fans descending")
  void seedsTeamTournamentByTeamFansDescending() {
    Tournament tournament = tournament(TeamSize.DUO);
    Wrestler ace = wrestler("Ace", 1L);
    Wrestler bea = wrestler("Bea", 2L);
    Wrestler cy = wrestler("Cy", 3L);
    Wrestler di = wrestler("Di", 4L);
    Team high = duoTeam("High Fans", 10L, 900L, ace, bea);
    Team low = duoTeam("Low Fans", 11L, 100L, cy, di);
    // Excluded from the pool: ad-hoc origin, inactive team, inactive member — despite fans.
    Team adHoc = duoTeam("One-Offs", 12L, 5000L, wrestler("Ed", 5L), wrestler("Fay", 6L));
    adHoc.setOrigin(TeamOrigin.AD_HOC);
    Team disbanded = duoTeam("Disbanded", 13L, 4000L, wrestler("Gil", 7L), wrestler("Hal", 8L));
    disbanded.setStatus(TeamStatus.DISBANDED);
    Wrestler retired = wrestler("Ivan", 9L);
    retired.setActive(false);
    Team inactiveMember = duoTeam("Benched", 14L, 3000L, retired, wrestler("Joy", 10L));
    when(teamRepository.findAll()).thenReturn(List.of(high, low, adHoc, disbanded, inactiveMember));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of());
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentEntry> entries = service.seedAuto(tournament, 2, null);

    assertThat(entries).extracting(TournamentEntry::getTeam).containsExactly(high, low);
    assertThat(entries).extracting(TournamentEntry::getSeed).containsExactly(1, 2);
  }

  @Test
  @DisplayName("Team seeding beyond the format's maximum entrants is rejected")
  void rejectsTeamSeedingBeyondFormatMax() {
    // Format max is 8 (setUp); a 50-eligible-team registry cannot be seeded into it — the same
    // range check the wrestler path enforces through seedWith (ATW-01g2 review).
    List<Team> fifty = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      Wrestler w1 = wrestler("A" + i, 100L + i * 2);
      Wrestler w2 = wrestler("B" + i, 101L + i * 2);
      fifty.add(duoTeam("Team " + i, 20L + i, 100L + i, w1, w2));
    }
    when(teamRepository.findAll()).thenReturn(fifty);

    assertThatThrownBy(() -> service.seedAuto(tournament(TeamSize.DUO), 50, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("out of range [2, 8]");
    verify(entryRepository, never()).save(any(TournamentEntry.class));
  }

  @Test
  @DisplayName("Team seeding clamps to the pool below the format max, like the wrestler path")
  void clampsTeamSeedingToEligiblePool() {
    Tournament tournament = tournament(TeamSize.DUO);
    Team high = duoTeam("High", 10L, 900L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    Team low = duoTeam("Low", 11L, 100L, wrestler("Cy", 3L), wrestler("Di", 4L));
    when(teamRepository.findAll()).thenReturn(List.of(high, low));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of());
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Requesting 5 from a 2-team registry seeds both, inside the format range.
    List<TournamentEntry> entries = service.seedAuto(tournament, 5, null);

    assertThat(entries).extracting(TournamentEntry::getTeam).containsExactly(high, low);
  }

  @Test
  @DisplayName("Contender-deciding and briefcase-deciding tournaments stay singles-only")
  void rejectsContenderDecidingTeamTournament() {
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Contender Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    false,
                    TeamSize.DUO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("singles");
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Briefcase Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    true,
                    TeamSize.DUO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("singles");
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  @DisplayName("Tournament team size must match the linked TEAM title's team size")
  void teamSizeMustMatchLinkedTeamTitle() {
    Title tagTitles = new Title();
    tagTitles.setName("World Tag Championship");
    tagTitles.setChampionshipType(ChampionshipType.TEAM);
    tagTitles.setTeamSize(TeamSize.DUO);
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // TRIO tournament linked to a DUO title — rejected.
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Mismatched Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    tagTitles,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    TeamSize.TRIO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("team size");

    // A team-size tournament cannot link a singles title.
    Title singlesTitle = new Title();
    singlesTitle.setName("World Championship");
    singlesTitle.setChampionshipType(ChampionshipType.SINGLE);
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Wrong Title Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    singlesTitle,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    TeamSize.DUO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TEAM");

    // Matching sizes create the tournament with its team size stamped.
    Tournament created =
        service.createTournament(
            "Dusty Cup",
            "SINGLE_ELIMINATION",
            null,
            tagTitles,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            false,
            false,
            TeamSize.DUO);
    assertThat(created.getTeamSize()).isEqualTo(TeamSize.DUO);
    assertThat(created.getLinkedTitle()).isSameAs(tagTitles);
  }

  @Test
  @DisplayName("An entry holding both wrestler and team is rejected by service validation")
  void rejectsEntryWithBothWrestlerAndTeam() {
    Tournament tournament = tournament(TeamSize.DUO);
    TournamentEntry corrupt =
        TournamentEntry.builder()
            .tournament(tournament)
            .wrestler(wrestler("Ace", 1L))
            .team(duoTeam("High Flyers", 10L, 500L, wrestler("Ace", 1L), wrestler("Bea", 2L)))
            .seed(1)
            .build();

    assertThatThrownBy(() -> TournamentService.validateEntrantShape(corrupt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one");
  }

  @Test
  @DisplayName("An entry holding neither wrestler nor team is rejected by service validation")
  void rejectsEntryWithNeitherWrestlerNorTeam() {
    Tournament tournament = tournament(TeamSize.DUO);
    TournamentEntry corrupt = TournamentEntry.builder().tournament(tournament).seed(1).build();

    assertThatThrownBy(() -> TournamentService.validateEntrantShape(corrupt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one");
  }

  @Test
  @DisplayName("entrantName/entrantId resolve through the entry's shape")
  void resolvesEntrantNameAndIdPerShape() {
    Tournament tournament = tournament(TeamSize.DUO);
    Wrestler ace = wrestler("Ace", 1L);
    Team duo = duoTeam("High Flyers", 10L, 500L, ace, wrestler("Bea", 2L));

    TournamentEntry individual =
        TournamentEntry.builder().tournament(tournament).wrestler(ace).seed(1).build();
    TournamentEntry team =
        TournamentEntry.builder().tournament(tournament).team(duo).seed(2).build();

    assertThat(individual.entrantName()).isEqualTo("Ace");
    assertThat(individual.entrantId()).isEqualTo(1L);
    assertThat(team.entrantName()).isEqualTo("High Flyers");
    assertThat(team.entrantId()).isEqualTo(10L);
    // A shapeless entry (the DB-constraint backstop makes this unpersistable) resolves to nulls
    // rather than throwing — display code guards on the null.
    TournamentEntry shapeless = TournamentEntry.builder().tournament(tournament).seed(3).build();
    assertThat(shapeless.entrantName()).isNull();
    assertThat(shapeless.entrantId()).isNull();
  }

  // ── Fixtures ─────────────────────────────────────────────────────────────

  private Tournament tournament(TeamSize teamSize) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setTeamSize(teamSize);
    tournament.setEntries(new ArrayList<>());
    tournament.setRounds(new ArrayList<>());
    return tournament;
  }

  private Wrestler wrestler(String name, long id) {
    return wrestler(name, id, Gender.MALE);
  }

  private Wrestler wrestler(String name, long id, Gender gender) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    wrestler.setGender(gender);
    wrestler.setActive(true);
    return wrestler;
  }

  private Team duoTeam(String name, long id, long fans, Wrestler... members) {
    return team(name, id, TeamSize.DUO, TeamOrigin.REGISTRY, TeamStatus.ACTIVE, fans, members);
  }

  private Team trioTeam(String name, long id, long fans, Wrestler... members) {
    return team(name, id, TeamSize.TRIO, TeamOrigin.REGISTRY, TeamStatus.ACTIVE, fans, members);
  }

  private Team team(
      String name,
      long id,
      TeamSize teamSize,
      TeamOrigin origin,
      TeamStatus status,
      long fans,
      Wrestler... members) {
    Team team = new Team();
    team.setId(id);
    team.setName(name);
    team.setMembers(new ArrayList<>(List.of(members)));
    team.setTeamSize(teamSize);
    team.setOrigin(origin);
    team.setStatus(status);
    team.setFans(fans);
    return team;
  }
}
