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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * Bridges the domain {@link Tournament} entity (already graph-initialized via {@code
 * TournamentService.findByIdWithDetails}) to {@link TournamentBracketModel}.
 */
public class TournamentEntityAdapter implements TournamentBracketModel {

  private final Tournament tournament;
  @Getter private final RenderMode renderMode;
  private final TournamentFormat format;

  public TournamentEntityAdapter(Tournament tournament, List<TournamentFormat> formats) {
    this.tournament = tournament;
    this.format =
        formats.stream()
            .filter(f -> f.getFormatId().equals(tournament.getFormatId()))
            .findFirst()
            .orElse(null);
    this.renderMode = format != null ? format.renderMode() : RenderMode.TREE;
  }

  @Override
  public int getTotalRounds() {
    return tournament.getRounds().size();
  }

  @Override
  public boolean isComplete() {
    // The format knows whether the bracket played through its final — lazy round generation
    // means "a decided match exists" is not the same as "the tournament is finished".
    return format != null && format.isComplete(tournament);
  }

  @Override
  public String getRoundName(int round) {
    return tournament.getRounds().stream()
        .filter(r -> r.getRoundNumber() == round)
        .findFirst()
        .map(r -> r.getRoundName())
        .orElse(null);
  }

  @Override
  public Optional<TournamentFormat.BracketProjection> getProjection() {
    return format != null ? format.projectBracket(tournament) : Optional.empty();
  }

  @Override
  public String getRoundTypeRuleLabel(int round) {
    // Mirror resolveRoundStipulation: booked segment rule → round fixedRule → pool → format
    // default — except the FINAL round, whose payoff books with the tournament's configured
    // payoff rule (e.g. TLC for the title final). The pool case picks randomly at booking time —
    // no rule claimed for it UNLESS the round's matches are already booked and agree.
    String roundRule = isFinalRound(round) ? null : bookedRuleOf(round);
    if (roundRule == null) {
      roundRule = roundRuleOf(round);
    }
    if (roundRule == null && isFinalRound(round)) {
      // The payoff rule IS the final's stipulation — pool random picks don't apply to it.
      roundRule =
          tournament.getPayoffSegmentRule() != null
              ? tournament.getPayoffSegmentRule().getName()
              : null;
    }
    if (roundRule == null && tournament.getAllowedRules().isEmpty()) {
      roundRule = format != null ? format.getDefaultRoundRuleName() : null;
    }
    return combine(typeNameOf(), roundRule);
  }

  /** True when {@code round} is the bracket's last round (the payoff/final). */
  private boolean isFinalRound(int round) {
    // The projection knows the FULL bracket shape (lazy final rounds have no persisted row):
    // 2 rounds when present, else the persisted rounds' max.
    return getProjection()
            .map(p -> p.roundNames().size())
            .orElseGet(
                () ->
                    tournament.getRounds().stream()
                        .mapToInt(TournamentRound::getRoundNumber)
                        .max()
                        .orElse(0))
        == round;
  }

  /**
   * The single rule shared by every booked segment in a round, or null (unbooked, or the segments
   * disagree — then per-match labels from {@link MatchModel#getTypeRuleLabel} still show each
   * match's actual rule).
   */
  private String bookedRuleOf(int round) {
    List<String> rules =
        tournament.getRounds().stream()
            .filter(r -> r.getRoundNumber() == round)
            .findFirst()
            .map(
                r ->
                    r.getMatches().stream()
                        .map(
                            m ->
                                m.getSegment() != null
                                        && m.getSegment().getSegmentRules() != null
                                        && !m.getSegment().getSegmentRules().isEmpty()
                                    ? m.getSegment().getSegmentRules().iterator().next().getName()
                                    : null)
                        .toList())
            .orElse(List.of());
    boolean anyBooked = rules.stream().anyMatch(r -> r != null && !r.isBlank());
    if (!anyBooked) {
      return null;
    }
    String first = rules.isEmpty() ? null : rules.get(0);
    boolean allSame = rules.stream().allMatch(r -> Objects.equals(r, first));
    return allBooked(rules) && allSame ? first : null;
  }

  /** True when every entry is a booked (non-null, non-blank) rule. */
  private static boolean allBooked(List<String> rules) {
    return rules.stream().allMatch(r -> r != null && !r.isBlank());
  }

