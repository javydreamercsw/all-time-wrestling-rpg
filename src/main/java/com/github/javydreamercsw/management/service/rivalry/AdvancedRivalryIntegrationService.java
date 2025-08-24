package com.github.javydreamercsw.management.service.rivalry;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.storyline.StorylineBranch;
import com.github.javydreamercsw.management.domain.storyline.StorylineBranchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.storyline.StorylineBranchingService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration service that coordinates all advanced rivalry features. Handles interactions between
 * individual rivalries, faction rivalries, multi-wrestler feuds, and storyline branching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdvancedRivalryIntegrationService {

  private final RivalryService rivalryService;
  private final FactionService factionService;
  private final FactionRivalryService factionRivalryService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final StorylineBranchingService storylineBranchingService;

  /**
   * Process a match result and trigger all relevant rivalry systems. This is the main entry point
   * for match outcome processing.
   */
  public void processMatchOutcome(MatchResult matchResult) {
    log.info(
        "Processing match outcome for advanced rivalry systems: Match ID {}", matchResult.getId());

    // Process individual wrestler rivalries
    processIndividualRivalries(matchResult);

    // Process faction rivalries
    processFactionRivalries(matchResult);

    // Process multi-wrestler feuds
    processMultiWrestlerFeuds(matchResult);

    // Process storyline branching
    storylineBranchingService.processMatchOutcome(matchResult);

    log.info("Completed processing match outcome for all rivalry systems");
  }

  /** Create a complex storyline involving multiple rivalry types. */
  public ComplexStorylineResult createComplexStoryline(
      String storylineName, List<Long> wrestlerIds, String description) {
    log.info("Creating complex storyline: {} with {} wrestlers", storylineName, wrestlerIds.size());

    ComplexStorylineResult result = new ComplexStorylineResult();

    // If 2 wrestlers, create individual rivalry
    if (wrestlerIds.size() == 2) {
      Optional<Rivalry> rivalry =
          rivalryService.createRivalry(wrestlerIds.get(0), wrestlerIds.get(1), description);
      rivalry.ifPresent(
          r -> {
            result.individualRivalry = r;
            log.info("Created individual rivalry: {}", r.getId());
          });
    }

    // If 3+ wrestlers, create multi-wrestler feud
    if (wrestlerIds.size() >= 3) {
      Optional<MultiWrestlerFeud> feud =
          multiWrestlerFeudService.createFeud(storylineName, description, "Complex storyline");

      if (feud.isPresent()) {
        result.multiWrestlerFeud = feud.get();

        // Add participants with roles
        for (int i = 0; i < wrestlerIds.size(); i++) {
          FeudRole role = determineFeudRole(i, wrestlerIds.size());
          multiWrestlerFeudService.addParticipant(feud.get().getId(), wrestlerIds.get(i), role);
        }

        log.info(
            "Created multi-wrestler feud: {} with {} participants",
            feud.get().getName(),
            wrestlerIds.size());
      }
    }

    // Check for faction involvement
    processFactionInvolvement(wrestlerIds, result);

    // Create storyline branches for future developments
    createStorylineBranches(storylineName, result);

    return result;
  }

  /** Escalate a rivalry to the next level based on heat and circumstances. */
  public RivalryEscalationResult escalateRivalry(Long rivalryId, String escalationReason) {
    Optional<Rivalry> rivalryOpt = rivalryService.getRivalryById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return new RivalryEscalationResult(false, "Rivalry not found", null);
    }

    Rivalry rivalry = rivalryOpt.get();
    RivalryEscalationResult result = new RivalryEscalationResult();
    result.originalRivalry = rivalry;

    // Check if wrestlers are in factions
    Optional<Faction> faction1 =
        factionService.getFactionForWrestler(rivalry.getWrestler1().getId());
    Optional<Faction> faction2 =
        factionService.getFactionForWrestler(rivalry.getWrestler2().getId());

    if (faction1.isPresent() && faction2.isPresent() && !faction1.get().equals(faction2.get())) {
      // Escalate to faction rivalry
      Optional<FactionRivalry> factionRivalry =
          factionRivalryService.addHeatBetweenFactions(
              faction1.get().getId(),
              faction2.get().getId(),
              rivalry.getHeat() / 2, // Transfer some heat to faction rivalry
              "Escalated from individual rivalry: " + escalationReason);

      if (factionRivalry.isPresent()) {
        result.factionRivalry = factionRivalry.get();
        result.escalated = true;
        result.escalationType = "FACTION_RIVALRY";

        log.info(
            "Escalated rivalry {} to faction rivalry between {} and {}",
            rivalryId,
            faction1.get().getName(),
            faction2.get().getName());
      }
    } else if (rivalry.getHeat() >= 30) {
      // Create multi-wrestler feud if heat is very high
      String feudName =
          rivalry.getWrestler1().getName() + " vs " + rivalry.getWrestler2().getName() + " Feud";
      Optional<MultiWrestlerFeud> feud =
          multiWrestlerFeudService.createFeud(
              feudName, "Escalated from high-heat rivalry", escalationReason);

      if (feud.isPresent()) {
        multiWrestlerFeudService.addParticipant(
            feud.get().getId(), rivalry.getWrestler1().getId(), FeudRole.ANTAGONIST);
        multiWrestlerFeudService.addParticipant(
            feud.get().getId(), rivalry.getWrestler2().getId(), FeudRole.PROTAGONIST);

        result.multiWrestlerFeud = feud.get();
        result.escalated = true;
        result.escalationType = "MULTI_WRESTLER_FEUD";

        log.info(
            "Escalated rivalry {} to multi-wrestler feud: {}", rivalryId, feud.get().getName());
      }
    }

    if (!result.escalated) {
      result.message = "Rivalry conditions not met for escalation";
    } else {
      result.message = "Rivalry successfully escalated to " + result.escalationType;
    }

    return result;
  }

  /** Get comprehensive rivalry overview for a wrestler. */
  public WrestlerRivalryOverview getWrestlerRivalryOverview(Long wrestlerId) {
    WrestlerRivalryOverview overview = new WrestlerRivalryOverview();

    // Individual rivalries
    overview.individualRivalries = rivalryService.getRivalriesForWrestler(wrestlerId);

    // Faction rivalries (if wrestler is in a faction)
    Optional<Faction> faction = factionService.getFactionForWrestler(wrestlerId);
    if (faction.isPresent()) {
      overview.faction = faction.get();
      overview.factionRivalries =
          factionRivalryService.getActiveRivalriesForFaction(faction.get().getId());
    }

    // Multi-wrestler feuds
    overview.multiWrestlerFeuds = multiWrestlerFeudService.getActiveFeudsForWrestler(wrestlerId);

    // Active storyline branches
    overview.activeStorylineBranches =
        storylineBranchingService.getActiveBranches().stream()
            .filter(branch -> isWrestlerInvolvedInBranch(wrestlerId, branch))
            .toList();

    return overview;
  }

  // ==================== PRIVATE HELPER METHODS ====================

  private void processIndividualRivalries(MatchResult matchResult) {
    List<Wrestler> wrestlers = matchResult.getWrestlers();

    // Add heat between all participants (for multi-person matches)
    for (int i = 0; i < wrestlers.size(); i++) {
      for (int j = i + 1; j < wrestlers.size(); j++) {
        Wrestler wrestler1 = wrestlers.get(i);
        Wrestler wrestler2 = wrestlers.get(j);

        // Add heat based on match outcome
        int heatGain = calculateHeatGain(matchResult, wrestler1, wrestler2);
        if (heatGain > 0) {
          rivalryService.addHeatBetweenWrestlers(
              wrestler1.getId(),
              wrestler2.getId(),
              heatGain,
              "Match outcome: " + matchResult.getMatchRulesAsString());
        }
      }
    }
  }

  private void processFactionRivalries(MatchResult matchResult) {
    List<Wrestler> wrestlers = matchResult.getWrestlers();

    // Check for faction involvement
    for (int i = 0; i < wrestlers.size(); i++) {
      for (int j = i + 1; j < wrestlers.size(); j++) {
        Wrestler wrestler1 = wrestlers.get(i);
        Wrestler wrestler2 = wrestlers.get(j);

        Optional<Faction> faction1 = factionService.getFactionForWrestler(wrestler1.getId());
        Optional<Faction> faction2 = factionService.getFactionForWrestler(wrestler2.getId());

        if (faction1.isPresent()
            && faction2.isPresent()
            && !faction1.get().equals(faction2.get())) {
          int heatGain = calculateFactionHeatGain(matchResult, faction1.get(), faction2.get());
          if (heatGain > 0) {
            factionRivalryService.addHeatBetweenFactions(
                faction1.get().getId(),
                faction2.get().getId(),
                heatGain,
                "Faction members competed: " + matchResult.getMatchRulesAsString());
          }
        }
      }
    }
  }

  private void processMultiWrestlerFeuds(MatchResult matchResult) {
    List<Wrestler> wrestlers = matchResult.getWrestlers();

    // Find feuds involving match participants
    for (Wrestler wrestler : wrestlers) {
      List<MultiWrestlerFeud> feuds =
          multiWrestlerFeudService.getActiveFeudsForWrestler(wrestler.getId());

      for (MultiWrestlerFeud feud : feuds) {
        // Check if multiple feud participants were in this match
        long participantsInMatch = wrestlers.stream().filter(feud::hasParticipant).count();

        if (participantsInMatch >= 2) {
          int heatGain = (int) (participantsInMatch * 2); // More participants = more heat
          multiWrestlerFeudService.addHeat(
              feud.getId(),
              heatGain,
              "Multiple feud participants in match: " + matchResult.getMatchRulesAsString());
        }
      }
    }
  }

  private void processFactionInvolvement(List<Long> wrestlerIds, ComplexStorylineResult result) {
    // Check if wrestlers from different factions are involved
    for (int i = 0; i < wrestlerIds.size(); i++) {
      for (int j = i + 1; j < wrestlerIds.size(); j++) {
        Optional<Faction> faction1 = factionService.getFactionForWrestler(wrestlerIds.get(i));
        Optional<Faction> faction2 = factionService.getFactionForWrestler(wrestlerIds.get(j));

        if (faction1.isPresent()
            && faction2.isPresent()
            && !faction1.get().equals(faction2.get())) {
          Optional<FactionRivalry> factionRivalry =
              factionRivalryService.createFactionRivalry(
                  faction1.get().getId(),
                  faction2.get().getId(),
                  "Created from complex storyline involving faction members");

          factionRivalry.ifPresent(
              fr -> {
                result.factionRivalry = fr;
                log.info(
                    "Created faction rivalry as part of complex storyline: {}",
                    fr.getDisplayName());
              });

          break; // Only create one faction rivalry per storyline
        }
      }
    }
  }

  private void createStorylineBranches(String storylineName, ComplexStorylineResult result) {
    // Create branches for potential future developments
    if (result.individualRivalry != null) {
      storylineBranchingService.createBranch(
          storylineName + " - Escalation Branch",
          "Branch for escalating the rivalry based on future outcomes",
          StorylineBranchType.RIVALRY_ESCALATION,
          7);
    }

    if (result.factionRivalry != null) {
      storylineBranchingService.createBranch(
          storylineName + " - Faction War Branch",
          "Branch for escalating faction rivalry to war games",
          StorylineBranchType.FACTION_DYNAMICS,
          8);
    }
  }

  private FeudRole determineFeudRole(int index, int totalParticipants) {
    if (index == 0) return FeudRole.ANTAGONIST;
    if (index == 1) return FeudRole.PROTAGONIST;
    if (index == 2 && totalParticipants > 3) return FeudRole.SECONDARY_ANTAGONIST;
    if (index == 3 && totalParticipants > 4) return FeudRole.SECONDARY_PROTAGONIST;
    return FeudRole.NEUTRAL;
  }

  private int calculateHeatGain(MatchResult matchResult, Wrestler wrestler1, Wrestler wrestler2) {
    int baseHeat = 2; // Base heat for participating in a match together

    // More heat if it was a title match
    if (matchResult.getIsTitleMatch()) {
      baseHeat += 3;
    }

    // More heat if there were stipulations
    if (matchResult.hasMatchRules()) {
      baseHeat += 2;
    }

    // More heat if one wrestler won
    if (matchResult.getWinner() != null
        && (matchResult.getWinner().equals(wrestler1)
            || matchResult.getWinner().equals(wrestler2))) {
      baseHeat += 1;
    }

    return baseHeat;
  }

  private int calculateFactionHeatGain(
      MatchResult matchResult, Faction faction1, Faction faction2) {
    int baseHeat = 1; // Base heat for faction members competing

    // Apply alignment multiplier
    double multiplier = faction1.getAlignment().getHeatMultiplier(faction2.getAlignment());

    return (int) Math.round(baseHeat * multiplier);
  }

  private boolean isWrestlerInvolvedInBranch(Long wrestlerId, StorylineBranch branch) {
    // This would need to check branch conditions and effects for wrestler involvement
    // For now, return false as a placeholder
    return false;
  }

  // ==================== RESULT CLASSES ====================

  public static class ComplexStorylineResult {
    public Rivalry individualRivalry;
    public FactionRivalry factionRivalry;
    public MultiWrestlerFeud multiWrestlerFeud;
    public List<StorylineBranch> createdBranches;
  }

  public static class RivalryEscalationResult {
    public boolean escalated;
    public String escalationType;
    public String message;
    public Rivalry originalRivalry;
    public FactionRivalry factionRivalry;
    public MultiWrestlerFeud multiWrestlerFeud;

    public RivalryEscalationResult() {}

    public RivalryEscalationResult(boolean escalated, String message, Rivalry originalRivalry) {
      this.escalated = escalated;
      this.message = message;
      this.originalRivalry = originalRivalry;
    }
  }

  public static class WrestlerRivalryOverview {
    public List<Rivalry> individualRivalries;
    public Faction faction;
    public List<FactionRivalry> factionRivalries;
    public List<MultiWrestlerFeud> multiWrestlerFeuds;
    public List<StorylineBranch> activeStorylineBranches;
  }
}
