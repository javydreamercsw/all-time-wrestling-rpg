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

import static com.github.javydreamercsw.management.config.CacheConfig.WRESTLERS_CACHE;
import static com.github.javydreamercsw.management.config.CacheConfig.WRESTLER_STATS_CACHE;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.expansion.ExpansionToggledEvent;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.utils.DiceBag;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueWrestlerState;
import com.github.javydreamercsw.management.domain.league.LeagueWrestlerStateRepository;

@Service
@Transactional
@Slf4j
public class WrestlerService {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private DramaEventRepository dramaEventRepository;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private LeagueWrestlerStateRepository leagueWrestlerStateRepository;
  @Autowired private Clock clock;
  @Autowired private ExpansionService expansionService;
  @Autowired private InjuryService injuryService;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private SegmentService segmentService;
  @Autowired private TitleService titleService;
  @Autowired private TierRecalculationService tierRecalculationService;
  @Autowired private TierBoundaryRepository tierBoundaryRepository;
  @Autowired private LegacyService legacyService;
  @Autowired private DefaultImageService imageService;

  /**
   * Find all active wrestlers filtered by alignment and gender.
   *
   * @param alignment Optional alignment filter
   * @param gender Optional gender filter
   * @param includedWrestlers Set of wrestlers to always include regardless of filters
   * @return Filtered list of wrestlers sorted by name
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllFiltered(
      @Nullable AlignmentType alignment,
      @Nullable Gender gender,
      @Nullable Set<Wrestler> includedWrestlers) {
    return findAll().stream()
        .filter(
            w -> {
              if (includedWrestlers != null && includedWrestlers.contains(w)) {
                return true;
              }
              boolean matchesAlignment =
                  alignment == null
                      || (w.getAlignment() != null
                          && w.getAlignment().getAlignmentType() == alignment);
              boolean matchesGender = gender == null || w.getGender() == gender;
              return matchesAlignment && matchesGender;
            })
        .sorted(Comparator.comparing(Wrestler::getName))
        .collect(Collectors.toList());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void createWrestler(@NonNull String name) {
    createWrestler(name, false, "", WrestlerTier.ROOKIE, null);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Wrestler createWrestler(@NonNull String name, boolean isPlayer, String description) {
    return createWrestler(name, isPlayer, description, WrestlerTier.ROOKIE, null);
  }

  /**
   * Create a new wrestler with ATW RPG defaults.
   *
   * @param name Wrestler name
   * @param isPlayer Whether this is a player-controlled wrestler
   * @param description Character description
   * @return The created wrestler
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Wrestler createWrestler(
      @NonNull String name, boolean isPlayer, String description, @NonNull WrestlerTier tier) {
    return createWrestler(name, isPlayer, description, tier, null);
  }

  /**
   * Create a new wrestler with ATW RPG defaults, optionally linking to an account.
   *
   * @param name Wrestler name
   * @param isPlayer Whether this is a player-controlled wrestler
   * @param description Character description
   * @param tier The tier of the wrestler
   * @param account The account to link to this wrestler (optional)
   * @return The created wrestler
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Wrestler createWrestler(
      @NonNull String name,
      boolean isPlayer,
      String description,
      @NonNull WrestlerTier tier,
      @Nullable Account account) {
    Wrestler wrestler =
        Wrestler.builder()
            .name(name)
            .description(description == null ? "Default Description" : description)
            .deckSize(15)
            .startingHealth(15)
            .lowHealth(0)
            .startingStamina(0)
            .lowStamina(0)
            .fans(0L)
            .gender(Gender.MALE)
            .isPlayer(isPlayer)
            .active(true)
            .bumps(0)
            .tier(tier)
            .account(account) // Set account here
            .expansionCode("BASE_GAME")
            .build();

    return save(wrestler);
  }

  /**
   * Assigns an account to an existing wrestler.
   *
   * @param wrestlerId The ID of the wrestler.
   * @param accountId The ID of the account to assign, or null to unassign.
   * @return The updated wrestler, or empty if wrestler not found.
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Wrestler> setAccountForWrestler(
      @NonNull Long wrestlerId, @Nullable Long accountId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              Account account = null;
              if (accountId != null) {
                account =
                    accountRepository
                        .findById(accountId)
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException(
                                    "Account not found with ID: " + accountId));
              }

              wrestler.setAccount(account);
              wrestler.setIsPlayer(account != null); // Set isPlayer based on account presence
              return wrestlerRepository.saveAndFlush(wrestler);
            });
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> list(@NonNull Pageable pageable) {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return wrestlerRepository.findAllBy(pageable).stream()
        .filter(w -> enabledExpansions.contains(w.getExpansionCode()))
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return wrestlerRepository.findAllByActiveTrue().stream()
        .filter(w -> enabledExpansions.contains(w.getExpansionCode()))
        .count();
  }

  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#wrestler)")
  public Wrestler save(@NonNull Wrestler wrestler) {
    wrestler.setCreationDate(clock.instant());
    return wrestlerRepository.saveAndFlush(wrestler);
  }

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void delete(@NonNull Wrestler wrestler) {
    dramaEventRepository.deleteByPrimaryWrestlerOrSecondaryWrestler(wrestler, wrestler);
    wrestlerRepository.delete(wrestler);
    log.debug("Deleted wrestler: {}", wrestler.getName());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllIncludingInactive() {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return wrestlerRepository.findAll(Sort.by(Sort.Direction.DESC, "fans")).stream()
        .filter(w -> enabledExpansions.contains(w.getExpansionCode()))
        .collect(Collectors.toList());
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAll() {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return wrestlerRepository.findAllByActiveTrue().stream()
        .filter(w -> enabledExpansions.contains(w.getExpansionCode()))
        .collect(Collectors.toList());
  }

  @org.springframework.context.event.EventListener
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void onExpansionToggled(ExpansionToggledEvent event) {
    log.info("Expansion '{}' toggled, evicting wrestler caches.", event.getExpansionCode());
  }

  /** Get wrestler by ID. */
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> getWrestlerById(Long id) {
    return wrestlerRepository.findById(id);
  }

