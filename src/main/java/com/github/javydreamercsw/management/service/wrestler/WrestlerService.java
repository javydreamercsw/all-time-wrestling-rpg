package com.github.javydreamercsw.management.service.wrestler;

import static com.github.javydreamercsw.management.config.CacheConfig.WRESTLERS_CACHE;
import static com.github.javydreamercsw.management.config.CacheConfig.WRESTLER_STATS_CACHE;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WrestlerService {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private Clock clock;
  @Autowired private InjuryService injuryService;

  public void createCard(@NonNull String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    // Set default card game values
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);
    // Set default ATW RPG values
    wrestler.setFans(0L);
    wrestler.setIsPlayer(false);
    wrestler.setBumps(0);
    save(wrestler);
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

  @CacheEvict(
      value = {WRESTLERS_CACHE, WRESTLER_STATS_CACHE},
      allEntries = true)
  public void delete(@NonNull Wrestler wrestler) {
    wrestlerRepository.delete(wrestler);
  }

  @Cacheable(value = WRESTLERS_CACHE, key = "'all'")
  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll();
  }

  /** Get wrestler by ID. */
  public Optional<Wrestler> getWrestlerById(Long id) {
    return wrestlerRepository.findById(id);
  }

  /** Find wrestler by ID (alias for getWrestlerById for consistency). */
  public Optional<Wrestler> findById(Long id) {
    return getWrestlerById(id);
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
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              wrestler.addFans(fanGain);
              return wrestlerRepository.saveAndFlush(wrestler);
            });
  }

  /**
   * Add a bump to a wrestler (injury system).
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
              if (injuryOccurred) {
                // Create injury using the injury service
                Optional<Injury> injury = injuryService.createInjuryFromBumps(wrestler.getId());
                injury.ifPresent(
                    value ->
                        System.out.println(
                            "Wrestler "
                                + wrestler.getName()
                                + " suffered an injury: "
                                + value.getName()
                                + " ("
                                + value.getSeverity().getDisplayName()
                                + ")"));
              }
              return wrestlerRepository.saveAndFlush(wrestler);
            });
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
  public List<Wrestler> getWrestlersByTier(WrestlerTier tier) {
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
  public boolean spendFans(Long wrestlerId, Long cost) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              if (wrestler.spendFans(cost)) {
                wrestlerRepository.saveAndFlush(wrestler);
                return true;
              }
              return false;
            })
        .orElse(false);
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
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);

    // Card game defaults
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(0);
    wrestler.setStartingStamina(0);
    wrestler.setLowStamina(0);

    // ATW RPG defaults
    wrestler.setFans(0L);
    wrestler.setIsPlayer(isPlayer);
    wrestler.setBumps(0);
    wrestler.setDescription(description);

    return save(wrestler);
  }

  public List<WrestlerDTO> findAllAsDTO() {
    return findAll().stream().map(WrestlerDTO::new).toList();
  }
}
