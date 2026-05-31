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
package com.github.javydreamercsw.management.service.injury;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing injuries in the ATW RPG system. Handles injury tracking, healing mechanics,
 * and health impact calculations.
 */
@Service
@Transactional
public class InjuryService {

  @Autowired private InjuryRepository injuryRepository;
  @Autowired private InjuryTypeRepository injuryTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private Clock clock;
  @Autowired private Random random;
  @Autowired private ApplicationEventPublisher eventPublisher;

  /** Create a new injury for a wrestler. Falls back to the Legacy Injury type if none specified. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public Optional<Injury> createInjury(
      final Long wrestlerId,
      final Long universeId,
      final Long injuryTypeId,
      final String name,
      final String description,
      final InjurySeverity severity,
      final String injuryNotes) {
    InjuryType injuryType = resolveInjuryType(injuryTypeId);
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              Universe universe = universeRepository.findById(universeId).orElseThrow();
              WrestlerState state = getWrestlerState(wrestlerId, universeId);
              Injury injury = new Injury();
              injury.setInjuryType(injuryType);
              injury.setWrestler(wrestler);
              injury.setUniverse(universe);
              injury.setName(name);
              injury.setDescription(description);
              injury.setSeverity(severity);
              injury.setHealthPenalty(severity.getRandomHealthPenalty(random));
              injury.setStaminaPenalty(severity.getRandomStaminaPenalty(random));
              injury.setHandSizePenalty(severity.getRandomHandSizePenalty(random));
              injury.setHealingCost(severity.getBaseHealingCost());
              injury.setIsActive(true);
              injury.setInjuryDate(Instant.now(clock));
              injury.setInjuryNotes(injuryNotes);
              injury.setCreationDate(Instant.now(clock));

              Injury savedInjury = injuryRepository.saveAndFlush(injury);
              eventPublisher.publishEvent(new WrestlerInjuryEvent(this, state, savedInjury));
              return savedInjury;
            });
  }

  /**
   * Create injury from bump system (3 bumps = 1 injury). This method should only be called when an
   * injury should be created (bumps already reset by Wrestler.addBump()).
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public Optional<Injury> createInjuryFromBumps(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    WrestlerState state =
        wrestlerStateRepository.findByWrestlerIdAndUniverseId(wrestlerId, universeId).orElseThrow();

    InjurySeverity severity = getRandomInjurySeverityForWrestler(state);
    String injuryName = generateInjuryName(severity);
    String description = generateInjuryDescription(severity);

    List<InjuryType> allTypes = injuryTypeRepository.findAll();
    InjuryType injuryType =
        allTypes.isEmpty()
            ? resolveInjuryType(null)
            : allTypes.get(random.nextInt(allTypes.size()));

    Injury injury = new Injury();
    injury.setInjuryType(injuryType);
    injury.setWrestler(state.getWrestler());
    injury.setUniverse(state.getUniverse());
    injury.setName(injuryName);
    injury.setDescription(description);
    injury.setSeverity(severity);
    injury.setHealthPenalty(severity.getRandomHealthPenalty(random));
    injury.setStaminaPenalty(severity.getRandomStaminaPenalty(random));
    injury.setHandSizePenalty(severity.getRandomHandSizePenalty(random));
    injury.setHealingCost(severity.getBaseHealingCost());
    injury.setIsActive(true);
    injury.setInjuryDate(Instant.now(clock));
    injury.setInjuryNotes(
        "Generated from bump accumulation (tier: " + state.getTier().name() + ")");
    injury.setCreationDate(Instant.now(clock));

    Injury savedInjury = injuryRepository.saveAndFlush(injury);
    eventPublisher.publishEvent(new WrestlerInjuryEvent(this, state, savedInjury));
    return Optional.of(savedInjury);
  }

  public WrestlerState getWrestlerState(final Long wrestlerId, final Long universeId) {
    return wrestlerStateRepository
        .findByWrestlerIdAndUniverseId(wrestlerId, universeId)
        .orElseGet(
            () -> {
              Wrestler wrestler = wrestlerRepository.findById(wrestlerId).orElseThrow();
              Universe universe = universeRepository.findById(universeId).orElseThrow();
              return wrestlerStateRepository.save(
                  WrestlerState.builder()
                      .wrestler(wrestler)
                      .universe(universe)
                      .physicalCondition(100)
                      .fans(0L)
                      .bumps(0)
                      .build());
            });
  }

  /** Attempt to heal an injury. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public HealingResult attemptHealing(@NonNull final Long injuryId) {
    return attemptHealing(injuryId, null);
  }

  /** Attempt to heal an injury. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public HealingResult attemptHealing(@NonNull final Long injuryId, final Integer diceRoll) {
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
    Universe universe = injury.getUniverse();

    if (universe == null) {
      return new HealingResult(false, "Injury is not associated with a universe", injury, 0, false);
    }

    // Use current universe state to check if they can afford it
    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerAndUniverse(wrestler, universe)
            .orElseThrow(() -> new IllegalStateException("Wrestler has no state in universe"));

    if (state.getFans() < injury.getHealingCost()) {
      return new HealingResult(
          false,
          "Wrestler cannot afford %,d fans healing cost".formatted(injury.getHealingCost()),
          injury,
          0,
          false);
    }

    // Use provided roll or generate random one
    int roll = diceRoll != null ? diceRoll : random.nextInt(6) + 1;
    boolean success = injury.getSeverity().isHealingSuccessful(roll);

    // Spend the fans regardless of success
    state.setFans(state.getFans() - injury.getHealingCost());
    wrestlerStateRepository.saveAndFlush(state);

    if (success) {
      injury.heal();
      injuryRepository.saveAndFlush(injury);
      eventPublisher.publishEvent(new WrestlerInjuryHealedEvent(this, state, injury));
    }

    String message =
        success
            ? "Injury healed successfully"
            : "Healing attempt failed (Rolled: %d, Needed: %d+)"
                .formatted(roll, injury.getSeverity().getHealingSuccessThreshold());

    return new HealingResult(success, message, injury, roll, true);
  }

  /** Force heal an injury (Admin only). Bypasses dice roll check by ensuring a successful roll. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public HealingResult forceHeal(@NonNull final Long injuryId) {
    // 6 is sufficient to heal even CRITICAL injuries (threshold 6)
    return attemptHealing(injuryId, 6);
  }

  /** Get injury by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      key = "#injuryId")
  public Optional<Injury> getInjuryById(@NonNull final Long injuryId) {
    return injuryRepository.findById(injuryId);
  }

  /** Get all injuries with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Injury> getAllInjuries(@NonNull final Pageable pageable) {
    return injuryRepository.findAllBy(pageable);
  }

  /** Get active injuries for a wrestler in a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      key = "'activeForWrestler:' + #wrestlerId + ':' + #universeId")
  public List<Injury> getActiveInjuriesForWrestler(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    Universe universe = universeRepository.findById(universeId).orElseThrow();
    return wrestlerRepository
        .findById(wrestlerId)
        .map(w -> injuryRepository.findActiveInjuriesForWrestler(w, universe))
        .orElse(List.of());
  }

  /** Get all injuries for a wrestler in a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      key = "'allForWrestler:' + #wrestlerId + ':' + #universeId")
  public List<Injury> getAllInjuriesForWrestler(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    Universe universe = universeRepository.findById(universeId).orElseThrow();
    return wrestlerRepository
        .findById(wrestlerId)
        .map(w -> injuryRepository.findByWrestlerAndUniverse(w, universe))
        .orElse(List.of());
  }

  @Deprecated
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Injury> getActiveInjuriesForWrestler(@NonNull final Long wrestlerId) {
    return getActiveInjuriesForWrestler(wrestlerId, 1L);
  }

  @Deprecated
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Injury> getAllInjuriesForWrestler(@NonNull final Long wrestlerId) {
    return getAllInjuriesForWrestler(wrestlerId, 1L);
  }

  @Deprecated
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public InjuryStats getInjuryStatsForWrestler(final Long wrestlerId) {
    return getInjuryStatsForWrestler(wrestlerId, 1L);
  }

  /** Get injuries by severity. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      key = "#severity")
  public List<Injury> getInjuriesBySeverity(@NonNull final InjurySeverity severity) {
    return injuryRepository.findBySeverity(severity);
  }

  /** Get all active injuries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      key = "'allActive'")
  public List<Injury> getAllActiveInjuries() {
    return injuryRepository.findAllActiveInjuries();
  }

  /** Get wrestlers with active injuries in a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getWrestlersWithActiveInjuries(@NonNull final Long universeId) {
    Universe universe = universeRepository.findById(universeId).orElseThrow();
    return injuryRepository.findWrestlersWithActiveInjuries(universe);
  }

  /** Get wrestlers with active injuries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getWrestlersWithActiveInjuries() {
    return injuryRepository.findWrestlersWithActiveInjuries();
  }

  /**
   * Heal an injury for free (e.g., via Backstage Action). Bypasses fan cost.
   *
   * @param injuryId The ID of the injury to heal.
   * @return The result of the healing attempt.
   */
  public HealingResult healInjuryFree(@NonNull final Long injuryId) {
    Optional<Injury> injuryOpt = injuryRepository.findById(injuryId);
    if (injuryOpt.isEmpty()) {
      return new HealingResult(false, "Injury not found", null, 0, false);
    }

    Injury injury = injuryOpt.get();
    if (!injury.canBeHealed()) {
      return new HealingResult(
          false, "Injury cannot be healed (already healed or inactive)", injury, 0, false);
    }

    // Use current universe state
    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerAndUniverse(injury.getWrestler(), injury.getUniverse())
            .orElseThrow(() -> new IllegalStateException("Wrestler has no state in universe"));

    // Heal without cost
    injury.heal();
    injuryRepository.save(injury);

    // Publish event
    eventPublisher.publishEvent(new WrestlerInjuryHealedEvent(this, state, injury));

    return new HealingResult(true, "Injury healed successfully (Free)", injury, 6, true);
  }

