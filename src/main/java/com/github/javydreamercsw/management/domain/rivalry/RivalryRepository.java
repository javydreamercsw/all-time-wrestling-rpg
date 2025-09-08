package com.github.javydreamercsw.management.domain.rivalry;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RivalryRepository
    extends JpaRepository<Rivalry, Long>, JpaSpecificationExecutor<Rivalry> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Rivalry> findAllBy(Pageable pageable);

  /** Find active rivalries. */
  List<Rivalry> findByIsActiveTrue();

  /** Find ended rivalries. */
  List<Rivalry> findByIsActiveFalse();

  /** Find rivalry between two specific wrestlers (regardless of order). */
  @Query(
      """
      SELECT r FROM Rivalry r
      WHERE ((r.wrestler1 = :wrestler1 AND r.wrestler2 = :wrestler2)
          OR (r.wrestler1 = :wrestler2 AND r.wrestler2 = :wrestler1))
      AND r.isActive = true
      """)
  Optional<Rivalry> findActiveRivalryBetween(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find all rivalries involving a specific wrestler. */
  @Query(
      """
      SELECT r FROM Rivalry r
      WHERE (r.wrestler1 = :wrestler OR r.wrestler2 = :wrestler)
      AND r.isActive = true
      """)
  List<Rivalry> findActiveRivalriesForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find rivalries that require matches at next show (10+ heat). */
  @Query("SELECT r FROM Rivalry r WHERE r.isActive = true AND r.heat >= 10")
  List<Rivalry> findRivalriesRequiringMatches();

  /** Find rivalries that can attempt resolution (20+ heat). */
  @Query("SELECT r FROM Rivalry r WHERE r.isActive = true AND r.heat >= 20")
  List<Rivalry> findRivalriesEligibleForResolution();

  /** Find rivalries requiring rule matches (30+ heat). */
  @Query("SELECT r FROM Rivalry r WHERE r.isActive = true AND r.heat >= 30")
  List<Rivalry> findRivalriesRequiringStipulationMatches();

  /** Find rivalries by heat level range. */
  @Query(
      """
      SELECT r FROM Rivalry r
      WHERE r.isActive = true
      AND r.heat >= :minHeat
      AND r.heat <= :maxHeat
      """)
  List<Rivalry> findByHeatRange(@Param("minHeat") int minHeat, @Param("maxHeat") int maxHeat);

  /** Find the hottest active rivalries. */
  @Query("SELECT r FROM Rivalry r WHERE r.isActive = true ORDER BY r.heat DESC")
  List<Rivalry> findHottestRivalries(Pageable pageable);

  /** Check if two wrestlers have any rivalry history (active or ended). */
  @Query(
      """
      SELECT COUNT(r) > 0 FROM Rivalry r
      WHERE (r.wrestler1 = :wrestler1 AND r.wrestler2 = :wrestler2)
         OR (r.wrestler1 = :wrestler2 AND r.wrestler2 = :wrestler1)
      """)
  boolean hasRivalryHistory(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Get rivalry count for a wrestler. */
  @Query(
      """
      SELECT COUNT(r) FROM Rivalry r
      WHERE (r.wrestler1 = :wrestler OR r.wrestler2 = :wrestler)
      AND r.isActive = true
      """)
  long countActiveRivalriesForWrestler(@Param("wrestler") Wrestler wrestler);

  @Query(
      """
      SELECT r FROM Rivalry r
      WHERE (r.isActive = true AND r.startedDate <= :endDate) OR
            (r.startedDate <= :endDate AND (r.endedDate IS NULL OR r.endedDate >= :startDate))
      """)
  List<Rivalry> findActiveRivalriesBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
