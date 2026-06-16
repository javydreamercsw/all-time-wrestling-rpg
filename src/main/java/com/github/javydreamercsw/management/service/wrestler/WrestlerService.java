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
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseWrestlerExclusionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.utils.DiceBag;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class WrestlerService {

  private final WrestlerRepository wrestlerRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final TierBoundaryRepository tierBoundaryRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final TierRecalculationService tierRecalculationService;
  private final DefaultImageService imageService;
  private final LegacyService legacyService;
  private final com.github.javydreamercsw.management.service.injury.InjuryService injuryService;
  private final SecurityUtils securityUtils;
  private final com.github.javydreamercsw.management.domain.universe.UniverseRepository
      universeRepository;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final UniverseContextService universeContextService;
  private final UniverseWrestlerExclusionRepository wrestlerExclusionRepository;
  private final UniverseSettingsService universeSettingsService;

  @Autowired
  public WrestlerService(
      final WrestlerRepository wrestlerRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final TierBoundaryRepository tierBoundaryRepository,
      final ApplicationEventPublisher eventPublisher,
      final TierRecalculationService tierRecalculationService,
      final DefaultImageService imageService,
      final LegacyService legacyService,
      @Lazy final com.github.javydreamercsw.management.service.injury.InjuryService injuryService,
      final SecurityUtils securityUtils,
      final com.github.javydreamercsw.management.domain.universe.UniverseRepository
          universeRepository,
      final WrestlerAlignmentRepository wrestlerAlignmentRepository,
      final UniverseContextService universeContextService,
      final UniverseWrestlerExclusionRepository wrestlerExclusionRepository,
      final UniverseSettingsService universeSettingsService) {
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.tierBoundaryRepository = tierBoundaryRepository;
    this.eventPublisher = eventPublisher;
    this.tierRecalculationService = tierRecalculationService;
    this.imageService = imageService;
    this.legacyService = legacyService;
    this.injuryService = injuryService;
    this.securityUtils = securityUtils;
    this.universeRepository = universeRepository;
    this.wrestlerAlignmentRepository = wrestlerAlignmentRepository;
    this.universeContextService = universeContextService;
    this.wrestlerExclusionRepository = wrestlerExclusionRepository;
    this.universeSettingsService = universeSettingsService;
  }

  @Transactional
  @CacheEvict(
      value = {CacheConfig.WRESTLERS_CACHE, CacheConfig.WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public Wrestler save(@NonNull final Wrestler wrestler) {
    return wrestlerRepository.save(wrestler);
  }

  @Transactional
  @CacheEvict(
      value = {CacheConfig.WRESTLERS_CACHE, CacheConfig.WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public void delete(@NonNull final Long id) {
    wrestlerRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findById(@NonNull final Long id) {
    return wrestlerRepository.findById(id);
  }

  /** Find wrestler by ID and fetch injuries and status cards. */
  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public Optional<Wrestler> findByIdWithDetails(final Long id) {
    Optional<Wrestler> wrestler = wrestlerRepository.findByIdWithStatuses(id);
    wrestler.ifPresent(
        w -> {
          w.getStatuses().size();
          w.getWrestlerStates().size();
          w.getAlignments().size();
        });
    return wrestler;
  }

  /** Get all wrestlers (alias for findAll for UI compatibility). */
  @PreAuthorize("isAuthenticated()")
  @Cacheable(value = CacheConfig.WRESTLERS_CACHE, key = "'all'")
  public List<Wrestler> getAllWrestlers() {
    return wrestlerRepository.findAll();
  }

  /**
   * Returns all active wrestlers with their {@code alignments} collection eagerly loaded, safe to
   * call from detached / non-transactional contexts such as Vaadin views. Uses a single JOIN FETCH
   * query to avoid LazyInitializationException when {@link Wrestler#getAlignment()} is called after
   * the session closes.
   */
  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public List<Wrestler> findAllActiveWithAlignments() {
    return wrestlerRepository.findAllWithAlignments().stream()
        .filter(w -> Boolean.TRUE.equals(w.getActive()))
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  @Cacheable(value = CacheConfig.WRESTLERS_CACHE, key = "'active'")
  public List<Wrestler> findAllActiveWrestlers() {
    return wrestlerRepository.findAllByActiveTrue();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public java.util.Map<Long, WrestlerState> getStateMapByUniverseId(
      @NonNull final Long universeId) {
    return wrestlerStateRepository.findByUniverseIdWithWrestler(universeId).stream()
        .filter(s -> s.getWrestler() != null)
        .collect(
            java.util.stream.Collectors.toMap(
                s -> s.getWrestler().getId(), s -> s, (existing, duplicate) -> existing));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByName(@NonNull final String name) {
    return wrestlerRepository.findByName(name);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByExternalId(@NonNull final String externalId) {
    return wrestlerRepository.findByExternalId(externalId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = CacheConfig.WRESTLERS_CACHE,
      key = "'all'")
  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll();
  }

  // ==================== ATW RPG METHODS ====================

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllByAccount(@NonNull final Account account) {
    return wrestlerRepository.findByAccount(account);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllIncludingInactive() {
    return wrestlerRepository.findAll();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Wrestler> findPageFiltered(
      @Nullable final Collection<String> enabledExpansionCodes,
      @Nullable final Collection<Long> excludedIds,
      final Pageable pageable) {
    Specification<Wrestler> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (enabledExpansionCodes != null && !enabledExpansionCodes.isEmpty()) {
            predicates.add(root.get("expansionCode").in(enabledExpansionCodes));
          }
          if (excludedIds != null && !excludedIds.isEmpty()) {
            predicates.add(root.get("id").in(excludedIds).not());
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return wrestlerRepository.findAll(spec, pageable);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countFiltered(
      @Nullable final Collection<String> enabledExpansionCodes,
      @Nullable final Collection<Long> excludedIds) {
    Specification<Wrestler> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (enabledExpansionCodes != null && !enabledExpansionCodes.isEmpty()) {
            predicates.add(root.get("expansionCode").in(enabledExpansionCodes));
          }
          if (excludedIds != null && !excludedIds.isEmpty()) {
            predicates.add(root.get("id").in(excludedIds).not());
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return wrestlerRepository.count(spec);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable final AlignmentType alignmentType,
      final com.github.javydreamercsw.base.domain.wrestler.@Nullable Gender gender,
      @Nullable final Long universeId) {
    return findAllFiltered(alignmentType, gender, universeId, (String) null, null);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable final AlignmentType alignmentType,
      final com.github.javydreamercsw.base.domain.wrestler.@Nullable Gender gender,
      @Nullable final String expansionCode) {
    return findAllFiltered(alignmentType, gender, null, expansionCode, null);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable final AlignmentType alignmentType,
      final com.github.javydreamercsw.base.domain.wrestler.@Nullable Gender gender,
      @Nullable final Long universeId,
      @Nullable final String expansionCode) {
    return findAllFiltered(alignmentType, gender, universeId, expansionCode, null);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable final AlignmentType alignmentType,
      final com.github.javydreamercsw.base.domain.wrestler.@Nullable Gender gender,
      @NonNull final Long universeId,
      @Nullable final Set<Wrestler> includedWrestlers) {
    return findAllFiltered(alignmentType, gender, universeId, null, includedWrestlers);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable final AlignmentType alignmentType,
      final com.github.javydreamercsw.base.domain.wrestler.@Nullable Gender gender,
      @Nullable final Long universeId,
      @Nullable final String expansionCode,
      @Nullable final Set<Wrestler> includedWrestlers) {
    final Long finalUniverseId =
        universeId != null ? universeId : universeContextService.getCurrentUniverseId();

    // Load excluded wrestler IDs for this universe once, outside the stream.
    // includedWrestlers acts as a force-include override (e.g. already-assigned segment
    // participants) that bypasses both the exclusion list and all other filters.
    final Set<Long> excludedIds =
        wrestlerExclusionRepository.findExcludedWrestlerIdsByUniverseId(finalUniverseId);

    // When no specific expansionCode is requested, restrict to the universe's enabled expansions.
    final Set<String> enabledExpansionCodes;
    if (expansionCode == null) {
      com.github.javydreamercsw.management.domain.universe.Universe universe =
          universeRepository.findById(finalUniverseId).orElse(null);
      enabledExpansionCodes =
          universe != null
              ? universeSettingsService.getEnabledExpansionCodesForUniverse(universe)
              : null;
    } else {
      enabledExpansionCodes = null;
    }

    return findAllActiveWrestlers().stream()
        .filter(
            w -> {
              if (includedWrestlers != null && includedWrestlers.contains(w)) {
                return true;
              }

              if (excludedIds.contains(w.getId())) {
                return false;
              }

              boolean matchesAlignment = alignmentType == null;
              if (alignmentType != null) {
                // Fetch alignment for specific universe
                Optional<WrestlerAlignment> alignment =
                    wrestlerAlignmentRepository.findByWrestlerAndUniverseId(w, finalUniverseId);
                if (alignment.isPresent()) {
                  matchesAlignment = alignment.get().getAlignmentType() == alignmentType;
                } else {
                  // Fallback to wrestler's default alignment if no universe-specific one found
                  matchesAlignment =
                      w.getAlignment() != null
                          && w.getAlignment().getAlignmentType() == alignmentType;
                }
              }

              boolean matchesGender = gender == null || w.getGender() == gender;
              boolean matchesExpansion =
                  expansionCode != null
                      ? expansionCode.equals(w.getExpansionCode())
                      : (enabledExpansionCodes == null
                          || enabledExpansionCodes.contains(w.getExpansionCode()));
              return matchesAlignment && matchesGender && matchesExpansion;
            })
        .sorted(Comparator.comparing(Wrestler::getName))
        .toList();
  }

  @Transactional
  @CacheEvict(
      value = {CacheConfig.WRESTLERS_CACHE, CacheConfig.WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public Wrestler createWrestler(
      @NonNull final String name, @NonNull final Boolean isPlayer, final String description) {
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
      value = {CacheConfig.WRESTLERS_CACHE, CacheConfig.WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public Wrestler createWrestler(
      @NonNull final String name,
      @NonNull final Boolean isPlayer,
      final String description,
      @NonNull final WrestlerTier tier,
      final Universe universe) {
    Wrestler wrestler = createWrestler(name, isPlayer, description);
    if (universe != null) {
      WrestlerState state = getOrCreateState(wrestler.getId(), universe.getId());
      state.setTier(tier);
      wrestlerStateRepository.save(state);
    }
    return wrestler;
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public void setAccountForWrestler(@NonNull final Long wrestlerId, final Long accountId) {
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
  public WrestlerState getOrCreateState(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    Optional<WrestlerState> existing =
        wrestlerStateRepository.findByWrestlerIdAndUniverseId(wrestlerId, universeId);
    if (existing.isPresent()) {
      return existing.get();
    }

    // Fallback for missing entities during tests/sync
    Wrestler wrestler = wrestlerRepository.findById(wrestlerId).orElse(null);
    Universe universe = universeRepository.findById(universeId).orElse(null);

    if (wrestler == null || universe == null) {
      String msg =
          "Cannot create persistent WrestlerState: Wrestler %d or Universe %d not found."
              .formatted(wrestlerId, universeId);
      log.error(msg);
      throw new IllegalStateException(msg);
    }

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
  @org.springframework.cache.annotation.Caching(
      evict = {
        @CacheEvict(value = CacheConfig.WRESTLERS_CACHE, key = "#wrestlerId"),
        @CacheEvict(
            value = CacheConfig.WRESTLER_STATS_CACHE,
            key = "#wrestlerId + ':' + #universeId")
      })
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<WrestlerState> awardFans(
      @NonNull final Long wrestlerId, @NonNull final Long universeId, @NonNull final Long fans) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    long tempFans = fans;
    if (fans > 0) {
      tempFans =
          (long) (fans * (1.0 + (state.getMorale() != null ? state.getMorale() : 100.0) / 500.0));
    }

    long totalFans = state.getFans() + tempFans;
    if (totalFans < 0) {
      totalFans = 0;
    }

    log.debug(
        "Awarding {} fans to wrestler {} in universe {}. New total: {}",
        tempFans,
        wrestlerId,
        universeId,
        totalFans);

    state.setFans(totalFans);
    tierRecalculationService.recalculateTier(state);
    WrestlerState savedState = wrestlerStateRepository.save(state);
    eventPublisher.publishEvent(new FanAwardedEvent(this, savedState, tempFans));

    return Optional.of(savedState);
  }

  /**
   * Add a bump to a wrestler, potentially leading to an injury.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @return The updated state, or empty if not found
   */
  @Transactional
  @org.springframework.cache.annotation.Caching(
      evict = {
        @CacheEvict(value = CacheConfig.WRESTLERS_CACHE, key = "#wrestlerId"),
        @CacheEvict(
            value = CacheConfig.WRESTLER_STATS_CACHE,
            key = "#wrestlerId + ':' + #universeId")
      })
  public Optional<WrestlerState> addBump(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);

    if (state.addBump()) {
      // Automatic injury triggered
      injuryService.createInjuryFromBumps(wrestlerId, universeId);
    }

    WrestlerState savedState = wrestlerStateRepository.save(state);
    eventPublisher.publishEvent(new WrestlerBumpEvent(this, savedState));

    return Optional.of(savedState);
  }

  /**
   * Heal a bump for a wrestler.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @return The updated state, or empty if not found
   */
  @Transactional
  @org.springframework.cache.annotation.Caching(
      evict = {
        @CacheEvict(value = CacheConfig.WRESTLERS_CACHE, key = "#wrestlerId"),
        @CacheEvict(
            value = CacheConfig.WRESTLER_STATS_CACHE,
            key = "#wrestlerId + ':' + #universeId")
      })
  public Optional<WrestlerState> healBump(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    if (state.getBumps() > 0) {
      state.setBumps(state.getBumps() - 1);
      WrestlerState savedState = wrestlerStateRepository.save(state);
      eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, savedState));
      return Optional.of(savedState);
    }
    log.warn(
        "Wrestler {} has 0 bumps in universe {}, cannot heal further.", wrestlerId, universeId);
    return Optional.of(state);
  }

  /**
   * Chance to heal a bump for a wrestler based on a dice roll.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @param diceBag The dice bag to use
   * @return The updated state, or empty if not found
   */
  @Transactional
  @org.springframework.cache.annotation.Caching(
      evict = {
        @CacheEvict(value = CacheConfig.WRESTLERS_CACHE, key = "#wrestlerId"),
        @CacheEvict(
            value = CacheConfig.WRESTLER_STATS_CACHE,
            key = "#wrestlerId + ':' + #universeId")
      })
  public Optional<WrestlerState> healChance(
      @NonNull final Long wrestlerId,
      @NonNull final Long universeId,
      @NonNull final DiceBag diceBag) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);

    boolean hasInjury =
        !injuryService.getActiveInjuriesForWrestler(wrestlerId, universeId).isEmpty();
    int roll = diceBag.roll();

    // Standard recovery roll: 4+ heals a bump. If injured, 5+ required.
    int target = hasInjury ? 5 : 4;

    if (roll >= target) {
      log.debug(
          "Wrestler {} rolled {} (target {}) and healed a bump in universe {}.",
          wrestlerId,
          roll,
          target,
          universeId);
      return healBump(wrestlerId, universeId);
    }

    log.debug(
        "Wrestler {} rolled {} (target {}) and failed to heal a bump in universe {}.",
        wrestlerId,
        roll,
        target,
        universeId);
    return Optional.of(state);
  }

  public Optional<WrestlerState> healChance(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    return healChance(wrestlerId, universeId, new DiceBag(6));
  }

  /**
   * Get all active wrestlers in a specific universe by their tier.
   *
   * @param tier The tier to filter by
   * @param universeId The universe ID
   * @return List of wrestlers
   */
  @Transactional(readOnly = true)
  public List<Wrestler> getWrestlersByTier(
      @NonNull final WrestlerTier tier, @NonNull final Long universeId) {
    return wrestlerStateRepository.findByUniverseIdAndTier(universeId, tier).stream()
        .map(WrestlerState::getWrestler)
        .toList();
  }

  /**
   * Get all active player-controlled wrestlers in a specific universe.
   *
   * @param universeId The universe ID
   * @return List of wrestlers
   */
  @Transactional(readOnly = true)
  public List<Wrestler> getPlayerWrestlers(@NonNull final Long universeId) {
    return wrestlerStateRepository.findByWrestlerIsPlayerTrueAndUniverseId(universeId).stream()
        .map(WrestlerState::getWrestler)
        .toList();
  }

  /**
   * Get all active NPC wrestlers in a specific universe.
   *
   * @param universeId The universe ID
   * @return List of wrestlers
   */
  @Transactional(readOnly = true)
  public List<Wrestler> getNpcWrestlers(@NonNull final Long universeId) {
    return wrestlerStateRepository.findByWrestlerIsPlayerFalseAndUniverseId(universeId).stream()
        .map(WrestlerState::getWrestler)
        .toList();
  }

  /**
   * Spend fans for a wrestler in a specific universe.
   *
   * @param wrestlerId The wrestler's ID
   * @param universeId The universe's ID
   * @param cost The number of fans to spend
   * @return true if successful
   */
  @Transactional
  public boolean spendFans(
      @NonNull final Long wrestlerId, @NonNull final Long universeId, @NonNull final Long cost) {
    return awardFans(wrestlerId, universeId, -cost).isPresent();
  }

  /**
   * Recalibrate the wrestler image path if needed.
   *
   * @param wrestler The wrestler
   * @return The resolved image source
   */
  public com.github.javydreamercsw.base.image.ImageResolution resolveWrestlerImage(
      @NonNull final Wrestler wrestler) {
    return imageService.resolveImage(wrestler.getName(), ImageCategory.WRESTLER);
  }

  /**
   * Recalibrates all wrestler fan counts in a universe based on their recent performance.
   *
   * @param universeId The universe ID
   */
  @Transactional
  public void recalibrateFanCounts(@NonNull final Long universeId) {
    List<Wrestler> wrestlers = findAll();
    WrestlerTier[] tiers = WrestlerTier.values();
    for (Wrestler w : wrestlers) {
      WrestlerState state = getOrCreateState(w.getId(), universeId);
      WrestlerTier currentTier = state.getTier();
      // ICON is demoted one tier down; all others reset to their tier's minimum
      if (currentTier.ordinal() > 0 && currentTier == WrestlerTier.ICON) {
        state.setFans(tiers[currentTier.ordinal() - 1].getMinFans());
      } else {
        state.setFans(currentTier.getMinFans());
      }
      wrestlerStateRepository.save(state);
    }
    log.info("Recalibrated wrestler tiers and fan counts for universe {}.", universeId);
  }

  /**
   * Resets all wrestler fan counts to zero and tier to ROOKIE in a universe.
   *
   * @param universeId The universe ID
   */
  @Transactional
  @CacheEvict(
      value = {CacheConfig.WRESTLERS_CACHE, CacheConfig.WRESTLER_STATS_CACHE},
      allEntries = true)
  public void resetAllFanCountsToZero(@NonNull final Long universeId) {
    wrestlerStateRepository.resetFansAndTierByUniverseId(universeId);
    log.info("Reset all wrestler fan counts to 0 and tier to ROOKIE in universe {}.", universeId);
  }

  /**
   * Resets the physical condition of a wrestler to 100% in a universe.
   *
   * @param wrestlerId The wrestler ID
   * @param universeId The universe ID
   */
  @Transactional
  @CacheEvict(value = CacheConfig.WRESTLER_STATS_CACHE, key = "#wrestlerId + ':' + #universeId")
  public void resetWearAndTear(@NonNull final Long wrestlerId, @NonNull final Long universeId) {
    WrestlerState state = getOrCreateState(wrestlerId, universeId);
    state.setPhysicalCondition(100);
    wrestlerStateRepository.save(state);
    log.debug(
        "Reset physical condition for wrestler {} to 100% in universe {}.", wrestlerId, universeId);
  }

  /**
   * Resets the physical condition of all wrestlers to 100% in a universe.
   *
   * @param universeId The universe ID
   */
  @Transactional
  @CacheEvict(value = CacheConfig.WRESTLER_STATS_CACHE, allEntries = true)
  public void resetAllWearAndTear(@NonNull final Long universeId) {
    wrestlerStateRepository.resetPhysicalConditionByUniverseId(universeId);
    log.info("Reset physical condition for all wrestlers to 100% in universe {}.", universeId);
  }
}