  /** Get total health penalty for a wrestler from active injuries in a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Integer getTotalHealthPenaltyForWrestler(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    return universeRepository
        .findById(universeId)
        .flatMap(
            universe ->
                wrestlerRepository
                    .findById(wrestlerId)
                    .map(w -> injuryRepository.getTotalHealthPenaltyForWrestler(w, universe)))
        .orElse(0);
  }

  /** Get healable injuries (active injuries). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Injury> getHealableInjuries() {
    return injuryRepository.findHealableInjuries();
  }

  /** Update injury information. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.INJURIES_CACHE,
      allEntries = true)
  public Optional<Injury> updateInjury(
      @NonNull final Long injuryId,
      final String name,
      final String description,
      final String injuryNotes) {
    return injuryRepository
        .findById(injuryId)
        .map(
            injury -> {
              if (name != null) {
                injury.setName(name);
              }
              if (description != null) {
                injury.setDescription(description);
              }
              if (injuryNotes != null) {
                injury.setInjuryNotes(injuryNotes);
              }
              return injuryRepository.saveAndFlush(injury);
            });
  }

  /** Find injury by external ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Injury> findByExternalId(@NonNull final String externalId) {
    return injuryRepository.findByExternalId(externalId);
  }

  /** Get injury statistics for a wrestler in a specific universe. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public InjuryStats getInjuryStatsForWrestler(final Long wrestlerId, final Long universeId) {
    WrestlerState state =
        wrestlerStateRepository.findByWrestlerIdAndUniverseId(wrestlerId, universeId).orElseThrow();
    Wrestler wrestler = state.getWrestler();
    Universe universe = state.getUniverse();

    List<Injury> activeInjuries =
        injuryRepository.findActiveInjuriesForWrestler(wrestler, universe);
    List<Injury> allInjuries = injuryRepository.findByWrestlerAndUniverse(wrestler, universe);
    Integer totalHealthPenalty = activeInjuries.stream().mapToInt(Injury::getHealthPenalty).sum();

    // Calculate effective health manually since we moved it from Wrestler
    int bonus = 0;
    int penalty = 0;
    if (wrestler.getAlignment() != null
        && wrestler.getAlignment().getCampaign() != null
        && wrestler.getAlignment().getCampaign().getState() != null) {
      bonus = wrestler.getAlignment().getCampaign().getState().getCampaignHealthBonus();
      penalty = wrestler.getAlignment().getCampaign().getState().getHealthPenalty();
    }

    int conditionPenalty = Math.min(5, (100 - state.getPhysicalCondition()) / 5);
    int effectiveHealth =
        wrestler.getStartingHealth()
            + bonus
            - penalty
            - state.getBumps()
            - totalHealthPenalty
            - conditionPenalty;
    effectiveHealth = Math.max(1, effectiveHealth);

    return new InjuryStats(
        wrestlerId,
        wrestler.getName(),
        activeInjuries.size(),
        allInjuries.size() - activeInjuries.size(),
        totalHealthPenalty,
        effectiveHealth,
        activeInjuries.stream().mapToLong(Injury::getHealingCost).sum());
  }

  /** Returns the requested InjuryType, or the Legacy Injury sentinel when id is null. */
  private InjuryType resolveInjuryType(final Long injuryTypeId) {
    if (injuryTypeId != null) {
      return injuryTypeRepository
          .findById(injuryTypeId)
          .orElseThrow(() -> new IllegalArgumentException("InjuryType not found: " + injuryTypeId));
    }
    return injuryTypeRepository
        .findByInjuryName("Legacy Injury")
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Legacy Injury sentinel not found — run Flyway migrations first."));
  }

