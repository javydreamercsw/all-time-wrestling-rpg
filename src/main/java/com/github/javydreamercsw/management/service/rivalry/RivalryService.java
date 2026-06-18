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
package com.github.javydreamercsw.management.service.rivalry;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.event.HeatChangeEvent;
import com.github.javydreamercsw.management.event.RivalryCompletedEvent;
import com.github.javydreamercsw.management.event.RivalryContinuesEvent;
import com.github.javydreamercsw.management.mapper.RivalryMapper;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing rivalries in the ATW RPG system. Handles heat management, rivalry
 * resolution, and storyline progression.
 */
@Service
@Transactional
public class RivalryService {

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired @Getter private RivalryMapper rivalryMapper;
  @Autowired private Clock clock;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private GameSettingService gameSettingService;

  /** Create a new rivalry between two wrestlers in the Default Universe. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> createRivalry(
      @NonNull final Long wrestler1Id,
      @NonNull final Long wrestler2Id,
      @NonNull final String storylineNotes) {
    return createRivalry(wrestler1Id, wrestler2Id, storylineNotes, 1L);
  }

  /** Create a new rivalry between two wrestlers in the given universe. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> createRivalry(
      @NonNull final Long wrestler1Id,
      @NonNull final Long wrestler2Id,
      @NonNull final String storylineNotes,
      @NonNull final Long universeId) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);
    Optional<Universe> universeOpt = universeRepository.findById(universeId);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty() || universeOpt.isEmpty()) {
      return Optional.empty();
    }

    Wrestler wrestler1 = wrestler1Opt.get();
    Wrestler wrestler2 = wrestler2Opt.get();

    // Check if rivalry already exists
    Optional<Rivalry> existingRivalry =
        rivalryRepository.findActiveRivalryBetween(wrestler1, wrestler2);
    if (existingRivalry.isPresent()) {
      return existingRivalry;
    }

    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setUniverse(universeOpt.get());
    rivalry.setHeat(0);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now(clock));
    rivalry.setStorylineNotes(storylineNotes);
    rivalry.setCreationDate(Instant.now(clock));

    return Optional.of(rivalryRepository.saveAndFlush(rivalry));
  }

  /** Add heat to a rivalry. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> addHeat(final Long rivalryId, final int heatGain, final String reason) {
    return rivalryRepository
        .findById(rivalryId)
        .filter(Rivalry::getIsActive)
        .map(
            rivalry -> {
              int oldHeat = rivalry.getHeat();
              rivalry.addHeat(heatGain, reason);
              Rivalry savedRivalry = rivalryRepository.saveAndFlush(rivalry);
              // Publish event
              eventPublisher.publishEvent(
                  new HeatChangeEvent(
                      this,
                      savedRivalry,
                      oldHeat,
                      reason,
                      List.of(rivalry.getWrestler1(), rivalry.getWrestler2())));
              return savedRivalry;
            });
  }

  /** Add heat between two specific wrestlers in the Default Universe. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> addHeatBetweenWrestlers(
      @NonNull final Long wrestler1Id,
      @NonNull final Long wrestler2Id,
      final int heatGain,
      @NonNull final String reason) {
    return addHeatBetweenWrestlers(wrestler1Id, wrestler2Id, heatGain, reason, 1L);
  }

  /** Add heat between two specific wrestlers in the given universe. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> addHeatBetweenWrestlers(
      @NonNull final Long wrestler1Id,
      @NonNull final Long wrestler2Id,
      final int heatGain,
      @NonNull final String reason,
      @NonNull final Long universeId) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return Optional.empty();
    }

    Optional<Rivalry> rivalryOpt =
        rivalryRepository.findActiveRivalryBetween(wrestler1Opt.get(), wrestler2Opt.get());

    if (rivalryOpt.isEmpty()) {
      rivalryOpt =
          createRivalry(wrestler1Id, wrestler2Id, "Auto-generated from heat event", universeId);
    }

    return rivalryOpt.flatMap(rivalry -> addHeat(rivalry.getId(), heatGain, reason));
  }

  /** Attempt to resolve a rivalry at a PLE (uses the configured PLE threshold). */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public ResolutionResult<Rivalry> attemptResolution(
      @NonNull final Long rivalryId,
      @NonNull final Integer wrestler1Roll,
      @NonNull final Integer wrestler2Roll) {
    return attemptResolution(
        rivalryId,
        wrestler1Roll,
        wrestler2Roll,
        gameSettingService.getRivalryResolutionThresholdPle());
  }