  @Override
  public int getCurrentRound() {
    Optional<Integer> inProgress =
        tournament.getRounds().stream()
            .filter(r -> r.getStatus() == TournamentRoundStatus.IN_PROGRESS)
            .map(r -> r.getRoundNumber())
            .findFirst();
    return inProgress.orElseGet(
        () ->
            tournament.getRounds().stream()
                .filter(r -> r.getStatus() == TournamentRoundStatus.COMPLETE)
                .mapToInt(r -> r.getRoundNumber())
                .max()
                .orElse(1));
  }

  @Override
  public List<MatchModel> getMatches() {
    return tournament.getRounds().stream()
        .flatMap(
            round ->
                round.getMatches().stream()
                    .map(
                        m ->
                            new EntityMatchModel(
                                round.getRoundNumber(),
                                m,
                                typeNameOf(),
                                // The pool case is decided at booking time — no label until then.
                                round.getFixedRule() != null
                                        || isFinalRound(round.getRoundNumber())
                                        || tournament.getAllowedRules().isEmpty()
                                    ? effectiveDefaultRule(round)
                                    : null)))
        .map(m -> (MatchModel) m)
        .toList();
  }

  /**
   * The rule a round's matches resolve to BEFORE booking, mirroring {@code
   * resolveRoundStipulation}: the round's fixed rule, else the tournament's payoff rule for the
   * final, else (pool empty only) the format's default.
   */
  private String effectiveDefaultRule(TournamentRound round) {
    if (round.getFixedRule() != null) {
      return round.getFixedRule().getName();
    }
    if (isFinalRound(round.getRoundNumber()) && tournament.getPayoffSegmentRule() != null) {
      return tournament.getPayoffSegmentRule().getName();
    }
    return format != null ? format.getDefaultRoundRuleName() : null;
  }

  /** A round's fixed rule name, or null. */
  private String roundRuleOf(int round) {
    return tournament.getRounds().stream()
        .filter(r -> r.getRoundNumber() == round)
        .findFirst()
        .map(r -> r.getFixedRule() != null ? r.getFixedRule().getName() : null)
        .orElse(null);
  }

  /** The format's round segment type resolved to its display name, or null. */
  private String typeNameOf() {
    if (format == null) {
      return null;
    }
    String type = format.getRoundSegmentTypeCode();
    return type == null || type.isBlank()
        ? null
        : WellKnownSegmentType.fromCode(type)
            .map(WellKnownSegmentType::getDisplayName)
            .orElse(null);
  }

  /** Joins the type and rule pieces; null/blank pieces collapse ("Type · Rule", "Type", "Rule"). */
  private static String combine(String typeName, String ruleName) {
    boolean hasType = typeName != null && !typeName.isBlank();
    boolean hasRule = ruleName != null && !ruleName.isBlank();
    if (hasType && hasRule) {
      return typeName + " · " + ruleName;
    }
    if (hasType) {
      return typeName;
    }
    return hasRule ? ruleName : null;
  }

  private record EntityMatchModel(
      int roundNumber, TournamentMatch match, String typeName, String fallbackRule)
      implements MatchModel {

    @Override
    public int getRound() {
      return roundNumber;
    }

    @Override
    public Long getWrestler1Id() {
      return match.getEntrant1().getWrestler().getId();
    }

    @Override
    public String getWrestler1Name() {
      return match.getEntrant1().getWrestler().getName();
    }

    @Override
    public Long getWrestler2Id() {
      return match.getEntrant2().getWrestler().getId();
    }

    @Override
    public String getWrestler2Name() {
      return match.getEntrant2().getWrestler().getName();
    }

    @Override
    public Long getWinnerId() {
      return match.getWinner() != null ? match.getWinner().getWrestler().getId() : null;
    }

    @Override
    public List<ExtraEntrant> getExtraEntrants() {
      if (!match.isMultiEntrant()) {
        return List.of();
      }
      // Slots 0 and 1 render through the classic two lines; slot 2+ land here.
      return match.entrants().stream()
          .skip(2)
          .map(e -> new ExtraEntrant(e.getWrestler().getName(), e.getWrestler().getId()))
          .toList();
    }

    @Override
    public String getTypeRuleLabel() {
      // A booked match's segment carries the rule actually applied when it was scheduled — show
      // that; unbooked matches fall back to the round-resolved rule passed from the round row.
      String booked = null;
      if (match.getSegment() != null
          && match.getSegment().getSegmentRules() != null
          && !match.getSegment().getSegmentRules().isEmpty()) {
        booked = match.getSegment().getSegmentRules().iterator().next().getName();
      }
      return combine(typeName, booked != null ? booked : fallbackRule);
    }

    @Override
    public boolean isPlayerMatch() {
      return false;
    }
  }
}
