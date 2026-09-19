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
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.util.Comparator;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books tournament-fed segments onto PLE template shows (ATW-oahn). When a show template pairs a
 * segment type with a tournament (AUTO_ATTACH), the tournament — not the AI — determines that
 * match's participants:
 *
 * <ul>
 *   <li>{@code SCHEDULED} → the tournament is auto-started (auto-seeded from the active roster)
 *   <li>{@code IN_PROGRESS} → the earliest round with open matches fills the segment; the result is
 *       recorded and the tournament advances in lockstep with the card
 *   <li>{@code COMPLETE} → winner showcase (champion vs. runner-up)
 * </ul>
 *
 * <p>Reuses the tournament lifecycle in {@link TournamentService} (auto-seeding, bracket
 * generation, result recording, round advancement) and the ATW RPG match mechanics in {@link
 * NPCSegmentResolutionService}. Returns empty (caller falls back to AI-proposed participants)
 * whenever the tournament cannot supply participants — approval never fails because of a
 * tournament.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentTemplateBookingService {

  private final TournamentService tournamentService;
  private final NPCSegmentResolutionService segmentResolutionService;

  /**
   * Result of a tournament-fed booking attempt. Returned empty when the tournament cannot supply
   * participants — the caller then falls back to the AI-proposed participants.
   */
  public record TournamentBooking(Segment segment, Tournament tournament, String detail) {}

  /**
   * Book the auto-attached segment for a type+tournament AUTO_ATTACH assignment row. The match is
   * created with the tournament's participants and the row's paired rule (if any).
   *
   * @param assignment the type+tournament pairing row
   * @param segmentType the auto-attached segment type (e.g. Abu Dhabi Rumble)
   * @param show the PLE being approved
   * @return the booking, or empty when the tournament cannot supply participants
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<TournamentBooking> bookTournamentFedSegment(
      @NonNull final ShowTemplateSegmentAssignment assignment,
      @NonNull final SegmentType segmentType,
      @NonNull final Show show) {
    Tournament tournament = assignment.getTournament();
    if (tournament == null) {
      return Optional.empty();
    }

    // Ensure the tournament is underway: auto-start SCHEDULED tournaments so their bracket can
    // feed this show's match. seedAuto persists entries without adding them to the in-memory
    // collection startTournament reads — re-fetch with the graph initialized first.
    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !autoStartTournament(tournament, show)) {
      return Optional.empty();
    }

    if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
      return bookCurrentRoundFedSegment(tournament, assignment, segmentType, show);
    }
    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      return bookWinnerShowcaseSegment(tournament, assignment, segmentType, show);
    }

    log.warn(
        "Tournament '{}' is {} — cannot feed participants for show '{}'",
        tournament.getName(),
        tournament.getStatus(),
        show.getName());
    return Optional.empty();
  }

  /**
   * Auto-seed (when unseeded) and start a SCHEDULED tournament, refreshing the in-memory instance
   * with the generated bracket. Returns false (with a warning) when the tournament cannot start —
   * never throws to the approval flow.
   */
  private boolean autoStartTournament(Tournament tournament, Show show) {
    try {
      if (tournament.getEntries().isEmpty()) {
        log.info(
            "Auto-seeding tournament '{}' from the active roster for show '{}'",
            tournament.getName(),
            show.getName());
        tournamentService.seedAuto(tournament, defaultEntrantCount(tournament), universeId(show));
        // seedAuto persists entries without touching the in-memory collection; re-fetch so
        // startTournament's entries check and generateBracket see the seeded roster.
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .ifPresent(refreshed -> copyLifecycleState(tournament, refreshed));
      }
      tournamentService.startTournament(tournament);
      return true;
    } catch (IllegalStateException | IllegalArgumentException e) {
      log.warn(
          "Cannot auto-start tournament '{}' for show '{}': {} — falling back to AI participants",
          tournament.getName(),
          show.getName(),
          e.getMessage());
      return false;
    }
  }

  private Optional<TournamentBooking> bookCurrentRoundFedSegment(
      final Tournament tournament,
      final ShowTemplateSegmentAssignment assignment,
      final SegmentType segmentType,
      final Show show) {
    Optional<TournamentMatch> openMatchOpt = findOpenMatch(tournament);
    if (openMatchOpt.isEmpty()) {
      // Every booked round is decided — advance the tournament once and retry.
      try {
        tournamentService.advanceToNextRound(tournament);
      } catch (IllegalStateException e) {
        log.warn(
            "Cannot advance tournament '{}': {} — falling back to AI participants",
            tournament.getName(),
            e.getMessage());
        return Optional.empty();
      }
      openMatchOpt = findOpenMatch(tournament);
    }
    if (openMatchOpt.isEmpty()) {
      log.warn(
          "Tournament '{}' has no bookable match — falling back to AI participants",
          tournament.getName());
      return Optional.empty();
    }

    TournamentMatch match = openMatchOpt.get();
    String stipulation =
        assignment.getSegmentRule() != null ? assignment.getSegmentRule().getName() : "";
    Segment segment =
        segmentResolutionService.resolveTeamSegment(
            new SegmentTeam(match.getEntrant1().getWrestler()),
            new SegmentTeam(match.getEntrant2().getWrestler()),
            segmentType,
            show,
            stipulation);

    // The match mechanics already decided a winner inside the segment — mirror it into the
    // bracket so the tournament advances in lockstep with the booked segment.
    match.setSegment(segment);
    tournamentService.markRoundInProgress(match.getRound());
    tournamentService.recordMatchResult(match, pickBracketWinner(match, segment));
    log.info(
        "Booked tournament-fed segment for '{}' ({}) on show '{}': {} vs {}",
        tournament.getName(),
        roundNameOf(match),
        show.getName(),
        match.getEntrant1().getWrestler().getName(),
        match.getEntrant2().getWrestler().getName());
    return Optional.of(
        new TournamentBooking(segment, tournament, roundNameOf(match) + " — tournament-fed"));
  }

  /**
   * The earliest round holding an open (winnerless) match — round 1 on a fresh bracket, later
   * rounds as earlier ones complete. Skips rounds already booked onto other shows (every match
   * linked to a segment).
   */
  private Optional<TournamentMatch> findOpenMatch(final Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
        .flatMap(r -> r.getMatches().stream())
        .filter(m -> m.getWinner() == null && m.getSegment() == null)
        .min(Comparator.comparingInt(m -> m.getRound().getRoundNumber()));
  }

  private String roundNameOf(final TournamentMatch match) {
    return match.getRound().getRoundName();
  }

  private Optional<TournamentBooking> bookWinnerShowcaseSegment(
      final Tournament tournament,
      final ShowTemplateSegmentAssignment assignment,
      final SegmentType segmentType,
      final Show show) {
    Optional<TournamentEntry> winnerOpt =
        tournament.getEntries().stream()
            .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
            .findFirst();
    if (winnerOpt.isEmpty()) {
      log.warn(
          "Tournament '{}' is COMPLETE but has no winner entry — falling back to AI participants",
          tournament.getName());
      return Optional.empty();
    }
    TournamentEntry winner = winnerOpt.get();

    // Runner-up = highest-seeded eliminated entrant (the final's loser) — the classic showcase.
    Optional<TournamentEntry> runnerUpOpt =
        tournament.getEntries().stream()
            .filter(e -> e.getStatus() == TournamentEntryStatus.ELIMINATED)
            .min(Comparator.comparingInt(TournamentEntry::getSeed));

    if (runnerUpOpt.isEmpty()) {
      log.warn(
          "Tournament '{}' has no eliminated entrants for a runner-up — falling back to AI",
          tournament.getName());
      return Optional.empty();
    }
    TournamentEntry runnerUp = runnerUpOpt.get();

    String stipulation =
        assignment.getSegmentRule() != null ? assignment.getSegmentRule().getName() : "";
    Segment segment =
        segmentResolutionService.resolveTeamSegment(
            new SegmentTeam(winner.getWrestler()),
            new SegmentTeam(runnerUp.getWrestler()),
            segmentType,
            show,
            stipulation);
    log.info(
        "Booked winner showcase for tournament '{}' on show '{}': {} vs {}",
        tournament.getName(),
        show.getName(),
        winner.getWrestler().getName(),
        runnerUp.getWrestler().getName());
    return Optional.of(
        new TournamentBooking(
            segment, tournament, "Winner showcase — " + winner.getWrestler().getName()));
  }

  /**
   * Derive the bracket winner from the booked segment's actual winners so the tournament state
   * matches the match result shown on the card.
   */
  private TournamentEntry pickBracketWinner(final TournamentMatch match, final Segment segment) {
    boolean entrant1Won =
        segment.getWinners().stream()
            .anyMatch(w -> w.getId().equals(match.getEntrant1().getWrestler().getId()));
    return entrant1Won ? match.getEntrant1() : match.getEntrant2();
  }

  /**
   * Default entrant count for auto-seeding: the format's maximum (a full bracket), or 8 when the
   * format cannot be resolved. Single-elimination expects a power of two; the format validates.
   */
  private int defaultEntrantCount(final Tournament tournament) {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(TournamentFormat::getMaxEntrants)
        .orElse(8);
  }

  /**
   * Copy the refreshed tournament's lifecycle state onto the caller's in-memory instance so the
   * roster and generated bracket are visible without re-fetching at every call site.
   */
  private void copyLifecycleState(Tournament target, Tournament refreshed) {
    target.setEntries(refreshed.getEntries());
    target.setRounds(refreshed.getRounds());
    target.setStatus(refreshed.getStatus());
  }

  private Long universeId(final Show show) {
    return show.getUniverse() != null ? show.getUniverse().getId() : null;
  }
}