  /** Attempt to resolve a rivalry with an explicit threshold. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public ResolutionResult<Rivalry> attemptResolution(
      @NonNull final Long rivalryId,
      @NonNull final Integer wrestler1Roll,
      @NonNull final Integer wrestler2Roll,
      final int threshold) {
    Optional<Rivalry> rivalryOpt = rivalryRepository.findById(rivalryId);

    if (rivalryOpt.isEmpty()) {
      return new ResolutionResult<>(false, "Rivalry not found", null, 0, 0, 0);
    }

    Rivalry rivalry = rivalryOpt.get();

    if (!rivalry.canAttemptResolution()) {
      return new ResolutionResult<>(
          false,
          "Rivalry needs at least 20 heat to attempt resolution (current: %d)"
              .formatted(rivalry.getHeat()),
          rivalry,
          0,
          0,
          0);
    }

    int roll1 = wrestler1Roll;
    int roll2 = wrestler2Roll;
    int total = roll1 + roll2;

    boolean resolved = rivalry.attemptResolution(roll1, roll2, threshold);

    if (resolved) {
      rivalry.endRivalry("Rivalry resolved successfully");
      rivalry.setIsActive(false);
      rivalryRepository.saveAndFlush(rivalry);
      eventPublisher.publishEvent(new RivalryCompletedEvent(this, rivalry));
    } else {
      eventPublisher.publishEvent(new RivalryContinuesEvent(this, rivalry));
    }

    return new ResolutionResult<>(
        resolved,
        resolved ? "Rivalry resolved successfully" : "Resolution attempt failed",
        rivalry,
        roll1,
        roll2,
        total);
  }

  /** End a rivalry manually. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> endRivalry(@NonNull final Long rivalryId, @NonNull final String reason) {
    return rivalryRepository
        .findById(rivalryId)
        .filter(Rivalry::getIsActive)
        .map(
            rivalry -> {
              rivalry.endRivalry(reason);
              return rivalryRepository.saveAndFlush(rivalry);
            });
  }

  /** Get rivalry by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      key = "#rivalryId")
  public Optional<Rivalry> getRivalryById(@NonNull final Long rivalryId) {
    return rivalryRepository.findById(rivalryId);
  }

  /**
   * Get a rivalry by ID and return it as a DTO.
   *
   * @param rivalryId The rivalry ID
   * @return Optional containing the rivalry DTO if found, otherwise empty
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<RivalryDTO> findByIdAsDTO(@NonNull final Long rivalryId) {
    return getRivalryById(rivalryId).map(rivalryMapper::toRivalryDTO);
  }

  /** Get all rivalries with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Rivalry> getAllRivalries(@NonNull final Pageable pageable) {
    return rivalryRepository.findAllBy(pageable);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Rivalry> getAllRivalriesWithWrestlers(@NonNull final Pageable pageable) {
    return rivalryRepository.findAllWithWrestlers(pageable);
  }

  /** Get active rivalries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      key = "'active'")
  public List<Rivalry> getActiveRivalries() {
    return rivalryRepository.findByIsActiveTrue();
  }

  /** Get active rivalries between two dates. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Rivalry> getActiveRivalriesBetween(final Instant startDate, final Instant endDate) {
    return rivalryRepository.findActiveRivalriesBetween(startDate, endDate);
  }

  /** Get rivalries for a specific wrestler. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      key = "#wrestlerId")
  public List<Rivalry> getRivalriesForWrestler(@NonNull final Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(rivalryRepository::findActiveRivalriesForWrestler)
        .orElse(List.of());
  }

  /**
   * Get all active rivalries for a specific wrestler as DTOs.
   *
   * @param wrestlerId The wrestler ID
   * @return List of rivalry DTOs
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<RivalryDTO> getActiveRivalriesForWrestlerAsDTO(@NonNull final Long wrestlerId) {
    return getRivalriesForWrestler(wrestlerId).stream().map(rivalryMapper::toRivalryDTO).toList();
  }

  /** Get rivalries requiring matches (10+ heat). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Rivalry> getRivalriesRequiringMatches() {
    return rivalryRepository.findRivalriesRequiringMatches();
  }

  /** Get rivalries eligible for resolution (20+ heat). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Rivalry> getRivalriesEligibleForResolution() {
    return rivalryRepository.findRivalriesEligibleForResolution();
  }

  /** Get rivalries requiring rule matches (30+ heat). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Rivalry> getRivalriesRequiringStipulationMatches() {
    return rivalryRepository.findRivalriesRequiringStipulationMatches();
  }

  /** Get rivalries by intensity level. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      key = "#intensity")
  public List<Rivalry> getRivalriesByIntensity(@NonNull final RivalryIntensity intensity) {
    return rivalryRepository.findByHeatRange(
        intensity.getMinHeat(),
        intensity.getMaxHeat() == Integer.MAX_VALUE ? 999 : intensity.getMaxHeat());
  }

  /** Get the hottest rivalries. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Rivalry> getHottestRivalries(final int limit) {
    return rivalryRepository.findHottestRivalries(
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  /** Update rivalry storyline notes. */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Optional<Rivalry> updateStorylineNotes(
      @NonNull final Long rivalryId, @NonNull final String storylineNotes) {
    return rivalryRepository
        .findById(rivalryId)
        .map(
            rivalry -> {
              rivalry.setStorylineNotes(storylineNotes);
              return rivalryRepository.saveAndFlush(rivalry);
            });
  }

