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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books tournament-fed segments (ATW-oahn) and paces a one-time tournament across the shows leading
 * to its PLE (ATW-z963).
 *
 * <p>Semantics for a one-time tournament paired with a PLE template:
 *
 * <ul>
 *   <li><b>Weekly shows</b> before the PLE host the non-final matches, distributed across the
 *       configured slots (ceil spread, one round at a time) so the bracket finishes as the PLE
 *       arrives
 *   <li><b>The PLE</b> hosts the payoff exactly once — the tournament final when the linked
 *       championship is vacant or no title is linked (the final is the title match), or champion vs
 *       tournament winner when a champion reigns (the final then plays on the last weekly show like
 *       every other round)
 *   <li><b>After the payoff</b> the pairing is consumed: the assignment row detaches from the
 *       tournament so later PLEs fall back to AI-proposed participants — nothing repeats
 * </ul>
 *
 * <p>Reuses the tournament lifecycle in {@link TournamentService} (auto-seeding, bracket
 * generation, result recording, round advancement), the pacing math in {@link
 * TournamentPacingService}, and the ATW RPG match mechanics in {@link NPCSegmentResolutionService}.
 * Returns empty (caller falls back to AI-proposed participants) whenever the tournament cannot
 * supply participants — approval never fails because of a tournament.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentTemplateBookingService {

  private final TournamentService tournamentService;
  private final NPCSegmentResolutionService segmentResolutionService;
  private final TournamentPacingService pacingService;
  private final SegmentTypeService segmentTypeService;
  private final ShowService showService;
  private final TournamentRepository tournamentRepository;

  /**
   * Result of a tournament-fed booking attempt. {@code titleMatch}/{@code title} mark the payoff:
   * the approval flow must set them on the segment (not the AI proposal's values) so the
   * adjudication path awards or defends the championship.
   */
  public record TournamentBooking(
      Segment segment, Tournament tournament, String detail, boolean titleMatch, Title title) {}

  /**
   * Book the payoff for a type+tournament AUTO_ATTACH assignment row on a PLE: the tournament final
   * (vacant or no championship) or champion vs tournament winner (champion reigns). Booked exactly
   * once — afterwards the pairing is consumed.
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
    if (tournament.getPayoffShow() != null) {
      // One-time show-attached tournament (ATW-xbn4): the host-show path owns all of its
      // booking — a template pairing must not double-book it.
      log.info(
          "Tournament '{}' is attached to show '{}' — its template pairing stays idle",
          tournament.getName(),
          tournament.getPayoffShow().getName());
      return Optional.empty();
    }

    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      // The bracket finished before the PLE (rounds paced onto weekly shows, or booked
      // manually). With a reigning champion the payoff is champion vs tournament winner;
      // without one the final itself already played — nothing left to book either way
      // after a showcase.
      Title linkedTitle = tournament.getLinkedTitle();
      if (linkedTitle != null && !tournamentService.isTitleVacant(linkedTitle)) {
        return bookChampionShowcase(
            tournament,
            segmentType,
            assignment.getSegmentRule(),
            show,
            () ->
                consumePairing(
                    assignment, tournament, show, "champion showcase booked at the PLE"));
      }
      consumePairing(assignment, tournament, show, "final already played before the PLE");
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
      return bookPleMatchForInProgressTournament(tournament, assignment, segmentType, show);
    }

    log.warn(
        "Tournament '{}' is {} — cannot feed participants for show '{}'",
        tournament.getName(),
        tournament.getStatus(),
        show.getName());
    return Optional.empty();
  }

  /**
   * Book a paced tournament round onto a weekly show (ATW-z963): the tournament's next open
   * match(es) book as extra card slots the AI did not propose. The PLE's payoff is never booked
   * here — it belongs to {@link #bookTournamentFedSegment} at the PLE itself. Pacing auto-starts a
   * SCHEDULED tournament at the first weekly slot.
   *
   * @param assignment a tournament assignment row on the weekly show's template
   * @param show the weekly show being approved
   * @return the bookings (possibly several when too few slots remain), or empty when no round is
   *     due on this show or the tournament cannot supply a match
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentBooking> bookWeeklyRounds(
      @NonNull final ShowTemplateSegmentAssignment assignment, @NonNull final Show show) {
    Tournament tournament = assignment.getTournament();
    if (tournament == null || tournament.getStatus() == TournamentStatus.COMPLETE) {
      return List.of();
    }
    if (tournament.getPayoffShow() != null) {
      // One-time show-attached tournament (ATW-xbn4): the host-show path owns all of its
      // booking — a template pairing must not double-book it.
      return List.of();
    }

    // Auto-start BEFORE pacing math: an unseeded bracket estimates zero matches, which would
    // read as "only the payoff remains" and strand the tournament unstarted.
    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !autoStartTournament(tournament, show)) {
      return List.of();
    }

    // How many matches this show should host: ceil(remaining non-payoff matches / weekly slots
    // left including this one). Without a target PLE there is no pacing target — book one open
    // match per week (legacy free-running behavior). The denominator counts every weekly slot
    // from this show (inclusive) to the PLE — counting only same-date shows would cram the
    // whole remainder onto the first slot.
    int matchesThisShow = 1;
    Optional<Show> targetPle = findTargetPle(tournament, show);
    if (targetPle.isPresent()) {
      TournamentPacingService.PacingPlan plan = pacingService.planFor(tournament, targetPle.get());
      long slots = pacingService.weeklyShowSlotsBefore(targetPle.get(), show.getShowDate()).size();
      if (plan.remainingNonFinal() == 0) {
        // Only the payoff remains — it plays at the PLE; nothing to book on weekly shows.
        return List.of();
      }
      matchesThisShow = (int) Math.ceil((double) plan.remainingNonFinal() / Math.max(1, slots));
    }

    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      return List.of();
    }

    // Weekly rounds use the standard one-on-one type — event-only types stay exclusive to the
    // PLE payoff.
    Optional<SegmentType> singlesType =
        segmentTypeService.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode());
    if (singlesType.isEmpty()) {
      log.warn(
          "Cannot book tournament rounds on weekly show '{}': the standard one-on-one segment"
              + " type is missing",
          show.getName());
      return List.of();
    }

    ArrayList<TournamentBooking> bookings = new ArrayList<>();
    for (int i = 0; i < matchesThisShow; i++) {
      Optional<TournamentBooking> booking =
          bookCurrentRoundFedSegment(tournament, assignment, singlesType.get(), show);
      if (booking.isEmpty()) {
        break;
      }
      bookings.add(
          new TournamentBooking(
              booking.get().segment(), tournament, booking.get().detail(), false, null));
    }
    return bookings;
  }

  // ── Show-attached one-time tournaments (ATW-xbn4) ──────────────────────────

  /**
   * One-time show-attached tournaments: books the payoff when {@code show} hosts one, and paced
   * round matches when {@code show} is a non-PLE show before a future host show — no template row
   * involved. Never blocks approval: empty results simply add nothing.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentBooking> bookShowAttachedTournamentSegments(@NonNull final Show show) {
    List<TournamentBooking> bookings = new ArrayList<>();

    // 1. Payoff: tournaments whose payoffShow is this show.
    for (Tournament tournament : tournamentRepository.findByPayoffShowId(show.getId())) {
      bookShowPayoff(tournament, show).ifPresent(bookings::add);
    }

    // 2. Paced rounds: tournaments with a future host show, booked on this non-PLE show.
    if (!show.isPremiumLiveEvent() && show.getShowDate() != null && show.getUniverse() != null) {
      for (Tournament tournament :
          tournamentRepository.findByUniverseIdAndPayoffShowIsNotNull(show.getUniverse().getId())) {
        Show payoffShow = tournament.getPayoffShow();
        if (payoffShow == null
            || payoffShow.getId().equals(show.getId())
            || payoffShow.getShowDate() == null
            || !payoffShow.getShowDate().isAfter(show.getShowDate())) {
          continue; // hosts its payoff here, or the host show is not ahead of this one
        }
        bookShowWeeklyRounds(tournament, show).forEach(bookings::add);
      }
    }
    return bookings;
  }

  /**
   * Booker-facing warnings for show-attached tournaments whose bracket cannot finish before their
   * host show (ATW-xbn4): approving this show's card would book a regular round match instead of
   * the payoff, and each re-approval drains one more — the marquee payoff degrades into catch-up
   * cards. Surfaced as an advisory on the planning card before approval (same pattern as the
   * MUST_BOOK rivalry warning). Empty when every hosted tournament can deliver its payoff.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<String> payoffCatchUpWarnings(@NonNull final Show show) {
    List<String> warnings = new ArrayList<>();
    for (Tournament tournament : tournamentRepository.findByPayoffShowId(show.getId())) {
      if (tournament.getStatus() == TournamentStatus.COMPLETE) {
        continue; // the payoff (showcase) is ready; nothing can degrade
      }
      // SCHEDULED + unseeded: the bracket is generated at start (round 1 = entries/2 matches)
      // and only ONE match can play at the payoff show — anything beyond a 2-entrant bracket
      // (sole match = final) cannot finish there.
      long openMatches;
      if (tournament.getStatus() == TournamentStatus.SCHEDULED) {
        long entrants = tournamentService.countEntries(tournament);
        openMatches = entrants > 0 ? Math.max(0, entrants - 1) : 0;
      } else {
        openMatches =
            tournament.getRounds() == null
                ? 0
                : tournament.getRounds().stream()
                    .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
                    .flatMap(r -> r.getMatches().stream())
                    .filter(m -> m.getWinner() == null && m.getSegment() == null)
                    .count();
      }
      // The payoff books the bracket's next open match when one remains, so only 2+ open
      // matches mean the bracket cannot finish here — the final itself arrives late.
      if (openMatches > 1) {
        warnings.add(
            ("%s cannot finish before its payoff show '%s' — %d bracket matches would still be"
                    + " open. Approving this card books a regular round match instead of the"
                    + " payoff; approve this show again later to catch up, or book rounds"
                    + " manually from the tournament view.")
                .formatted(tournament.getName(), show.getName(), openMatches));
      }
    }
    return warnings;
  }

  /**
   * What {@link #bookShowAttachedTournamentSegments} would book on this show, as preview rows for
   * the planning card (ATW-xbn4) — no tournament state is touched, no segments are built. Mirrors
   * the trigger's scan exactly: the payoff for tournaments hosted here, paced rounds for
   * tournaments with a future host show. Empty when nothing is due.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentSlotPreview> previewShowAttachedTournamentSlots(@NonNull final Show show) {
    List<TournamentSlotPreview> previews = new ArrayList<>();

    for (Tournament tournament : tournamentRepository.findByPayoffShowId(show.getId())) {
      String shape = payoffShapeOf(tournament);
      if (shape == null) {
        continue; // COMPLETE with no champion to showcase — nothing would book
      }
      // The vacant-title payoff final carries the championship; the champion showcase does too.
      boolean titleOnTheLine =
          tournament.getLinkedTitle() != null
              && ("Champion showcase".equals(shape)
                  || tournamentService.isTitleVacant(tournament.getLinkedTitle()));
      // Seeded/complete brackets preview real participants; unseeded ones show a placeholder.
      List<List<String>> teams = payoffTeamsOf(tournament, shape);
      previews.add(
          new TournamentSlotPreview(
              tournament.getName(),
              payoffTypeOf(tournament, show).getName(),
              tournament.getPayoffSegmentRule() != null
                  ? tournament.getPayoffSegmentRule().getName()
                  : null,
              shape,
              titleOnTheLine ? tournament.getLinkedTitle() : null,
              teams));
    }

    if (!show.isPremiumLiveEvent() && show.getShowDate() != null && show.getUniverse() != null) {
      for (Tournament tournament :
          tournamentRepository.findByUniverseIdAndPayoffShowIsNotNull(show.getUniverse().getId())) {
        Show payoffShow = tournament.getPayoffShow();
        if (payoffShow == null
            || payoffShow.getId().equals(show.getId())
            || payoffShow.getShowDate() == null
            || !payoffShow.getShowDate().isAfter(show.getShowDate())) {
          continue;
        }
        int matches = plannedRoundCountFor(tournament, show); // 0 when only the payoff remains
        if (matches > 0) {
          // One preview row per match, each with its real pairing when the bracket is generated
          // (seed math makes round-1 pairings predictable); placeholders when unseeded.
          List<TournamentMatch> openMatches =
              tournament.getRounds() == null
                  ? List.of()
                  : tournament.getRounds().stream()
                      .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
                      .flatMap(r -> r.getMatches().stream())
                      .filter(m -> m.getWinner() == null && m.getSegment() == null)
                      .sorted(Comparator.comparingInt(m -> m.getRound().getRoundNumber()))
                      .toList();
          for (int i = 0; i < matches; i++) {
            TournamentMatch open = i < openMatches.size() ? openMatches.get(i) : null;
            previews.add(
                new TournamentSlotPreview(
                    tournament.getName(),
                    "One on One",
                    null,
                    open != null
                        ? "Round " + open.getRound().getRoundNumber() + " — bracket match"
                        : "Round match",
                    null,
                    entrantNamesOf(open)));
          }
        }
      }
    }
    return previews;
  }

  /** Both entrants' names for a match, or a placeholder team layout when unseeded/unknown. */
  private List<List<String>> entrantNamesOf(@Nullable TournamentMatch match) {
    if (match == null
        || match.getEntrant1() == null
        || match.getEntrant2() == null
        || match.getEntrant1().getWrestler() == null
        || match.getEntrant2().getWrestler() == null) {
      return List.of(List.of("Tournament bracket"), List.of("Tournament bracket"));
    }
    return List.of(
        List.of(match.getEntrant1().getWrestler().getName()),
        List.of(match.getEntrant2().getWrestler().getName()));
  }

  /**
   * Real participant teams for the payoff preview when the bracket can supply them; placeholders
   * otherwise. Champion showcase: champion(s) vs tournament winner. Final: the bracket's last open
   * match (known only once the bracket reaches round N-1); a SCHEDULED+seeded bracket's round-1
   * pairings are pure seed math (1 vs N, 2 vs N-1), but the final's participants are NOT knowable
   * until the earlier rounds play — placeholders honestly say so.
   */
  private List<List<String>> payoffTeamsOf(Tournament tournament, String shape) {
    if ("Champion showcase".equals(shape)) {
      Title linkedTitle = tournament.getLinkedTitle();
      List<Wrestler> champions = tournamentService.currentChampionsOf(linkedTitle);
      Wrestler winner =
          tournament.getEntries().stream()
              .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
              .findFirst()
              .map(TournamentEntry::getWrestler)
              .orElse(null);
      if (!champions.isEmpty() && winner != null) {
        List<List<String>> teams = new ArrayList<>();
        champions.forEach(c -> teams.add(List.of(c.getName())));
        teams.add(List.of(winner.getName()));
        return teams;
      }
      return List.of(List.of("Champion"), List.of("Tournament winner"));
    }
    if ("Payoff final".equals(shape)
        && tournament.getStatus() == TournamentStatus.IN_PROGRESS
        && tournament.getRounds() != null) {
      TournamentMatch open =
          tournament.getRounds().stream()
              .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
              .flatMap(r -> r.getMatches().stream())
              .filter(m -> m.getWinner() == null && m.getSegment() == null)
              .min(Comparator.comparingInt(m -> m.getRound().getRoundNumber()))
              .orElse(null);
      if (open != null) {
        return entrantNamesOf(open);
      }
    }
    return List.of(List.of("Tournament bracket"), List.of("Tournament bracket"));
  }

  /**
   * The payoff's shape on this show: "Final" (bracket still running, or will start here — the final
   * IS the payoff when the linked championship is vacant or absent) or "Champion showcase" (bracket
   * finished, a champion reigns). Null when nothing would book: the bracket completed without a
   * champion to showcase.
   */
  private String payoffShapeOf(Tournament tournament) {
    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      Title linkedTitle = tournament.getLinkedTitle();
      if (linkedTitle != null && !tournamentService.isTitleVacant(linkedTitle)) {
        return "Champion showcase";
      }
      return null;
    }
    return "Payoff final";
  }

  /**
   * How many round matches {@link #bookShowWeeklyRounds} would book for this tournament on this
   * show — the pacing math without the booking. Non-positive when the tournament would not feed
   * this show (already started elsewhere, unseedable roster, only the payoff remaining, missing
   * One-on-One type).
   */
  private int plannedRoundCountFor(Tournament tournament, Show show) {
    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      return 0;
    }
    Show payoffShow = tournament.getPayoffShow();
    int matchesThisShow = 1;
    if (payoffShow != null && payoffShow.getShowDate() != null) {
      TournamentPacingService.PacingPlan plan = pacingService.planFor(tournament, payoffShow);
      long slots = pacingService.weeklyShowSlotsBefore(payoffShow, show.getShowDate()).size();
      if (plan.remainingNonFinal() == 0) {
        return 0;
      }
      matchesThisShow = (int) Math.ceil((double) plan.remainingNonFinal() / Math.max(1, slots));
    }
    // An unstarted tournament starts here; anything else must be underway to feed rounds.
    boolean feeds =
        tournament.getStatus() == TournamentStatus.SCHEDULED
            || tournament.getStatus() == TournamentStatus.IN_PROGRESS;
    if (!feeds
        || segmentTypeService.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()).isEmpty()) {
      return 0;
    }
    // A SCHEDULED tournament's bracket is generated at start — its round-1 matches do not exist
    // yet, so the open-match cap applies only to an IN_PROGRESS bracket.
    if (tournament.getStatus() == TournamentStatus.SCHEDULED) {
      return matchesThisShow;
    }
    long open =
        tournament.getRounds() == null
            ? 0
            : tournament.getRounds().stream()
                .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
                .flatMap(r -> r.getMatches().stream())
                .filter(m -> m.getWinner() == null && m.getSegment() == null)
                .count();
    return Math.min(matchesThisShow, (int) open);
  }

  /**
   * One planning-card preview row for a show-attached tournament slot. {@code expectedTitle} is set
   * when the slot will carry the linked championship (vacant-title payoff final). {@code teams}
   * carries the real match-up when the bracket can supply it (seeded/underway), or a placeholder
   * layout when it cannot (unseeded, or the final's participants are unknowable before earlier
   * rounds play).
   */
  public record TournamentSlotPreview(
      String tournamentName,
      String typeName,
      String ruleName,
      String shape,
      Title expectedTitle,
      List<List<String>> teams) {}

  /**
   * Payoff for a one-time tournament whose payoffShow is this show. Same semantics as the template
   * path: final (title match when the linked championship is vacant) or champion vs tournament
   * winner; the host-show link is consumed so it never books twice.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<TournamentBooking> bookShowPayoff(
      @NonNull final Tournament tournament, @NonNull final Show show) {
    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      Title linkedTitle = tournament.getLinkedTitle();
      if (linkedTitle != null && !tournamentService.isTitleVacant(linkedTitle)) {
        return bookChampionShowcase(
            tournament,
            payoffTypeOf(tournament, show),
            tournament.getPayoffSegmentRule(),
            show,
            () -> consumeShowLink(tournament, show, "champion showcase booked"));
      }
      consumeShowLink(tournament, show, "final already played before the host show");
      return Optional.empty();
    }

    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !autoStartTournament(tournament, show)) {
      return Optional.empty();
    }

    if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
      return bookPayoffInProgress(
          tournament,
          payoffTypeOf(tournament, show),
          tournament.getPayoffSegmentRule(),
          show,
          () -> consumeShowLink(tournament, show, "payoff booked at the host show"));
    }

    log.warn(
        "Tournament '{}' is {} — cannot feed participants for show '{}'",
        tournament.getName(),
        tournament.getStatus(),
        show.getName());
    return Optional.empty();
  }

  /**
   * Paced weekly round(s) for a tournament whose payoffShow is still in the future. The pacing
   * target is the payoffShow itself — no template scan. Rounds use the standard One-on-One type.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentBooking> bookShowWeeklyRounds(
      @NonNull final Tournament tournament, @NonNull final Show show) {
    if (tournament.getStatus() == TournamentStatus.COMPLETE) {
      return List.of();
    }
    // Auto-start BEFORE pacing math: an unseeded bracket estimates zero matches, which would
    // read as "only the payoff remains" and strand the tournament unstarted until its host
    // show — where a round-1 match would book instead of the payoff.
    if (tournament.getStatus() == TournamentStatus.SCHEDULED
        && !autoStartTournament(tournament, show)) {
      return List.of();
    }
    Show payoffShow = tournament.getPayoffShow();
    int matchesThisShow = 1;
    if (payoffShow != null && payoffShow.getShowDate() != null) {
      TournamentPacingService.PacingPlan plan = pacingService.planFor(tournament, payoffShow);
      // Denominator = every weekly slot from this show (inclusive) to the payoff — the share
      // THIS show must carry so the remaining matches finish before the host show. Counting
      // only same-date shows would cram the whole remainder onto the first slot (found in the
      // sandbox: 8-entrant bracket, 3 weekly slots → ceil(7/3)=3 per show, not 7 on one card).
      long slots = pacingService.weeklyShowSlotsBefore(payoffShow, show.getShowDate()).size();
      if (plan.remainingNonFinal() == 0) {
        // Only the payoff remains — it plays at the host show; nothing to book on weekly shows.
        return List.of();
      }
      matchesThisShow = (int) Math.ceil((double) plan.remainingNonFinal() / Math.max(1, slots));
    }

    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
      return List.of();
    }

    Optional<SegmentType> singlesType =
        segmentTypeService.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode());
    if (singlesType.isEmpty()) {
      log.warn(
          "Cannot book tournament rounds on weekly show '{}': the standard one-on-one segment"
              + " type is missing",
          show.getName());
      return List.of();
    }

    ArrayList<TournamentBooking> bookings = new ArrayList<>();
    for (int i = 0; i < matchesThisShow; i++) {
      Optional<TournamentBooking> booking =
          bookCurrentRoundFedSegment(tournament, singlesType.get(), null, show);
      if (booking.isEmpty()) {
        break;
      }
      bookings.add(booking.get());
    }
    return bookings;
  }

  /** The payoff segment type: the tournament's choice, falling back to One-on-One. */
  private SegmentType payoffTypeOf(Tournament tournament, Show show) {
    if (tournament.getPayoffSegmentType() != null) {
      return tournament.getPayoffSegmentType();
    }
    return segmentTypeService
        .findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode())
        .orElseGet(
            () -> {
              log.warn(
                  "Tournament '{}' has no payoff type and the standard one-on-one segment type is"
                      + " missing — cannot book payoff for show '{}'",
                  tournament.getName(),
                  show.getName());
              return null;
            });
  }

  /** Detach a one-time tournament from its host show so the payoff cannot fire twice. */
  private void consumeShowLink(
      @NonNull final Tournament tournament,
      @NonNull final Show show,
      @NonNull final String reason) {
    tournamentService.clearPayoffShow(tournament);
    log.info(
        "Consumed host-show link for '{}' (was '{}') — {}",
        tournament.getName(),
        show.getName(),
        reason);
  }

  // ── PLE payoff ──────────────────────────────────────────────────────────────

  /**
   * The PLE-side booking for a tournament still in progress: normally the next open match — and
   * when that match is the final under FINAL_AT_PLE pacing, it IS the payoff (title match when the
   * linked championship is vacant) and consumes the pairing.
   */
  private Optional<TournamentBooking> bookPleMatchForInProgressTournament(
      final Tournament tournament,
      final ShowTemplateSegmentAssignment assignment,
      final SegmentType segmentType,
      final Show show) {
    return bookPayoffInProgress(
        tournament,
        segmentType,
        assignment.getSegmentRule(),
        show,
        () -> consumePairing(assignment, tournament, show, "final booked at the PLE"));
  }

  /**
   * Shared IN_PROGRESS payoff booking: the next open match — the bracket final under FINAL_AT_PLE
   * pacing is the payoff (title match when the linked championship is vacant) and runs {@code
   * consumeAction}.
   */
  private Optional<TournamentBooking> bookPayoffInProgress(
      final Tournament tournament,
      final SegmentType segmentType,
      final SegmentRule payoffRule,
      final Show show,
      final Runnable consumeAction) {
    Optional<TournamentMatch> openMatchOpt = nextBookableMatch(tournament);
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
      openMatchOpt = nextBookableMatch(tournament);
    }
    if (openMatchOpt.isEmpty()) {
      log.warn(
          "Tournament '{}' has no bookable match — falling back to AI participants",
          tournament.getName());
      return Optional.empty();
    }
    TournamentMatch match = openMatchOpt.get();
    boolean isFinal = isBracketFinal(tournament, match);
    TournamentPacingService.PayoffKind payoffKind = pacingService.payoffKindOf(tournament);

    // A reigning champion means the payoff is champion-vs-winner, not a bracket match — book the
    // open match as a regular bout (pacing puts the final on the last weekly show). Vacant/no
    // title: the final at the payoff show is the payoff (title match when vacant).
    boolean payoff = payoffKind == TournamentPacingService.PayoffKind.FINAL_AT_PLE && isFinal;
    Title linkedTitle = tournament.getLinkedTitle();
    boolean titleOnTheLine = payoff && linkedTitle != null;

    Segment segment =
        resolveSegment(
            match, tournament, segmentType, show, stipulationOf(payoffRule), titleOnTheLine);

    match.setSegment(segment);
    tournamentService.markRoundInProgress(match.getRound());
    tournamentService.recordMatchResult(match, pickBracketWinner(match, segment));
    if (payoff) {
      // The final's result completes the bracket — flip the tournament so the winner entry
      // exists before the show is adjudicated.
      tournamentService.advanceToNextRound(tournament);
      consumeAction.run();
    }
    log.info(
        "Booked tournament segment for '{}' ({}) on PLE '{}': {} vs {}{}",
        tournament.getName(),
        roundNameOf(match),
        show.getName(),
        match.getEntrant1().getWrestler().getName(),
        match.getEntrant2().getWrestler().getName(),
        payoff ? " — PAYOFF" : "");
    return Optional.of(
        new TournamentBooking(
            segment,
            tournament,
            roundNameOf(match) + " — tournament-fed",
            titleOnTheLine,
            titleOnTheLine ? linkedTitle : null));
  }

  /**
   * The payoff when the bracket finished before the payoff show and a champion reigns: champion vs
   * tournament winner as the title match. Runs {@code consumeAction} on success.
   */
  private Optional<TournamentBooking> bookChampionShowcase(
      final Tournament tournament,
      final SegmentType segmentType,
      final SegmentRule payoffRule,
      final Show show,
      final Runnable consumeAction) {
    Title linkedTitle = tournament.getLinkedTitle();
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
    List<Wrestler> champions = tournamentService.currentChampionsOf(linkedTitle);
    if (champions.isEmpty()) {
      // Champion lost the belt elsewhere between the final and the payoff show — no showcase.
      consumeAction.run();
      return Optional.empty();
    }

    Segment segment =
        segmentResolutionService.resolveTeamSegment(
            new SegmentTeam(champions),
            new SegmentTeam(winner.getWrestler()),
            segmentType,
            show,
            stipulationOf(payoffRule));
    segment.setIsTitleSegment(true);
    segment.getTitles().add(linkedTitle);
    segment.setNarration(
        "Tournament payoff: "
            + tournament.getName()
            + " champion showcase. The tournament winner ("
            + winner.getWrestler().getName()
            + ") challenges the reigning "
            + linkedTitle.getName()
            + " champion"
            + (champions.size() > 1 ? "s" : "")
            + ". Narrate the tournament's conclusion and the champion's response to the"
            + " bracket's winner.");
    consumeAction.run();
    log.info(
        "Booked champion showcase for tournament '{}' on show '{}': {} vs {}",
        tournament.getName(),
        show.getName(),
        champions.stream().map(Wrestler::getName).collect(Collectors.joining(" & ")),
        winner.getWrestler().getName());
    return Optional.of(
        new TournamentBooking(
            segment,
            tournament,
            "Champion showcase — champion vs " + winner.getWrestler().getName(),
            true,
            linkedTitle));
  }

  /** Detach the tournament from the assignment row so the pairing cannot fire again. */
  private void consumePairing(
      @NonNull final ShowTemplateSegmentAssignment assignment,
      @NonNull final Tournament tournament,
      @NonNull final Show show,
      @NonNull final String reason) {
    if (assignment.getTournament() == null) {
      return;
    }
    if (assignment.getSegmentType() != null || assignment.getSegmentRule() != null) {
      // Keep the row as a plain type/rule pairing — only the tournament detaches.
      assignment.setTournament(null);
    }
    // Tournament-only rows would become invalid (no target) — remove them from the template
    // so the orphanRemoval mapping deletes the row.
    assignment.getTemplate().getSegmentAssignments().remove(assignment);
    log.info(
        "Consumed tournament pairing for '{}' on template of show '{}' ({}) — later shows fall"
            + " back to AI-proposed participants",
        tournament.getName(),
        show.getName(),
        reason);
  }

  // ── Round booking (shared by PLE and weekly paths) ─────────────────────────

  /**
   * Book the tournament's next open match as a segment with the given type. Advances the bracket
   * once when every booked round is decided but the next round is not generated yet.
   */
  private Optional<TournamentBooking> bookCurrentRoundFedSegment(
      final Tournament tournament,
      final ShowTemplateSegmentAssignment assignment,
      final SegmentType segmentType,
      final Show show) {
    return bookCurrentRoundFedSegment(tournament, segmentType, assignment.getSegmentRule(), show);
  }

  /** Assignment-free overload: the rule comes straight from the caller (nullable). */
  private Optional<TournamentBooking> bookCurrentRoundFedSegment(
      final Tournament tournament,
      final SegmentType segmentType,
      final SegmentRule rule,
      final Show show) {
    Optional<TournamentMatch> openMatchOpt = nextBookableMatch(tournament);
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
      openMatchOpt = nextBookableMatch(tournament);
    }
    if (openMatchOpt.isEmpty()) {
      log.warn(
          "Tournament '{}' has no bookable match — falling back to AI participants",
          tournament.getName());
      return Optional.empty();
    }

    TournamentMatch match = openMatchOpt.get();
    Segment segment =
        resolveSegment(match, tournament, segmentType, show, stipulationOf(rule), false);

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
        new TournamentBooking(
            segment, tournament, roundNameOf(match) + " — tournament-fed", false, null));
  }

  /**
   * The earliest round holding an open (winnerless) match — round 1 on a fresh bracket, later
   * rounds as earlier ones complete. Skips rounds already booked onto other shows (every match
   * linked to a segment).
   */
  private Optional<TournamentMatch> nextBookableMatch(final Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
        .flatMap(r -> r.getMatches().stream())
        .filter(m -> m.getWinner() == null && m.getSegment() == null)
        .min(Comparator.comparingInt(m -> m.getRound().getRoundNumber()));
  }

  /**
   * Whether booking {@code match} completes the bracket — the pacing math (total estimate minus
   * booked matches) says exactly one match remains.
   */
  private boolean isBracketFinal(Tournament tournament, TournamentMatch match) {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(fmt -> fmt.estimateTotalMatches(tournament))
        .map(total -> total - countBooked(tournament) == 1 && match.getWinner() == null)
        .orElse(false);
  }

  private int countBooked(Tournament tournament) {
    return (int)
        tournament.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .filter(m -> m.getSegment() != null || m.getWinner() != null)
            .count();
  }

  /** The next PLE (from this show's date forward a year) whose template pairs this tournament. */
  private Optional<Show> findTargetPle(final Tournament tournament, final Show from) {
    if (from.getShowDate() == null) {
      return Optional.empty();
    }
    return showService
        .getShowsByDateRange(from.getShowDate(), from.getShowDate().plusYears(1))
        .stream()
        .filter(Show::isPremiumLiveEvent)
        .filter(
            s ->
                s.getTemplate() != null
                    && s.getTemplate().getTournamentAssignments().stream()
                        .anyMatch(
                            a ->
                                a.getTournament() != null
                                    && tournament.getId() != null
                                    && tournament.getId().equals(a.getTournament().getId())))
        .findFirst();
  }

  private String stipulationOf(@Nullable final SegmentRule rule) {
    return rule != null ? rule.getName() : "";
  }

  /**
   * Create the match segment for a bracket match. Title payoffs are flagged as title segments
   * carrying the linked championship so adjudication awards or defends it at finalization.
   *
   * <p>Narration carries the tournament context (tournament, round, stakes) so AI narration knows
   * what this match is and why it matters — the Narrate dialog generates from the narration field.
   */
  private Segment resolveSegment(
      final TournamentMatch match,
      final Tournament tournament,
      final SegmentType segmentType,
      final Show show,
      final String stipulation,
      final boolean titleOnTheLine) {
    Segment segment =
        segmentResolutionService.resolveTeamSegment(
            new SegmentTeam(match.getEntrant1().getWrestler()),
            new SegmentTeam(match.getEntrant2().getWrestler()),
            segmentType,
            show,
            stipulation);
    segment.setNarration(tournamentNotesOf(tournament, match, titleOnTheLine));
    if (titleOnTheLine && tournament.getLinkedTitle() != null) {
      segment.setIsTitleSegment(true);
      segment.getTitles().add(tournament.getLinkedTitle());
    }
    return segment;
  }

  /** Booking narration describing the tournament stakes for AI narration. */
  private String tournamentNotesOf(
      final Tournament tournament, final TournamentMatch match, final boolean titleOnTheLine) {
    StringBuilder notes = new StringBuilder();
    notes
        .append("Tournament match: ")
        .append(tournament.getName())
        .append(" — ")
        .append(roundNameOf(match))
        .append(". ");
    if (titleOnTheLine && tournament.getLinkedTitle() != null) {
      notes
          .append("The winner advances in the tournament; the ")
          .append(tournament.getLinkedTitle().getName())
          .append(" is on the line. ");
    } else {
      notes.append("The winner advances in the tournament. ");
    }
    if (tournament.getLinkedTitle() != null && !titleOnTheLine) {
      notes
          .append("The tournament winner earns a ")
          .append(tournament.getLinkedTitle().getName())
          .append(" opportunity at the tournament's payoff show. ");
    }
    notes.append("Narrate the tournament stakes and the crowd's investment in the bracket.");
    return notes.toString();
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

  private String roundNameOf(final TournamentMatch match) {
    return match.getRound().getRoundName();
  }

  // ── Auto-start (unchanged from ATW-oahn) ────────────────────────────────────

  /**
   * Auto-seed (when unseeded) and start a SCHEDULED tournament, refreshing the in-memory instance
   * with the generated bracket. Returns false (with a warning) when the tournament cannot start —
   * never throws to the approval flow.
   *
   * <p>Every predictable failure is pre-flighted <em>before</em> entering the nested transactional
   * calls: an exception thrown out of a joined {@code @Transactional} method marks the approval
   * transaction rollback-only even when caught here, surfacing later as a confusing {@code
   * UnexpectedRollbackException} at commit. The remaining catch is a dead-man switch for genuine
   * races (roster shrinking between check and seed).
   */
  private boolean autoStartTournament(Tournament tournament, Show show) {
    Optional<TournamentFormat> formatOpt = tournamentService.findFormat(tournament.getFormatId());
    if (formatOpt.isEmpty()) {
      log.warn(
          "Cannot auto-start tournament '{}' for show '{}': format '{}' not found — falling back"
              + " to AI participants",
          tournament.getName(),
          show.getName(),
          tournament.getFormatId());
      return false;
    }
    // Ask the repository (not the lazy collection): calling getEntries() would initialize it
    // empty inside the approval transaction, and an initialized collection never re-queries —
    // startTournament would then fail its own entrants check right after seeding.
    if (!tournamentService.hasEntries(tournament.getId())) {
      int minEntrants = formatOpt.get().getMinEntrants();
      int eligible =
          tournamentService
              .findEligibleWrestlersSortedByFans(tournament.getLinkedTitle(), universeId(show))
              .size();
      if (eligible < minEntrants) {
        log.warn(
            "Cannot auto-start tournament '{}' for show '{}': {} eligible wrestlers available,"
                + " the format needs at least {} — falling back to AI participants",
            tournament.getName(),
            show.getName(),
            eligible,
            minEntrants);
        return false;
      }
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
