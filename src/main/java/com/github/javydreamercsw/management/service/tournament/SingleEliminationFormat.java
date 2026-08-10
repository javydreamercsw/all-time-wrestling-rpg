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
}
