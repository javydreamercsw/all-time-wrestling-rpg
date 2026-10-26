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

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Round-robin: every entrant faces every other entrant once. Rounds are generated using the circle
 * method so no entrant sits out more than once per round (for odd-sized groups).
 */
@Component
public class RoundRobinFormat implements TournamentFormat {

  public static final String FORMAT_ID = "ROUND_ROBIN";

  @Override
  public String getFormatId() {
    return FORMAT_ID;
  }

  @Override
  public String getDisplayName() {
    return "Round Robin";
  }

  @Override
  public int getMinEntrants() {
    return 3;
  }

  @Override
  public int getMaxEntrants() {
    return 16;
  }

  @Override
  public List<TournamentRound> generateBracket(Tournament tournament, TournamentFormatContext ctx) {
    List<TournamentEntry> entries = new ArrayList<>(tournament.getEntries());
    // Add a bye entry if odd number
    if (entries.size() % 2 != 0) {
      entries.add(null);
    }
    int n = entries.size();
    int totalRounds = n - 1;
    List<TournamentRound> rounds = new ArrayList<>();

    for (int r = 0; r < totalRounds; r++) {
      TournamentRound round =
          TournamentRound.builder()
              .tournament(tournament)
              .roundNumber(r + 1)
              .roundName("Round " + (r + 1))
              .status(TournamentRoundStatus.PENDING)
              .build();
      round = ctx.getRoundRepository().save(round);

      List<TournamentMatch> matches = new ArrayList<>();
      for (int i = 0; i < n / 2; i++) {
        TournamentEntry e1 = entries.get(i);
        TournamentEntry e2 = entries.get(n - 1 - i);
        if (e1 != null && e2 != null) {
          matches.add(TournamentMatch.builder().round(round).entrant1(e1).entrant2(e2).build());
        }
      }
      ctx.getMatchRepository().saveAll(matches);
      round.setMatches(matches);
      rounds.add(round);

      // Rotate: fix first element, rotate the rest
      TournamentEntry last = entries.remove(n - 1);
      entries.add(1, last);
    }

    return rounds;
  }

  @Override
  public List<TournamentMatch> advanceRound(Tournament tournament, TournamentFormatContext ctx) {
    // All rounds are pre-generated in generateBracket; nothing to advance
    return List.of();
  }

  @Override
  public boolean isComplete(Tournament tournament) {
    return tournament.getRounds().stream()
        .allMatch(r -> r.getStatus() == TournamentRoundStatus.COMPLETE);
  }

  @Override
  public int estimateTotalMatches(Tournament tournament) {
    int entrants = tournament.getEntries().size();
    // Each round-robin round pairs N/2 entrants across N-1 rounds → N*(N-1)/2 matches.
    return entrants < 2 ? 0 : entrants * (entrants - 1) / 2;
  }

  @Override
  public boolean eliminatesLosers() {
    return false;
  }

