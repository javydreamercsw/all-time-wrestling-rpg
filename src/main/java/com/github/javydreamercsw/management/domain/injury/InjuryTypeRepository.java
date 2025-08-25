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
      "SELECT it FROM InjuryType it WHERE it.specialEffects IS NOT NULL "
          + "AND TRIM(it.specialEffects) != '' AND it.specialEffects != 'N/A'")
  List<InjuryType> findWithSpecialEffects();

  /** Find injury types by minimum health effect penalty. */
  @Query("SELECT it FROM InjuryType it WHERE it.healthEffect <= :maxPenalty")
  List<InjuryType> findByHealthEffectLessThanEqual(@Param("maxPenalty") Integer maxPenalty);

  /** Get count of injury types by effect type. */
  @Query(
      "SELECT COUNT(CASE WHEN it.healthEffect IS NOT NULL AND it.healthEffect != 0 THEN 1 END) as"
          + " healthCount, COUNT(CASE WHEN it.staminaEffect IS NOT NULL AND it.staminaEffect != 0"
          + " THEN 1 END) as staminaCount, COUNT(CASE WHEN it.cardEffect IS NOT NULL AND"
          + " it.cardEffect != 0 THEN 1 END) as cardCount, COUNT(CASE WHEN it.specialEffects IS NOT"
          + " NULL AND TRIM(it.specialEffects) != '' AND it.specialEffects != 'N/A' THEN 1 END) as"
          + " specialCount FROM InjuryType it")
  Object[] getEffectTypeCounts();
}
