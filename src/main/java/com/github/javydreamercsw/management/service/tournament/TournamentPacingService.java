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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paces a one-time tournament's bracket across the shows leading to the paired PLE (ATW-z963).
 *
 * <p>Semantics: the tournament's non-final matches play out on the weekly shows configured before
 * the PLE, and the PLE itself hosts the payoff exactly once — the tournament final when the linked
 * championship is vacant or no title is linked (the final IS the title match), or champion vs
 * tournament winner when a champion reigns (the final then plays out on the last weekly show like
 * every other round). After the payoff the pairing is consumed so nothing repeats.
 *
 * <p>Bracket rounds generate lazily (round 1 at start; later rounds on advancement), so the pacing
 * math works from the format's match-count estimate instead of scanning rounds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentPacingService {

  /** What the PLE itself hosts for this tournament — the payoff shape. */
  public enum PayoffKind {
    /**
     * The linked championship is vacant (or no title is linked): the PLE hosts the tournament final
     * — a marquee match, and a title match when the title is vacant.
     */
    FINAL_AT_PLE,
    /**
     * A reigning champion holds the linked title: the final happens on the last weekly show before
     * the PLE (like every other round), and the PLE hosts champion vs tournament winner as the
     * title match.
     */
    CHAMPION_SHOWCASE_AT_PLE
  }

  /** The pacing plan for one tournament feeding one PLE. */
  public record PacingPlan(
      /** Whether the PLE hosts the final or the champion showcase. */
      PayoffKind payoffKind,
      /** Weekly (non-PLE) shows before the PLE that can host rounds, soonest first. */
      List<Show> roundSlots,
      /** Matches the full bracket holds (format estimate). */
      int totalMatches,
      /** Matches already booked or decided (segment attached or winner set). */
      int bookedMatches,
      /** Non-payoff matches still awaiting a slot. */
      int remainingNonFinal) {}

  private final TournamentService tournamentService;
  private final ShowService showService;
  private final Clock clock;

  /**
   * The shows configured between {@code from} (inclusive) and the PLE's date (exclusive) that are
   * not PLEs themselves — the slots the tournament's non-final matches pace across. Ordered soonest
   * first. A show counts as a PLE when its type or its template's show type is the PLE category.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Show> weeklyShowSlotsBefore(@NonNull final Show ple, @NonNull final LocalDate from) {
    LocalDate end =
        ple.getShowDate() != null
            ? ple.getShowDate()
            : LocalDate.now(clock).plusWeeks(4); // unconfigured date: assume a month out
    return showService.getShowsByDateRange(from, end).stream()
        .filter(s -> !s.isPremiumLiveEvent() && s.getType().getCategory() != ShowCategory.PLE)
        .filter(s -> s.getShowDate().isBefore(end))
        .toList();
  }

  /**
   * Build the pacing plan for a tournament feeding the given PLE.
   *
   * @param tournament the paired tournament (any lifecycle state)
   * @param ple the PLE being approved (or the next upcoming PLE for weekly-show pacing)
   * @return the plan, or empty when the format cannot be resolved
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public PacingPlan planFor(@NonNull final Tournament tournament, @NonNull final Show ple) {
    int total =
        tournamentService
            .findFormat(tournament.getFormatId())
            .map(fmt -> fmt.estimateTotalMatches(tournament))
            .orElse(0);
    PayoffKind payoffKind = payoffKindOf(tournament);
    int booked = countBooked(tournament);
    int remaining = Math.max(0, total - booked);
    // The payoff match itself plays at the PLE; everything else paces across the weekly slots.
    // With a reigning champion the bracket's own final also plays on a weekly show, so ALL
    // remaining matches are round matches.
    int remainingNonFinal =
        payoffKind == PayoffKind.FINAL_AT_PLE ? Math.max(0, remaining - 1) : remaining;
    return new PacingPlan(
        payoffKind,
        weeklyShowSlotsBefore(ple, LocalDate.now(clock)),
        total,
        booked,
        remainingNonFinal);
  }

  /**
   * The payoff shape: FINAL_AT_PLE when no title is linked or the linked championship is vacant;
   * CHAMPION_SHOWCASE_AT_PLE when a champion reigns.
   */
  public PayoffKind payoffKindOf(@NonNull final Tournament tournament) {
    return tournament.getLinkedTitle() == null
            || tournamentService.isTitleVacant(tournament.getLinkedTitle())
        ? PayoffKind.FINAL_AT_PLE
        : PayoffKind.CHAMPION_SHOWCASE_AT_PLE;
  }

  /** Matches already booked (segment attached) or decided (winner recorded). */
  private int countBooked(Tournament tournament) {
    return (int)
        tournament.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .filter(m -> m.getSegment() != null || m.getWinner() != null)
            .count();
  }
}
