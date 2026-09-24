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
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SingleEliminationFormat implements TournamentFormat {

  public static final String FORMAT_ID = "SINGLE_ELIMINATION";

  @Override
  public String getFormatId() {
    return FORMAT_ID;
  }

  @Override
  public String getDisplayName() {
    return "Single Elimination";
  }

  @Override
  public int getMinEntrants() {
    return 4;
  }

  @Override
  public int getMaxEntrants() {
    return 64;
  }

  @Override
  public List<TournamentRound> generateBracket(Tournament tournament, TournamentFormatContext ctx) {
    List<TournamentEntry> seeds = tournament.getEntries();
    int totalRounds = (int) Math.ceil(Math.log(seeds.size()) / Math.log(2));
    List<TournamentRound> rounds = new ArrayList<>();

    // Only generate Round 1 matches upfront; subsequent rounds are generated on advancement
    TournamentRound round1 =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(1)
            .roundName(roundName(1, totalRounds))
            .status(TournamentRoundStatus.PENDING)
            .build();
    round1 = ctx.getRoundRepository().save(round1);

    // Pair top seed vs bottom seed (1 vs N, 2 vs N-1, ...)
    List<TournamentMatch> matches = new ArrayList<>();
    int n = seeds.size();
    for (int i = 0; i < n / 2; i++) {
      matches.add(
          TournamentMatch.builder()
              .round(round1)
              .entrant1(seeds.get(i))
              .entrant2(seeds.get(n - 1 - i))
              .build());
    }
    ctx.getMatchRepository().saveAll(matches);
    round1.setMatches(matches);
    rounds.add(round1);
    return rounds;
  }

  @Override
  public List<TournamentMatch> advanceRound(Tournament tournament, TournamentFormatContext ctx) {
    List<TournamentRound> allRounds =
        ctx.getRoundRepository().findByTournamentIdOrderByRoundNumberAsc(tournament.getId());

    TournamentRound lastComplete =
        allRounds.stream()
            .filter(r -> r.getStatus() == TournamentRoundStatus.COMPLETE)
            .reduce((a, b) -> b)
            .orElse(null);

    if (lastComplete == null) {
      return List.of();
    }

    List<TournamentEntry> winners =
        lastComplete.getMatches().stream().map(TournamentMatch::getWinner).toList();

    if (winners.size() == 1) {
      // Tournament complete
      return List.of();
    }

    int nextRoundNumber = lastComplete.getRoundNumber() + 1;
    int totalRounds = (int) Math.ceil(Math.log(tournament.getEntries().size()) / Math.log(2));

    TournamentRound nextRound =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(nextRoundNumber)
            .roundName(roundName(nextRoundNumber, totalRounds))
            .status(TournamentRoundStatus.PENDING)
            .build();
    nextRound = ctx.getRoundRepository().save(nextRound);

    List<TournamentMatch> matches = new ArrayList<>();
    for (int i = 0; i < winners.size(); i += 2) {
      matches.add(
          TournamentMatch.builder()
              .round(nextRound)
              .entrant1(winners.get(i))
              .entrant2(winners.get(i + 1))
              .build());
    }
    ctx.getMatchRepository().saveAll(matches);
    return matches;
  }

  @Override
  public boolean isComplete(Tournament tournament) {
    return tournament.getRounds().stream()
        .anyMatch(
            r ->
                r.getStatus() == TournamentRoundStatus.COMPLETE
                    && r.getMatches().size() == 1
                    && r.getMatches().get(0).getWinner() != null);
  }

  private String roundName(int round, int totalRounds) {
    int remaining = totalRounds - round;
    return switch (remaining) {
      case 0 -> "Final";
      case 1 -> "Semi-Final";
      case 2 -> "Quarter-Final";
      default -> "Round " + round;
    };
  }

  @Override
  public int estimateTotalMatches(Tournament tournament) {
    int entrants = tournament.getEntries().size();
    // A single-elimination bracket of N entrants holds exactly N-1 matches. Below the
    // format minimum the bracket cannot generate — report the configured minimum's count
    // so pacing still computes a plan (it will fail the eligibility pre-flight anyway).
    if (entrants < 2) {
      return 0;
    }
    return entrants - 1;
  }

  /**
   * Full bracket projection: round 1 pairs 1vN/2vN-1 from the seeds (overlaid with persisted
   * round-1 state by position); every later round is N/2^(r-1) "Winner of Match X/Y" placeholder
   * matches — N-1 matches total, the final a single 2-slot match, all rendered before the lazy
   * rounds generate.
   */
  @Override
  public Optional<BracketProjection> projectBracket(Tournament tournament) {
    List<TournamentEntry> seeds = tournament.getEntries();
    int n = seeds.size();
    if (n < 2) {
      return Optional.empty();
    }
    int totalRounds = (int) Math.ceil(Math.log(n) / Math.log(2));

    // Persisted round-1 matches for winner overlay (position-ordered, round 1 generates first).
    List<TournamentMatch> round1Real =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .map(TournamentRound::getMatches)
            .orElse(List.of());

    List<ProjectedMatch> matches = new ArrayList<>();
    int matchNumber = 1;
    for (int i = 0; i < n / 2; i++) {
      TournamentEntry s1 = seeds.get(i);
      TournamentEntry s2 = seeds.get(n - 1 - i);
      TournamentMatch real = i < round1Real.size() ? round1Real.get(i) : null;
      matches.add(
          new ProjectedMatch(
              matchNumber++,
              1,
              List.of(
                  new ProjectedSlot(wrestlerNameOf(s1), wrestlerIdOf(s1), null, false),
                  new ProjectedSlot(wrestlerNameOf(s2), wrestlerIdOf(s2), null, false)),
              winnerIdOf(real),
              winnerNameOf(real)));
    }

    // Later rounds: each match pairs the winners of the previous round's consecutive matches.
    // Match numbers are global 1-based, so round r's match j references (r-1)'s matches
    // 2j-1 and 2j by number. A decided source match propagates its winner (advancing slot);
    // an undecided one keeps the "Winner of Match N" placeholder.
    int prevRoundFirst = 1;
    int prevRoundCount = n / 2;
    for (int round = 2; round <= totalRounds; round++) {
      for (int j = 0; j < prevRoundCount / 2; j++) {
        int firstSource = prevRoundFirst + 2 * j;
        matches.add(
            new ProjectedMatch(
                matchNumber++,
                round,
                List.of(
                    slotFromSource(matches, firstSource), slotFromSource(matches, firstSource + 1)),
                null,
                null));
      }
      prevRoundFirst = matchNumber - prevRoundCount / 2;
      prevRoundCount /= 2;
    }

    List<String> roundNames = new ArrayList<>();
    for (int round = 1; round <= totalRounds; round++) {
      roundNames.add(roundName(round, totalRounds));
    }
    return Optional.of(new BracketProjection(roundNames, matches));
  }

  private String wrestlerNameOf(TournamentEntry entry) {
    return entry != null && entry.getWrestler() != null ? entry.getWrestler().getName() : null;
  }

  /**
   * The slot for "winner of source match": resolved to the source's decided winner (advancing) —
   * round-1 matches overlay persisted state, so their winners propagate too.
   */
  private ProjectedSlot slotFromSource(List<ProjectedMatch> built, int sourceMatchNumber) {
    return built.stream()
        .filter(m -> m.matchNumber() == sourceMatchNumber)
        .findFirst()
        .map(
            src ->
                src.decidedWinnerId() != null
                    ? new ProjectedSlot(
                        src.decidedWinnerName(), src.decidedWinnerId(), sourceMatchNumber, true)
                    : new ProjectedSlot(null, null, sourceMatchNumber, false))
        .orElse(new ProjectedSlot(null, null, sourceMatchNumber, false));
  }

  private Long wrestlerIdOf(TournamentEntry entry) {
    return entry != null && entry.getWrestler() != null ? entry.getWrestler().getId() : null;
  }

  private Long winnerIdOf(TournamentMatch match) {
    return match != null && match.getWinner() != null && match.getWinner().getWrestler() != null
        ? match.getWinner().getWrestler().getId()
        : null;
  }

  private String winnerNameOf(TournamentMatch match) {
    return match != null && match.getWinner() != null && match.getWinner().getWrestler() != null
        ? match.getWinner().getWrestler().getName()
        : null;
  }
}
