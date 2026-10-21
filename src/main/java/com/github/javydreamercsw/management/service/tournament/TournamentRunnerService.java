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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a tournament standalone — no show template, no PLE, no AI provider. The booker-driven path
 * for a fresh install: "Run Round" plays the current round's open matches end-to-end, "Run
 * Tournament" plays out the whole bracket, and both funnel through the same booking core ({@link
 * BracketMatchResolver}) and lifecycle ({@link TournamentService}) the automated template path
 * uses, so the results carry full RPG consequences (health, stamina, fans, injuries) by
 * construction.
 *
 * <p>Every persisted match needs a host show ({@code Segment.show} is NOT NULL): when the booker
 * picks none, one is auto-created ("<tournament> Night"). No AI is touched anywhere on this path —
 * winners come from the RPG probability engine inside {@link
 * com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService}, narration is
 * left to the existing Narrate dialog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentRunnerService {

  private final TournamentService tournamentService;
  private final BracketMatchResolver resolver;
  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final UniverseContextService universeContextService;
  private final TitleOpportunityService titleOpportunityService;

  /** Result of running one round. */
  public record RoundRunReport(
      Show hostShow, TournamentRound round, int matchesPlayed, boolean tournamentComplete) {}

  /** Result of running the whole remaining bracket. */
  public record TournamentRunReport(
      Show hostShow, int matchesPlayed, boolean tournamentComplete, TournamentEntry winner) {}

  /**
   * Play every open match of the earliest non-complete round.
   *
   * @param tournament an IN_PROGRESS tournament (Start Tournament remains the explicit seeding
   *     step)
   * @param requestedShowId the show to book the matches on, or null to auto-create a host show
   * @return what was played and whether the run completed the tournament
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public RoundRunReport runRound(
      @NonNull final Tournament tournament, @Nullable final Long requestedShowId) {
    // Re-fetch inside this transaction: the caller (the detail view) passes a detached instance,
    // and match resolution reads lazy wrestler state that a detached graph cannot initialize.
    Tournament managed = requireManaged(tournament);
    Show hostShow = resolveHostShow(managed, requestedShowId);
    TournamentRound round = currentRunnableRound(managed);
    int played = playOpenMatches(managed, round, hostShow);
    boolean complete = advanceIfRoundDecided(managed);
    if (complete) {
      grantBriefcaseOnRunnerCompletion(managed);
    }
    log.info(
        "Ran round '{}' of tournament '{}': {} matches on show '{}'",
        round.getRoundName(),
        managed.getName(),
        played,
        hostShow.getName());
    return new RoundRunReport(hostShow, round, played, complete);
  }

  /**
   * Play out the entire remaining bracket on one host show. Loop safety: the iteration cap is the
   * format's total-match estimate (plus a generous margin), so a pathological state — a match that
   * never resolves, a format that never completes — ends the loop with a warning instead of hanging
   * the request.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentRunReport runTournament(
      @NonNull final Tournament tournament, @Nullable final Long requestedShowId) {
    Tournament managed = requireManaged(tournament);
    Show hostShow = resolveHostShow(managed, requestedShowId);
    int cap = safetyCap(managed);
    int played = 0;
    int iterations = 0;
    boolean complete = false;
    while (played < cap && iterations < cap && !complete) {
      iterations++;
      Optional<TournamentMatch> open = resolver.nextBookableMatch(managed);
      if (open.isEmpty()) {
        complete = advanceIfRoundDecided(managed);
        if (!complete) {
          // Nothing playable and the format says more matches should exist — stop rather than spin.
          log.warn(
              "Tournament '{}' stopped early: no runnable matches remain before completion",
              managed.getName());
          break;
        }
        continue;
      }
      TournamentRound round = currentRunnableRound(managed);
      played += playOpenMatches(managed, round, hostShow);
      complete = advanceIfRoundDecided(managed);
    }
    if ((played >= cap || iterations >= cap) && !complete) {
      log.warn(
          "Tournament '{}' hit the safety cap ({} matches / {} iterations) — stopping",
          managed.getName(),
          played,
          iterations);
    }
    if (complete) {
      grantBriefcaseOnRunnerCompletion(managed);
    }
    TournamentEntry winner = winnerOf(managed);
    log.info(
        "Ran tournament '{}': {} matches on show '{}', complete={}",
        managed.getName(),
        played,
        hostShow.getName(),
        complete);
    return new TournamentRunReport(hostShow, played, complete, winner);
  }

  /**
   * Resolve the caller's (possibly detached) tournament instance to a managed one and enforce the
   * IN_PROGRESS preconditions on it. The unit tests exercise this path with in-memory entities
   * whose id is null — those pass through as-is (no repository round-trip is possible).
   */
  private Tournament requireManaged(Tournament tournament) {
    if (tournament.getId() == null) {
      requireInProgress(tournament);
      return tournament;
    }
    Tournament managed =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Tournament not found: " + tournament.getId()));
    requireInProgress(managed);
    return managed;
  }

  /**
   * Create the auto host show for a tournament ("&lt;tournament&gt; Night"). Public because the
   * degraded Book-Round dialog uses it too — it books unresolved segments rather than running them.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Show autoCreateHostShow(@NonNull final Tournament tournament) {
    return doAutoCreateHostShow(tournament);
  }

  /** Whether any open (winnerless, unbooked) match remains — gates the Run buttons. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean hasRunnableWork(@NonNull final Tournament tournament) {
    return resolver.nextBookableMatch(tournament).isPresent();
  }

  /** How many open matches remain — the Run Tournament confirm-dialog copy. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long remainingMatches(@NonNull final Tournament tournament) {
    return tournament.getRounds().stream()
        .flatMap(r -> r.getMatches().stream())
        .filter(m -> m.getWinner() == null && m.getSegment() == null)
        .count();
  }

  // ── Internals ───────────────────────────────────────────────────────────────

  private void requireInProgress(Tournament tournament) {
    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Tournament '"
              + tournament.getName()
              + "' is "
              + tournament.getStatus()
              + " — start it before running it");
    }
  }

  /**
   * The requested show, or a fresh "<tournament> Night" show when none was picked (the
   * fresh-install case: no shows exist to pick from, and the button path verifies that before
   * passing null).
   */
  private Show resolveHostShow(Tournament tournament, @Nullable Long requestedShowId) {
    if (requestedShowId != null) {
      return showService
          .getShowById(requestedShowId)
          .orElseThrow(() -> new IllegalArgumentException("Show not found: " + requestedShowId));
    }
    return doAutoCreateHostShow(tournament);
  }

  private Show doAutoCreateHostShow(Tournament tournament) {
    List<ShowType> showTypes = showTypeService.findAll();
    if (showTypes.isEmpty()) {
      throw new IllegalStateException(
          "No show types are configured — seed the catalog before running tournaments");
    }
    String name =
        tournament.getName() == null || tournament.getName().isBlank()
            ? "Tournament Night"
            : tournament.getName().trim() + " Night";
    Show show =
        showService.createShow(
            name,
            "Auto-created host show for tournament '" + tournament.getName() + "'.",
            showTypes.get(0).getId(),
            LocalDate.now(),
            null,
            null,
            universeContextService.getCurrentUniverseId(),
            null,
            null,
            null);
    log.info("Auto-created host show '{}' for tournament '{}'", name, tournament.getName());
    return show;
  }

  /** Earliest round by round number holding at least one open match. */
  private TournamentRound currentRunnableRound(Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(r -> r.getMatches().stream().anyMatch(m -> m.getWinner() == null))
        .min(Comparator.comparingInt(TournamentRound::getRoundNumber))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Tournament '" + tournament.getName() + "' has no open matches to run"));
  }

  /** Resolve, record, and (first match only) activate every open match of the round. */
  private int playOpenMatches(Tournament tournament, TournamentRound round, Show hostShow) {
    SegmentType roundType =
        resolver
            .roundSegmentTypeOf(tournament)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cannot run tournament '"
                            + tournament.getName()
                            + "': its round segment type is missing from the catalog"));
    List<TournamentMatch> open =
        round.getMatches().stream()
            .filter(m -> m.getWinner() == null && m.getSegment() == null)
            .toList();
    for (TournamentMatch match : open) {
      String stipulation =
          tournamentService.resolveRoundStipulation(
              tournament, round, null, resolver.defaultRoundRuleOf(tournament));
      boolean titleOnTheLine =
          tournament.getLinkedTitle() != null && resolver.isBracketFinal(tournament, match);
      Segment segment =
          resolver.resolveSegment(
              match, tournament, roundType, hostShow, stipulation, titleOnTheLine);
      match.setSegment(segment);
      tournamentService.markRoundInProgress(round);
      tournamentService.recordMatchResult(match, resolver.pickBracketWinner(match, segment));
      log.info(
          "Ran tournament match for '{}' ({}): {} vs {} — winner decided",
          tournament.getName(),
          round.getRoundName(),
          match.getEntrant1().entrantName(),
          match.getEntrant2().entrantName());
    }
    return open.size();
  }

  /**
   * Advance the bracket when the runnable round is fully decided — the single completion choke
   * point ({@code markWinner} stamps the status, winner entry, trophy reign, and manual-completion
   * briefcase grant).
   */
  private boolean advanceIfRoundDecided(Tournament tournament) {
    boolean anyOpen =
        tournament.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .anyMatch(m -> m.getWinner() == null && m.getSegment() == null);
    if (anyOpen) {
      return false;
    }
    tournamentService.advanceToNextRound(tournament);
    return isComplete(tournament);
  }

  private boolean isComplete(Tournament tournament) {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(fmt -> fmt.isComplete(tournament))
        .orElse(false);
  }

  private int safetyCap(Tournament tournament) {
    int estimate =
        tournamentService
            .findFormat(tournament.getFormatId())
            .map(fmt -> fmt.estimateTotalMatches(tournament))
            .orElse(64);
    return estimate + 1;
  }

  /**
   * Briefcase grant for the standalone path (ATW-8p72 contract): {@code
   * grantBriefcaseOnManualCompletion} fires only when the final's segment was never booked, and
   * this runner books every segment — so without this call a runner-completed briefcase tournament
   * would wait for the host show's adjudication. The grant is idempotent (one opportunity per
   * awarding tournament), so a later adjudication of the final's segment skips cleanly.
   */
  private void grantBriefcaseOnRunnerCompletion(Tournament tournament) {
    if (!tournament.isBriefcaseDeciding() || tournament.getStatus() != TournamentStatus.COMPLETE) {
      return;
    }
    tournament.getEntries().stream()
        .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
        .map(TournamentEntry::getWrestler)
        .findFirst()
        .ifPresent(winner -> titleOpportunityService.grantFromTournament(tournament, winner));
  }

  private TournamentEntry winnerOf(Tournament tournament) {
    return tournament.getEntries().stream()
        .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
        .findFirst()
        .orElse(null);
  }
}
