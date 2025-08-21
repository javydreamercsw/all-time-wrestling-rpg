package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService.ResolutionResult;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing faction rivalries in the ATW RPG system. Handles faction rivalry creation,
 * heat management, and resolution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FactionRivalryService {

  private final FactionRivalryRepository factionRivalryRepository;
  private final FactionRepository factionRepository;
  private final Clock clock;
  private final Random random = new Random();

  /** Get all faction rivalries with pagination. */
  @Transactional(readOnly = true)
  public Page<FactionRivalry> getAllFactionRivalries(Pageable pageable) {
    return factionRivalryRepository.findAllBy(pageable);
  }

  /** Get faction rivalry by ID. */
  @Transactional(readOnly = true)
  public Optional<FactionRivalry> getFactionRivalryById(Long id) {
    return factionRivalryRepository.findById(id);
  }

  /** Get all active faction rivalries. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getActiveFactionRivalries() {
    return factionRivalryRepository.findByIsActiveTrue();
  }

  /** Get active rivalries for a specific faction. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getActiveRivalriesForFaction(Long factionId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);

    if (factionOpt.isEmpty()) {
      return List.of();
    }

    return factionRivalryRepository.findActiveRivalriesForFaction(factionOpt.get());
  }

  /** Create a new faction rivalry. */
  public Optional<FactionRivalry> createFactionRivalry(
      Long faction1Id, Long faction2Id, String storylineNotes) {
    Optional<Faction> faction1Opt = factionRepository.findById(faction1Id);
    Optional<Faction> faction2Opt = factionRepository.findById(faction2Id);

    if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
      return Optional.empty();
    }

    Faction faction1 = faction1Opt.get();
    Faction faction2 = faction2Opt.get();

    // Check if rivalry already exists
    Optional<FactionRivalry> existingRivalry =
        factionRivalryRepository.findActiveRivalryBetween(faction1, faction2);
    if (existingRivalry.isPresent()) {
      return existingRivalry; // Return existing rivalry
    }

    // Validate factions can have rivalry
    if (!canHaveRivalry(faction1, faction2)) {
      log.warn("Factions {} and {} cannot have a rivalry", faction1.getName(), faction2.getName());
      return Optional.empty();
    }

    // Create new rivalry
    FactionRivalry rivalry = new FactionRivalry();
    rivalry.setFaction1(faction1);
    rivalry.setFaction2(faction2);
    rivalry.setHeat(0);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(clock.instant());
    rivalry.setStorylineNotes(storylineNotes);
    rivalry.setCreationDate(clock.instant());

    FactionRivalry savedRivalry = factionRivalryRepository.saveAndFlush(rivalry);

    log.info(
        "Created faction rivalry: {} vs {} (ID: {})",
        faction1.getName(),
        faction2.getName(),
        savedRivalry.getId());

    return Optional.of(savedRivalry);
  }

  /** Add heat to a faction rivalry. */
  public Optional<FactionRivalry> addHeat(Long rivalryId, int heatGain, String reason) {
    return factionRivalryRepository
        .findById(rivalryId)
        .filter(rivalry -> rivalry.getIsActive())
        .map(
            rivalry -> {
              // Apply alignment heat multiplier
              double multiplier = rivalry.getAlignmentHeatMultiplier();
              int adjustedHeatGain = (int) Math.round(heatGain * multiplier);

              rivalry.addHeat(adjustedHeatGain, reason);

              FactionRivalry savedRivalry = factionRivalryRepository.saveAndFlush(rivalry);

              log.info(
                  "Added {} heat to faction rivalry {} (total: {}, reason: {})",
                  adjustedHeatGain,
                  rivalry.getDisplayName(),
                  rivalry.getHeat(),
                  reason);

              return savedRivalry;
            });
  }

  /** Add heat between two specific factions. */
  public Optional<FactionRivalry> addHeatBetweenFactions(
      Long faction1Id, Long faction2Id, int heatGain, String reason) {
    Optional<Faction> faction1Opt = factionRepository.findById(faction1Id);
    Optional<Faction> faction2Opt = factionRepository.findById(faction2Id);

    if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
      return Optional.empty();
    }

    // Find or create rivalry
    Optional<FactionRivalry> rivalryOpt =
        factionRivalryRepository.findActiveRivalryBetween(faction1Opt.get(), faction2Opt.get());

    if (rivalryOpt.isEmpty()) {
      // Create new rivalry if none exists
      rivalryOpt = createFactionRivalry(faction1Id, faction2Id, "Auto-generated from heat event");
    }

    return rivalryOpt.flatMap(rivalry -> addHeat(rivalry.getId(), heatGain, reason));
  }

  /** Attempt to resolve a faction rivalry with dice rolls. */
  public ResolutionResult attemptResolution(
      Long rivalryId, Integer faction1Roll, Integer faction2Roll) {
    Optional<FactionRivalry> rivalryOpt = factionRivalryRepository.findById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return new ResolutionResult(false, "Faction rivalry not found", null, 0, 0, 0);
    }

    FactionRivalry rivalry = rivalryOpt.get();

    if (!rivalry.canAttemptResolution()) {
      return new ResolutionResult(
          false,
          String.format(
              "Faction rivalry needs at least 20 heat to attempt resolution (current: %d)",
              rivalry.getHeat()),
          null, // No individual rivalry to return
          0,
          0,
          0);
    }

    // Use provided rolls or generate random ones
    int roll1 = faction1Roll != null ? faction1Roll : random.nextInt(20) + 1;
    int roll2 = faction2Roll != null ? faction2Roll : random.nextInt(20) + 1;
    int total = roll1 + roll2;

    boolean resolved = rivalry.attemptResolution(roll1, roll2);

    if (resolved) {
      factionRivalryRepository.saveAndFlush(rivalry);

      log.info(
          "Faction rivalry {} resolved with dice roll: {} + {} = {}",
          rivalry.getDisplayName(),
          roll1,
          roll2,
          total);
    } else {
      log.info(
          "Faction rivalry {} resolution failed with dice roll: {} + {} = {}",
          rivalry.getDisplayName(),
          roll1,
          roll2,
          total);
    }

    return new ResolutionResult(
        resolved,
        resolved ? "Faction rivalry resolved successfully" : "Faction resolution attempt failed",
        null, // No individual rivalry to return
        roll1,
        roll2,
        total);
  }

  /** End a faction rivalry. */
  public Optional<FactionRivalry> endFactionRivalry(Long rivalryId, String reason) {
    Optional<FactionRivalry> rivalryOpt = factionRivalryRepository.findById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return Optional.empty();
    }

    FactionRivalry rivalry = rivalryOpt.get();

    if (!rivalry.getIsActive()) {
      return Optional.of(rivalry); // Already ended
    }

    rivalry.endRivalry(reason);
    FactionRivalry savedRivalry = factionRivalryRepository.saveAndFlush(rivalry);

    log.info("Ended faction rivalry: {} (reason: {})", rivalry.getDisplayName(), reason);

    return Optional.of(savedRivalry);
  }

  /** Get faction rivalries requiring matches at next show. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getRivalriesRequiringMatches() {
    return factionRivalryRepository.findRivalriesRequiringMatches();
  }

  /** Get faction rivalries eligible for resolution. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getRivalriesEligibleForResolution() {
    return factionRivalryRepository.findRivalriesEligibleForResolution();
  }

  /** Get faction rivalries requiring stipulation matches. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getRivalriesRequiringStipulationMatches() {
    return factionRivalryRepository.findRivalriesRequiringStipulationMatches();
  }

  /** Get hottest faction rivalries. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getHottestRivalries(int limit) {
    return factionRivalryRepository.findHottestRivalries(Pageable.ofSize(limit));
  }

  /** Get Face vs Heel rivalries. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getFaceVsHeelRivalries() {
    return factionRivalryRepository.findFaceVsHeelRivalries();
  }

  /** Get tag team rivalries. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getTagTeamRivalries() {
    return factionRivalryRepository.findTagTeamRivalries();
  }

  /** Get rivalries involving stables. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getRivalriesInvolvingStables() {
    return factionRivalryRepository.findRivalriesInvolvingStables();
  }

  /** Check if two factions can have a rivalry. */
  private boolean canHaveRivalry(Faction faction1, Faction faction2) {
    // Both factions must be active
    if (!faction1.getIsActive() || !faction2.getIsActive()) {
      return false;
    }

    // Cannot have rivalry with self
    if (faction1.equals(faction2)) {
      return false;
    }

    // Both factions must have at least one member
    if (faction1.getMemberCount() == 0 || faction2.getMemberCount() == 0) {
      return false;
    }

    return true;
  }

  /** Get total wrestlers involved in faction rivalries. */
  @Transactional(readOnly = true)
  public Long getTotalWrestlersInRivalries() {
    Long total = factionRivalryRepository.countTotalWrestlersInRivalries();
    return total != null ? total : 0L;
  }

  /** Get rivalries with the most wrestlers involved. */
  @Transactional(readOnly = true)
  public List<FactionRivalry> getRivalriesWithMostWrestlers(int limit) {
    return factionRivalryRepository.findRivalriesWithMostWrestlers(Pageable.ofSize(limit));
  }
}
