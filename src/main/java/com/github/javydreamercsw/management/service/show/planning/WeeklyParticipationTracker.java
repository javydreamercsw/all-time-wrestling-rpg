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
import java.time.temporal.ChronoUnit;
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
 * ("build toward the cash-in with matches") needs the match side. "Booked this week" spans ALL
 * shows in the seven days before the show date — production runs multiple weeklies (Timeless +
 * Continuum), so a wrestler on Monday's card should not headline Tuesday's.
 */
@Component
public class WeeklyParticipationTracker {

  /** The local window that defines "this week": the 7 days before the show date. */
  static final int THIS_WEEK_DAYS = 7;

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
   * (non-promo type). Segments dated within {@code showDate - 7d} set the this-week flag.
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
    Instant weekStart = showDate.minus(THIS_WEEK_DAYS, ChronoUnit.DAYS);
    for (ShowPlanningSegmentDTO segment : recentSegments) {
      if (segment == null || segment.getParticipants() == null) {
        continue;
      }
      boolean promo = isPromoSegment(segment);
      boolean thisWeek =
          segment.getSegmentDate() != null
              && !segment.getSegmentDate().isBefore(weekStart)
              && segment.getSegmentDate().isBefore(showDate.plus(1, ChronoUnit.DAYS));
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
                prior.bookedThisWeek() || thisWeek,
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
