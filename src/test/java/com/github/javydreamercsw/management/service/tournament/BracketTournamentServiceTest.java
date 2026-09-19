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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import java.util.List;
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
  @Mock private com.github.javydreamercsw.management.domain.show.ShowRepository showRepository;
  @Mock private ShowBookingService showBookingService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private TournamentFormat format;

  private TournamentService tournamentService;

  private Tournament tournament;
  private Title womensTitle;

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
            List.of(format));

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setEntries(new java.util.ArrayList<>());
    tournament.setRounds(new java.util.ArrayList<>());
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

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> tournamentService.seedAuto(tournament, 4, 1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not enough eligible wrestlers")
        .hasMessageContaining("at least 4");
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
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        new com.github.javydreamercsw.management.domain.wrestler.WrestlerState();
    state.setFans(fans);
    state.setUniverse(universe);
    w.getWrestlerStates().add(state);
    return w;
  }
}
