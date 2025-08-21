package com.github.javydreamercsw.management.service.wrestler;

import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class WrestlerService {

  private final WrestlerRepository wrestlerRepository;
  private final Clock clock;

  WrestlerService(WrestlerRepository wrestlerRepository, Clock clock) {
    this.wrestlerRepository = wrestlerRepository;
    this.clock = clock;
  }

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

  public List<Wrestler> list(Pageable pageable) {
    return wrestlerRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return wrestlerRepository.count();
  }

  public Wrestler save(@NonNull Wrestler wrestler) {
    wrestler.setCreationDate(clock.instant());
    return wrestlerRepository.saveAndFlush(wrestler);
  }

  public void delete(@NonNull Wrestler wrestler) {
    wrestlerRepository.delete(wrestler);
  }

  public List<Wrestler> findAll() {
    return wrestlerRepository.findAll();
  }

  // ==================== ATW RPG METHODS ====================

  /**
   * Award fans to a wrestler and update their tier.
   *
   * @param wrestlerId The wrestler's ID
   * @param fanGain The number of fans to award (can be negative for losses)
   * @return The updated wrestler
   */
  public Optional<Wrestler> awardFans(Long wrestlerId, Long fanGain) {
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
  public Optional<Wrestler> addBump(Long wrestlerId) {
    return wrestlerRepository
        .findById(wrestlerId)
        .map(
            wrestler -> {
              boolean injuryOccurred = wrestler.addBump();
              if (injuryOccurred) {
                // TODO: Handle injury creation when injury system is implemented
                // For now, just log it
                System.out.println("Wrestler " + wrestler.getName() + " suffered an injury!");
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
  public List<Wrestler> getEligibleWrestlers(TitleTier titleTier) {
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
   * @param wrestlingStyle Wrestling style/gimmick
   * @return The created wrestler
   */
  public Wrestler createAtwWrestler(
      @NonNull String name, boolean isPlayer, String description, String wrestlingStyle) {
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
    wrestler.setWrestlingStyle(wrestlingStyle);

    return save(wrestler);
  }
}