  /** Find wrestler by ID (alias for getWrestlerById for consistency). */
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findById(Long id) {
    return getWrestlerById(id);
  }

  /** Find wrestler by ID and fetch injuries. */
  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByIdWithInjuries(Long id) {
    return wrestlerRepository.findByIdWithInjuries(id);
  }

  /** Get all wrestlers (alias for findAll for UI compatibility). */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getAllWrestlers() {
    return findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByName(String name) {
    return wrestlerRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findByAccount(@NonNull Account account) {
    return wrestlerRepository.findByAccount(account);
  }

  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findAllByAccount(@NonNull Account account) {
    return wrestlerRepository.findAllByAccount(account);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Wrestler> findByExternalId(String externalId) {
    return wrestlerRepository.findByExternalId(externalId);
  }

  public LeagueWrestlerState getOrCreateState(Long wrestlerId, Long leagueId) {
    return leagueWrestlerStateRepository
        .findByWrestlerIdAndLeagueId(wrestlerId, leagueId)
        .orElseGet(
            () -> {
              Wrestler wrestler =
                  wrestlerRepository
                      .findById(wrestlerId)
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  "Wrestler not found with ID: " + wrestlerId));
              League league =
                  leagueRepository
                      .findById(leagueId)
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException("League not found with ID: " + leagueId));
              LeagueWrestlerState state =
                  LeagueWrestlerState.builder()
                      .wrestler(wrestler)
                      .league(league)
                      .currentHealth(wrestler.getStartingHealth())
                      .tier(WrestlerTier.ROOKIE)
                      .fans(0L)
                      .bumps(0)
                      .morale(100)
                      .managementStamina(100)
                      .physicalCondition(100)
                      .build();
              return leagueWrestlerStateRepository.save(state);
            });
  }

  // ==================== ATW RPG METHODS ====================

  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<LeagueWrestlerState> awardFans(
      @NonNull Long wrestlerId, @NonNull Long leagueId, @NonNull Long fans) {
    LeagueWrestlerState state = getOrCreateState(wrestlerId, leagueId);
    if (fans < 0 && state.getFans() < -fans) {
      return Optional.empty(); // Not enough fans
    }
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
    LeagueWrestlerState savedState = leagueWrestlerStateRepository.save(state);
    eventPublisher.publishEvent(new FanAwardedEvent(this, savedState.getWrestler(), tempFans));

    if (savedState.getWrestler().getAccount() != null) {
      legacyService.updateLegacyScore(savedState.getWrestler().getAccount());
    }

    return Optional.of(savedState);
  }

