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

import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.utils.DiceBag;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WrestlerService {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DramaEventRepository dramaEventRepository;
  @Autowired private Clock clock;
  @Autowired private InjuryService injuryService;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private SegmentService segmentService; // Autowire SegmentService
  @Autowired private TitleService titleService; // Autowire TitleService

  public void createWrestler(@NonNull String name) {
    createWrestler(name, false, "");
  }

  public List<Wrestler> list(@NonNull Pageable pageable) {
    return wrestlerRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return wrestlerRepository.count();
  }

  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public Wrestler save(@NonNull Wrestler wrestler) {
    wrestler.setCreationDate(clock.instant());
    return wrestlerRepository.saveAndFlush(wrestler);
  }

  @Transactional
  public void delete(@NonNull Wrestler wrestler) {
    dramaEventRepository.deleteByPrimaryWrestlerOrSecondaryWrestler(wrestler, wrestler);
    wrestlerRepository.delete(wrestler);
    log.info("Deleted wrestler: {}", wrestler.getName());
  }

  // @Cacheable(value = WRESTLERS_CACHE, key = "'all'")
  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll(Sort.by(Sort.Direction.DESC, "fans"));
  }

  /** Get wrestler by ID. */
  public Optional<Wrestler> getWrestlerById(Long id) {
    return wrestlerRepository.findById(id);
  }

  /** Find wrestler by ID (alias for getWrestlerById for consistency). */
  public Optional<Wrestler> findById(Long id) {
    return getWrestlerById(id);
  }

  /** Find wrestler by ID and fetch injuries. */
  public Optional<Wrestler> findByIdWithInjuries(Long id) {
    return wrestlerRepository.findByIdWithInjuries(id);
  }

  /** Get all wrestlers (alias for findAll for UI compatibility). */
  public List<Wrestler> getAllWrestlers() {
    return findAll();
  }

  @Cacheable(value = WRESTLERS_CACHE, key = "'name:' + #name")
  public Optional<Wrestler> findByName(String name) {
    return wrestlerRepository.findByName(name);
  }

  public Optional<Wrestler> findByExternalId(String externalId) {
    return wrestlerRepository.findByExternalId(externalId);
  }

  // ==================== ATW RPG METHODS ====================

  /**
   * Award fans to a wrestler and update their tier.
   *
   * @param wrestlerId The wrestler's ID
   * @param fanGain The number of fans to award (can be negative for losses)
   * @return The updated wrestler
   */
  public Optional<Wrestler> awardFans(@NonNull Long wrestlerId, @NonNull Long fanGain) {
    return updateFans(wrestlerId, fanGain);
  }

  private Optional<Wrestler> updateFans(@NonNull Long wrestlerId, @NonNull Long fanChange) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              if (fanChange < 0 && !wrestler.canAfford(-fanChange)) {
                return null; // Not enough fans
              }
              long tempFans = fanChange;
              if (fanChange > 0) {
                switch (wrestler.getTier()) {
                  case ICON -> tempFans = tempFans * 90 / 100;
                  case MAIN_EVENTER -> tempFans = tempFans * 93 / 100;
                  case MIDCARDER -> tempFans = tempFans * 95 / 100;
                  case CONTENDER -> tempFans = tempFans * 97 / 100;
                }
                tempFans = Math.round(tempFans / 1000.0) * 1000;
              }
              wrestler.addFans(tempFans);
              Wrestler savedWrestler = wrestlerRepository.saveAndFlush(wrestler);
              eventPublisher.publishEvent(new FanAwardedEvent(this, savedWrestler, tempFans));
              return savedWrestler;
            });
  }

  /**
   * Attempt to heal a bump to a wrestler (injury system).
   *
   * @param wrestlerId The wrestler's ID
   * @return The updated wrestler, or empty if not found
   */
  public Optional<Wrestler> addBump(@NonNull Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              boolean injuryOccurred = wrestler.addBump();
              // Always publish WrestlerBumpEvent when a bump is added
              eventPublisher.publishEvent(new WrestlerBumpEvent(this, wrestler));
              if (injuryOccurred) {
                // Create injury using the injury service only if a new injury occurred
                injuryService.createInjuryFromBumps(wrestlerId);
              }
              return wrestlerRepository.saveAndFlush(wrestler);
            });
  }

  /**
   * Heal a bump from a wrestler.
   *
   * @param wrestlerId The wrestler's ID
   * @return The updated wrestler, or empty if not found
   */
  public Optional<Wrestler> healBump(@NonNull Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              if (wrestler.getBumps() > 0) {
                wrestler.setBumps(wrestler.getBumps() - 1);
                log.info(
                    "Wrestler {} healed a bump: {} (was {})",
                    wrestler.getName(),
                    wrestler.getBumps(),
                    wrestler.getBumps() + 1);
                eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, wrestler));
              }
              return wrestlerRepository.saveAndFlush(wrestler);
            });
  }

  /**
   * Add a bump to a wrestler (injury system).
   *
   * @param wrestlerId The wrestler's ID
   * @return The updated wrestler, or empty if not found
   */
  public Optional<Wrestler> healChance(@NonNull Long wrestlerId, @NonNull DiceBag diceBag) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              wrestler
                  .getActiveInjuries()
                  .forEach(
                      injury -> {
                        if (injuryService
                            .attemptHealing(injury.getId(), new DiceBag(20).roll())
                            .success()) {
                          log.info(
                              "Wrestler {} healed an injury: {} ({})",
                              wrestler.getName(),
                              injury.getName(),
                              injury.getSeverity().getDisplayName());
                        }
                      });

              if (wrestler.getBumps() > 0) {
                if (diceBag.roll() > 3) {
                  wrestler.setBumps(wrestler.getBumps() - 1);
                  log.info(
                      "Wrestler {} healed a bump: {} (was {})",
                      wrestler.getName(),
                      wrestler.getBumps(),
                      wrestler.getBumps() + 1);
                  eventPublisher.publishEvent(new WrestlerBumpHealedEvent(this, wrestler));
                }
              }

              return wrestlerRepository.saveAndFlush(wrestler);
            });
  }

  public Optional<Wrestler> healChance(@NonNull Long wrestlerId) {
    return healChance(wrestlerId, new DiceBag(6));
  }

  /**
   * Get all wrestlers eligible for a specific title.
   *
   * @param titleTier The title tier to check eligibility for
   * @return List of eligible wrestlers
   */
  public List<Wrestler> getEligibleWrestlers(@NonNull WrestlerTier titleTier) {
    return wrestlerRepository.findAll().stream()
        .filter(wrestler -> wrestler.isEligibleForTitle(titleTier))
        .toList();
  }

  /**
   * Get all wrestlers in a specific tier.
   *
   * @param tier The wrestler tier
   * @return List of wrestlers in that tier
   */
  public List<Wrestler> getWrestlersByTier(@NonNull WrestlerTier tier) {
    return wrestlerRepository.findAll().stream()
        .filter(wrestler -> wrestler.getTier() == tier)
        .toList();
  }

  /**
   * Get all player-controlled wrestlers.
   *
   * @return List of player wrestlers
   */
  public List<Wrestler> getPlayerWrestlers() {
    return wrestlerRepository.findAll().stream().filter(Wrestler::getIsPlayer).toList();
  }

  /**
   * Get all NPC wrestlers.
   *
   * @return List of NPC wrestlers
   */
  public List<Wrestler> getNpcWrestlers() {
    return wrestlerRepository.findAll().stream()
        .filter(wrestler -> !wrestler.getIsPlayer())
        .toList();
  }

  /**
   * Spend fans for a wrestler action.
   *
   * @param wrestlerId The wrestler's ID
   * @param cost The fan cost
   * @return true if successful, false if wrestler not found or insufficient fans
   */
  public boolean spendFans(@NonNull Long wrestlerId, @NonNull Long cost) {
    return updateFans(wrestlerId, -cost).isPresent();
  }

  /**
   * Create a new wrestler with ATW RPG defaults.
   *
   * @param name Wrestler name
   * @param isPlayer Whether this is a player-controlled wrestler
   * @param description Character description
   * @return The created wrestler
   */
  public Wrestler createWrestler(@NonNull String name, boolean isPlayer, String description) {
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
            .bumps(0)
            .build();

    return save(wrestler);
  }

  public List<WrestlerDTO> findAllAsDTO() {
    return findAll().stream().map(WrestlerDTO::new).toList();
  }

  /**
   * Get statistics for a wrestler, including wins, losses, and titles held.
   *
   * @param wrestlerId The ID of the wrestler
   * @return An Optional containing WrestlerStats if the wrestler is found, otherwise empty.
   */
  @Cacheable(value = WRESTLER_STATS_CACHE, key = "#wrestlerId")
  public Optional<WrestlerStats> getWrestlerStats(@NonNull Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              long wins = segmentService.countWinsByWrestler(wrestler);
              long totalMatchSegments = segmentService.countMatchSegmentsByWrestler(wrestler);
              long losses = totalMatchSegments - wins;
              long titlesHeld = titleService.findTitlesByChampion(wrestler).size();

              return new WrestlerStats(wins, losses, titlesHeld);
            });
  }
}
