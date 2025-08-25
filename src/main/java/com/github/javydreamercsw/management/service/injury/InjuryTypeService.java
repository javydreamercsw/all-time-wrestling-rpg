package com.github.javydreamercsw.management.service.injury;

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing injury types in the ATW RPG card game system. Handles CRUD operations and
 * business logic for injury type reference data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InjuryTypeService {

  private final InjuryTypeRepository injuryTypeRepository;

  // ==================== CRUD OPERATIONS ====================

  /** Creates a new injury type. */
  public InjuryType createInjuryType(
      String injuryName,
      Integer healthEffect,
      Integer staminaEffect,
      Integer cardEffect,
      String specialEffects) {
    log.debug("Creating injury type: {}", injuryName);

    if (injuryTypeRepository.existsByInjuryName(injuryName)) {
      throw new IllegalArgumentException("Injury type already exists: " + injuryName);
    }

    InjuryType injuryType = new InjuryType();
    injuryType.setInjuryName(injuryName);
    injuryType.setHealthEffect(healthEffect);
    injuryType.setStaminaEffect(staminaEffect);
    injuryType.setCardEffect(cardEffect);
    injuryType.setSpecialEffects(specialEffects);

    InjuryType saved = injuryTypeRepository.saveAndFlush(injuryType);
    log.info("Created injury type: {} with ID: {}", injuryName, saved.getId());
    return saved;
  }

  /** Updates an existing injury type. */
  public InjuryType updateInjuryType(InjuryType injuryType) {
    log.debug("Updating injury type: {}", injuryType.getInjuryName());

    if (!injuryTypeRepository.existsById(injuryType.getId())) {
      throw new IllegalArgumentException("Injury type not found: " + injuryType.getId());
    }

    InjuryType updated = injuryTypeRepository.saveAndFlush(injuryType);
    log.info("Updated injury type: {}", updated.getInjuryName());
    return updated;
  }

  /** Deletes an injury type by ID. */
  public void deleteInjuryType(Long id) {
    log.debug("Deleting injury type with ID: {}", id);

    if (!injuryTypeRepository.existsById(id)) {
      throw new IllegalArgumentException("Injury type not found: " + id);
    }

    injuryTypeRepository.deleteById(id);
    log.info("Deleted injury type with ID: {}", id);
  }

  // ==================== QUERY OPERATIONS ====================

  /** Finds all injury types. */
  @Transactional(readOnly = true)
  public List<InjuryType> findAll() {
    return injuryTypeRepository.findAll();
  }

  /** Finds injury type by ID. */
  @Transactional(readOnly = true)
  public Optional<InjuryType> findById(Long id) {
    return injuryTypeRepository.findById(id);
  }

  /** Finds injury type by name. */
  @Transactional(readOnly = true)
  public Optional<InjuryType> findByName(String injuryName) {
    return injuryTypeRepository.findByInjuryName(injuryName);
  }

  /** Finds injury type by external ID. */
  @Transactional(readOnly = true)
  public Optional<InjuryType> findByExternalId(String externalId) {
    return injuryTypeRepository.findByExternalId(externalId);
  }

  /** Checks if injury type exists by external ID. */
  @Transactional(readOnly = true)
  public boolean existsByExternalId(String externalId) {
    return injuryTypeRepository.existsByExternalId(externalId);
  }

  /** Finds all injury types ordered by severity (most severe first). */
  @Transactional(readOnly = true)
  public List<InjuryType> findAllOrderedBySeverity() {
    return injuryTypeRepository.findAllOrderedBySeverity();
  }

  /** Finds injury types with health effects. */
  @Transactional(readOnly = true)
  public List<InjuryType> findWithHealthEffects() {
    return injuryTypeRepository.findWithHealthEffects();
  }

  /** Finds injury types with stamina effects. */
  @Transactional(readOnly = true)
  public List<InjuryType> findWithStaminaEffects() {
    return injuryTypeRepository.findWithStaminaEffects();
  }

  /** Finds injury types with card effects. */
  @Transactional(readOnly = true)
  public List<InjuryType> findWithCardEffects() {
    return injuryTypeRepository.findWithCardEffects();
  }

  /** Finds injury types with special effects. */
  @Transactional(readOnly = true)
  public List<InjuryType> findWithSpecialEffects() {
    return injuryTypeRepository.findWithSpecialEffects();
  }

  // ==================== STATISTICS ====================

  /** Gets statistics about injury type effects. */
  @Transactional(readOnly = true)
  public InjuryTypeStats getInjuryTypeStats() {
    Object[] counts = injuryTypeRepository.getEffectTypeCounts();
    if (counts != null && counts.length >= 4) {
      return new InjuryTypeStats(
          ((Number) counts[0]).intValue(), // healthCount
          ((Number) counts[1]).intValue(), // staminaCount
          ((Number) counts[2]).intValue(), // cardCount
          ((Number) counts[3]).intValue() // specialCount
          );
    }
    return new InjuryTypeStats(0, 0, 0, 0);
  }

  /** Statistics about injury type effects. */
  public record InjuryTypeStats(
      int healthEffectCount, int staminaEffectCount, int cardEffectCount, int specialEffectCount) {
    public int getTotalTypes() {
      return healthEffectCount + staminaEffectCount + cardEffectCount + specialEffectCount;
    }
  }
}
