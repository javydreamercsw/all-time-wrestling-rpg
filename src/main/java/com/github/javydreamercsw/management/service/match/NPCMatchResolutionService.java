package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
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
 * Service for automatically resolving matches between NPC wrestlers using the ATW RPG system's fan
 * weight and tier mechanics. Provides realistic match outcomes based on wrestler statistics and
 * storyline considerations.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class NPCMatchResolutionService {

  @Autowired private MatchResultRepository matchResultRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private MatchRuleService matchRuleService;
  @Autowired private Clock clock;
  @Autowired private Random random;

  /**
   * Resolve a team-based match using ATW RPG mechanics. This is the core method that handles all
   * match types: singles (1v1), tag team (2v2), handicap (1v2), and complex multi-team scenarios.
   *
   * @param team1 First team (can have 1+ members)
   * @param team2 Second team (can have 1+ members)
   * @param matchType Type of match
   * @param show Show where the match takes place
   * @param stipulation Optional match stipulation
   * @return MatchResult with determined winner and details
   */
  @Transactional
  public MatchResult resolveTeamMatch(
      @NonNull MatchTeam team1,
      @NonNull MatchTeam team2,
      @NonNull MatchType matchType,
      @NonNull Show show,
      String stipulation) {

    // Default to "Standard Match" if no stipulation provided
    String finalStipulation =
        (stipulation != null && !stipulation.trim().isEmpty()) ? stipulation : "Standard Match";

    log.info(
        "Resolving team match: {} vs {} on show {} ({})",
        team1.getTeamName(),
        team2.getTeamName(),
        show.getName(),
        finalStipulation);

    // Refresh all wrestlers from database to ensure lazy collections are loaded
    MatchTeam freshTeam1 = refreshTeam(team1);
    MatchTeam freshTeam2 = refreshTeam(team2);

    // Calculate team statistics
    TeamStatsCalculator calculator = new TeamStatsCalculator();
    freshTeam1.calculateTeamStats(calculator);
    freshTeam2.calculateTeamStats(calculator);

    // Calculate win probabilities based on team stats
    TeamMatchProbabilities probabilities = calculateTeamMatchProbabilities(freshTeam1, freshTeam2);

    // Determine winning team using weighted random selection
    MatchTeam winningTeam = determineWinningTeam(freshTeam1, freshTeam2, probabilities);

    // Create match result
    MatchResult result = new MatchResult();
    result.setShow(show);
    result.setMatchType(matchType);
    result.setWinner(winningTeam.getPrimaryWrestler()); // Use primary wrestler as winner
    result.setMatchDate(clock.instant());
    applyMatchRules(result, finalStipulation);
    result.setIsNpcGenerated(true);

    // Add all participants from both teams
    addTeamParticipants(result, freshTeam1, winningTeam.equals(freshTeam1));
    addTeamParticipants(result, freshTeam2, winningTeam.equals(freshTeam2));

    // Generate match details based on team composition
    generateTeamMatchDetails(result, freshTeam1, freshTeam2, probabilities);

    // Save and return
    MatchResult savedResult = matchResultRepository.save(result);

    log.info(
        "Team match resolved: {} defeated {} ({}% probability)",
        winningTeam.getTeamName(),
        winningTeam.equals(freshTeam1) ? freshTeam2.getTeamName() : freshTeam1.getTeamName(),
        winningTeam.equals(freshTeam1)
            ? probabilities.team1WinProbability()
            : probabilities.team2WinProbability());

    return savedResult;
  }

  /**
   * Resolve a multi-team match (3+ teams) using ATW RPG mechanics. This handles complex scenarios
   * like triple threat tag matches, faction warfare, etc.
   *
   * @param teams List of teams participating (each team can have 1+ members)
   * @param matchType Type of match
   * @param show Show where the match takes place
   * @param stipulation Optional match stipulation
   * @return MatchResult with determined winner and details
   */
  @Transactional
  public MatchResult resolveMultiTeamMatch(
      @NonNull List<MatchTeam> teams,
      @NonNull MatchType matchType,
      @NonNull Show show,
      String stipulation) {

    if (teams.size() < 3) {
      throw new IllegalArgumentException("Multi-team match requires at least 3 teams");
    }

    // Default to "Standard Match" if no stipulation provided
    String finalStipulation =
        (stipulation != null && !stipulation.trim().isEmpty()) ? stipulation : "Standard Match";

    log.info(
        "Resolving {}-team match on show {} ({}): {}",
        teams.size(),
        show.getName(),
        finalStipulation,
        teams.stream().map(MatchTeam::getTeamName).toList());

    // Refresh all teams from database
    List<MatchTeam> freshTeams = teams.stream().map(this::refreshTeam).toList();

    // Calculate team statistics
    TeamStatsCalculator calculator = new TeamStatsCalculator();
    freshTeams.forEach(team -> team.calculateTeamStats(calculator));

    // Determine winning team using weighted random selection
    MatchTeam winningTeam = determineMultiTeamWinner(freshTeams);

    // Create match result
    MatchResult result = new MatchResult();
    result.setShow(show);
    result.setMatchType(matchType);
    result.setWinner(winningTeam.getPrimaryWrestler());
    result.setMatchDate(clock.instant());
    applyMatchRules(result, finalStipulation);
    result.setIsNpcGenerated(true);

    // Add all participants from all teams
    for (MatchTeam team : freshTeams) {
      addTeamParticipants(result, team, team.equals(winningTeam));
    }

    // Generate match details based on team composition
    generateMultiTeamMatchDetails(result, freshTeams);

    // Save and return
    MatchResult savedResult = matchResultRepository.save(result);

    log.info(
        "Multi-team match resolved: {} defeated {} other teams",
        winningTeam.getTeamName(),
        freshTeams.size() - 1);

    return savedResult;
  }

  /** Get tier bonus for match calculations. */
  private int getTierBonus(@NonNull WrestlerTier tier) {
    return switch (tier) {
      case ROOKIE -> 0;
      case RISER -> 2;
      case CONTENDER -> 4;
      case INTERTEMPORAL_TIER -> 6;
      case MAIN_EVENTER -> 8;
      case ICON -> 10;
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

  /** Refresh a team by reloading all wrestlers from database. */
  private MatchTeam refreshTeam(@NonNull MatchTeam team) {
    List<Wrestler> refreshedWrestlers =
        team.getMembers().stream()
            .map(w -> wrestlerRepository.findById(w.getId()).orElse(w))
            .toList();
    return new MatchTeam(refreshedWrestlers, team.getTeamName());
  }

  /** Add all team members as participants in the match result. */
  private void addTeamParticipants(
      @NonNull MatchResult result, @NonNull MatchTeam team, boolean isWinningTeam) {
    for (Wrestler wrestler : team.getMembers()) {
      result.addParticipant(wrestler, isWinningTeam);
    }
  }

  /** Calculate team match probabilities based on combined team statistics. */
  private TeamMatchProbabilities calculateTeamMatchProbabilities(
      @NonNull MatchTeam team1, @NonNull MatchTeam team2) {
    int team1TotalWeight = team1.getTotalWeight();
    int team2TotalWeight = team2.getTotalWeight();
    int totalWeight = team1TotalWeight + team2TotalWeight;

    // Convert to percentages
    double team1Probability = (double) team1TotalWeight / totalWeight * 100;
    double team2Probability = (double) team2TotalWeight / totalWeight * 100;

    log.debug(
        "Team match probabilities calculated - {}: {}% (TW:{}, ATB:{}, THP:{}), {}: {}% (TW:{},"
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

    return new TeamMatchProbabilities(
        team1Probability, team2Probability, team1TotalWeight, team2TotalWeight);
  }

  /** Determine winning team using weighted random selection. */
  private MatchTeam determineWinningTeam(
      @NonNull MatchTeam team1,
      @NonNull MatchTeam team2,
      @NonNull TeamMatchProbabilities probabilities) {
    double randomValue = random.nextDouble() * 100;
    return randomValue < probabilities.team1WinProbability() ? team1 : team2;
  }

  /** Determine winning team from multiple teams using weighted random selection. */
  private MatchTeam determineMultiTeamWinner(@NonNull List<MatchTeam> teams) {
    // Calculate total weight across all teams
    int totalWeight = teams.stream().mapToInt(MatchTeam::getTotalWeight).sum();

    // Generate random value
    double randomValue = random.nextDouble() * totalWeight;

    // Find winning team based on cumulative weights
    double cumulativeWeight = 0;
    for (MatchTeam team : teams) {
      cumulativeWeight += team.getTotalWeight();
      if (randomValue <= cumulativeWeight) {
        return team;
      }
    }

    // Fallback (should never happen)
    return teams.get(0);
  }

  /** Generate match details based on team composition and statistics. */
  private void generateTeamMatchDetails(
      @NonNull MatchResult result,
      @NonNull MatchTeam team1,
      @NonNull MatchTeam team2,
      @NonNull TeamMatchProbabilities probabilities) {
    // Calculate base duration based on team sizes
    int totalParticipants = team1.getSize() + team2.getSize();
    int baseDuration = 8 + random.nextInt(12); // 8-19 minutes base

    // Add time for team complexity
    int teamComplexityBonus =
        Math.max(0, totalParticipants - 2) * 2; // +2 minutes per extra participant

    // Add time for tier level
    int avgTierBonus = (int) ((team1.getAverageTierBonus() + team2.getAverageTierBonus()) / 2);
    int tierTimeBonus = avgTierBonus / 2;

    int duration = Math.min(35, baseDuration + teamComplexityBonus + tierTimeBonus);
    result.setDurationMinutes(duration);

    // Generate match rating based on combined talent and team dynamics
    int baseRating = 2 + random.nextInt(2); // 2-3 base rating
    int talentBonus = avgTierBonus / 3;
    int teamDynamicsBonus =
        totalParticipants > 2 ? 1 : 0; // Multi-person matches can be more exciting

    int rating = Math.min(5, baseRating + talentBonus + teamDynamicsBonus);
    result.setMatchRating(rating);
  }

  /** Generate match details for multi-team matches. */
  private void generateMultiTeamMatchDetails(
      @NonNull MatchResult result, @NonNull List<MatchTeam> teams) {
    // Calculate total participants across all teams
    int totalParticipants = teams.stream().mapToInt(MatchTeam::getSize).sum();

    // Base duration increases with complexity
    int baseDuration = 10 + random.nextInt(15); // 10-24 minutes base
    int complexityBonus =
        Math.max(0, totalParticipants - 3) * 2; // +2 minutes per extra participant
    int teamCountBonus = Math.max(0, teams.size() - 2) * 3; // +3 minutes per extra team

    // Calculate average tier bonus across all teams
    double avgTierBonus =
        teams.stream().mapToDouble(MatchTeam::getAverageTierBonus).average().orElse(0.0);
    int tierTimeBonus = (int) (avgTierBonus / 2);

    int duration = Math.min(45, baseDuration + complexityBonus + teamCountBonus + tierTimeBonus);
    result.setDurationMinutes(duration);

    // Generate match rating based on complexity and talent
    int baseRating = 2 + random.nextInt(2); // 2-3 base rating
    int talentBonus = (int) (avgTierBonus / 3);
    int complexityRatingBonus =
        totalParticipants > 4 ? 1 : 0; // Multi-team matches can be more exciting

    int rating = Math.min(5, baseRating + talentBonus + complexityRatingBonus);
    result.setMatchRating(rating);
  }

  /** Apply match rules to a match result based on stipulation string. */
  private void applyMatchRules(@NonNull MatchResult result, String stipulation) {
    if (stipulation == null
        || stipulation.trim().isEmpty()
        || "Standard Match".equals(stipulation)) {
      // No special rules for standard matches
      return;
    }

    // Try to find exact match rule by stipulation name
    matchRuleService.findByName(stipulation).ifPresent(result::addMatchRule);

    // If no rules were applied and it's not a standard match, log a warning
    if (result.getMatchRules().isEmpty()) {
      log.warn("No match rules found for stipulation: {}", stipulation);
    }
  }

  /** Calculate individual wrestler weight for multi-person matches. */
  private WrestlerWeight calculateWrestlerWeight(@NonNull Wrestler wrestler) {
    int fanWeight = wrestler.getFanWeight();
    int tierBonus = getTierBonus(wrestler.getTier());
    int healthPenalty = getHealthPenalty(wrestler);
    int totalWeight = Math.max(1, fanWeight + tierBonus - healthPenalty);

    return new WrestlerWeight(wrestler, totalWeight, fanWeight, tierBonus, healthPenalty);
  }

  /** Determine winner in multi-person match using weighted random selection. */
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

  /** Generate match details for multi-person matches. */
  private void generateMultiPersonMatchDetails(
      @NonNull MatchResult result, @NonNull List<WrestlerWeight> wrestlerWeights) {
    // Multi-person matches tend to be longer and more chaotic
    int baseDuration = 12 + random.nextInt(15); // 12-26 minutes base
    int maxTierBonus = wrestlerWeights.stream().mapToInt(WrestlerWeight::tierBonus).max().orElse(0);
    int duration = Math.min(30, baseDuration + maxTierBonus / 2);
    result.setDurationMinutes(duration);

    // Rating influenced by overall talent level
    int avgTierBonus =
        (int) wrestlerWeights.stream().mapToInt(WrestlerWeight::tierBonus).average().orElse(0);
    int baseRating = 2 + random.nextInt(2); // 2-3 base rating
    int rating = Math.min(5, baseRating + avgTierBonus / 3);
    result.setMatchRating(rating);
  }

  /** Data class for wrestler weight calculations. */
  public record WrestlerWeight(
      @NonNull Wrestler wrestler,
      int totalWeight,
      int fanWeight,
      int tierBonus,
      int healthPenalty) {}

  /** Data class for team match probability calculations. */
  public record TeamMatchProbabilities(
      double team1WinProbability,
      double team2WinProbability,
      int team1TotalWeight,
      int team2TotalWeight) {}

  /** Result data class for match resolution. */
  public record MatchResolutionResult(
      @NonNull MatchResult matchResult,
      @NonNull TeamMatchProbabilities probabilities,
      String summary) {}

  /** Calculator for team statistics used in match resolution. */
  public class TeamStatsCalculator {

    /** Calculate total team weight based on individual wrestler stats. */
    public int calculateTeamWeight(@NonNull MatchTeam team) {
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
    public double calculateAverageTierBonus(@NonNull MatchTeam team) {
      return team.getMembers().stream()
          .mapToInt(wrestler -> getTierBonus(wrestler.getTier()))
          .average()
          .orElse(0.0);
    }

    /** Calculate total health penalty for the team. */
    public int calculateTeamHealthPenalty(@NonNull MatchTeam team) {
      return team.getMembers().stream()
          .mapToInt(NPCMatchResolutionService.this::getHealthPenalty)
          .sum();
    }
  }
}
