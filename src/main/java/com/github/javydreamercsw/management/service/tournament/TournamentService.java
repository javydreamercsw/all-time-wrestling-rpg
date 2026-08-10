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
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
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

  public Optional<TournamentFormat> findFormat(String formatId) {
    return formats.stream().filter(f -> f.getFormatId().equals(formatId)).findFirst();
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findAll() {
    return tournamentRepository.findAll();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findByUniverse(Universe universe) {
    return tournamentRepository.findByUniverseIdOrderByStartDateDesc(universe.getId());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findById(Long id) {
    return tournamentRepository.findById(id);
  }

  /** Like {@link #findById} but initializes all lazy collections for use outside a transaction. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findByIdWithDetails(Long id) {
    return tournamentRepository.findById(id).map(this::initializeGraph);
  }

  private Tournament initializeGraph(Tournament t) {
    t.getEntries().forEach(e -> e.getWrestler().getName());
    t.getRounds()
        .forEach(
            r ->
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
                        }));
    return t;
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name, String formatId, Universe universe, Title linkedTitle, LocalDate startDate) {
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
    return tournamentRepository.save(t);
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
    List<Wrestler> active =
        wrestlerRepository.findAllByActiveTrue().stream()
            .sorted(Comparator.comparingLong((Wrestler w) -> w.getFans(universeId)).reversed())
            .limit(count)
            .toList();

    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 0; i < active.size(); i++) {
      entries.add(addEntry(tournament, active.get(i), i + 1));
    }
    return entries;
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
    fmt.generateBracket(tournament, formatContext());
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    return tournamentRepository.save(tournament);
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

      // Reserve a slot on the show
      reservationService.reserveSlot(
          show, ShowSegmentReservationPurpose.TOURNAMENT_ROUND, match.getId(), label);

      // Book the actual segment
      showBookingService
          .bookSpecificMatch(
              show, match.getEntrant1().getWrestler(), match.getEntrant2().getWrestler(), label)
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
