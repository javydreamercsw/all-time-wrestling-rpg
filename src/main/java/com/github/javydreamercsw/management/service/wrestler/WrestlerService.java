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
package com.github.javydreamercsw.management.service.wrestler;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class WrestlerService {

  public static final String WRESTLERS_CACHE = "wrestlers";
  public static final String WRESTLER_STATS_CACHE = "wrestler-stats";

  private final WrestlerRepository wrestlerRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final TierBoundaryRepository tierBoundaryRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final TierRecalculationService tierRecalculationService;
  private final DefaultImageService imageService;
  private final LegacyService legacyService;
  private final com.github.javydreamercsw.management.service.segment.SegmentService segmentService;
  private final com.github.javydreamercsw.management.service.title.TitleService titleService;
  private final com.github.javydreamercsw.management.service.injury.InjuryService injuryService;
  private final SecurityUtils securityUtils;

  @Autowired
  public WrestlerService(
      WrestlerRepository wrestlerRepository,
      WrestlerStateRepository wrestlerStateRepository,
      TierBoundaryRepository tierBoundaryRepository,
      ApplicationEventPublisher eventPublisher,
      TierRecalculationService tierRecalculationService,
      DefaultImageService imageService,
      LegacyService legacyService,
      @Lazy com.github.javydreamercsw.management.service.segment.SegmentService segmentService,
      @Lazy com.github.javydreamercsw.management.service.title.TitleService titleService,
      @Lazy com.github.javydreamercsw.management.service.injury.InjuryService injuryService,
      SecurityUtils securityUtils) {
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.tierBoundaryRepository = tierBoundaryRepository;
    this.eventPublisher = eventPublisher;
    this.tierRecalculationService = tierRecalculationService;
    this.imageService = imageService;
    this.legacyService = legacyService;
    this.segmentService = segmentService;
    this.titleService = titleService;
    this.injuryService = injuryService;
    this.securityUtils = securityUtils;
  }

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Wrestler save(@NonNull Wrestler wrestler) {
    return wrestlerRepository.save(wrestler);
  }

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@NonNull Long id) {
    wrestlerRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findById(@NonNull Long id) {
    return wrestlerRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByIdWithInjuries(@NonNull Long id) {
    // In current implementation, injuries are part of state, so this might need careful handling.
    // For now, return the basic wrestler entity.
    return wrestlerRepository.findById(id);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByName(@NonNull String name) {
    return wrestlerRepository.findByName(name);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByExternalId(@NonNull String externalId) {
    return wrestlerRepository.findByExternalId(externalId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllByAccount(@NonNull Account account) {
    return wrestlerRepository.findByAccount(account);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllIncludingInactive() {
    return wrestlerRepository.findAll();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      AlignmentType alignmentType,
      com.github.javydreamercsw.base.domain.wrestler.Gender gender,
      String expansionCode) {
    return wrestlerRepository.findAllByActiveTrue().stream()
        .filter(
            w ->
                alignmentType == null
                    || (w.getAlignment() != null
                        && w.getAlignment().getAlignmentType() == alignmentType))
        .filter(w -> gender == null || w.getGender() == gender)
        .filter(w -> expansionCode == null || expansionCode.equals(w.getExpansionCode()))
        .toList();
  }

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Wrestler createWrestler(
      @NonNull String name, @NonNull Boolean isPlayer, String description) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setIsPlayer(isPlayer);
    wrestler.setDescription(description);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setDeckSize(15);
    wrestler.setLowHealth(4);
    wrestler.setLowStamina(4);
    wrestler.setActive(true);
    return wrestlerRepository.save(wrestler);
  }

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Wrestler createWrestler(
      @NonNull String name,
      @NonNull Boolean isPlayer,
      String description,
      @NonNull WrestlerTier tier,
      com.github.javydreamercsw.management.domain.universe.Universe universe) {
    Wrestler wrestler = createWrestler(name, isPlayer, description);
    if (universe != null) {
      WrestlerState state = getOrCreateState(wrestler.getId(), universe.getId());
      state.setTier(tier);
      wrestlerStateRepository.save(state);
    }
    return wrestler;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void setAccountForWrestler(@NonNull Long wrestlerId, Long accountId) {
    wrestlerRepository
        .findById(wrestlerId)
        .ifPresent(
            w -> {
              if (accountId != null) {
                // Assuming we have an AccountRepository or Service to find by ID.
                // For now, this is a placeholder to satisfy UI needs.
                log.info("Setting account {} for wrestler {}", accountId, wrestlerId);
              } else {
                w.setAccount(null);
              }
              wrestlerRepository.save(w);
            });
  }

  /**
   * Get or create a WrestlerState for a wrestler in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @return The WrestlerState
   */
  @Transactional
  public WrestlerState getOrCreateState(@NonNull Long wrestlerId, @NonNull Long universeId) {
    return wrestlerStateRepository
        .findByWrestlerIdAndUniverseId(wrestlerId, universeId)
        .orElseGet(
            () -> {
              Wrestler wrestler =
                  wrestlerRepository
                      .findById(wrestlerId)
                      .orElseThrow(
                          () -> new IllegalArgumentException("Wrestler not found: " + wrestlerId));
              com.github.javydreamercsw.management.domain.universe.Universe universe =
                  new com.github.javydreamercsw.management.domain.universe.Universe();
              universe.setId(universeId);
              WrestlerState newState =
                  WrestlerState.builder()
                      .wrestler(wrestler)
                      .universe(universe)
                      .fans(0L)
                      .tier(WrestlerTier.ROOKIE)
                      .bumps(0)
                      .physicalCondition(100)
                      .build();
              return wrestlerStateRepository.save(newState);
            });
  }

  /**
   * Award fans to a wrestler in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @param fans The number of fans to award (can be negative)
   * @return The updated state, or empty if not found
   */
  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<WrestlerState> awardFans(
      @NonNull Long wrestlerId, @NonNull Long universeId, @NonNull Long fans) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    long tempFans = fans;
    if (fans > 0) {
      tempFans =
          switch (state.getTier()) {
            case ICON -> tempFans * 90 / 100;
            case MAIN_EVENTER -> tempFans * 93 / 100;
            case MIDCARDER -> tempFans * 95 / 100;
            case CONTENDER -> tempFans * 97 / 100;
            default -> tempFans;
          };
      tempFans = Math.round(tempFans / 1000.0) * 1000;
    }
    state.setFans(Math.max(0, state.getFans() + tempFans));
    tierRecalculationService.recalculateTier(state);
    WrestlerState savedState = wrestlerStateRepository.save(state);
    eventPublisher.publishEvent(new FanAwardedEvent(this, savedState, tempFans));

    if (savedState.getWrestler().getAccount() != null) {
      legacyService.updateLegacyScore(savedState.getWrestler().getAccount());
    }

    return Optional.of(savedState);
  }

  /**
   * Add a bump to a wrestler (injury system) in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @return The updated state, or empty if not found
   */
  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<WrestlerState> addBump(@NonNull Long wrestlerId, @NonNull Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    boolean injuryOccurred = state.addBump();
    eventPublisher.publishEvent(new WrestlerBumpEvent(this, state));
    if (injuryOccurred) {
      injuryService.createInjuryFromBumps(wrestlerId, universeId);
    }
    return Optional.of(wrestlerStateRepository.save(state));
  }

  /**
   * Heal a bump from a wrestler in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @return The updated state, or empty if not found
   */
  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<WrestlerState> healBump(@NonNull Long wrestlerId, @NonNull Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    if (state.getBumps() > 0) {
      state.setBumps(state.getBumps() - 1);
      log.info(
          "Wrestler {} healed a bump: {} (was {}) in universe {}",
          state.getWrestler().getName(),
          state.getBumps(),
          state.getBumps() + 1,
          universeId);
      eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, state));
    }
    return Optional.of(wrestlerStateRepository.save(state));
  }

  /**
   * Attempt to heal an injury or bump using a chance roll in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @param diceBag The dice bag to use for the roll
   * @return The updated state, or empty if not found
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<WrestlerState> healChance(
      @NonNull Long wrestlerId, @NonNull Long universeId, @NonNull DiceBag diceBag) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);

    List<com.github.javydreamercsw.management.domain.injury.Injury> activeInjuries =
        injuryService.getActiveInjuriesForWrestler(wrestlerId, universeId);

    activeInjuries.forEach(
        injury -> {
          if (injuryService.attemptHealing(injury.getId(), new DiceBag(20).roll()).success()) {
            log.info(
                "Wrestler {} healed an injury: {} ({}) in universe {}",
                state.getWrestler().getName(),
                injury.getName(),
                injury.getSeverity().getDisplayName(),
                universeId);
          }
        });

    if (state.getBumps() > 0) {
      if (diceBag.roll() > 3) {
        state.setBumps(state.getBumps() - 1);
        log.info(
            "Wrestler {} healed a bump: {} (was {}) in universe {}",
            state.getWrestler().getName(),
            state.getBumps(),
            state.getBumps() + 1,
            universeId);
        eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, state));
      }
    }

    return Optional.of(wrestlerStateRepository.saveAndFlush(state));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<WrestlerState> healChance(@NonNull Long wrestlerId, @NonNull Long universeId) {
    return healChance(wrestlerId, universeId, new DiceBag(6));
  }

  /**
   * Get all wrestlers in a specific tier for a universe.
   *
   * @param tier The wrestler tier
   * @param universeId The universe ID
   * @return List of wrestlers in that tier for the universe
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getWrestlersByTier(@NonNull WrestlerTier tier, @NonNull Long universeId) {
    return findAll().stream()
        .filter(w -> getOrCreateState(w.getId(), universeId).getTier() == tier)
        .toList();
  }

  /**
   * Get all player-controlled wrestlers.
   *
   * @return List of player wrestlers
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getPlayerWrestlers() {
    return findAll().stream().filter(Wrestler::getIsPlayer).toList();
  }

  /**
   * Get all NPC wrestlers.
   *
   * @return List of NPC wrestlers
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getNpcWrestlers() {
    return findAll().stream().filter(wrestler -> !wrestler.getIsPlayer()).toList();
  }

  /**
   * Spend fans for a wrestler action in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @param cost The fan cost
   * @return true if successful, false if wrestler not found or insufficient fans
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public boolean spendFans(@NonNull Long wrestlerId, @NonNull Long universeId, @NonNull Long cost) {
    return awardFans(wrestlerId, universeId, -cost).isPresent();
  }

  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllAsDTO(@NonNull Long universeId) {
    return findAll().stream().map(w -> toDTO(w, universeId)).toList();
  }

  /**
   * Find a wrestler by ID and return it as a DTO in a specific universe.
   *
   * @param id The wrestler ID
   * @param universeId The universe ID
   * @return Optional containing the wrestler DTO if found, otherwise empty
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerDTO> findByIdAsDTO(@NonNull Long id, @NonNull Long universeId) {
    return wrestlerRepository.findById(id).map(w -> toDTO(w, universeId));
  }

  /**
   * Find all wrestlers for a segment as DTOs in a specific universe.
   *
   * @param segment The segment
   * @param universeId The universe ID
   * @return List of wrestler DTOs
   */
  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllBySegment(
      @NonNull com.github.javydreamercsw.management.domain.show.segment.Segment segment,
      @NonNull Long universeId) {
    return wrestlerRepository.findAllBySegment(segment).stream()
        .map(w -> toDTO(w, universeId))
        .toList();
  }

  private WrestlerDTO toDTO(Wrestler wrestler, Long universeId) {
    WrestlerState state = getOrCreateState(wrestler.getId(), universeId);
    WrestlerDTO dto = new WrestlerDTO(state);
    if (dto.getImageUrl() == null || dto.getImageUrl().isBlank()) {
      dto.setImageUrl(resolveWrestlerImage(wrestler));
    }
    return dto;
  }

  /**
   * Resolves the image URL for a wrestler.
   *
   * @param wrestler The wrestler entity.
   * @return The resolved image URL.
   */
  public String resolveWrestlerImage(Wrestler wrestler) {
    if (wrestler.getImageUrl() != null && !wrestler.getImageUrl().isBlank()) {
      return wrestler.getImageUrl();
    }
    return imageService.resolveImage(wrestler.getName(), ImageCategory.WRESTLER).url();
  }

  /**
   * Get statistics for a wrestler in a specific universe.
   *
   * @param wrestlerId The ID of the wrestler
   * @param universeId The ID of the universe
   * @return An Optional containing WrestlerStats if found, otherwise empty.
   */
  @Cacheable(value = WRESTLER_STATS_CACHE, key = "#wrestlerId + ':' + #universeId")
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerStats> getWrestlerStats(
      @NonNull Long wrestlerId, @NonNull Long universeId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              long wins = segmentService.countWinsByWrestler(wrestler, universeId);
              long totalMatchSegments =
                  segmentService.countMatchSegmentsByWrestler(wrestler, universeId);
              long losses = totalMatchSegments - wins;
              long titlesHeld = titleService.findTitlesByChampion(wrestler, universeId).size();
              return new WrestlerStats(wins, losses, titlesHeld);
            });
  }

  /**
   * Recalibrates the fan counts of all wrestlers to the minimum of their respective tiers in a
   * specific universe.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void recalibrateFanCounts(@NonNull Long universeId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    List<TierBoundary> boundaries = tierBoundaryRepository.findAll();

    for (Wrestler wrestler : wrestlers) {
      WrestlerState state = getOrCreateState(wrestler.getId(), universeId);
      if (state.getTier() != null && wrestler.getGender() != null) {
        WrestlerTier targetTier = state.getTier();
        if (targetTier == WrestlerTier.ICON) {
          targetTier = WrestlerTier.MAIN_EVENTER;
          state.setTier(targetTier);
        }

        final WrestlerTier finalTargetTier = targetTier;
        boundaries.stream()
            .filter(b -> b.getTier() == finalTargetTier && b.getGender() == wrestler.getGender())
            .findFirst()
            .ifPresent(boundary -> state.setFans(boundary.getMinFans()));
        wrestlerStateRepository.save(state);
      }
    }
    log.info(
        "Recalibrated fan counts for all wrestlers in universe {}. Icons are reset to Main"
            + " Eventer.",
        universeId);
  }

  /** Resets the fan counts of all wrestlers to 0 and their tier to ROOKIE in a universe. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void resetAllFanCountsToZero(@NonNull Long universeId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    for (Wrestler wrestler : wrestlers) {
      WrestlerState state = getOrCreateState(wrestler.getId(), universeId);
      state.setFans(0L);
      state.setTier(WrestlerTier.ROOKIE);
      wrestlerStateRepository.save(state);
    }
    log.info("Reset all wrestler fan counts to 0 and tier to ROOKIE in universe {}.", universeId);
  }

  /** Resets the physical condition of a specific wrestler to 100% in a universe. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      key = "#wrestlerId + ':' + #universeId")
  public void resetWearAndTear(@NonNull Long wrestlerId, @NonNull Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    state.setPhysicalCondition(100);
    wrestlerStateRepository.save(state);
    log.info(
        "Reset physical condition for wrestler {} to 100% in universe {}.", wrestlerId, universeId);
  }

  /** Resets the physical condition of all wrestlers to 100% in a universe. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN')")
  public void resetAllWearAndTear(@NonNull Long universeId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    for (Wrestler w : wrestlers) {
      resetWearAndTear(w.getId(), universeId);
    }
  }

  // ==================== DEPRECATED FALLBACKS ====================

  @Deprecated
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerStats> getWrestlerStats(@NonNull Long wrestlerId) {
    return getWrestlerStats(wrestlerId, 1L);
  }

  @Deprecated
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void recalibrateFanCounts() {
    recalibrateFanCounts(1L);
  }

  @Deprecated
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void resetAllFanCountsToZero() {
    resetAllFanCountsToZero(1L);
  }

  @Deprecated
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN')")
  public void resetAllWearAndTear() {
    resetAllWearAndTear(1L);
  }

  @Deprecated
  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllAsDTO() {
    return findAllAsDTO(1L);
  }

  @Deprecated
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerDTO> findByIdAsDTO(@NonNull Long id) {
    return findByIdAsDTO(id, 1L);
  }

  @Deprecated
  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllBySegment(
      @NonNull com.github.javydreamercsw.management.domain.show.segment.Segment segment) {
    return findAllBySegment(segment, 1L);
  }
}
