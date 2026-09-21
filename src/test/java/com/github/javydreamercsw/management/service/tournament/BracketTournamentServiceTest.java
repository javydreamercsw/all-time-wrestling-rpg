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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchParticipant;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the bracket {@link TournamentService} lifecycle basics (ATW-oahn evaluation). */
@ExtendWith(MockitoExtension.class)
class BracketTournamentServiceTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private TournamentEntryRepository entryRepository;
  @Mock private TournamentRoundRepository roundRepository;
  @Mock private TournamentMatchRepository matchRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ShowRepository showRepository;
  @Mock private ShowBookingService showBookingService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private TournamentFormat format;

  private TournamentService tournamentService;

  private Tournament tournament;

  @BeforeEach
  void setUp() {
    lenient().when(format.getFormatId()).thenReturn("SINGLE_ELIMINATION");
    lenient().when(format.getMinEntrants()).thenReturn(4);
    lenient().when(format.getMaxEntrants()).thenReturn(8);
    // Construct manually: Mockito cannot inject a plain List into the constructor.
    tournamentService =
        new TournamentService(
            tournamentRepository,
            entryRepository,
            roundRepository,
            matchRepository,
            wrestlerRepository,
            showRepository,
            showBookingService,
            reservationService,
            titleReignRepository,
            List.of(format));

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setEntries(new ArrayList<>());
    tournament.setRounds(new ArrayList<>());
    lenient()
        .when(tournamentRepository.save(any(Tournament.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    lenient()
        .when(entryRepository.save(any(TournamentEntry.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void seedAuto_respectsLinkedTitleGenderConstraint() {
    Title womensTitle = new Title();
    womensTitle.setId(5L);
    womensTitle.setName("Women's Championship");
    womensTitle.setGender(Gender.FEMALE);
    tournament.setLinkedTitle(womensTitle);

    Wrestler female = wrestler(1L, "Female Star", Gender.FEMALE, 900L);
    Wrestler female2 = wrestler(2L, "Female Champ", Gender.FEMALE, 800L);
    Wrestler female3 = wrestler(4L, "Female Contender", Gender.FEMALE, 700L);
    Wrestler female4 = wrestler(5L, "Female Underdog", Gender.FEMALE, 600L);
    Wrestler male = wrestler(3L, "Male Star", Gender.MALE, 9999L);
    when(wrestlerRepository.findAllByGenderAndActive(Gender.FEMALE, true))
        .thenReturn(List.of(female, female2, female3, female4));
    lenient().when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(male));

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 4, 1L);

    assertThat(entries).hasSize(4);
    assertThat(entries.stream().map(e -> e.getWrestler().getGender())).containsOnly(Gender.FEMALE);
  }

  @Test
  void seedAuto_withoutLinkedTitle_seedsWholeActiveRoster() {
    Wrestler a = wrestler(1L, "A", Gender.FEMALE, 500L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 400L);
    Wrestler c = wrestler(3L, "C", Gender.FEMALE, 300L);
    Wrestler d = wrestler(4L, "D", Gender.MALE, 200L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c, d));

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 4, 1L);

    assertThat(entries).hasSize(4);
    // Sorted by fans descending: A (500) seeds 1, B (400) seeds 2.
    assertThat(entries.get(0).getWrestler().getName()).isEqualTo("A");
    assertThat(entries.get(1).getSeed()).isEqualTo(2);
  }

  @Test
  void seedAuto_rosterSmallerThanRequested_clampsToRosterSize() {
    // 8 entrants requested, only 3 available: seed all 3 (min-entrant check is 4 in the
    // default stub, so raise the format minimum for this scenario).
    lenient().when(format.getMinEntrants()).thenReturn(2);
    Wrestler a = wrestler(1L, "A", Gender.MALE, 300L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 200L);
    Wrestler c = wrestler(3L, "C", Gender.MALE, 100L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c));

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 8, 1L);

    assertThat(entries).hasSize(3);
  }

  @Test
  void seedAuto_rosterBelowFormatMinimum_failsWithClearMessage() {
    Wrestler a = wrestler(1L, "A", Gender.MALE, 300L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a));

    Assertions.assertThatThrownBy(() -> tournamentService.seedAuto(tournament, 4, 1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not enough eligible wrestlers")
        .hasMessageContaining("at least 4");
  }

  @Test
  void seedAuto_titleLinkedTournament_excludesCurrentChampion() {
    // The reigning champion holds the belt the tournament awards — they must not be seeded
    // (they cannot win it from themselves), even as the top fan draw.
    Title mensTitle = new Title();
    mensTitle.setId(7L);
    mensTitle.setName("ATW World");
    tournament.setLinkedTitle(mensTitle);

    Wrestler champion = wrestler(1L, "The Champ", Gender.MALE, 9999L);
    Wrestler a = wrestler(2L, "A", Gender.MALE, 800L);
    Wrestler b = wrestler(3L, "B", Gender.MALE, 700L);
    Wrestler c = wrestler(4L, "C", Gender.MALE, 600L);
    Wrestler d = wrestler(5L, "D", Gender.MALE, 500L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(champion, a, b, c, d));

    TitleReign reign = new TitleReign();
    reign.getChampions().add(champion);
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(7L)).thenReturn(List.of(reign));

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 4, 1L);

    assertThat(entries).hasSize(4);
    assertThat(entries.stream().map(e -> e.getWrestler().getName()))
        .as("champion must be excluded; next four by fans fill the bracket")
        .containsExactly("A", "B", "C", "D");
  }

  @Test
  void seedAuto_vacantTitle_seedsEveryone() {
    Title mensTitle = new Title();
    mensTitle.setId(7L);
    mensTitle.setName("ATW World");
    tournament.setLinkedTitle(mensTitle);

    Wrestler a = wrestler(1L, "A", Gender.MALE, 800L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 700L);
    Wrestler c = wrestler(3L, "C", Gender.MALE, 600L);
    Wrestler d = wrestler(4L, "D", Gender.MALE, 500L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c, d));
    // Vacant — no active reign.
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(7L)).thenReturn(List.of());

    List<TournamentEntry> entries = tournamentService.seedAuto(tournament, 4, 1L);

    assertThat(entries).hasSize(4);
    assertThat(entries.stream().map(e -> e.getWrestler().getName()))
        .containsExactly("A", "B", "C", "D");
  }

  @Test
  void reorderSeeds_reordersAndPersistsNewSeedOrder() {
    Wrestler a = wrestler(1L, "A", Gender.MALE, 800L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 700L);
    Wrestler c = wrestler(3L, "C", Gender.MALE, 600L);
    Wrestler d = wrestler(4L, "D", Gender.MALE, 500L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c, d));
    List<TournamentEntry> seeded = tournamentService.seedAuto(tournament, 4, 1L);
    // entry ids assigned in seed order by the lenient save stub
    for (int i = 0; i < seeded.size(); i++) {
      seeded.get(i).setId((long) (i + 1));
    }
    lenient().when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(entryRepository.findByTournamentIdOrderBySeedAsc(1L))
        .thenReturn(
            List.of(seeded.get(0), seeded.get(1), seeded.get(2), seeded.get(3)),
            // Second read reflects the persisted new order.
            List.of(seeded.get(2), seeded.get(0), seeded.get(1), seeded.get(3)));

    // New order: C first, then A, B, D.
    List<TournamentEntry> reordered = tournamentService.reorderSeeds(1L, List.of(3L, 1L, 2L, 4L));

    assertThat(reordered).extracting(TournamentEntry::getSeed).containsExactly(1, 2, 3, 4);
    assertThat(reordered.get(0).getWrestler().getName()).isEqualTo("C");
    assertThat(reordered.get(1).getWrestler().getName()).isEqualTo("A");
  }

  @Test
  void replaceEntrant_swapsWrestlerKeepingSeed() {
    Wrestler a = wrestler(1L, "A", Gender.MALE, 800L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 700L);
    Wrestler c = wrestler(3L, "C", Gender.MALE, 600L);
    Wrestler d = wrestler(4L, "D", Gender.MALE, 500L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c, d));
    List<TournamentEntry> seeded = tournamentService.seedAuto(tournament, 4, 1L);
    TournamentEntry entryB = seeded.get(1);
    entryB.setId(22L);
    entryB.setTournament(tournament);

    Wrestler outsider = wrestler(9L, "Outsider", Gender.MALE, 100L);
    lenient().when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    lenient().when(entryRepository.findById(22L)).thenReturn(Optional.of(entryB));
    when(wrestlerRepository.findById(9L)).thenReturn(Optional.of(outsider));
    when(entryRepository.existsByTournamentIdAndWrestlerId(1L, 9L)).thenReturn(false);

    TournamentEntry replaced = tournamentService.replaceEntrant(1L, 22L, 9L);

    assertThat(replaced.getSeed()).isEqualTo(2);
    assertThat(replaced.getWrestler().getName()).isEqualTo("Outsider");
  }

  @Test
  void replaceEntrant_wrestlerAlreadyEntered_isRejected() {
    Wrestler a = wrestler(1L, "A", Gender.MALE, 800L);
    Wrestler b = wrestler(2L, "B", Gender.MALE, 700L);
    Wrestler c = wrestler(3L, "C", Gender.MALE, 600L);
    Wrestler d = wrestler(4L, "D", Gender.MALE, 500L);
    when(wrestlerRepository.findAllByActiveTrue()).thenReturn(List.of(a, b, c, d));
    List<TournamentEntry> seeded = tournamentService.seedAuto(tournament, 4, 1L);
    TournamentEntry entryB = seeded.get(1);
    entryB.setId(22L);
    entryB.setTournament(tournament);

    lenient().when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    lenient().when(entryRepository.findById(22L)).thenReturn(Optional.of(entryB));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(a));
    when(entryRepository.existsByTournamentIdAndWrestlerId(1L, 1L)).thenReturn(true);

    Assertions.assertThatThrownBy(() -> tournamentService.replaceEntrant(1L, 22L, 1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already entered");
  }

  @Test
  void isTitleVacant_readsFromReignTable() {
    Title title = new Title();
    title.setId(7L);
    // Vacant — no active reign.
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(7L)).thenReturn(List.of());
    assertThat(tournamentService.isTitleVacant(title)).isTrue();

    // A reigning champion flips it — reign table read, not the in-memory title list.
    TitleReign reign = new TitleReign();
    Wrestler champion = wrestler(1L, "A", Gender.MALE, 800L);
    reign.getChampions().add(champion);
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(7L))
        .thenReturn(List.of(reign));
    assertThat(tournamentService.isTitleVacant(title)).isFalse();
  }

  @Test
  void currentChampionsOf_dedupesAndHandlesNull() {
    assertThat(tournamentService.currentChampionsOf(null)).isEmpty();

    Title title = new Title();
    title.setId(7L);
    Wrestler champion = wrestler(1L, "A", Gender.MALE, 800L);
    TitleReign reign = new TitleReign();
    reign.getChampions().add(champion);
    // A tag-team title shared by two entrants in one reign, plus a second reign.
    reign.getChampions().add(champion);
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(7L)).thenReturn(List.of(reign));

    assertThat(tournamentService.currentChampionsOf(title)).containsExactly(champion);
  }

  @Test
  void recordMatchResult_multiEntrantEliminatesAllLosers() {
    // ATW-oloa: a Free-for-All has 3+ entrants — every non-winner must be eliminated, not just
    // the classic entrant2.
    TournamentEntry winner = entry(1);
    TournamentEntry loser2 = entry(2);
    TournamentEntry loser3 = entry(3);
    TournamentMatch match = new TournamentMatch();
    match.setId(11L);
    match.setEntrant1(winner);
    match.setEntrant2(loser2);
    match.setParticipants(
        new ArrayList<>(
            List.of(
                participant(match, winner, 0),
                participant(match, loser2, 1),
                participant(match, loser3, 2))));
    TournamentRound round = new TournamentRound();
    round.setId(21L);
    match.setRound(round);

    when(matchRepository.findByRoundIdAndWinnerIsNull(21L)).thenReturn(List.of());

    tournamentService.recordMatchResult(match, winner);

    assertThat(winner.getStatus()).isNotEqualTo(TournamentEntryStatus.ELIMINATED);
    assertThat(loser2.getStatus()).isEqualTo(TournamentEntryStatus.ELIMINATED);
    assertThat(loser3.getStatus()).isEqualTo(TournamentEntryStatus.ELIMINATED);
    assertThat(match.getWinner()).isSameAs(winner);
    assertThat(round.getStatus()).isEqualTo(TournamentRoundStatus.COMPLETE);
  }

  @Test
  void recordMatchResult_classicShape_roundStaysOpen() {
    // With another open match in the round, the round must not flip to COMPLETE.
    TournamentEntry winner = entry(1);
    TournamentEntry loser = entry(2);
    TournamentMatch match = new TournamentMatch();
    match.setId(11L);
    match.setEntrant1(winner);
    match.setEntrant2(loser);
    TournamentRound round = new TournamentRound();
    round.setId(21L);
    match.setRound(round);

    when(matchRepository.findByRoundIdAndWinnerIsNull(21L)).thenReturn(List.of(match));

    tournamentService.recordMatchResult(match, winner);

    assertThat(loser.getStatus()).isEqualTo(TournamentEntryStatus.ELIMINATED);
    assertThat(round.getStatus()).isNotEqualTo(TournamentRoundStatus.COMPLETE);
    verify(roundRepository, never()).save(any(TournamentRound.class));
  }

  @Test
  void createTournament_withPayoffFields_persistsThem() {
    // The one-time host-show binding: payoff show/type/rule ride through creation.
    Universe universe = new Universe();
    universe.setId(1L);
    Show payoffShow = new Show();
    payoffShow.setId(3L);
    payoffShow.setUniverse(universe);
    SegmentType payoffType = new SegmentType();
    SegmentRule payoffRule = new SegmentRule();
    Title linked = new Title();
    linked.setId(7L);
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    Tournament created =
        tournamentService.createTournament(
            "Crown Cup",
            "SINGLE_ELIMINATION",
            universe,
            linked,
            LocalDate.of(2026, 6, 1),
            List.of(),
            payoffShow,
            payoffType,
            payoffRule);

    assertThat(created.getPayoffShow()).isSameAs(payoffShow);
    assertThat(created.getPayoffSegmentType()).isSameAs(payoffType);
    assertThat(created.getPayoffSegmentRule()).isSameAs(payoffRule);
    assertThat(created.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);
  }

  @Test
  void createTournament_hostShowFromAnotherUniverse_rejected() {
    // The host show must live in the tournament's universe — validated at creation time.
    Universe universe = new Universe();
    universe.setId(1L);
    Universe otherUniverse = new Universe();
    otherUniverse.setId(2L);
    Show payoffShow = new Show();
    payoffShow.setId(3L);
    payoffShow.setUniverse(otherUniverse);

    Assertions.assertThatThrownBy(
            () ->
                tournamentService.createTournament(
                    "Crown Cup",
                    "SINGLE_ELIMINATION",
                    universe,
                    null,
                    null,
                    List.of(),
                    payoffShow,
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("universe");
  }

  @Test
  void updateTournament_setPayoffFieldsFalse_keepsExistingPayoffFields() {
    // The compat delegate must not clobber the payoff binding — setPayoffFields=false leaves
    // payoffShow/type/rule exactly as they were.
    Tournament existing = new Tournament();
    existing.setId(1L);
    existing.setName("Crown Cup");
    existing.setFormatId("SINGLE_ELIMINATION");
    Show existingShow = new Show();
    existingShow.setId(3L);
    existing.setPayoffShow(existingShow);
    SegmentType existingType = new SegmentType();
    existing.setPayoffSegmentType(existingType);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(tournamentRepository.save(any(Tournament.class))).thenAnswer(inv -> inv.getArgument(0));

    Tournament updated =
        tournamentService.updateTournament(
            1L,
            "Renamed Cup",
            "SINGLE_ELIMINATION",
            null,
            null,
            List.of(),
            null,
            null,
            null,
            false);

    assertThat(updated.getName()).isEqualTo("Renamed Cup");
    assertThat(updated.getPayoffShow()).isSameAs(existingShow);
    assertThat(updated.getPayoffSegmentType()).isSameAs(existingType);
  }

  @Test
  void updateTournament_setPayoffFieldsTrue_replacesAndValidates() {
    // The wizard's save: the new payoff fields land, and a cross-universe host show is rejected
    // at edit time too.
    Universe universe = new Universe();
    universe.setId(1L);
    Tournament existing = new Tournament();
    existing.setId(1L);
    existing.setName("Crown Cup");
    existing.setFormatId("SINGLE_ELIMINATION");
    existing.setUniverse(universe);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(existing));

    Universe otherUniverse = new Universe();
    otherUniverse.setId(2L);
    Show foreignShow = new Show();
    foreignShow.setId(9L);
    foreignShow.setUniverse(otherUniverse);

    Assertions.assertThatThrownBy(
            () ->
                tournamentService.updateTournament(
                    1L,
                    "Crown Cup",
                    "SINGLE_ELIMINATION",
                    null,
                    null,
                    List.of(),
                    foreignShow,
                    null,
                    null,
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("universe");
  }

  private static TournamentEntry entry(long id) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName("Wrestler " + id);
    TournamentEntry e = new TournamentEntry();
    e.setId(id);
    e.setWrestler(w);
    e.setStatus(TournamentEntryStatus.ACTIVE);
    return e;
  }

  private static TournamentMatchParticipant participant(
      TournamentMatch match, TournamentEntry entry, int slot) {
    TournamentMatchParticipant p = new TournamentMatchParticipant();
    p.setMatch(match);
    p.setEntry(entry);
    p.setSlot(slot);
    return p;
  }

  private static Wrestler wrestler(Long id, String name, Gender gender, Long fans) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setActive(true);
    w.setIsPlayer(false);
    w.setGender(gender);
    Universe universe = new Universe();
    universe.setId(1L);
    WrestlerState state = new WrestlerState();
    state.setFans(fans);
    state.setUniverse(universe);
    w.getWrestlerStates().add(state);
    return w;
  }
}
