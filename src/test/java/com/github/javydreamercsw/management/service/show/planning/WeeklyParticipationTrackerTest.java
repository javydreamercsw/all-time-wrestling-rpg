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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pins for the cross-show weekly participation tracker (ATW-wbch). */
class WeeklyParticipationTrackerTest {

  // Friday 2026-10-02 — the Continuum date from the field bug (calendar-week anchor).
  private static final Instant SHOW_DATE =
      LocalDate.of(2026, 10, 2).atStartOfDay(ZoneOffset.UTC).toInstant();

  private final WeeklyParticipationTracker tracker = new WeeklyParticipationTracker();

  private static ShowPlanningSegmentDTO segment(
      String type, List<String> participants, int daysAgo) {
    ShowPlanningSegmentDTO dto = new ShowPlanningSegmentDTO();
    dto.setSegmentType(type);
    dto.setParticipants(participants);
    dto.setSegmentDate(SHOW_DATE.minus(daysAgo, ChronoUnit.DAYS));
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
    // Production case: Timeless + Continuum both run weekly — a Wednesday match and a Friday
    // match in the SAME calendar week both set the guard.
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(
            List.of(
                segment("One on One", List.of("Rey Jaguar"), 2), // Wednesday, same calendar week
                segment("One on One", List.of("Rey Jaguar"), 20)), // two+ weeks ago
            SHOW_DATE);

    WeeklyParticipationTracker.Participation rey =
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar"));
    assertEquals(2, rey.matchCount());
    assertTrue(rey.bookedThisWeek(), "A match 2 days before the show in the same week blocks");
  }

  @Test
  void tally_lastWeeksEpisode_doesNotBlockThisWeek() {
    // THE CONTINUUM COLLAPSE (2026-11-11 field bug): a rolling 7-day window caught the SAME
    // show's previous episode every week — a weekly cadence guarantees it — blocking the whole
    // roster permanently (1 match of 7 slots). Last Friday's episode is LAST week: it must not
    // block this Friday's card. (09-25 is exactly one week back from 2026-10-02.)
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(segment("One on One", List.of("Rey Jaguar"), 7)), SHOW_DATE);

    assertFalse(
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar")).bookedThisWeek(),
        "Last week's match must not block this week's card");
  }

  @Test
  void tally_pleMatchDoesNotBlockTheWeek() {
    // "Same week (excluding PLE)": Era of Champions ran the Sunday before the planned Friday.
    // Its matches are the payoff the weeklies build toward — they never set the guard.
    ShowPlanningSegmentDTO pleMatch = segment("One on One", List.of("Taya Valkyrie"), 5);
    pleMatch.setPremiumLiveEvent(true);
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(pleMatch), SHOW_DATE);

    assertFalse(
        tally.get(WeeklyParticipationTracker.normalize("Taya Valkyrie")).bookedThisWeek(),
        "A PLE match must not block this week's card");
  }

  @Test
  void tally_promoDoesNotBlockMatches() {
    // Talking segments are non-physical: a Monday promo doesn't block a Friday match.
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(segment("Promo", List.of("Mukundi Shumba"), 2)), SHOW_DATE);

    WeeklyParticipationTracker.Participation shumba =
        tally.get(WeeklyParticipationTracker.normalize("Mukundi Shumba"));
    assertEquals(1, shumba.promoCount());
    assertFalse(
        shumba.bookedThisWeek(), "A promo never blocks a match slot (it still counts for promos)");
  }

  @Test
  void tally_sameWeekEarlierDay_countsAsThisWeek() {
    // A Tuesday match before a Friday show, same calendar week: the guard holds.
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(segment("One on One", List.of("Kurt Angle"), 3)), SHOW_DATE);

    assertTrue(tally.get(WeeklyParticipationTracker.normalize("Kurt Angle")).bookedThisWeek());
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
        tracker.tally(List.of(segment("One on One", List.of("rey jaguar"), 2)), SHOW_DATE);

    Set<String> booked = tracker.bookedThisWeek(tally, List.of("Rey Jaguar", "JAX FELIX"));

    assertTrue(booked.contains(WeeklyParticipationTracker.normalize("Rey Jaguar")));
    assertFalse(booked.contains(WeeklyParticipationTracker.normalize("Jax Felix")));
  }

  @Test
  void tally_nullSegments_returnsEmptyTally() {
    assertTrue(tracker.tally(null, SHOW_DATE).isEmpty(), "No recent segments, no tally");
  }

  @Test
  void tally_malformedEntries_areSkipped() {
    // Null segment entries and entries without participants never crash the tally.
    ShowPlanningSegmentDTO noParticipants = segment("One on One", List.of("A"), 2);
    noParticipants.setParticipants(null);
    ShowPlanningSegmentDTO blankNames =
        segment("One on One", java.util.Arrays.asList("A", " ", null), 2);

    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(java.util.Arrays.asList(null, noParticipants, blankNames), SHOW_DATE);

    // Only the non-blank name lands; the PLE flag is unset on these hand-built segments.
    WeeklyParticipationTracker.Participation a =
        tally.get(WeeklyParticipationTracker.normalize("A"));
    assertEquals(1, a.matchCount(), "The one real participant counts exactly one match");
  }

  @Test
  void tally_thisWeekMatch_datedAfterShow_doesNotSetGuard() {
    // A match dated AFTER the show being planned is future data — it must not set the
    // this-week guard (the guard exists to stop re-booking before the fact).
    ShowPlanningSegmentDTO future =
        segment("One on One", List.of("Rey Jaguar"), -3); // 3 days ahead
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(future), SHOW_DATE);

    assertFalse(
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar")).bookedThisWeek(),
        "A future-dated match never sets the this-week guard");
  }

  @Test
  void tally_pleMatchSameWeek_doesNotSetGuard() {
    // PLE-exempt semantics: a PLE match in the same calendar week is the payoff — the weeklies
    // build toward it, so it must not block this week's card.
    ShowPlanningSegmentDTO ple = segment("One on One", List.of("Rey Jaguar"), 2);
    ple.setPremiumLiveEvent(true);
    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(ple), SHOW_DATE);

    WeeklyParticipationTracker.Participation rey =
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar"));
    assertTrue(rey.matchCount() == 1, "The PLE match still counts as an appearance");
    assertFalse(rey.bookedThisWeek(), "A PLE match never sets the this-week guard (PLE-exempt)");
  }

  @Test
  void tally_nullSegmentDate_isCountedButNeverSetsGuard() {
    // Hand-built segments without a date can't be week-anchored — they count, but guard-free.
    ShowPlanningSegmentDTO undated = new ShowPlanningSegmentDTO();
    undated.setSegmentType("One on One");
    undated.setParticipants(List.of("Rey Jaguar"));

    Map<String, WeeklyParticipationTracker.Participation> tally =
        tracker.tally(List.of(undated), SHOW_DATE);

    WeeklyParticipationTracker.Participation rey =
        tally.get(WeeklyParticipationTracker.normalize("Rey Jaguar"));
    assertEquals(1, rey.matchCount());
    assertFalse(rey.bookedThisWeek(), "No date, no week guard");
  }
}
