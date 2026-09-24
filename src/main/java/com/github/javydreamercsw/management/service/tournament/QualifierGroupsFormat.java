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

import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchParticipant;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Qualifier groups feeding a single multi-entrant final (ATW-oloa): N entrants split into
 * equal-sized Free-for-All qualifiers — every wrestler whose qualifier they win advances — and the
 * winners meet in one M-entrant final. The classic example: 12 entrants in four 3-man qualifiers →
 * a 4-man Free-for-All TLC title match at the payoff show.
 *
 * <p>Group size adapts to the entrant count: 4-5 entrants → one group per 2-3 wrestlers with the
 * best-fit split; larger fields aim for 3-wrestler groups (never below 2, never above 5). A
 * tournament may pin the group size explicitly ({@code qualifierGroupSize}) — e.g. 18 entrants at
 * size 6 → three 6-man qualifiers → a 3-man final. Every wrestler plays exactly one qualifier; only
 * group winners survive to the final.
 */
@Component
public class QualifierGroupsFormat implements TournamentFormat {

  public static final String FORMAT_ID = "QUALIFIER_GROUPS";

  /** Upper clamp for a tournament's configured qualifier group size. */
  public static final int MAX_GROUP_SIZE = 10;

  @Override
  public String getFormatId() {
    return FORMAT_ID;
  }

  @Override
  public String getDisplayName() {
    return "Qualifier Groups → Multi-Man Final";
  }

  @Override
  public int getMinEntrants() {
    return 4;
  }

  @Override
  public int getMaxEntrants() {
    return 25;
  }

  @Override
  public List<TournamentRound> generateBracket(Tournament tournament, TournamentFormatContext ctx) {
    List<TournamentEntry> seeds = tournament.getEntries();
    List<List<TournamentEntry>> groups = splitIntoGroups(tournament, seeds);

    TournamentRound qualifierRound =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.PENDING)
            .build();
    qualifierRound = ctx.getRoundRepository().save(qualifierRound);

    List<TournamentMatch> qualifiers = new ArrayList<>();
    for (List<TournamentEntry> group : groups) {
      qualifiers.add(buildMultiEntrantMatch(qualifierRound, group, ctx));
    }
    qualifierRound.setMatches(qualifiers);
    return List.of(qualifierRound);
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
    if (lastComplete == null || lastComplete.getRoundNumber() != 1) {
      // Only the qualifier round advances to the final; beyond that the tournament is done.
      return List.of();
    }

    List<TournamentEntry> winners =
        lastComplete.getMatches().stream().map(TournamentMatch::getWinner).toList();
    if (winners.size() < 2) {
      return List.of();
    }

    TournamentRound finalRound =
        TournamentRound.builder()
            .tournament(tournament)
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.PENDING)
            .build();
    finalRound = ctx.getRoundRepository().save(finalRound);

    TournamentMatch finalMatch = buildMultiEntrantMatch(finalRound, winners, ctx);
    finalRound.setMatches(List.of(finalMatch));
    return List.of(finalMatch);
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

  @Override
  public int estimateTotalMatches(Tournament tournament) {
    // One qualifier per group + one final = groups + 1. Groups derive from the same split the
    // bracket generation uses, so pacing matches reality without persisting anything.
    return splitIntoGroups(tournament, tournament.getEntries()).size() + 1;
  }

  @Override
  public String getRoundSegmentTypeCode() {
    return WellKnownSegmentType.FREE_FOR_ALL.getCode();
  }

  @Override
  public String getDefaultRoundRuleName() {
    return "Free-For-All"; // qualifier scrambles are No-DQ by convention
  }

  /**
   * Split seeds into groups: the tournament's {@code qualifierGroupSize} when set (validated at
   * creation — the bracket needs at least two groups), otherwise aim for 3-wrestler groups with a
   * balanced adjustment.
   */
  private List<List<TournamentEntry>> splitIntoGroups(
      Tournament tournament, List<TournamentEntry> seeds) {
    int n = seeds.size();
    Integer configured = tournament.getQualifierGroupSize();
    if (configured != null && n >= 2 * groupSizeOf(configured)) {
      return contiguousGroups(seeds, n / groupSizeOf(configured));
    }
    // Choose the group count whose sizes are most balanced: ceil(n/3) groups, then spread.
    int groupCount = Math.max(1, (int) Math.ceil(n / 3.0));
    return contiguousGroups(seeds, groupCount);
  }

  /** Clamped configured group size (2..10). */
  private int groupSizeOf(int configured) {
    return Math.max(2, Math.min(configured, MAX_GROUP_SIZE));
  }

  /** Split seeds into {@code groupCount} groups, snake-seeding the strongest across them. */
  private List<List<TournamentEntry>> contiguousGroups(
      List<TournamentEntry> seeds, int groupCount) {
    int n = seeds.size();
    int base = n / groupCount;
    int remainder = n % groupCount;
    List<List<TournamentEntry>> groups = new ArrayList<>();
    int index = 0;
    for (int g = 0; g < groupCount; g++) {
      int size = base + (g < remainder ? 1 : 0);
      groups.add(new ArrayList<>(seeds.subList(index, index + size)));
      index += size;
    }
    return groups;
  }

  private TournamentMatch buildMultiEntrantMatch(
      TournamentRound round, List<TournamentEntry> entrants, TournamentFormatContext ctx) {
    // The classic columns need two entrants; mirror slots 0 and 1 (every multi-entrant match
    // here has at least two by construction — groups never split below 2, the final needs 2+).
    TournamentMatch match =
        TournamentMatch.builder()
            .round(round)
            .entrant1(entrants.get(0))
            .entrant2(entrants.get(1))
            .build();
    match = ctx.getMatchRepository().save(match);
    List<TournamentMatchParticipant> participants = new ArrayList<>();
    for (int i = 0; i < entrants.size(); i++) {
      participants.add(
          TournamentMatchParticipant.builder().match(match).entry(entrants.get(i)).slot(i).build());
    }
    match.getParticipants().clear();
    match.getParticipants().addAll(participants);
    ctx.getMatchRepository().save(match);
    return match;
  }
}
