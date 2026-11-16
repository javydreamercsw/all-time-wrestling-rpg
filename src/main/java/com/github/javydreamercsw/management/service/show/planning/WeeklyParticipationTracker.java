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

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Per-wrestler participation counts computed locally from the planning context's recent segments
 * (the 21-day cross-show window the context already carries — ATW-eqo0 successor). Pure function:
 * no repository access, so the planner's fairness logic is deterministic and testable.
 *
 * <p>Counts are split match vs promo because the two budgets fill differently and the holder rule
 * ("build toward the cash-in with matches") needs the match side. "Booked this week" is
 * <b>calendar-week</b> semantics (the ISO Monday–Sunday week containing the show date, computed
 * with epoch-day math — no locale-sensitive week fields) and <b>match-only</b>: a rolling 7-day
 * window would catch the SAME show's previous episode every week (a weekly cadence guarantees it)
 * and block everyone permanently — the Continuum 2026-10-02 collapse (1 match of 7 slots); and
 * talking segments are non-physical, so a promo never blocks a match. Multiple weeklies (Timeless +
 * Continuum) share the calendar week, so a wrestler on Monday's card does not headline Friday's.
 */
@Component
public class WeeklyParticipationTracker {

  /**
   * Participation snapshot per wrestler name (context casing preserved; tracker comparisons are
   * case-insensitive via {@link #normalize}).
   */
  public record Participation(
      int matchCount, int promoCount, boolean bookedThisWeek, Instant lastAppearance) {}

  /** All zero/empty snapshot — a wrestler with no recent activity. */
  static Participation idle() {
    return new Participation(0, 0, false, null);
  }

  /**
   * Counts every participant on every recent segment, split by whether the segment is a match
   * (non-promo type). Match segments dated in the SAME calendar week as {@code showDate} (and not
   * after the show date) set the this-week flag.
   *
   * @param recentSegments the context's cross-show recent segments (up to 21 days back)
   * @param showDate the date of the show being planned
   */
  public Map<String, Participation> tally(
      List<ShowPlanningSegmentDTO> recentSegments, Instant showDate) {
    Map<String, Participation> tally = new HashMap<>();
    if (recentSegments == null) {
      return tally;
    }
    int showWeek = isoWeekIndex(showDate);
    for (ShowPlanningSegmentDTO segment : recentSegments) {
      if (segment == null || segment.getParticipants() == null) {
        continue;
      }
      boolean promo = isPromoSegment(segment);
      // "Same week, excluding PLE": PLE matches never set the guard — the PLE is the payoff all
      // weeklies build toward, and blocking on it would starve the week after every PLE.
      boolean thisWeekMatch =
          !promo
              && !segment.isPremiumLiveEvent()
              && segment.getSegmentDate() != null
              && !segment.getSegmentDate().isAfter(showDate)
              && isoWeekIndex(segment.getSegmentDate()) == showWeek;
      for (String name : segment.getParticipants()) {
        if (name == null || name.isBlank()) {
          continue;
        }
        String key = normalize(name);
        Participation prior = tally.getOrDefault(key, idle());
        tally.put(
            key,
            new Participation(
                prior.matchCount() + (promo ? 0 : 1),
                prior.promoCount() + (promo ? 1 : 0),
                prior.bookedThisWeek() || thisWeekMatch,
                laterOf(prior.lastAppearance(), segment.getSegmentDate())));
      }
    }
    return tally;
  }

  /** The under-served roster: zero match appearances first, then promo-deficit ascending. */
  public List<String> underServedFirst(
      Map<String, Participation> tally, List<String> availableNames) {
    record Entry(String name, int matchCount, int promoCount) {}
    return availableNames.stream()
        .map(
            name -> {
              Participation p = tally.getOrDefault(normalize(name), idle());
              return new Entry(name, p.matchCount(), p.promoCount());
            })
        .sorted(
            (a, b) -> {
              int byMatch = Integer.compare(a.matchCount, b.matchCount);
              if (byMatch != 0) {
                return byMatch;
              }
              int byPromo = Integer.compare(a.promoCount, b.promoCount);
              if (byPromo != 0) {
                return byPromo;
              }
              return a.name.compareTo(b.name); // deterministic tiebreak
            })
        .map(Entry::name)
        .toList();
  }

  /** Wrestlers (from {@code availableNames}) who already appeared in the this-week window. */
  public Set<String> bookedThisWeek(Map<String, Participation> tally, List<String> availableNames) {
    Set<String> booked = new HashSet<>();
    for (String name : availableNames) {
      if (tally.getOrDefault(normalize(name), idle()).bookedThisWeek()) {
        booked.add(normalize(name));
      }
    }
    return booked;
  }

  static String normalize(String name) {
    return name.trim().toLowerCase();
  }

  /**
   * Calendar-week index: days since the epoch divided by 7, with the epoch (1970-01-01, a Thursday)
   * aligned so weeks run Monday–Sunday. Same integer ⇒ same ISO week. Epoch-day math keeps this
   * stable across locales and avoids WeekFields (whose week start varies by country).
   */
  static int isoWeekIndex(Instant instant) {
    long epochDay = instant.atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay();
    // 1970-01-01 was a Thursday; shifting by 3 days puts Monday (1970-01-05) on a multiple of 7.
    return (int) Math.floorDiv(epochDay - 3, 7L);
  }

  private static boolean isPromoSegment(ShowPlanningSegmentDTO segment) {
    return segment.getSegmentType() != null
        && segment.getSegmentType().toLowerCase().contains("promo");
  }

  private static Instant laterOf(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    if (b == null || a.isAfter(b)) {
      return a;
    }
    return b;
  }
}
