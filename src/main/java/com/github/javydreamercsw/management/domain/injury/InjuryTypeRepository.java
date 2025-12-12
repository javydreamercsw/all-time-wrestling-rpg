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
package com.github.javydreamercsw.management.domain.injury;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing InjuryType entities. Provides access to injury type reference data for
 * the card game.
 */
@Repository
public interface InjuryTypeRepository extends JpaRepository<InjuryType, Long> {

  /** Find injury type by name. */
  Optional<InjuryType> findByInjuryName(String injuryName);

  /** Find injury type by external ID (e.g., Notion page ID). */
  Optional<InjuryType> findByExternalId(String externalId);

  /** Check if injury type exists by external ID. */
  boolean existsByExternalId(String externalId);

  /** Check if injury type exists by name. */
  boolean existsByInjuryName(String injuryName);

  /** Find all injury types ordered by total penalty (most severe first). */
  @Query(
      "SELECT it FROM InjuryType it ORDER BY "
          + "(ABS(COALESCE(it.healthEffect, 0)) + "
          + " ABS(COALESCE(it.staminaEffect, 0)) + "
          + " ABS(COALESCE(it.cardEffect, 0))) DESC")
  List<InjuryType> findAllOrderedBySeverity();

  /** Find injury types with health effects. */
  @Query("SELECT it FROM InjuryType it WHERE it.healthEffect IS NOT NULL AND it.healthEffect != 0")
  List<InjuryType> findWithHealthEffects();

  /** Find injury types with stamina effects. */
  @Query(
      "SELECT it FROM InjuryType it WHERE it.staminaEffect IS NOT NULL AND it.staminaEffect != 0")
  List<InjuryType> findWithStaminaEffects();

  /** Find injury types with card effects. */
  @Query("SELECT it FROM InjuryType it WHERE it.cardEffect IS NOT NULL AND it.cardEffect != 0")
  List<InjuryType> findWithCardEffects();

  /** Find injury types with special effects. */
  @Query(
      "SELECT it FROM InjuryType it WHERE it.specialEffects IS NOT NULL AND it.specialEffects <>"
          + " ''")
  List<InjuryType> findWithSpecialEffects();

  /** Find injury types by minimum health effect penalty. */
  @Query("SELECT it FROM InjuryType it WHERE it.healthEffect <= :maxPenalty")
  List<InjuryType> findByHealthEffectLessThanEqual(@Param("maxPenalty") Integer maxPenalty);

  /** Get count of injury types by effect type. */
  @Query(
      "SELECT COUNT(it) FROM InjuryType it WHERE it.healthEffect IS NOT NULL AND it.healthEffect <>"
          + " 0")
  Long countWithHealthEffects();

  @Query(
      "SELECT COUNT(it) FROM InjuryType it WHERE it.staminaEffect IS NOT NULL AND it.staminaEffect"
          + " <> 0")
  Long countWithStaminaEffects();

  @Query(
      "SELECT COUNT(it) FROM InjuryType it WHERE it.cardEffect IS NOT NULL AND it.cardEffect <> 0")
  Long countWithCardEffects();

  @Query(
      "SELECT COUNT(it) FROM InjuryType it WHERE it.specialEffects IS NOT NULL AND"
          + " it.specialEffects <> ''")
  Long countWithSpecialEffects();
}
