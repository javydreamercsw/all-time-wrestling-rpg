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
package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final ApplicationEventPublisher eventPublisher;
  private final Random random = new Random();

  /** Get all faction rivalries with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<FactionRivalry> getAllFactionRivalries(Pageable pageable) {
    return factionRivalryRepository.findAllBy(pageable);
  }

  /** Get all faction rivalries with factions eagerly loaded. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<FactionRivalry> getAllFactionRivalriesWithFactions(Pageable pageable) {
    return factionRivalryRepository.findAllWithFactions(pageable);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<FactionRivalry> getFactionRivalryById(Long id) {
    return factionRivalryRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getActiveFactionRivalries() {
    return factionRivalryRepository.findByIsActiveTrue();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getActiveRivalriesForFaction(Long factionId) {
    Optional<Faction> factionOpt = factionRepository.findById(factionId);

    if (factionOpt.isEmpty()) {
      return List.of();
    }

    return factionRivalryRepository.findActiveRivalriesForFaction(factionOpt.get());
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
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

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public Optional<FactionRivalry> addHeat(Long rivalryId, int heatGain, String reason) {
    return factionRivalryRepository
        .findById(rivalryId)
        .filter(FactionRivalry::getIsActive)
        .map(
            rivalry -> {
              // Apply alignment heat multiplier
              double multiplier = rivalry.getIntensityHeatMultiplier();
              int adjustedHeatGain = (int) Math.round(heatGain * multiplier);

              int oldHeat = rivalry.getHeat();
              rivalry.addHeat(adjustedHeatGain, reason);

              FactionRivalry savedRivalry = factionRivalryRepository.saveAndFlush(rivalry);

              log.info(
                  "Added {} heat to faction rivalry {} (total: {}, reason: {})",
                  adjustedHeatGain,
                  rivalry.getDisplayName(),
                  rivalry.getHeat(),
                  reason);

              eventPublisher.publishEvent(
                  new FactionHeatChangeEvent(
                      this,
                      savedRivalry,
                      oldHeat,
                      reason,
                      Stream.concat(
                              rivalry.getFaction1().getMembers().stream(),
                              rivalry.getFaction2().getMembers().stream())
                          .collect(Collectors.toList())));

              return savedRivalry;
            });
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
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

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public ResolutionResult<FactionRivalry> attemptResolution(
      Long rivalryId, Integer faction1Roll, Integer faction2Roll) {
    Optional<FactionRivalry> rivalryOpt = factionRivalryRepository.findById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return new ResolutionResult<>(false, "Faction rivalry not found", null, 0, 0, 0);
    }

    FactionRivalry rivalry = rivalryOpt.get();

    if (!rivalry.canAttemptResolution()) {
      return new ResolutionResult<>(
          false,
          String.format(
              "Faction rivalry needs at least 20 heat to attempt resolution (current: %d)",
              rivalry.getHeat()),
          rivalry,
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

    return new ResolutionResult<>(
        resolved,
        resolved ? "Faction rivalry resolved successfully" : "Faction resolution attempt failed",
        rivalry,
        roll1,
        roll2,
        total);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
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

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getRivalriesRequiringMatches() {
    return factionRivalryRepository.findRivalriesRequiringMatches();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getRivalriesEligibleForResolution() {
    return factionRivalryRepository.findRivalriesEligibleForResolution();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getRivalriesRequiringStipulationMatches() {
    return factionRivalryRepository.findRivalriesRequiringStipulationMatches();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getHottestRivalries(int limit) {
    return factionRivalryRepository.findHottestRivalries(Pageable.ofSize(limit));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getTagTeamRivalries() {
    return factionRivalryRepository.findTagTeamRivalries();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getRivalriesInvolvingStables() {
    return factionRivalryRepository.findRivalriesInvolvingStables();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<FactionRivalry> getFactionRivalryBetweenFactions(
      @NonNull Long faction1Id, @NonNull Long faction2Id) {
    Optional<Faction> faction1Opt = factionRepository.findById(faction1Id);
    Optional<Faction> faction2Opt = factionRepository.findById(faction2Id);

    if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
      return Optional.empty();
    }

    return factionRivalryRepository.findActiveRivalryBetween(faction1Opt.get(), faction2Opt.get());
  }

  /** Check if two factions can have a rivalry. */
  private boolean canHaveRivalry(Faction faction1, Faction faction2) {
    // Both factions must be active
    if (!faction1.isActive() || !faction2.isActive()) {
      return false;
    }

    // Cannot have rivalry with self
    if (faction1.equals(faction2)) {
      return false;
    }

    // Both factions must have at least one member
    return faction1.getMemberCount() != 0 && faction2.getMemberCount() != 0;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Long getTotalWrestlersInRivalries() {
    Long total = factionRivalryRepository.countTotalWrestlersInRivalries();
    return total != null ? total : 0L;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<FactionRivalry> getRivalriesWithMostWrestlers(int limit) {
    return factionRivalryRepository.findRivalriesWithMostWrestlers(Pageable.ofSize(limit));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<FactionRivalry> findByExternalId(@NonNull String externalId) {
    return factionRivalryRepository.findByExternalId(externalId);
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public FactionRivalry save(@NonNull FactionRivalry rivalry) {
    return factionRivalryRepository.saveAndFlush(rivalry);
  }
}
