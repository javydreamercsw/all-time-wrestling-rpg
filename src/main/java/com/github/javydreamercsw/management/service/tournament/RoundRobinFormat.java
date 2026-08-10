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
}
