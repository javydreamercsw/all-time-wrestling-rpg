package com.github.javydreamercsw.management.service.injury;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing injuries in the ATW RPG system. Handles injury tracking, healing mechanics,
 * and health impact calculations.
 */
@Service
@Transactional
public class InjuryService {

  private final InjuryRepository injuryRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;
  private final Random random;

  public InjuryService(
      InjuryRepository injuryRepository, WrestlerRepository wrestlerRepository, Clock clock) {
    this.injuryRepository = injuryRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.clock = clock;
    this.random = new Random();
  }

  /** Create a new injury for a wrestler. */
  public Optional<Injury> createInjury(
      Long wrestlerId,
      String name,
      String description,
      InjurySeverity severity,
      String injuryNotes) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              Injury injury = new Injury();
              injury.setWrestler(wrestler);
              injury.setName(name);
              injury.setDescription(description);
              injury.setSeverity(severity);
              injury.setHealthPenalty(severity.getRandomHealthPenalty());
              injury.setHealingCost(severity.getBaseHealingCost());
              injury.setIsActive(true);
              injury.setInjuryDate(Instant.now(clock));
              injury.setInjuryNotes(injuryNotes);
              injury.setCreationDate(Instant.now(clock));

              return injuryRepository.saveAndFlush(injury);
            });
  }

  /**
   * Create injury from bump system (3 bumps = 1 injury). This method should only be called when an
   * injury should be created (bumps already reset by Wrestler.addBump()).
   */
  public Optional<Injury> createInjuryFromBumps(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              // Create injury with severity based on wrestler tier
              InjurySeverity severity = getRandomInjurySeverityForWrestler(wrestler);
              String injuryName = generateInjuryName(severity);
              String description = generateInjuryDescription(severity);

              return createInjury(
                      wrestlerId,
                      injuryName,
                      description,
                      severity,
                      "Generated from bump accumulation (tier: " + wrestler.getTier().name() + ")")
                  .orElse(null);
            });
  }

  /** Attempt to heal an injury. */
  public HealingResult attemptHealing(Long injuryId, Integer diceRoll) {
    Optional<Injury> injuryOpt = injuryRepository.findById(injuryId);

    if (injuryOpt.isEmpty()) {
      return new HealingResult(false, "Injury not found", null, 0, false);
    }

    Injury injury = injuryOpt.get();

    if (!injury.canBeHealed()) {
      return new HealingResult(
          false, "Injury cannot be healed (already healed or inactive)", injury, 0, false);
    }

    // Check if wrestler can afford healing cost
    Wrestler wrestler = injury.getWrestler();
    if (!wrestler.canAfford(injury.getHealingCost())) {
      return new HealingResult(
          false,
          String.format("Wrestler cannot afford %,d fans healing cost", injury.getHealingCost()),
          injury,
          0,
          false);
    }

    // Use provided roll or generate random one
    int roll = diceRoll != null ? diceRoll : random.nextInt(6) + 1;
    boolean success = injury.getSeverity().isHealingSuccessful(roll);

    // Spend the fans regardless of success
    wrestler.spendFans(injury.getHealingCost());
    wrestlerRepository.saveAndFlush(wrestler);

    if (success) {
      injury.heal();
      injuryRepository.saveAndFlush(injury);
    }

    return new HealingResult(
        success,
        success ? "Injury healed successfully" : "Healing attempt failed",
        injury,
        roll,
        true);
  }

  /** Get injury by ID. */
  @Transactional(readOnly = true)
  public Optional<Injury> getInjuryById(Long injuryId) {
    return injuryRepository.findById(injuryId);
  }

  /** Get all injuries with pagination. */
  @Transactional(readOnly = true)
  public Page<Injury> getAllInjuries(Pageable pageable) {
    return injuryRepository.findAllBy(pageable);
  }

  /** Get active injuries for a wrestler. */
  @Transactional(readOnly = true)
  public List<Injury> getActiveInjuriesForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(injuryRepository::findActiveInjuriesForWrestler)
        .orElse(List.of());
  }

  /** Get all injuries for a wrestler. */
  @Transactional(readOnly = true)
  public List<Injury> getAllInjuriesForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(injuryRepository::findByWrestler)
        .orElse(List.of());
  }

  /** Get injuries by severity. */
  @Transactional(readOnly = true)
  public List<Injury> getInjuriesBySeverity(InjurySeverity severity) {
    return injuryRepository.findBySeverity(severity);
  }

  /** Get all active injuries. */
  @Transactional(readOnly = true)
  public List<Injury> getAllActiveInjuries() {
    return injuryRepository.findAllActiveInjuries();
  }

  /** Get wrestlers with active injuries. */
  @Transactional(readOnly = true)
  public List<Wrestler> getWrestlersWithActiveInjuries() {
    return injuryRepository.findWrestlersWithActiveInjuries();
  }

  /** Get total health penalty for a wrestler. */
  @Transactional(readOnly = true)
  public Integer getTotalHealthPenaltyForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(injuryRepository::getTotalHealthPenaltyForWrestler)
        .orElse(0);
  }

  /** Get healable injuries (active injuries). */
  @Transactional(readOnly = true)
  public List<Injury> getHealableInjuries() {
    return injuryRepository.findHealableInjuries();
  }

  /** Update injury information. */
  public Optional<Injury> updateInjury(
      Long injuryId, String name, String description, String injuryNotes) {
    return injuryRepository
        .findById(injuryId)
        .map(
            injury -> {
              if (name != null) injury.setName(name);
              if (description != null) injury.setDescription(description);
              if (injuryNotes != null) injury.setInjuryNotes(injuryNotes);
              return injuryRepository.saveAndFlush(injury);
            });
  }

  /** Get injury statistics for a wrestler. */
  @Transactional(readOnly = true)
  public InjuryStats getInjuryStatsForWrestler(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              List<Injury> activeInjuries =
                  injuryRepository.findActiveInjuriesForWrestler(wrestler);
              List<Injury> allInjuries = injuryRepository.findByWrestler(wrestler);
              Integer totalHealthPenalty =
                  injuryRepository.getTotalHealthPenaltyForWrestler(wrestler);

              return new InjuryStats(
                  wrestlerId,
                  wrestler.getName(),
                  activeInjuries.size(),
                  allInjuries.size() - activeInjuries.size(),
                  totalHealthPenalty,
                  wrestler.getEffectiveStartingHealth(), // Already includes injury penalty
                  activeInjuries.stream().mapToLong(injury -> injury.getHealingCost()).sum());
            })
        .orElse(null);
  }

  /** Generate random injury severity based on probability. */
  private InjurySeverity getRandomInjurySeverity() {
    int roll = random.nextInt(100) + 1;

    if (roll <= 50) return InjurySeverity.MINOR; // 50% chance
    if (roll <= 80) return InjurySeverity.MODERATE; // 30% chance
    if (roll <= 95) return InjurySeverity.SEVERE; // 15% chance
    return InjurySeverity.CRITICAL; // 5% chance
  }

  /**
   * Generate random injury severity based on wrestler tier. Higher tier wrestlers are more
   * resilient and get less severe injuries.
   */
  private InjurySeverity getRandomInjurySeverityForWrestler(Wrestler wrestler) {
    int roll = random.nextInt(100) + 1;

    // Adjust probabilities based on wrestler tier
    // Higher tier wrestlers are more experienced and resilient
    return switch (wrestler.getTier()) {
      case ROOKIE -> {
        // Rookies are more prone to severe injuries (inexperienced)
        if (roll <= 35) yield InjurySeverity.MINOR; // 35% chance
        if (roll <= 65) yield InjurySeverity.MODERATE; // 30% chance
        if (roll <= 90) yield InjurySeverity.SEVERE; // 25% chance
        yield InjurySeverity.CRITICAL; // 10% chance
      }
      case RISER -> {
        // Risers have some experience but still learning
        if (roll <= 40) yield InjurySeverity.MINOR; // 40% chance
        if (roll <= 70) yield InjurySeverity.MODERATE; // 30% chance
        if (roll <= 92) yield InjurySeverity.SEVERE; // 22% chance
        yield InjurySeverity.CRITICAL; // 8% chance
      }
      case CONTENDER -> {
        // Contenders are experienced and more careful
        if (roll <= 45) yield InjurySeverity.MINOR; // 45% chance
        if (roll <= 75) yield InjurySeverity.MODERATE; // 30% chance
        if (roll <= 94) yield InjurySeverity.SEVERE; // 19% chance
        yield InjurySeverity.CRITICAL; // 6% chance
      }
      case INTERTEMPORAL_TIER -> {
        // Elite wrestlers know how to protect themselves
        if (roll <= 55) yield InjurySeverity.MINOR; // 55% chance
        if (roll <= 80) yield InjurySeverity.MODERATE; // 25% chance
        if (roll <= 96) yield InjurySeverity.SEVERE; // 16% chance
        yield InjurySeverity.CRITICAL; // 4% chance
      }
      case MAIN_EVENTER -> {
        // Main eventers are very experienced
        if (roll <= 60) yield InjurySeverity.MINOR; // 60% chance
        if (roll <= 85) yield InjurySeverity.MODERATE; // 25% chance
        if (roll <= 97) yield InjurySeverity.SEVERE; // 12% chance
        yield InjurySeverity.CRITICAL; // 3% chance
      }
      case ICON -> {
        // Icons are legends who know how to work safely
        if (roll <= 65) yield InjurySeverity.MINOR; // 65% chance
        if (roll <= 88) yield InjurySeverity.MODERATE; // 23% chance
        if (roll <= 98) yield InjurySeverity.SEVERE; // 10% chance
        yield InjurySeverity.CRITICAL; // 2% chance
      }
    };
  }

  /** Generate injury name based on severity. */
  private String generateInjuryName(InjurySeverity severity) {
    String[] minorInjuries = {"Bruised Ribs", "Twisted Ankle", "Minor Cut", "Muscle Strain"};
    String[] moderateInjuries = {
      "Sprained Wrist", "Bruised Shoulder", "Minor Concussion", "Pulled Muscle"
    };
    String[] severeInjuries = {
      "Dislocated Shoulder", "Knee Injury", "Back Strain", "Severe Bruising"
    };
    String[] criticalInjuries = {"Broken Ribs", "Torn ACL", "Severe Concussion", "Fractured Bone"};

    String[] injuries =
        switch (severity) {
          case MINOR -> minorInjuries;
          case MODERATE -> moderateInjuries;
          case SEVERE -> severeInjuries;
          case CRITICAL -> criticalInjuries;
        };

    return injuries[random.nextInt(injuries.length)];
  }

  /** Generate injury description based on severity. */
  private String generateInjuryDescription(InjurySeverity severity) {
    return switch (severity) {
      case MINOR -> "A minor injury that should heal quickly with proper rest.";
      case MODERATE -> "A moderate injury that requires some time to heal properly.";
      case SEVERE ->
          "A severe injury that significantly impacts performance and requires extended recovery.";
      case CRITICAL ->
          "A critical injury that poses serious health risks and requires immediate medical"
              + " attention.";
    };
  }

  /** Healing result data class. */
  public record HealingResult(
      boolean success, String message, Injury injury, int diceRoll, boolean fansSpent) {}

  /** Injury statistics data class. */
  public record InjuryStats(
      Long wrestlerId,
      String wrestlerName,
      int activeInjuries,
      int healedInjuries,
      int totalHealthPenalty,
      int effectiveHealth,
      long totalHealingCost) {}
}
