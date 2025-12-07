/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for automatically resolving segments between NPC wrestlers using the ATW RPG system's fan
 * weight and tier mechanics. Provides realistic segment outcomes based on wrestler statistics and
 * storyline considerations.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class NPCSegmentResolutionService {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private Clock clock;
  @Autowired protected Random random;

  /**
   * Resolve a team-based segment using ATW RPG mechanics. This is the core method that handles all
   * segment types: singles (1v1), tag team (2v2), handicap (1v2), and complex multi-team scenarios.
   *
   * @param team1 First team (can have 1+ members)
   * @param team2 Second team (can have 1+ members)
   * @param segmentType Type of segment
   * @param show Show where the segment takes place
   * @param stipulation Optional segment rule
   * @return Segment with determined winner and details
   */
  @Transactional
  public Segment resolveTeamSegment(
      @NonNull SegmentTeam team1,
      @NonNull SegmentTeam team2,
      @NonNull SegmentType segmentType,
      @NonNull Show show,
      String stipulation) {

    // Default to "Standard Match" if no rule provided
    String finalStipulation =
        (stipulation != null && !stipulation.trim().isEmpty()) ? stipulation : "Standard Match";

    log.info(
        "Resolving team segment: {} vs {} on show {} ({})",
        team1.getTeamName(),
        team2.getTeamName(),
        show.getName(),
        finalStipulation);

    // Calculate team statistics
    TeamStatsCalculator calculator = new TeamStatsCalculator();
    team1.calculateTeamStats(calculator);
    team2.calculateTeamStats(calculator);

    // Calculate win probabilities based on team stats
    TeamSegmentProbabilities probabilities = calculateTeamSegmentProbabilities(team1, team2);

    // Determine winning team using weighted random selection
    SegmentTeam winningTeam = determineWinningTeam(team1, team2, probabilities);

    // Create segment
    Segment result = new Segment();
    result.setShow(show);
    result.setSegmentType(segmentType);
    result.setSegmentDate(clock.instant());
    applySegmentRules(result, finalStipulation);
    result.setIsNpcGenerated(true);

    // Add all participants from both teams
    addTeamParticipants(result, team1);
    addTeamParticipants(result, team2);
    result.setWinners(winningTeam.getMembers());

    // Save and return
    Segment savedResult = segmentRepository.save(result);

    log.info(
        "Team segment resolved: {} defeated {} ({}% probability)",
        winningTeam.getTeamName(),
        winningTeam.equals(team1) ? team2.getTeamName() : team1.getTeamName(),
        winningTeam.equals(team1)
            ? probabilities.team1WinProbability()
            : probabilities.team2WinProbability());

    return savedResult;
  }

  /**
   * Resolve a multi-team segment (3+ teams) using ATW RPG mechanics. This handles complex scenarios
   * like triple threat tag matches, faction warfare, etc.
   *
   * @param teams List of teams participating (each team can have 1+ members)
   * @param segmentType Type of segment
   * @param show Show where the segment takes place
   * @param stipulation Optional segment rule
   * @return Segment with determined winner and details
   */
  @Transactional
  public Segment resolveMultiTeamSegment(
      @NonNull List<SegmentTeam> teams,
      @NonNull SegmentType segmentType,
      @NonNull Show show,
      String stipulation) {

    if (teams.size() < 3) {
      throw new IllegalArgumentException("Multi-team segment requires at least 3 teams");
    }

    // Default to "Standard Match" if no rule provided
    String finalStipulation =
        (stipulation != null && !stipulation.trim().isEmpty()) ? stipulation : "Standard Match";

    log.info(
        "Resolving {}-team segment on show {} ({}): {}",
        teams.size(),
        show.getName(),
        finalStipulation,
        teams.stream().map(SegmentTeam::getTeamName).toList());

    // Calculate team statistics
    TeamStatsCalculator calculator = new TeamStatsCalculator();
    teams.forEach(team -> team.calculateTeamStats(calculator));

    // Determine winning team using weighted random selection
    SegmentTeam winningTeam = determineMultiTeamWinner(teams);

    // Create segment
    Segment result = new Segment();
    result.setShow(show);
    result.setSegmentType(segmentType);
    result.setSegmentDate(clock.instant());
    applySegmentRules(result, finalStipulation);
    result.setIsNpcGenerated(true);

    // Add all participants from all teams
    for (SegmentTeam team : teams) {
      addTeamParticipants(result, team);
    }
    result.setWinners(winningTeam.getMembers());

    // Save and return
    Segment savedResult = segmentRepository.save(result);

    log.info(
        "Multi-team segment resolved: {} defeated {} other teams",
        winningTeam.getTeamName(),
        teams.size() - 1);

    return savedResult;
  }

  /** Get tier bonus for segment calculations. */
  private int getTierBonus(@NonNull WrestlerTier tier) {
    return switch (tier) {
      case ROOKIE -> 0;
      case RISER -> 4;
      case CONTENDER -> 8;
      case MIDCARDER -> 12;
      case MAIN_EVENTER -> 16;
      case ICON -> 20;
    };
  }

  /** Get health penalty based on bumps and injuries. */
  private int getHealthPenalty(@NonNull Wrestler wrestler) {
    int penalty = 0;

    // Bump penalties (each bump reduces effectiveness)
    penalty += wrestler.getBumps();

    // Injury penalties (active injuries significantly reduce effectiveness)
    long activeInjuries = wrestler.getInjuries().stream().filter(Injury::isCurrentlyActive).count();
    penalty += (int) activeInjuries * 3; // Each active injury = -3 penalty

    return penalty;
  }

  /** Add all team members as participants in the segment. */
  private void addTeamParticipants(@NonNull Segment result, @NonNull SegmentTeam team) {
    for (Wrestler wrestler : team.getMembers()) {
      wrestlerRepository.findById(wrestler.getId()).ifPresent(result::addParticipant);
    }
  }

  /** Calculate team segment probabilities based on combined team statistics. */
  private TeamSegmentProbabilities calculateTeamSegmentProbabilities(
      @NonNull SegmentTeam team1, @NonNull SegmentTeam team2) {
    int team1TotalWeight = team1.getTotalWeight();
    int team2TotalWeight = team2.getTotalWeight();
    log.debug("Team1 Total Weight: {}", team1TotalWeight);
    log.debug("Team2 Total Weight: {}", team2TotalWeight);
    int totalWeight = team1TotalWeight + team2TotalWeight;

    // Convert to percentages
    double team1Probability = (double) team1TotalWeight / totalWeight * 100;
    double team2Probability = (double) team2TotalWeight / totalWeight * 100;

    log.debug(
        "Team segment probabilities calculated - {}: {}% (TW:{}, ATB:{}, THP:{}), {}: {}% (TW:{},"
            + " ATB:{}, THP:{})",
        team1.getTeamName(),
        String.format("%.1f", team1Probability),
        team1TotalWeight,
        String.format("%.1f", team1.getAverageTierBonus()),
        team1.getTotalHealthPenalty(),
        team2.getTeamName(),
        String.format("%.1f", team2Probability),
        team2TotalWeight,
        String.format("%.1f", team2.getAverageTierBonus()),
        team2.getTotalHealthPenalty());

    return new TeamSegmentProbabilities(
        team1Probability, team2Probability, team1TotalWeight, team2TotalWeight);
  }

  /** Determine winning team using weighted random selection. */
  private SegmentTeam determineWinningTeam(
      @NonNull SegmentTeam team1,
      @NonNull SegmentTeam team2,
      @NonNull TeamSegmentProbabilities probabilities) {
    int totalWeight = probabilities.team1TotalWeight() + probabilities.team2TotalWeight();
    int randomValue = random.nextInt(totalWeight);

    if (randomValue < probabilities.team1TotalWeight()) {
      return team1;
    } else {
      return team2;
    }
  }

  /** Determine winning team from multiple teams using weighted random selection. */
  private SegmentTeam determineMultiTeamWinner(@NonNull List<SegmentTeam> teams) {
    // Calculate total weight across all teams
    int totalWeight = teams.stream().mapToInt(SegmentTeam::getTotalWeight).sum();

    // Generate random value
    double randomValue = random.nextDouble() * totalWeight;

    // Find winning team based on cumulative weights
    double cumulativeWeight = 0;
    for (SegmentTeam team : teams) {
      cumulativeWeight += team.getTotalWeight();
      if (randomValue <= cumulativeWeight) {
        return team;
      }
    }

    // Fallback (should never happen)
    return teams.get(0);
  }

  /** Apply segment rules to a segment based on rule string. */
  private void applySegmentRules(@NonNull Segment result, String stipulation) {
    if (stipulation == null
        || stipulation.trim().isEmpty()
        || "Standard Match".equals(stipulation)) {
      // No special rules for standard segments
      return;
    }

    // Try to find exact segment rule by rule name
    segmentRuleService.findByName(stipulation).ifPresent(result::addSegmentRule);

    // If no rules were applied and it's not a standard segment, log a warning
    if (result.getSegmentRules().isEmpty()) {
      log.warn("No segment rules found for rule: {}", stipulation);
    }
  }

  /** Calculate individual wrestler weight for multi-person segments. */
  private WrestlerWeight calculateWrestlerWeight(@NonNull Wrestler wrestler) {
    int fanWeight = wrestler.getFanWeight();
    int tierBonus = getTierBonus(wrestler.getTier());
    int healthPenalty = getHealthPenalty(wrestler);
    int totalWeight = Math.max(1, fanWeight + tierBonus - healthPenalty);

    return new WrestlerWeight(wrestler, totalWeight, fanWeight, tierBonus, healthPenalty);
  }

  /** Determine winner in multi-person segment using weighted random selection. */
  private Wrestler determineMultiPersonWinner(@NonNull List<WrestlerWeight> wrestlerWeights) {
    int totalWeight = wrestlerWeights.stream().mapToInt(WrestlerWeight::totalWeight).sum();
    int randomValue = random.nextInt(totalWeight);

    int currentWeight = 0;
    for (WrestlerWeight wrestlerWeight : wrestlerWeights) {
      currentWeight += wrestlerWeight.totalWeight();
      if (randomValue < currentWeight) {
        return wrestlerWeight.wrestler();
      }
    }

    // Fallback (should never happen)
    return wrestlerWeights.get(0).wrestler();
  }

  /** Data class for wrestler weight calculations. */
  public record WrestlerWeight(
      @NonNull Wrestler wrestler,
      int totalWeight,
      int fanWeight,
      int tierBonus,
      int healthPenalty) {}

  /** Data class for team segment probability calculations. */
  public record TeamSegmentProbabilities(
      double team1WinProbability,
      double team2WinProbability,
      int team1TotalWeight,
      int team2TotalWeight) {}

  /** Result data class for segment resolution. */
  public record SegmentResolutionResult(
      @NonNull Segment segment, @NonNull TeamSegmentProbabilities probabilities, String summary) {}

  /** Calculator for team statistics used in segment resolution. */
  public class TeamStatsCalculator {

    /** Calculate total team weight based on individual wrestler stats. */
    public int calculateTeamWeight(@NonNull SegmentTeam team) {
      return team.getMembers().stream()
          .mapToInt(
              wrestler -> {
                int fanWeight = wrestler.getFanWeight();
                int tierBonus = getTierBonus(wrestler.getTier());
                int healthPenalty = getHealthPenalty(wrestler);
                return Math.max(1, fanWeight + tierBonus - healthPenalty);
              })
          .sum();
    }

    /** Calculate average tier bonus for the team. */
    public double calculateAverageTierBonus(@NonNull SegmentTeam team) {
      return team.getMembers().stream()
          .mapToInt(wrestler -> getTierBonus(wrestler.getTier()))
          .average()
          .orElse(0.0);
    }

    /** Calculate total health penalty for the team. */
    public int calculateTeamHealthPenalty(@NonNull SegmentTeam team) {
      return team.getMembers().stream()
          .mapToInt(NPCSegmentResolutionService.this::getHealthPenalty)
          .sum();
    }
  }
}
