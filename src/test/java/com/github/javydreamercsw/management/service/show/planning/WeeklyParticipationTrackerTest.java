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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pins for the cross-show weekly participation tracker (ATW-wbch). */
class WeeklyParticipationTrackerTest {

  private static final Instant SHOW_DATE =
      LocalDate.of(2026, 9, 28).atStartOfDay(ZoneOffset.UTC).toInstant();

  private final WeeklyParticipationTracker tracker = new WeeklyParticipationTracker();

  private static ShowPlanningSegmentDTO segment(
      String type, List<String> participants, int daysAgo) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setSegmentType(type);
    dto.setParticipants(participants);
    dto.setSegmentDate(SHOW_DATE.minus(daysAgo, java.time.temporal.ChronoUnit.DAYS));
    return dto;
  }

  @Test
  void tally_countsMatchesAndPromosSeparately() {
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(
            List.of(
                segment("One on One", List.of("Jax Felix", "Rey Jaguar"), 10),
                segment("Promo", List.of("Jax Felix"), 5)),
            SHOW_DATE);

    WeeklyParticipationTracker.Participation jax =
        tally.get(WeeklyParticipationTracker.normalize("Jax Felix"));
    assertEquals(1, jax.matchCount(), "One match appearance");
    assertEquals(1, jax.promoCount(), "One promo appearance");
  }

  @Test
  void tally_thisWeekFlagSpansAllShows() {
    // Production case: Timeless + Continuum both run weekly — a Monday match and a Friday match
    // both fall inside the 7-day window before the planned show.
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(
            List.of(
                segment("One on One", List.of("Rey Jaguar"), 2), // Monday, this week
                segment("One on One", List.of("Rey Jaguar"), 20)), // three weeks ago
            SHOW_DATE);

    WeeklyParticipationTracker.Participation rey =
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar"));
    assertEquals(2, rey.matchCount());
    assertTrue(rey.bookedThisWeek(), "A match 2 days before the show is this week");
  }

  @Test
  void tally_edgeOfWindow_countsAsThisWeek() {
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(segment("One on One", List.of("Kurt Angle"), 7)), SHOW_DATE);

    assertTrue(
        tally.get(WeeklyParticipationTracker.normalize("Kurt Angle")).bookedThisWeek(),
        "Exactly 7 days before counts as this week");
  }

  @Test
  void tally_freeForAllIsAMatchNotAPromo() {
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(
            List.of(segment("Free-for-All", List.of("Rey Jaguar", "Jax Felix", "Dynamite Kid"), 7)),
            SHOW_DATE);

    assertEquals(
        1,
        tally.get(WeeklyParticipationTracker.normalize("Dynamite Kid")).matchCount(),
        "Free-for-All counts as a match");
  }

  @Test
  void underServedFirst_zeroMatchesBeforeLowPromoCounts() {
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(
            List.of(
                segment("Promo", List.of("Shumba"), 3),
                segment("Promo", List.of("Shumba"), 10),
                segment("One on One", List.of("Rey Jaguar"), 10)),
            SHOW_DATE);

    List<String> order =
        tracker.underServedFirst(tally, List.of("Shumba", "Rey Jaguar", "Johnny All Time", "OMZ"));

    // Zero-match wrestlers first (alphabetical tiebreak), then Rey Jaguar (1 match).
    assertEquals(List.of("Johnny All Time", "OMZ", "Shumba", "Rey Jaguar"), order);
  }

  @Test
  void bookedThisWeek_normalizesCasing() {
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(segment("One on One", List.of("rey jaguar"), 3)), SHOW_DATE);

    Set<String> booked = tracker.bookedThisWeek(tally, List.of("Rey Jaguar", "JAX FELIX"));

    assertTrue(booked.contains(WeeklyParticipationTracker.normalize("Rey Jaguar")));
    assertFalse(booked.contains(WeeklyParticipationTracker.normalize("Jax Felix")));
  }
}
