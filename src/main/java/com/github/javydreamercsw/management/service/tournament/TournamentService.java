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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("bracketTournamentService")
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

  private final TournamentRepository tournamentRepository;
  private final TournamentEntryRepository entryRepository;
  private final TournamentRoundRepository roundRepository;
  private final TournamentMatchRepository matchRepository;
  private final WrestlerRepository wrestlerRepository;
  private final ShowRepository showRepository;
  private final ShowBookingService showBookingService;
  private final ShowSegmentReservationService reservationService;
  private final List<TournamentFormat> formats;

  private TournamentFormatContext formatContext() {
    return new TournamentFormatContext(roundRepository, matchRepository);
  }

  // ── Format registry ──────────────────────────────────────────────────────

  public List<TournamentFormat> getAvailableFormats() {
    return List.copyOf(formats);
  }

  public Optional<TournamentFormat> findFormat(@NonNull String formatId) {
    return formats.stream().filter(f -> f.getFormatId().equals(formatId)).findFirst();
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findAll() {
    return tournamentRepository.findAll();
  }

  /**
   * Session-safe entrant count for detached grid rows — the entries collection is lazy and the list
   * view renders outside a transaction (TournamentListView).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countEntries(@NonNull Tournament tournament) {
    return entryRepository.countByTournamentId(tournament.getId());
  }

  /**
   * Whether the tournament has any persisted entrants — asked via a count query rather than {@code
   * getEntries().isEmpty()}, because inside an active transaction that call initializes the lazy
   * collection empty and an initialized collection never re-queries (a caller that then seeds would
   * still see "no entrants" for the rest of the transaction, ATW-oahn).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean hasEntries(@NonNull Long tournamentId) {
    return entryRepository.countByTournamentId(tournamentId) > 0;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findByUniverse(@NonNull Universe universe) {
    return tournamentRepository.findByUniverseIdOrderByStartDateDesc(universe.getId());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findById(@NonNull Long id) {
    return tournamentRepository.findById(id);
  }

  /** Like {@link #findById} but initializes all lazy collections for use outside a transaction. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findByIdWithDetails(Long id) {
    return tournamentRepository.findById(id).map(this::initializeGraph);
  }

  private Tournament initializeGraph(@NonNull Tournament t) {
    t.getEntries().forEach(e -> e.getWrestler().getName());
    t.getAllowedRules().forEach(SegmentRule::getName);
    // The detail view reads the linked championship's name outside the session.
    if (t.getLinkedTitle() != null) {
      t.getLinkedTitle().getName();
    }
    t.getRounds()
        .forEach(
            r -> {
              if (r.getFixedRule() != null) {
                r.getFixedRule().getName();
              }
              r.getMatches()
                  .forEach(
                      m -> {
                        m.getEntrant1().getWrestler().getName();
                        m.getEntrant2().getWrestler().getName();
                        if (m.getWinner() != null) {
                          m.getWinner().getWrestler().getName();
                        }
                        if (r.getShow() != null) {
                          r.getShow().getName();
                        }
                      });
            });
    return t;
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules) {
    findFormat(formatId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + formatId));
    Tournament t = new Tournament();
    t.setName(name);
    t.setFormatId(formatId);
    t.setUniverse(universe);
    t.setLinkedTitle(linkedTitle);
    t.setStartDate(startDate);
    t.setStatus(TournamentStatus.SCHEDULED);
    t.setEntries(new ArrayList<>());
    t.setRounds(new ArrayList<>());
    t.setAllowedRules(allowedRules != null ? new ArrayList<>(allowedRules) : new ArrayList<>());
    return tournamentRepository.save(t);
  }

  /**
   * Update a tournament's editable metadata. Only SCHEDULED tournaments can be edited — once a
   * bracket is underway its structure (format, entrants) is locked.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament updateTournament(
      @NonNull final Long id,
      @NonNull final String name,
      final String formatId,
      final Title linkedTitle,
      final LocalDate startDate,
      final List<SegmentRule> allowedRules) {
    Tournament t =
        tournamentRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + id));
    if (t.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException(
          "Only SCHEDULED tournaments can be edited — '" + t.getName() + "' is " + t.getStatus());
    }
    t.setName(name);
    if (formatId != null && !formatId.equals(t.getFormatId())) {
      if (!t.getEntries().isEmpty()) {
        throw new IllegalStateException(
            "Cannot change the format of a tournament that already has entrants");
      }
      findFormat(formatId)
          .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + formatId));
      t.setFormatId(formatId);
    }
    t.setLinkedTitle(linkedTitle);
    t.setStartDate(startDate);
    t.setAllowedRules(allowedRules != null ? new ArrayList<>(allowedRules) : new ArrayList<>());
    return tournamentRepository.save(t);
  }

  /**
   * Delete a tournament. Only SCHEDULED ones (no bracket in flight, no booked segments to orphan);
   * deletes entries, rounds, and their matches first since match → round is a plain FK with no JPA
   * cascade.
   *
   * @return true when deleted, false when the tournament does not exist
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public boolean deleteTournament(@NonNull final Long id) {
    Tournament t = tournamentRepository.findById(id).orElse(null);
    if (t == null) {
      return false;
    }
    if (t.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException(
          "Only SCHEDULED tournaments can be deleted — '"
              + t.getName()
              + "' is "
              + t.getStatus()
              + ". Start a new one instead.");
    }
    t.getRounds()
        .forEach(round -> matchRepository.deleteAll(matchRepository.findByRoundId(round.getId())));
    roundRepository.deleteAll(roundRepository.findByTournamentIdOrderByRoundNumberAsc(id));
    entryRepository.deleteAll(entryRepository.findByTournamentIdOrderBySeedAsc(id));
    tournamentRepository.delete(t);
    return true;
  }

  // ── Entry management ──────────────────────────────────────────────────────

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentEntry addEntry(Tournament tournament, Wrestler wrestler, int seed) {
    if (entryRepository.existsByTournamentIdAndWrestlerId(tournament.getId(), wrestler.getId())) {
      throw new IllegalStateException(
          wrestler.getName() + " is already entered in this tournament");
    }
    TournamentEntry entry =
        TournamentEntry.builder().tournament(tournament).wrestler(wrestler).seed(seed).build();
    return entryRepository.save(entry);
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentEntry> seedAuto(Tournament tournament, int count, Long universeId) {
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));
    if (count < fmt.getMinEntrants() || count > fmt.getMaxEntrants()) {
      throw new IllegalArgumentException(
          "Entrant count "
              + count
              + " out of range ["
              + fmt.getMinEntrants()
              + ", "
              + fmt.getMaxEntrants()
              + "]");
    }
    // A tournament linked to a gender-restricted championship seeds only eligible wrestlers
    // (e.g. the ATW World title is male-exclusive).
    Gender genderConstraint =
        tournament.getLinkedTitle() != null ? tournament.getLinkedTitle().getGender() : null;
    List<Wrestler> active =
        (genderConstraint != null
                ? wrestlerRepository.findAllByGenderAndActive(genderConstraint, true)
                : wrestlerRepository.findAllByActiveTrue())
            .stream()
                .sorted(Comparator.comparingLong((Wrestler w) -> w.getFans(universeId)).reversed())
                .limit(count)
                .toList();

    if (active.size() < fmt.getMinEntrants()) {
      throw new IllegalStateException(
          "Not enough eligible wrestlers to seed '"
              + tournament.getName()
              + "': "
              + active.size()
              + " available, the format needs at least "
              + fmt.getMinEntrants()
              + (genderConstraint != null
                  ? " (eligibility limited by the linked championship's gender constraint)"
                  : ""));
    }
    if (active.size() < count) {
      // Requested more than the eligible roster holds — seed everyone available instead of
      // failing (the wizard caps its field at the roster size; API callers may not).
      log.info(
          "Seeding '{}' with {} entrants ({} requested, roster holds only that many)",
          tournament.getName(),
          active.size(),
          count);
    }

    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 0; i < active.size(); i++) {
      entries.add(addEntry(tournament, active.get(i), i + 1));
    }
    // Deliberately NOT re-reading the entries onto the in-memory instance: within the caller's
    // transaction a later getEntries() initializes the lazy collection fresh (auto-flush makes
    // the persisted rows visible), and replacing the collection instance wholesale is illegal
    // for an orphanRemoval mapping. Detached callers re-fetch via findByIdWithDetails.
    return entries;
  }

  /**
   * How many active wrestlers are eligible to seed a tournament linked to the given title — the
   * active roster, narrowed by the title's gender constraint when it has one. The creation wizard
   * caps its entrant-count field at this value.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public int countEligibleEntrants(Title linkedTitle) {
    return findEligibleWrestlersSortedByFans(linkedTitle, null).size();
  }

  /**
   * The active wrestlers eligible to seed a tournament linked to the given title, sorted by fans
   * (most fans first) — the same pool and ordering {@link #seedAuto} uses. Transactional so
   * detached UI callers can read fan counts (they walk the lazy wrestlerStates collection) and
   * render the seeding preview without LazyInitializationException.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findEligibleWrestlersSortedByFans(Title linkedTitle, Long universeId) {
    Gender genderConstraint = linkedTitle != null ? linkedTitle.getGender() : null;
    return (genderConstraint != null
            ? wrestlerRepository.findAllByGenderAndActive(genderConstraint, true)
            : wrestlerRepository.findAllByActiveTrue())
        .stream()
            .sorted(Comparator.comparingLong((Wrestler w) -> w.getFans(universeId)).reversed())
            .toList();
  }

  // ── Bracket lifecycle ─────────────────────────────────────────────────────

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament startTournament(Tournament tournament) {
    if (tournament.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException("Tournament is not in SCHEDULED state");
    }
    if (tournament.getEntries().isEmpty()) {
      throw new IllegalStateException("No entrants — seed the tournament before starting");
    }
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));
    List<TournamentRound> generated = fmt.generateBracket(tournament, formatContext());
    // The format persists the bracket through the owning side; attach it to the in-memory
    // collection so callers holding this instance (template-fed approval, ATW-oahn) can scan
    // round 1 without re-fetching — a lazy collection already initialized inside the current
    // transaction never re-queries.
    attachGenerated(tournament, generated.stream().flatMap(r -> r.getMatches().stream()).toList());
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    return tournamentRepository.save(tournament);
  }

  /**
   * Make freshly generated bracket content visible on the in-memory tournament. A lazy collection
   * that was already initialized within the current transaction never re-queries, so rounds the
   * format persisted through the owning side would otherwise be invisible to in-memory scans (e.g.
   * the template-fed booking path's open-match lookup).
   */
  private void attachGenerated(Tournament tournament, List<TournamentMatch> generated) {
    for (TournamentMatch match : generated) {
      TournamentRound round = match.getRound();
      TournamentRound attached =
          tournament.getRounds().stream()
              .filter(r -> r.getId() != null && r.getId().equals(round.getId()))
              .findFirst()
              .orElseGet(
                  () -> {
                    tournament.getRounds().add(round);
                    return round;
                  });
      if (attached.getMatches().stream()
          .noneMatch(m -> m.getId() != null && m.getId().equals(match.getId()))) {
        attached.getMatches().add(match);
      }
    }
  }

  /**
   * Mark a PENDING round IN_PROGRESS when one of its matches is booked through any path. The manual
   * flow sets this in {@link #bookRoundOnShow}; template-fed booking (ATW-oahn) books matches one
   * at a time outside it, so it calls this to keep the UI's "Book Round on Show" button coherent —
   * a round with a booked match is no longer offerable for booking.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void markRoundInProgress(TournamentRound round) {
    if (round.getStatus() == TournamentRoundStatus.PENDING) {
      round.setStatus(TournamentRoundStatus.IN_PROGRESS);
      roundRepository.save(round);
    }
  }

  /** Book all pending matches in the current round onto a show, creating segment reservations. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void bookRoundOnShow(Tournament tournament, TournamentRound round, Show show) {
    List<TournamentMatch> unbooked =
        matchRepository.findByRoundIdAndWinnerIsNull(round.getId()).stream()
            .filter(m -> m.getSegment() == null)
            .toList();

    for (TournamentMatch match : unbooked) {
      String label =
          tournament.getName()
              + " — "
              + round.getRoundName()
              + ": "
              + match.getEntrant1().getWrestler().getName()
              + " vs "
              + match.getEntrant2().getWrestler().getName();

      String stipulation = resolveStipulation(tournament, round, label);

      // Reserve a slot on the show
      reservationService.reserveSlot(
          show, ShowSegmentReservationPurpose.TOURNAMENT_ROUND, match.getId(), label);

      // Book the actual segment with the resolved stipulation
      showBookingService
          .bookSpecificMatch(
              show,
              match.getEntrant1().getWrestler(),
              match.getEntrant2().getWrestler(),
              label,
              stipulation)
          .ifPresent(
              segment -> {
                match.setSegment(segment);
                matchRepository.save(match);
              });
    }

    round.setShow(show);
    round.setStatus(TournamentRoundStatus.IN_PROGRESS);
    roundRepository.save(round);
  }

  /**
   * Assign a fixed segment rule to a round. Set {@code null} to clear and revert to the
   * tournament's allowed-rules pool.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentRound setRoundFixedRule(TournamentRound round, SegmentRule rule) {
    round.setFixedRule(rule);
    return roundRepository.save(round);
  }

  /**
   * Resolve the segment rule stipulation for a match:
   *
   * <ol>
   *   <li>Round's fixedRule (if set)
   *   <li>Random pick from tournament's allowedRules pool (if non-empty)
   *   <li>Fallback string (preserves legacy behaviour)
   * </ol>
   */
  private String resolveStipulation(Tournament tournament, TournamentRound round, String fallback) {
    if (round.getFixedRule() != null) {
      return round.getFixedRule().getName();
    }
    List<SegmentRule> pool = tournament.getAllowedRules();
    if (!pool.isEmpty()) {
      return pool.get(ThreadLocalRandom.current().nextInt(pool.size())).getName();
    }
    return fallback;
  }

  /** Record the winner of a match and, if the round is now fully decided, mark it complete. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void recordMatchResult(TournamentMatch match, TournamentEntry winner) {
    TournamentEntry loser =
        match.getEntrant1().equals(winner) ? match.getEntrant2() : match.getEntrant1();
    loser.setStatus(TournamentEntryStatus.ELIMINATED);
    entryRepository.save(loser);

    match.setWinner(winner);
    matchRepository.save(match);

    // Check if round is complete
    TournamentRound round = match.getRound();
    boolean roundComplete = matchRepository.findByRoundIdAndWinnerIsNull(round.getId()).isEmpty();
    if (roundComplete) {
      round.setStatus(TournamentRoundStatus.COMPLETE);
      roundRepository.save(round);
    }
  }

  /** Advance to the next round (generates matches for single-elimination). */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentMatch> advanceToNextRound(Tournament tournament) {
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));

    if (fmt.isComplete(tournament)) {
      markWinner(tournament);
      return List.of();
    }
    return fmt.advanceRound(tournament, formatContext());
  }

  private void markWinner(Tournament tournament) {
    tournament.setStatus(TournamentStatus.COMPLETE);
    tournament.setEndDate(LocalDate.now());
    tournamentRepository.save(tournament);
    entryRepository
        .findByTournamentIdAndStatus(tournament.getId(), TournamentEntryStatus.ACTIVE)
        .stream()
        .findFirst()
        .ifPresent(
            entry -> {
              entry.setStatus(TournamentEntryStatus.WINNER);
              entryRepository.save(entry);
            });
  }

  /** Find the next upcoming show after today for use in the creation wizard. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Show> findNextShow(Universe universe) {
    List<Show> upcoming =
        showRepository.findByShowDateGreaterThanEqualOrderByShowDate(
            LocalDate.now(), PageRequest.of(0, 5));
    return upcoming.stream()
        .filter(s -> s.getUniverse() == null || s.getUniverse().equals(universe))
        .findFirst();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<TournamentRound> getCurrentRound(Tournament tournament) {
    return roundRepository.findFirstByTournamentIdAndStatusOrderByRoundNumberAsc(
        tournament.getId(), TournamentRoundStatus.IN_PROGRESS);
  }
}
