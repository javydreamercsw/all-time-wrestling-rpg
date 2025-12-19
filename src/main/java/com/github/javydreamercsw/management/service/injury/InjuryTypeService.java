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

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @Autowired private InjuryTypeRepository injuryTypeRepository;

  // ==================== CRUD OPERATIONS ====================

  /** Creates a new injury type. */
  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
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

  /** Updates an existing injury type with individual fields. */
  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Optional<InjuryType> updateInjuryType(
      Long id,
      String injuryName,
      Integer healthEffect,
      Integer staminaEffect,
      Integer cardEffect,
      String specialEffects) {
    log.debug("Updating injury type with ID: {}", id);

    Optional<InjuryType> existingOpt = injuryTypeRepository.findById(id);
    if (existingOpt.isEmpty()) {
      return Optional.empty();
    }

    InjuryType existing = existingOpt.get();

    // Check for name conflicts (excluding current record)
    if (!existing.getInjuryName().equals(injuryName)
        && injuryTypeRepository.existsByInjuryName(injuryName)) {
      throw new IllegalArgumentException("Injury type already exists: " + injuryName);
    }

    existing.setInjuryName(injuryName);
    existing.setHealthEffect(healthEffect);
    existing.setStaminaEffect(staminaEffect);
    existing.setCardEffect(cardEffect);
    existing.setSpecialEffects(specialEffects);

    InjuryType updated = injuryTypeRepository.saveAndFlush(existing);
    log.info("Updated injury type: {}", updated.getInjuryName());
    return Optional.of(updated);
  }

  /** Updates an existing injury type. */
  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public InjuryType updateInjuryType(@NonNull InjuryType injuryType) {
    log.debug("Updating injury type: {}", injuryType.getInjuryName());

    if (!injuryTypeRepository.existsById(injuryType.getId())) {
      throw new IllegalArgumentException("Injury type not found: " + injuryType.getId());
    }

    InjuryType updated = injuryTypeRepository.saveAndFlush(injuryType);
    log.info("Updated injury type: {}", updated.getInjuryName());
    return updated;
  }

  /** Deletes an injury type by ID. Returns true if deleted, false if not found. */
  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public boolean deleteInjuryType(Long id) {
    log.debug("Deleting injury type with ID: {}", id);

    if (!injuryTypeRepository.existsById(id)) {
      return false;
    }

    // TODO: Add check for references from other entities (e.g., active injuries)
    // For now, we'll allow deletion but this should be enhanced later
    try {
      injuryTypeRepository.deleteById(id);
      log.info("Deleted injury type with ID: {}", id);
      return true;
    } catch (Exception e) {
      log.error("Failed to delete injury type with ID: {}", id, e);
      throw new IllegalStateException(
          "Cannot delete injury type - it may be referenced by other records");
    }
  }

  // ==================== QUERY OPERATIONS ====================

  /** Finds all injury types. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findAll() {
    return injuryTypeRepository.findAll();
  }

  /** Finds injury type by ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<InjuryType> findById(Long id) {
    return injuryTypeRepository.findById(id);
  }

  /** Gets injury type by ID (alias for findById for consistency with other services). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<InjuryType> getInjuryTypeById(@NonNull Long id) {
    return findById(id);
  }

  /** Gets all injury types with pagination. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public org.springframework.data.domain.Page<InjuryType> getAllInjuryTypes(
      @NonNull Pageable pageable) {
    return injuryTypeRepository.findAll(pageable);
  }

  /** Counts all injury types. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countAll() {
    return injuryTypeRepository.count();
  }

  /** Finds injury type by name. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<InjuryType> findByName(@NonNull String injuryName) {
    return injuryTypeRepository.findByInjuryName(injuryName);
  }

  /** Finds injury type by external ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<InjuryType> findByExternalId(@NonNull String externalId) {
    return injuryTypeRepository.findByExternalId(externalId);
  }

  /** Checks if injury type exists by external ID. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean existsByExternalId(@NonNull String externalId) {
    return injuryTypeRepository.existsByExternalId(externalId);
  }

  /** Finds all injury types ordered by severity (most severe first). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findAllOrderedBySeverity() {
    return injuryTypeRepository.findAllOrderedBySeverity();
  }

  /** Finds injury types with health effects. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findWithHealthEffects() {
    return injuryTypeRepository.findWithHealthEffects();
  }

  /** Finds injury types with stamina effects. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findWithStaminaEffects() {
    return injuryTypeRepository.findWithStaminaEffects();
  }

  /** Finds injury types with card effects. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findWithCardEffects() {
    return injuryTypeRepository.findWithCardEffects();
  }

  /** Finds injury types with special effects. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<InjuryType> findWithSpecialEffects() {
    return injuryTypeRepository.findWithSpecialEffects();
  }

  // ==================== STATISTICS ====================

  /** Gets statistics about injury type effects. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public InjuryTypeStats getInjuryTypeStats() {
    Long healthCount = injuryTypeRepository.countWithHealthEffects();
    Long staminaCount = injuryTypeRepository.countWithStaminaEffects();
    Long cardCount = injuryTypeRepository.countWithCardEffects();
    Long specialCount = injuryTypeRepository.countWithSpecialEffects();

    return new InjuryTypeStats(
        healthCount != null ? healthCount.intValue() : 0,
        staminaCount != null ? staminaCount.intValue() : 0,
        cardCount != null ? cardCount.intValue() : 0,
        specialCount != null ? specialCount.intValue() : 0);
  }

  /** Statistics about injury type effects. */
  public record InjuryTypeStats(
      int healthEffectCount, int staminaEffectCount, int cardEffectCount, int specialEffectCount) {
    public int getTotalTypes() {
      return healthEffectCount + staminaEffectCount + cardEffectCount + specialEffectCount;
    }
  }
}