  /**
   * Attempt to heal a bump to a wrestler (injury system).
   *
   * @param wrestlerId The wrestler's ID
   * @return The updated wrestler, or empty if not found
   */
  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<LeagueWrestlerState> addBump(@NonNull Long wrestlerId, @NonNull Long leagueId) {
    LeagueWrestlerState state = getOrCreateState(wrestlerId, leagueId);
    boolean injuryOccurred = state.addBump();
    // Always publish WrestlerBumpEvent when a bump is added
    eventPublisher.publishEvent(new WrestlerBumpEvent(this, state.getWrestler()));
    if (injuryOccurred) {
      // Create injury using the injury service only if a new injury occurred
      injuryService.createInjuryFromBumps(wrestlerId, leagueId);
    }
    return Optional.of(leagueWrestlerStateRepository.save(state));
  }

  /**
   * Heal a bump from a wrestler.
   *
   * @param wrestlerId The wrestler's ID
   * @param leagueId The league's ID
   * @return The updated state, or empty if not found
   */
  @Transactional
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<LeagueWrestlerState> healBump(@NonNull Long wrestlerId, @NonNull Long leagueId) {
    LeagueWrestlerState state = getOrCreateState(wrestlerId, leagueId);
    if (state.getBumps() > 0) {
      state.setBumps(state.getBumps() - 1);
      log.info(
          "Wrestler {} healed a bump: {} (was {}) in league {}",
          state.getWrestler().getName(),
          state.getBumps(),
          state.getBumps() + 1,
          leagueId);
      eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, state.getWrestler()));
    }
    return Optional.of(leagueWrestlerStateRepository.save(state));
  }

  /**
   * Add a bump to a wrestler (injury system).
   *
   * @param wrestlerId The wrestler's ID
   * @param leagueId The league's ID
   * @return The updated state, or empty if not found
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<LeagueWrestlerState> healChance(
      @NonNull Long wrestlerId, @NonNull Long leagueId, @NonNull DiceBag diceBag) {
    LeagueWrestlerState state = getOrCreateState(wrestlerId, leagueId);
    state
        .getActiveInjuries()
        .forEach(
            injury -> {
              if (injuryService.attemptHealing(injury.getId(), new DiceBag(20).roll()).success()) {
                log.info(
                    "Wrestler {} healed an injury: {} ({}) in league {}",
                    state.getWrestler().getName(),
                    injury.getName(),
                    injury.getSeverity().getDisplayName(),
                    leagueId);
              }
            });

    if (state.getBumps() > 0) {
      if (diceBag.roll() > 3) {
        state.setBumps(state.getBumps() - 1);
        log.info(
            "Wrestler {} healed a bump: {} (was {}) in league {}",
            state.getWrestler().getName(),
            state.getBumps(),
            state.getBumps() + 1,
            leagueId);
        eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, state.getWrestler()));
      }
    }

    return Optional.of(leagueWrestlerStateRepository.saveAndFlush(state));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<LeagueWrestlerState> healChance(@NonNull Long wrestlerId, @NonNull Long leagueId) {
    return healChance(wrestlerId, leagueId, new DiceBag(6));
  }

  /**
   * Get all wrestlers in a specific tier for a league.
   *
   * @param tier The wrestler tier
   * @param leagueId The league ID
   * @return List of wrestlers in that tier for the league
   */
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> getWrestlersByTier(@NonNull WrestlerTier tier, @NonNull Long leagueId) {
    return findAll().stream()
        .filter(w -> getOrCreateState(w.getId(), leagueId).getTier() == tier)
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
   * Spend fans for a wrestler action.
   *
   * @param wrestlerId The wrestler's ID
   * @param leagueId The league's ID
   * @param cost The fan cost
   * @return true if successful, false if wrestler not found or insufficient fans
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public boolean spendFans(@NonNull Long wrestlerId, @NonNull Long leagueId, @NonNull Long cost) {
    return awardFans(wrestlerId, leagueId, -cost).isPresent();
  }

  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllAsDTO(@NonNull Long leagueId) {
    return findAll().stream().map(w -> toDTO(w, leagueId)).toList();
  }

  /**
   * Find a wrestler by ID and return it as a DTO.
   *
   * @param id The wrestler ID
   * @param leagueId The league ID
   * @return Optional containing the wrestler DTO if found, otherwise empty
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerDTO> findByIdAsDTO(@NonNull Long id, @NonNull Long leagueId) {
    return wrestlerRepository.findById(id).map(w -> toDTO(w, leagueId));
  }

  /**
   * Find all wrestlers for a segment as DTOs.
   *
   * @param segment The segment
   * @param leagueId The league ID
   * @return List of wrestler DTOs
   */
  @PreAuthorize("isAuthenticated()")
  public List<WrestlerDTO> findAllBySegment(
      @NonNull com.github.javydreamercsw.management.domain.show.segment.Segment segment,
      @NonNull Long leagueId) {
    return wrestlerRepository.findAllBySegment(segment).stream()
        .map(w -> toDTO(w, leagueId))
        .toList();
  }

  private WrestlerDTO toDTO(Wrestler wrestler, Long leagueId) {
    LeagueWrestlerState state = getOrCreateState(wrestler.getId(), leagueId);
    WrestlerDTO dto = new WrestlerDTO(state);
    if (dto.getImageUrl() == null || dto.getImageUrl().isBlank()) {
      dto.setImageUrl(resolveWrestlerImage(wrestler));
    }
    return dto;
  }

  /**
   * Resolves the image URL for a wrestler, using the default image system if no specific URL is
   * set.
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
   * Get statistics for a wrestler, including wins, losses, and titles held.
   *
   * @param wrestlerId The ID of the wrestler
   * @param leagueId The ID of the league
   * @return An Optional containing WrestlerStats if the wrestler is found, otherwise empty.
   */
  @Cacheable(value = WRESTLER_STATS_CACHE, key = "#wrestlerId + ':' + #leagueId")
  @PreAuthorize("isAuthenticated()")
  public Optional<WrestlerStats> getWrestlerStats(@NonNull Long wrestlerId, @NonNull Long leagueId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              // Note: segmentService counts might need league context too
              long wins = segmentService.countWinsByWrestler(wrestler, leagueId);
              long totalMatchSegments = segmentService.countMatchSegmentsByWrestler(wrestler, leagueId);
              long losses = totalMatchSegments - wins;
              long titlesHeld = titleService.findTitlesByChampion(wrestler, leagueId).size();

              return new WrestlerStats(wins, losses, titlesHeld);
            });
  }

  /**
   * Recalibrates the fan counts of all wrestlers to the minimum of their respective tiers in a
   * league.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void recalibrateFanCounts(@NonNull Long leagueId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    List<TierBoundary> boundaries = tierBoundaryRepository.findAll();

    for (Wrestler wrestler : wrestlers) {
      LeagueWrestlerState state = getOrCreateState(wrestler.getId(), leagueId);
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
        leagueWrestlerStateRepository.save(state);
      }
    }
    log.info(
        "Recalibrated fan counts for all wrestlers in league {}. Icons are reset to Main Eventer.",
        leagueId);
  }

  /** Resets the fan counts of all wrestlers to 0 and their tier to ROOKIE in a league. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void resetAllFanCountsToZero(@NonNull Long leagueId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();

    for (Wrestler wrestler : wrestlers) {
      LeagueWrestlerState state = getOrCreateState(wrestler.getId(), leagueId);
      state.setFans(0L);
      state.setTier(WrestlerTier.ROOKIE);
      leagueWrestlerStateRepository.save(state);
    }
    log.info("Reset all wrestler fan counts to 0 and tier to ROOKIE in league {}.", leagueId);
  }

  /** Resets the physical condition of a specific wrestler to 100% in a league. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      key = "#wrestlerId + ':' + #leagueId")
  public void resetWearAndTear(@NonNull Long wrestlerId, @NonNull Long leagueId) {
    LeagueWrestlerState state = getOrCreateState(wrestlerId, leagueId);
    state.setPhysicalCondition(100);
    leagueWrestlerStateRepository.save(state);
    log.info(
        "Reset physical condition for wrestler {} to 100% in league {}.", wrestlerId, leagueId);
  }

  /** Resets the physical condition of all wrestlers to 100% in a league. */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN')")
  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void resetAllWearAndTear(@NonNull Long leagueId) {
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    for (Wrestler wrestler : wrestlers) {
      LeagueWrestlerState state = getOrCreateState(wrestler.getId(), leagueId);
      state.setPhysicalCondition(100);
      leagueWrestlerStateRepository.save(state);
    }
    log.info("Reset physical condition for all wrestlers to 100% in league {}.", leagueId);
  }
}