  /**
   * Standings over the decided matches on hand: points = 2·W + 1·D (ATW-2aq3). Ties resolve
   * head-to-head (points earned in matches among the tied group only), then by lower seed number.
   * Undecided matches count to nobody.
   */
  @Override
  public List<TournamentFormat.Standing> standings(Tournament tournament) {
    List<TournamentEntry> entries = tournament.getEntries();
    if (entries.isEmpty()) {
      return List.of();
    }

    Map<TournamentEntry, int[]> record = new HashMap<>(); // {wins, draws, losses}
    List<TournamentMatch> decidedMatches = new ArrayList<>();
    for (TournamentRound round : tournament.getRounds()) {
      for (TournamentMatch match : round.getMatches()) {
        if (match.getWinner() == null) {
          continue;
        }
        decidedMatches.add(match);
        List<TournamentEntry> entrants = match.entrants();
        for (TournamentEntry entrant : entrants) {
          record.putIfAbsent(entrant, new int[3]);
        }
        if (match.isMultiEntrant()) {
          // Free-for-All shape (ATW-oloa): the winner wins; everyone else — the current
          // model has no mid-pack placements — loses.
          record.get(match.getWinner())[0]++;
          for (TournamentEntry entrant : entrants) {
            if (!entrant.equals(match.getWinner())) {
              record.get(entrant)[2]++;
            }
          }
        } else {
          record.get(match.getWinner())[0]++;
          TournamentEntry loser =
              entrants.get(0).equals(match.getWinner()) ? entrants.get(1) : entrants.get(0);
          record.get(loser)[2]++;
        }
      }
    }
    for (TournamentEntry entry : entries) {
      record.putIfAbsent(entry, new int[3]);
    }

    // Points and seed order first — head-to-head only separates equal-point groups.
    Comparator<TournamentEntry> byPointsThenSeed =
        Comparator.comparingInt((TournamentEntry e) -> -pointsOf(record.get(e)))
            .thenComparingInt(TournamentEntry::getSeed);
    List<TournamentEntry> ordered = new ArrayList<>(entries);
    ordered.sort(byPointsThenSeed);

    // Walk the ordered list in equal-point groups; resolve each group's internal order by
    // head-to-head points among its members, falling back to seed.
    List<TournamentFormat.Standing> standings = new ArrayList<>();
    int i = 0;
    int rank = 1;
    while (i < ordered.size()) {
      int points = pointsOf(record.get(ordered.get(i)));
      int j = i;
      while (j < ordered.size() && pointsOf(record.get(ordered.get(j))) == points) {
        j++;
      }
      List<TournamentEntry> group = ordered.subList(i, j);
      Comparator<TournamentEntry> byH2hThenSeed =
          Comparator.comparingInt((TournamentEntry e) -> -h2hPoints(e, group, decidedMatches))
              .thenComparingInt(TournamentEntry::getSeed);
      group.sort(byH2hThenSeed);
      for (TournamentEntry entry : group) {
        int[] r = record.get(entry);
        standings.add(
            new TournamentFormat.Standing(
                rank++,
                entry.getId(),
                entry.entrantName(),
                entry.getSeed(),
                r[0],
                r[1],
                r[2],
                pointsOf(r)));
      }
      i = j;
    }
    return standings;
  }

  /**
   * Head-to-head points {@code entry} earned against the other members of {@code group}: 2 per win
   * over a group member, 0 per loss or outsider result. Matches against outsiders don't count — the
   * tiebreak compares how the tied entrants fared against each other.
   */
  private int h2hPoints(
      TournamentEntry entry, List<TournamentEntry> group, List<TournamentMatch> decidedMatches) {
    Set<Long> groupIds = group.stream().map(TournamentEntry::getId).collect(Collectors.toSet());
    int h2h = 0;
    for (TournamentMatch match : decidedMatches) {
      List<TournamentEntry> entrants = match.entrants();
      if (!entrants.contains(entry)) {
        continue;
      }
      boolean againstGroupMember =
          entrants.stream().anyMatch(e -> !e.equals(entry) && groupIds.contains(e.getId()));
      if (againstGroupMember && match.getWinner().equals(entry)) {
        h2h += 2;
      }
    }
    return h2h;
  }

  private int pointsOf(int[] record) {
    return 2 * record[0] + record[1];
  }

  /**
   * The champion is the standings leader — the points (then head-to-head, then seed) winner. A
   * bracket with no decided match crowns nobody: an all-zero table has no leader.
   */
  @Override
  public Optional<TournamentEntry> selectWinner(Tournament tournament) {
    List<TournamentFormat.Standing> table = standings(tournament);
    if (table.isEmpty()) {
      return Optional.empty();
    }
    TournamentFormat.Standing leader = table.get(0);
    if (leader.wins() == 0 && leader.draws() == 0 && leader.losses() == 0) {
      return Optional.empty();
    }
    Long winnerId = leader.entryId();
    return tournament.getEntries().stream()
        .filter(e -> e.getId() != null && e.getId().equals(winnerId))
        .findFirst();
  }

  @Override
  public RenderMode renderMode() {
    return RenderMode.ROUND_ROBIN_GRID;
  }
}
