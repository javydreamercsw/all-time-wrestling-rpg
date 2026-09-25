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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pacing-math unit tests (ATW-z963.1): slots, payoff kind, and distribution. */
@ExtendWith(MockitoExtension.class)
class TournamentPacingServiceTest {

  @Mock private TournamentService tournamentService;
  @Mock private ShowService showService;
  @Mock private TournamentFormat format;

  private TournamentPacingService service;
  private Tournament tournament;
  private Show ple;
  private ShowType pleType;
  private ShowType weeklyType;

  private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);
  private final Clock fixedClock =
      Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    service = new TournamentPacingService(tournamentService, showService, fixedClock);

    tournament = new Tournament();
    tournament.setId(5L);
    tournament.setName("Crown Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.SCHEDULED);

    pleType = showType("PLE", ShowCategory.PLE);
    weeklyType = showType("Weekly", ShowCategory.OTHER);
    ple = show(1L, "Big PLE", LocalDate.of(2026, 6, 22), pleType);
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
  }

  @Test
  @DisplayName("Vacant linked title → FINAL_AT_PLE")
  void payoffKind_vacantTitle() {
    Title title = new Title();
    title.setId(7L);
    tournament.setLinkedTitle(title);
    lenient().when(tournamentService.isTitleVacant(title)).thenReturn(true);
    assertEquals(TournamentPacingService.PayoffKind.FINAL_AT_PLE, service.payoffKindOf(tournament));
  }

  @Test
  @DisplayName("No linked title → FINAL_AT_PLE")
  void payoffKind_noTitle() {
    assertEquals(TournamentPacingService.PayoffKind.FINAL_AT_PLE, service.payoffKindOf(tournament));
  }

  @Test
  @DisplayName("Reigning champion → CHAMPION_SHOWCASE_AT_PLE")
  void payoffKind_reigningChampion() {
    Title title = new Title();
    title.setId(7L);
    tournament.setLinkedTitle(title);
    lenient().when(tournamentService.isTitleVacant(title)).thenReturn(false);
    assertEquals(
        TournamentPacingService.PayoffKind.CHAMPION_SHOWCASE_AT_PLE,
        service.payoffKindOf(tournament));
  }

  @Test
  @DisplayName("Slots exclude PLEs and anything on/after the PLE date")
  void slots_excludePlesAndLater() {
    Show week1 = show(2L, "Week 1", LocalDate.of(2026, 6, 8), weeklyType);
    Show otherPle = show(3L, "Other PLE", LocalDate.of(2026, 6, 10), pleType); // PLE category
    Show week2 = show(4L, "Week 2", LocalDate.of(2026, 6, 15), weeklyType);
    Show onPleDay = show(5L, "Same-day", LocalDate.of(2026, 6, 22), weeklyType);
    Show after = show(6L, "After", LocalDate.of(2026, 6, 29), weeklyType);
    lenient()
        .when(showService.getShowsByDateRange(TODAY, LocalDate.of(2026, 6, 22)))
        .thenReturn(List.of(week1, otherPle, week2, onPleDay, after));

    List<Show> slots = service.weeklyShowSlotsBefore(ple, TODAY);

    // otherPle excluded (PLE category), onPleDay/after excluded (not before the PLE date).
    assertEquals(List.of(week1, week2), slots);
  }

  @Test
  @DisplayName("FINAL_AT_PLE: 8-entrant bracket paced onto 2 weekly slots leaves 4+1")
  void plan_finalAtPle_8entrants() {
    for (int i = 1; i < 8 + 1; i++) {
      tournament.getEntries().add(TournamentEntry.builder().seed(i).build());
    }
    // 7 total matches, none booked. Remaining = 7, non-final = 6.
    lenient().when(format.estimateTotalMatches(tournament)).thenReturn(7);
    Show week1 = show(2L, "Week 1", LocalDate.of(2026, 6, 8), weeklyType);
    Show week2 = show(3L, "Week 2", LocalDate.of(2026, 6, 15), weeklyType);
    lenient()
        .when(showService.getShowsByDateRange(TODAY, LocalDate.of(2026, 6, 22)))
        .thenReturn(List.of(week1, week2));

    TournamentPacingService.PacingPlan plan = service.planFor(tournament, ple);

    assertEquals(TournamentPacingService.PayoffKind.FINAL_AT_PLE, plan.payoffKind());
    assertEquals(7, plan.totalMatches());
    assertEquals(0, plan.bookedMatches());
    assertEquals(6, plan.remainingNonFinal());
    assertEquals(List.of(week1, week2), plan.roundSlots());
  }

  @Test
  @DisplayName("CHAMPION_SHOWCASE: the bracket final also counts as a round match")
  void plan_championShowcase_allRemainingAreRounds() {
    Title title = new Title();
    title.setId(7L);
    tournament.setLinkedTitle(title);
    lenient().when(tournamentService.isTitleVacant(title)).thenReturn(false);
    tournament.getEntries().add(TournamentEntry.builder().seed(1).build());
    tournament.getEntries().add(TournamentEntry.builder().seed(2).build());
    lenient().when(format.estimateTotalMatches(tournament)).thenReturn(3);
    lenient().when(showService.getShowsByDateRange(any(), any())).thenReturn(List.of());

    TournamentPacingService.PacingPlan plan = service.planFor(tournament, ple);

    assertEquals(TournamentPacingService.PayoffKind.CHAMPION_SHOWCASE_AT_PLE, plan.payoffKind());
    // All 3 remaining matches (including the bracket final) pace onto weekly shows.
    assertEquals(3, plan.remainingNonFinal());
  }

  @Test
  @DisplayName("Booked matches reduce the remaining count")
  void plan_countsBookedMatches() {
    tournament.getEntries().add(TournamentEntry.builder().seed(1).build());
    tournament.getEntries().add(TournamentEntry.builder().seed(2).build());
    // estimateTotalMatches uses entrants; here stub 3 with 1 booked → remaining 2, non-final 2.
    lenient().when(format.estimateTotalMatches(tournament)).thenReturn(3);
    lenient().when(showService.getShowsByDateRange(any(), any())).thenReturn(List.of());
    TournamentRound round = TournamentRound.builder().roundNumber(1).roundName("Round 1").build();
    TournamentMatch booked = TournamentMatch.builder().build();
    booked.setSegment(new Segment());
    round.getMatches().add(booked);
    tournament.getRounds().add(round);

    TournamentPacingService.PacingPlan plan = service.planFor(tournament, ple);

    assertEquals(1, plan.bookedMatches());
    // 2 remaining, minus the payoff match at the PLE → 1 round match left to pace.
    assertEquals(1, plan.remainingNonFinal());
  }

  private static Show show(Long id, String name, LocalDate date, ShowType type) {
    Show s = new Show();
    s.setId(id);
    s.setName(name);
    s.setShowDate(date);
    s.setType(type);
    return s;
  }

  private static ShowType showType(String name, ShowCategory category) {
    ShowType t = new ShowType();
    t.setName(name);
    t.setCategory(category);
    return t;
  }
}
