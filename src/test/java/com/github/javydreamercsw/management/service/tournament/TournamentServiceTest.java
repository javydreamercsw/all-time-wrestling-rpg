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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
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
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRecurrence;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.tournament.TrophyReign;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for team entrants in tournaments (ATW-01g2): the {@code addEntry(Tournament, Team,
 * seed)} validation chain (team-size gate, per-member gender, wrestler-uniqueness across entry
 * shapes), team auto-seeding by team fans, and the creation-time exclusions (contender/briefcase
 * stay singles-only; tournament team size must match the linked TEAM title).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private TournamentEntryRepository entryRepository;
  @Mock private TournamentRoundRepository roundRepository;
  @Mock private TournamentMatchRepository matchRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private ShowRepository showRepository;
  @Mock private ShowBookingService showBookingService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TrophyReignRepository trophyReignRepository;
  @Mock private TrophyReignService trophyReignService;
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
            universeRepository,
            showRepository,
            showBookingService,
            reservationService,
            titleReignRepository,
            segmentRuleRepository,
            titleOpportunityService,
            trophyReignRepository,
            trophyReignService,
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
  @DisplayName(
      "Team seeding is scoped to the tournament's universe — other-universe teams never seed")
  void teamSeedingExcludesOtherUniverseTeams() {
    Tournament tournament = tournament(TeamSize.DUO);
    Universe universe = new Universe();
    universe.setId(7L);
    tournament.setUniverse(universe);

    Team local = duoTeam("Local Duo", 10L, 300L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    local.setUniverse(universe);
    Team localLow = duoTeam("Local Low", 12L, 100L, wrestler("Gil", 7L), wrestler("Hal", 8L));
    localLow.setUniverse(universe);
    Team foreign = duoTeam("Foreign Duo", 11L, 9000L, wrestler("Cy", 3L), wrestler("Di", 4L));
    foreign.setUniverse(otherUniverse(9L));
    // A team with no universe counts as unassigned — it never matches a scoped tournament.
    Team unassigned =
        duoTeam("Unassigned Duo", 13L, 5000L, wrestler("Ed", 5L), wrestler("Fay", 6L));
    when(teamRepository.findAll()).thenReturn(List.of(local, localLow, foreign, unassigned));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of());
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentEntry> entries = service.seedAuto(tournament, 3, 7L);

    // Only the tournament-universe teams seed — the higher-fans foreign team is invisible.
    assertThat(entries).extracting(TournamentEntry::getTeam).containsExactly(local, localLow);
  }

  @Test
  @DisplayName("Team eligibility count is scoped to the given universe")
  void teamEligibilityCountExcludesOtherUniverseTeams() {
    Universe universe = new Universe();
    universe.setId(7L);
    Team local = duoTeam("Local Duo", 10L, 300L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    local.setUniverse(universe);
    Team foreign = duoTeam("Foreign Duo", 11L, 9000L, wrestler("Cy", 3L), wrestler("Di", 4L));
    foreign.setUniverse(otherUniverse(9L));
    when(teamRepository.findAll()).thenReturn(List.of(local, foreign));
    when(universeRepository.findById(7L)).thenReturn(Optional.of(universe));

    assertThat(service.countEligibleTeamEntrants(TeamSize.DUO, null, null, 7L)).isEqualTo(1);
    // No universe filter (null) counts every eligible team regardless of universe.
    assertThat(service.countEligibleTeamEntrants(TeamSize.DUO, null, null, null)).isEqualTo(2);
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
                    TeamSize.DUO,
                    null))
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
                    TeamSize.DUO,
                    null))
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
                    TeamSize.TRIO,
                    null))
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
                    TeamSize.DUO,
                    null))
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
            TeamSize.DUO,
            null);
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

  // ── Fix-wave regressions (final whole-branch review, ATW-01g2) ────────────

  @Test
  @DisplayName("bookRoundOnShow books a team-tournament round through team sides, not solo paths")
  void bookRoundOnShow_teamTournament_booksTeamMatch() {
    Tournament tournament = tournament(TeamSize.DUO);
    Show show = new Show();
    show.setId(50L);
    show.setName("Monday Night");

    // Registry team members in slot order — entrant.getWrestler() is NULL on both sides.
    Wrestler ace = wrestler("Ace", 1L);
    Wrestler bea = wrestler("Bea", 2L);
    Wrestler cy = wrestler("Cy", 3L);
    Wrestler di = wrestler("Di", 4L);
    Team side1 = duoTeam("High Flyers", 10L, 500L, ace, bea);
    Team side2 = duoTeam("Grounders", 11L, 400L, cy, di);

    TournamentEntry entry1 =
        TournamentEntry.builder().tournament(tournament).team(side1).seed(1).build();
    TournamentEntry entry2 =
        TournamentEntry.builder().tournament(tournament).team(side2).seed(2).build();
    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.PENDING)
            .build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(entry1).entrant2(entry2).round(round).build();
    when(matchRepository.findByRoundIdAndWinnerIsNull(round.getId())).thenReturn(List.of(match));

    Segment booked = new Segment();
    booked.setSegmentType(new SegmentType());
    when(showBookingService.bookSpecificTeamMatch(
            eq(show), eq(List.of(ace, bea)), eq(List.of(cy, di)), any(), any()))
        .thenReturn(Optional.of(booked));

    service.bookRoundOnShow(tournament, round, show);

    // The team-aware path ran with full member sides — never the solo-wrestler overload
    // whose @NonNull params the null entrant wrestlers would trip.
    verify(showBookingService)
        .bookSpecificTeamMatch(eq(show), eq(List.of(ace, bea)), eq(List.of(cy, di)), any(), any());
    verify(showBookingService, never())
        .bookSpecificMatch(any(Show.class), any(Wrestler.class), any(Wrestler.class), any(), any());
    assertThat(match.getSegment()).isSameAs(booked);
    assertThat(round.getStatus()).isEqualTo(TournamentRoundStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("bookRoundOnShow keeps the solo path for a singles-tournament round")
  void bookRoundOnShow_singlesTournament_booksSoloMatch() {
    Tournament tournament = tournament(null);
    tournament.setEntries(new ArrayList<>());
    tournament.setRounds(new ArrayList<>());
    Show show = new Show();
    show.setId(50L);

    Wrestler ace = wrestler("Ace", 1L);
    Wrestler bea = wrestler("Bea", 2L);
    TournamentEntry entry1 =
        TournamentEntry.builder().tournament(tournament).wrestler(ace).seed(1).build();
    TournamentEntry entry2 =
        TournamentEntry.builder().tournament(tournament).wrestler(bea).seed(2).build();
    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.PENDING)
            .build();
    TournamentMatch match =
        TournamentMatch.builder().entrant1(entry1).entrant2(entry2).round(round).build();
    when(matchRepository.findByRoundIdAndWinnerIsNull(round.getId())).thenReturn(List.of(match));
    when(showBookingService.bookSpecificMatch(eq(show), eq(ace), eq(bea), any(), any()))
        .thenReturn(Optional.of(new Segment()));

    service.bookRoundOnShow(tournament, round, show);

    verify(showBookingService).bookSpecificMatch(eq(show), eq(ace), eq(bea), any(), any());
    verify(showBookingService, never()).bookSpecificTeamMatch(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "createNextEdition of a recurring TEAM tournament carries the same teamSize (fix-wave C4)")
  void createNextEdition_copiesTeamSize() {
    Tournament completed = tournament(TeamSize.DUO);
    completed.setRecurrence(TournamentRecurrence.ANNUAL);
    when(tournamentRepository.findByParentId(1L)).thenReturn(Optional.empty());
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<Tournament> next = service.createNextEdition(completed);

    assertThat(next).isPresent();
    assertThat(next.get().getTeamSize()).isEqualTo(TeamSize.DUO);
  }

  @Test
  @DisplayName(
      "A linked TEAM title's gender constraint applies with the tournament gender unset (C5)")
  void teamEntry_rejectsMemberNotMatchingLinkedTitleGender() {
    Tournament tournament = tournament(TeamSize.DUO);
    Title womensTagTitles = new Title();
    womensTagTitles.setId(7L);
    womensTagTitles.setName("Women's Tag Championship");
    womensTagTitles.setChampionshipType(ChampionshipType.TEAM);
    womensTagTitles.setGender(Gender.FEMALE);
    tournament.setLinkedTitle(womensTagTitles);
    // Tournament gender unset — the linked title's constraint is the only one.

    Team mixed =
        duoTeam("Mixed Pair", 12L, 500L, wrestler("Bea", 2L, Gender.FEMALE), wrestler("Ace", 1L));

    assertThatThrownBy(() -> service.addEntry(tournament, mixed, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("gender");
  }

  @Test
  @DisplayName("Linked-title gender narrows the team eligibility pool and its count (C5)")
  void linkedTitleGender_narrowsTeamEligibilityPoolAndCount() {
    Title womensTagTitles = new Title();
    womensTagTitles.setId(7L);
    womensTagTitles.setName("Women's Tag Championship");
    womensTagTitles.setChampionshipType(ChampionshipType.TEAM);
    womensTagTitles.setGender(Gender.FEMALE);
    Tournament probe = new Tournament();
    probe.setTeamSize(TeamSize.DUO);
    probe.setLinkedTitle(womensTagTitles);

    Team womens =
        duoTeam(
            "Belles",
            10L,
            500L,
            wrestler("Bea", 2L, Gender.FEMALE),
            wrestler("Cara", 3L, Gender.FEMALE));
    Team mixed =
        duoTeam("Mixed Pair", 12L, 9000L, wrestler("Bea", 2L, Gender.FEMALE), wrestler("Ace", 1L));
    when(teamRepository.findAll()).thenReturn(List.of(womens, mixed));

    // The higher-fans mixed team is excluded through the linked title's constraint.
    assertThat(service.findEligibleTeamsSortedByFans(probe)).containsExactly(womens);
    assertThat(service.countEligibleTeamEntrants(TeamSize.DUO, null, womensTagTitles, null))
        .isEqualTo(1);
  }

  @Test
  @DisplayName(
      "Teams created WITHOUT an explicit universe resolve through the session's current universe")
  void teamEligibilityFindsTeamsCreatedWithoutExplicitUniverse() {
    // The fixture blind spot that hid C1: teams built without touching setUniverse — the
    // shape every production creation path (registry dialog, wizard one-off) produces.
    Universe universe = new Universe();
    universe.setId(7L);
    Team local = duoTeam("Local Duo", 10L, 300L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    local.setUniverse(universe);
    Team localLow = duoTeam("Local Low", 12L, 100L, wrestler("Gil", 7L), wrestler("Hal", 8L));
    localLow.setUniverse(universe);
    Team foreign = duoTeam("Foreign Duo", 11L, 9000L, wrestler("Cy", 3L), wrestler("Di", 4L));
    foreign.setUniverse(otherUniverse(9L));
    when(teamRepository.findAll()).thenReturn(List.of(local, localLow, foreign));

    Tournament tournament = tournament(TeamSize.DUO);
    tournament.setUniverse(universe);
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L)).thenReturn(List.of());
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(universeRepository.findById(7L)).thenReturn(Optional.of(universe));

    // Seeding from the universe-scoped tournament finds the (universe-stamped) registry teams.
    List<TournamentEntry> entries = service.seedAuto(tournament, 2, 7L);
    assertThat(entries).extracting(TournamentEntry::getTeam).containsExactly(local, localLow);

    // And the wizard's eligibility count sees them through the same predicate.
    assertThat(service.countEligibleTeamEntrants(TeamSize.DUO, null, null, 7L)).isEqualTo(2);
  }

  // ── Trophy payoff (named trophy / bragging-rights tournaments) ─────────────

  @Test
  @DisplayName("A named trophy tournament persists the trimmed trophy name")
  void createTournament_trophy_persistsTrimmedName() {
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tournament created =
        service.createTournament(
            "Dusty Cup",
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
            false,
            null,
            "  Dusty Cup  ");

    assertThat(created.getTrophyName()).isEqualTo("Dusty Cup");
    assertThat(created.getLinkedTitle()).isNull();
  }

  @Test
  @DisplayName("Trophy tournaments are legal for teams — team + trophy creates")
  void createTournament_teamWithTrophy_allowed() {
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tournament created =
        service.createTournament(
            "Team Trophy Cup",
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
            false,
            TeamSize.DUO,
            "Dusty Cup");

    assertThat(created.getTeamSize()).isEqualTo(TeamSize.DUO);
    assertThat(created.getTrophyName()).isEqualTo("Dusty Cup");
  }

  @Test
  @DisplayName("A blank trophy name is rejected — a UI slip is not a trophy tournament")
  void createTournament_blankTrophyName_rejected() {
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Blank Trophy Cup",
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
                    false,
                    null,
                    "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Trophy name is required");
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  @DisplayName("The trophy cannot share a tournament with a title, contender, or briefcase payoff")
  void createTournament_trophyConflicts_rejected() {
    Title title = new Title();
    title.setName("World Championship");
    title.setChampionshipType(ChampionshipType.SINGLE);

    // Trophy + linked title.
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Both Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    title,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    null,
                    "Dusty Cup"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Choose one payoff mode");

    // Trophy + contender-deciding (with the title a contender tournament needs — the trophy
    // check fires after the contender preconditions are satisfied).
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Both Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    title,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    false,
                    null,
                    "Dusty Cup"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Choose one payoff mode");

    // Trophy + briefcase-deciding.
    assertThatThrownBy(
            () ->
                service.createTournament(
                    "Both Cup",
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
                    null,
                    "Dusty Cup"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Choose one payoff mode");
    verify(tournamentRepository, never()).save(any(Tournament.class));
  }

  @Test
  @DisplayName("updateTournament: a name arms the trophy, \"\" clears it, null leaves it untouched")
  void updateTournament_trophyNameFlagSemantics() {
    Tournament existing = tournament(null);
    existing.setTrophyName("Dusty Cup");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // null → untouched.
    service.updateTournament(
        1L, "Cup", null, null, null, List.of(), null, null, null, false, null, null, null);
    assertThat(existing.getTrophyName()).isEqualTo("Dusty Cup");

    // "" → cleared (no-payoff bragging-rights edition).
    service.updateTournament(
        1L, "Cup", null, null, null, List.of(), null, null, null, false, null, null, "");
    assertThat(existing.getTrophyName()).isNull();

    // A new name arms it again (trimmed).
    service.updateTournament(
        1L, "Cup", null, null, null, List.of(), null, null, null, false, null, null, " Iron Cup ");
    assertThat(existing.getTrophyName()).isEqualTo("Iron Cup");
  }

  @Test
  @DisplayName(
      "updateTournament rejects a trophy payoff on a tournament that keeps a title linkage")
  void updateTournament_trophyWithTitle_rejected() {
    Title tagTitles = new Title();
    tagTitles.setName("World Tag Championship");
    tagTitles.setChampionshipType(ChampionshipType.TEAM);
    tagTitles.setTeamSize(TeamSize.DUO);
    Tournament existing = tournament(TeamSize.DUO);
    existing.setLinkedTitle(tagTitles);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                service.updateTournament(
                    1L,
                    "Cup",
                    null,
                    tagTitles,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    "Dusty Cup"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Choose one payoff mode");
  }

  @Test
  @DisplayName("markWinner on a trophy tournament stamps a singles reign (idempotent)")
  void markWinner_trophyTournament_recordsSinglesReign() {
    Tournament trophyCup = tournament(null);
    trophyCup.setTrophyName("Dusty Cup");
    Wrestler ace = wrestler("Ace", 1L);
    TournamentEntry winner =
        TournamentEntry.builder().tournament(trophyCup).wrestler(ace).seed(1).build();
    when(entryRepository.findByTournamentIdAndStatus(1L, TournamentEntryStatus.ACTIVE))
        .thenReturn(List.of(winner));
    when(format.isComplete(trophyCup)).thenReturn(true); // final done → markWinner
    when(trophyReignRepository.existsByTournamentId(1L)).thenReturn(false);
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceToNextRound(trophyCup); // format.isComplete → markWinner

    assertThat(trophyCup.getStatus()).isEqualTo(TournamentStatus.COMPLETE);
    verify(trophyReignRepository)
        .save(
            argThat(
                reign ->
                    "Dusty Cup".equals(reign.getTrophyName())
                        && reign.getYearWon() == LocalDate.now().getYear()
                        && reign.getWinnerWrestler() == ace
                        && reign.getWinnerTeam() == null));
  }

  @Test
  @DisplayName("markWinner on a team trophy tournament stamps the team as the reign winner")
  void markWinner_trophyTournament_recordsTeamReign() {
    Tournament trophyCup = tournament(TeamSize.DUO);
    trophyCup.setTrophyName("Dusty Cup");
    Team duo = duoTeam("High Flyers", 10L, 500L, wrestler("Ace", 1L), wrestler("Bea", 2L));
    TournamentEntry winner =
        TournamentEntry.builder().tournament(trophyCup).team(duo).seed(1).build();
    when(entryRepository.findByTournamentIdAndStatus(1L, TournamentEntryStatus.ACTIVE))
        .thenReturn(List.of(winner));
    when(format.isComplete(trophyCup)).thenReturn(true); // final done → markWinner
    when(trophyReignRepository.existsByTournamentId(1L)).thenReturn(false);
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceToNextRound(trophyCup);

    verify(trophyReignRepository)
        .save(
            argThat(
                reign ->
                    "Dusty Cup".equals(reign.getTrophyName())
                        && reign.getWinnerTeam() == duo
                        && reign.getWinnerWrestler() == null));
  }

  @Test
  @DisplayName("A second markWinner run cannot duplicate the reign (unique tournament FK)")
  void markWinner_trophyTournament_idempotent() {
    Tournament trophyCup = tournament(null);
    trophyCup.setTrophyName("Dusty Cup");
    TournamentEntry winner =
        TournamentEntry.builder()
            .tournament(trophyCup)
            .wrestler(wrestler("Ace", 1L))
            .seed(1)
            .build();
    when(entryRepository.findByTournamentIdAndStatus(1L, TournamentEntryStatus.ACTIVE))
        .thenReturn(List.of(winner));
    when(format.isComplete(trophyCup)).thenReturn(true); // final done → markWinner
    when(trophyReignRepository.existsByTournamentId(1L)).thenReturn(true);
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceToNextRound(trophyCup);

    verify(trophyReignRepository, never()).save(any(TrophyReign.class));
  }

  @Test
  @DisplayName("A trophy-reign save failure is swallowed — the tournament still completes")
  void markWinner_trophySaveFailure_completionUnaffected() {
    Tournament trophyCup = tournament(null);
    trophyCup.setTrophyName("Dusty Cup");
    TournamentEntry winner =
        TournamentEntry.builder()
            .tournament(trophyCup)
            .wrestler(wrestler("Ace", 1L))
            .seed(1)
            .build();
    when(entryRepository.findByTournamentIdAndStatus(1L, TournamentEntryStatus.ACTIVE))
        .thenReturn(List.of(winner));
    when(format.isComplete(trophyCup)).thenReturn(true); // final done → markWinner
    when(format.isComplete(trophyCup)).thenReturn(true); // final done → markWinner
    when(trophyReignRepository.existsByTournamentId(1L)).thenReturn(false);
    when(trophyReignRepository.save(any(TrophyReign.class)))
        .thenThrow(new RuntimeException("db down"));
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceToNextRound(trophyCup);

    assertThat(trophyCup.getStatus()).isEqualTo(TournamentStatus.COMPLETE);
    assertThat(trophyCup.getEndDate()).isNotNull();
  }

  @Test
  @DisplayName("trophyLineage and trophyReignOf delegate to the read-side service")
  void trophyLineage_delegatesToTrophyReignService() {
    TrophyReign reign = new TrophyReign();
    when(trophyReignService.lineageOf("Dusty Cup")).thenReturn(List.of(reign));
    when(trophyReignService.reignOfTournament(1L)).thenReturn(reign);

    assertThat(service.trophyLineage("Dusty Cup")).containsExactly(reign);
    assertThat(service.trophyReignOf(1L)).contains(reign);
  }

  // ── Randomize seeds (unbiased draw) ──────────────────────────────────────

  @Test
  @DisplayName("randomizeSeeds reshuffles the same entrants into a seed permutation of 1..n")
  void randomizeSeeds_producesSeedPermutation() {
    Tournament tournament = tournament(TeamSize.DUO);
    List<TournamentEntry> stored = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      stored.add(
          TournamentEntry.builder()
              .id((long) (i + 1))
              .team(
                  duoTeam(
                      "Team " + i,
                      10L + i,
                      100L + i,
                      wrestler("A" + i, 100L + i),
                      wrestler("B" + i, 200L + i)))
              .seed(i + 1)
              .build());
    }
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L))
        .thenReturn(new ArrayList<>(stored))
        .thenAnswer(inv -> new ArrayList<>(stored));
    when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentEntry> result = service.randomizeSeeds(1L);

    // The shuffle persists exactly one new seed per entry: seeds 1..n over the same entrant
    // set, no duplicates, nobody lost or added. (A random shuffle can coincide with the input
    // order, so the contract is the permutation, not a different order.)
    assertThat(result).hasSameSizeAs(stored);
    assertThat(result)
        .extracting(TournamentEntry::getSeed)
        .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
    assertThat(result)
        .extracting(TournamentEntry::getId)
        .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);
    for (TournamentEntry entry : result) {
      assertThat(entry.getSeed()).isBetween(1, 6);
    }
    verify(entryRepository, atLeastOnce()).save(any(TournamentEntry.class));
  }

  @Test
  @DisplayName("randomizeSeeds is locked outside SCHEDULED, like manual reordering")
  void randomizeSeeds_lockedOutsideScheduled() {
    Tournament tournament = tournament(TeamSize.DUO);
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> service.randomizeSeeds(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("locked");
    verify(entryRepository, never()).save(any(TournamentEntry.class));
  }

  @Test
  @DisplayName("randomizeSeeds is locked once the bracket is generated")
  void randomizeSeeds_lockedWithBracket() {
    Tournament tournament = tournament(TeamSize.DUO);
    tournament.setRounds(
        new ArrayList<>(
            List.of(TournamentRound.builder().roundNumber(1).roundName("Round 1").build())));
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> service.randomizeSeeds(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("locked");
    verify(entryRepository, never()).save(any(TournamentEntry.class));
  }

  // ── Detached-instance start (live tour regression) ─────────────────────────

  @Test
  @DisplayName(
      "startTournament re-resolves a detached instance whose entries were seeded after"
          + " it was loaded")
  void startTournament_reResolvesDetachedInstanceSeededAfterLoad() {
    // The live-tested failure (tour step 3): the view fetched the empty tournament, seedAuto
    // persisted entrants through its own transaction, and the view's stale in-memory copy
    // (entries initialized empty before seeding) hit startTournament — the entrants check read
    // the stale collection, not the database.
    Tournament stale = tournament(null); // entries: [] (initialized before seeding)
    stale.setStatus(TournamentStatus.SCHEDULED);

    // The managed graph the repository would return inside startTournament's transaction.
    Tournament managed = tournament(null);
    managed.setStatus(TournamentStatus.SCHEDULED);
    Wrestler w1 = wrestler("Apollo", 1L);
    Wrestler w2 = wrestler("Tormenta", 2L);
    managed
        .getEntries()
        .add(TournamentEntry.builder().id(101L).tournament(managed).wrestler(w1).seed(1).build());
    managed
        .getEntries()
        .add(TournamentEntry.builder().id(102L).tournament(managed).wrestler(w2).seed(2).build());

    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(managed));
    when(format.generateBracket(eq(managed), any()))
        .thenAnswer(
            inv -> {
              TournamentRound round =
                  TournamentRound.builder()
                      .tournament(managed)
                      .roundNumber(1)
                      .roundName("Quarter-Final")
                      .status(TournamentRoundStatus.PENDING)
                      .build();
              TournamentMatch match =
                  TournamentMatch.builder().entrant1(managed.getEntries().getFirst()).build();
              match.setRound(round);
              round.setMatches(new ArrayList<>(List.of(match)));
              return new ArrayList<>(List.of(round));
            });
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tournament started = service.startTournament(stale);

    // Started from the managed graph, not the stale copy: bracket generated, IN_PROGRESS.
    assertThat(started.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    verify(format).generateBracket(eq(managed), any());
    assertThat(started.getRounds()).hasSize(1);
  }

  @Test
  @DisplayName("startTournament keeps working when handed an already-managed instance")
  void startTournament_managedInstanceStillStarts() {
    Tournament managed = tournament(null);
    managed.setStatus(TournamentStatus.SCHEDULED);
    managed
        .getEntries()
        .add(
            TournamentEntry.builder()
                .id(101L)
                .tournament(managed)
                .wrestler(wrestler("Apollo", 1L))
                .seed(1)
                .build());
    managed
        .getEntries()
        .add(
            TournamentEntry.builder()
                .id(102L)
                .tournament(managed)
                .wrestler(wrestler("Tormenta", 2L))
                .seed(2)
                .build());
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(managed));
    when(format.generateBracket(eq(managed), any()))
        .thenAnswer(
            inv -> {
              TournamentRound round =
                  TournamentRound.builder()
                      .tournament(managed)
                      .roundNumber(1)
                      .roundName("Quarter-Final")
                      .status(TournamentRoundStatus.PENDING)
                      .build();
              return new ArrayList<>(List.of(round));
            });
    when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Tournament started = service.startTournament(managed);

    assertThat(started.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    verify(format).generateBracket(eq(managed), any());
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
    // C1 regression shape: a fixture-built team carries no universe unless a test stamps one —
    // production stamps it at create (TeamService.createTeam scopes to the current universe).
    return team(name, id, TeamSize.DUO, TeamOrigin.REGISTRY, TeamStatus.ACTIVE, fans, members);
  }

  private Universe otherUniverse(long id) {
    Universe universe = new Universe();
    universe.setId(id);
    return universe;
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