  /** Get rivalry statistics. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public RivalryStats getRivalryStats(@NonNull final Long rivalryId) {
    return rivalryRepository
        .findById(rivalryId)
        .map(
            rivalry ->
                new RivalryStats(
                    rivalry.getId(),
                    rivalry.getWrestler1().getName(),
                    rivalry.getWrestler2().getName(),
                    rivalry.getHeat(),
                    rivalry.getIntensity(),
                    rivalry.mustWrestleNextShow(),
                    rivalry.canAttemptResolution(),
                    rivalry.requiresStipulationMatch(),
                    rivalry.getDurationDays(),
                    rivalry.getIsActive(),
                    rivalry.getHeatEvents().size()))
        .orElse(null);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.RIVALRIES_CACHE,
      allEntries = true)
  public Rivalry save(@NonNull final Rivalry rivalry) {
    return rivalryRepository.saveAndFlush(rivalry);
  }

  /** Get rivalry between two wrestlers. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Rivalry> getRivalryBetweenWrestlers(
      @NonNull final Long wrestler1Id, @NonNull final Long wrestler2Id) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return Optional.empty();
    }

    return rivalryRepository.findActiveRivalryBetween(wrestler1Opt.get(), wrestler2Opt.get());
  }

  /** Check if two wrestlers have rivalry history. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean hasRivalryHistory(
      @NonNull final Long wrestler1Id, @NonNull final Long wrestler2Id) {
    Optional<Wrestler> wrestler1Opt = wrestlerRepository.findById(wrestler1Id);
    Optional<Wrestler> wrestler2Opt = wrestlerRepository.findById(wrestler2Id);

    if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
      return false;
    }

    return rivalryRepository.hasRivalryHistory(wrestler1Opt.get(), wrestler2Opt.get());
  }

  /** Rivalry statistics data class. */
  public record RivalryStats(
      Long rivalryId,
      String wrestler1Name,
      String wrestler2Name,
      int heat,
      RivalryIntensity intensity,
      boolean mustWrestleNextShow,
      boolean canAttemptResolution,
      boolean requiresStipulationMatch,
      long durationDays,
      boolean isActive,
      int heatEventCount) {}
}