  /**
   * Generate random injury severity based on wrestler tier. Higher tier wrestlers are more
   * resilient and get less severe injuries.
   */
  private InjurySeverity getRandomInjurySeverityForWrestler(
      @NonNull final WrestlerState wrestlerData) {
    int roll = random.nextInt(100) + 1;

    // Adjust probabilities based on wrestler tier
    // Higher tier wrestlers are more experienced and resilient
    return switch (wrestlerData.getTier()) {
      case ROOKIE -> {
        // Rookies are more prone to severe injuries (inexperienced)
        if (roll <= 35) {
          yield InjurySeverity.MINOR; // 35% chance
        }
        if (roll <= 65) {
          yield InjurySeverity.MODERATE; // 30% chance
        }
        if (roll <= 90) {
          yield InjurySeverity.SEVERE; // 25% chance
        }
        yield InjurySeverity.CRITICAL; // 10% chance
      }
      case RISER -> {
        // Risers have some experience but still learning
        if (roll <= 40) {
          yield InjurySeverity.MINOR; // 40% chance
        }
        if (roll <= 70) {
          yield InjurySeverity.MODERATE; // 30% chance
        }
        if (roll <= 92) {
          yield InjurySeverity.SEVERE; // 22% chance
        }
        yield InjurySeverity.CRITICAL; // 8% chance
      }
      case CONTENDER -> {
        // Contenders are experienced and more careful
        if (roll <= 45) {
          yield InjurySeverity.MINOR; // 45% chance
        }
        if (roll <= 75) {
          yield InjurySeverity.MODERATE; // 30% chance
        }
        if (roll <= 94) {
          yield InjurySeverity.SEVERE; // 19% chance
        }
        yield InjurySeverity.CRITICAL; // 6% chance
      }
      case MIDCARDER -> {
        // Elite wrestlers know how to protect themselves
        if (roll <= 55) {
          yield InjurySeverity.MINOR; // 55% chance
        }
        if (roll <= 80) {
          yield InjurySeverity.MODERATE; // 25% chance
        }
        if (roll <= 96) {
          yield InjurySeverity.SEVERE; // 16% chance
        }
        yield InjurySeverity.CRITICAL; // 4% chance
      }
      case MAIN_EVENTER -> {
        // Main eventers are very experienced
        if (roll <= 60) {
          yield InjurySeverity.MINOR; // 60% chance
        }
        if (roll <= 85) {
          yield InjurySeverity.MODERATE; // 25% chance
        }
        if (roll <= 97) {
          yield InjurySeverity.SEVERE; // 12% chance
        }
        yield InjurySeverity.CRITICAL; // 3% chance
      }
      case ICON -> {
        // Icons are legends who know how to work safely
        if (roll <= 65) {
          yield InjurySeverity.MINOR; // 65% chance
        }
        if (roll <= 88) {
          yield InjurySeverity.MODERATE; // 23% chance
        }
        if (roll <= 98) {
          yield InjurySeverity.SEVERE; // 10% chance
        }
        yield InjurySeverity.CRITICAL; // 2% chance
      }
    };
  }

  /** Generate injury name based on severity. */
  private String generateInjuryName(@NonNull final InjurySeverity severity) {
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
  private String generateInjuryDescription(@NonNull final InjurySeverity severity) {
    return switch (severity) {
      case MINOR -> "A minor injury that should heal quickly with proper rest.";
      case MODERATE -> "A moderate injury that requires some time to heal properly.";
      case SEVERE ->
          "A severe injury that significantly impacts performance and requires extended recovery.";
      case CRITICAL ->
          """
          A critical injury that poses serious health risks and requires immediate medical\
           attention.\
          """;
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
