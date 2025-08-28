package com.github.javydreamercsw.management.domain.feud;

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

public interface MultiWrestlerFeudRepository
    extends JpaRepository<MultiWrestlerFeud, Long>, JpaSpecificationExecutor<MultiWrestlerFeud> {

  // If you don't need a total row count, Slice is better than Page.
  Page<MultiWrestlerFeud> findAllBy(Pageable pageable);

  /** Find feud by name. */
  Optional<MultiWrestlerFeud> findByName(String name);

  /** Check if feud name exists. */
  boolean existsByName(String name);

  /** Find active feuds. */
  List<MultiWrestlerFeud> findByIsActiveTrue();

  /** Find ended feuds. */
  List<MultiWrestlerFeud> findByIsActiveFalse();

  /** Find feuds involving a specific wrestler. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE p.wrestler = :wrestler AND p.isActive = true AND f.isActive = true
      """)
  List<MultiWrestlerFeud> findActiveFeudsForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find all feuds (active and inactive) involving a specific wrestler. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE p.wrestler = :wrestler
      """)
  List<MultiWrestlerFeud> findAllFeudsForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find feuds that require matches at next show (10+ heat). */
  @Query("SELECT f FROM MultiWrestlerFeud f WHERE f.isActive = true AND f.heat >= 10")
  List<MultiWrestlerFeud> findFeudsRequiringMatches();

  /** Find feuds that can attempt resolution (20+ heat). */
  @Query("SELECT f FROM MultiWrestlerFeud f WHERE f.isActive = true AND f.heat >= 20")
  List<MultiWrestlerFeud> findFeudsEligibleForResolution();

  /** Find feuds requiring stipulation matches (30+ heat). */
  @Query("SELECT f FROM MultiWrestlerFeud f WHERE f.isActive = true AND f.heat >= 30")
  List<MultiWrestlerFeud> findFeudsRequiringStipulationMatches();

  /** Find feuds by heat level range. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      AND f.heat >= :minHeat
      AND f.heat <= :maxHeat
      """)
  List<MultiWrestlerFeud> findByHeatRange(
      @Param("minHeat") int minHeat, @Param("maxHeat") int maxHeat);

  /** Find the hottest active feuds. */
  @Query("SELECT f FROM MultiWrestlerFeud f WHERE f.isActive = true ORDER BY f.heat DESC")
  List<MultiWrestlerFeud> findHottestFeuds(Pageable pageable);

  /** Find feuds by participant count range. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      AND (SELECT COUNT(p) FROM FeudParticipant p WHERE p.feud = f AND p.isActive = true) >= :minParticipants
      AND (SELECT COUNT(p) FROM FeudParticipant p WHERE p.feud = f AND p.isActive = true) <= :maxParticipants
      """)
  List<MultiWrestlerFeud> findByParticipantCountRange(
      @Param("minParticipants") int minParticipants, @Param("maxParticipants") int maxParticipants);

  /** Find valid multi-wrestler feuds (3+ active participants). */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      AND (SELECT COUNT(p) FROM FeudParticipant p WHERE p.feud = f AND p.isActive = true) >= 3
      """)
  List<MultiWrestlerFeud> findValidMultiWrestlerFeuds();

  /** Find feuds with specific roles. */
  @Query(
      """
      SELECT DISTINCT f FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE f.isActive = true AND p.isActive = true AND p.role = :role
      """)
  List<MultiWrestlerFeud> findFeudsWithRole(@Param("role") FeudRole role);

  /** Find feuds with both protagonists and antagonists. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      AND EXISTS (SELECT p1 FROM FeudParticipant p1 WHERE p1.feud = f AND p1.isActive = true AND p1.role = 'PROTAGONIST')
      AND EXISTS (SELECT p2 FROM FeudParticipant p2 WHERE p2.feud = f AND p2.isActive = true AND p2.role = 'ANTAGONIST')
      """)
  List<MultiWrestlerFeud> findFeudsWithProtagonistsAndAntagonists();

  /** Find feuds with wild card participants. */
  @Query(
      """
      SELECT DISTINCT f FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE f.isActive = true AND p.isActive = true AND p.role = 'WILD_CARD'
      """)
  List<MultiWrestlerFeud> findFeudsWithWildCards();

  /** Find recent feuds (within specified days). */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.startedDate >= :sinceDate
      ORDER BY f.startedDate DESC
      """)
  List<MultiWrestlerFeud> findRecentFeuds(@Param("sinceDate") Instant sinceDate);

  /** Count active feuds for a wrestler. */
  @Query(
      """
      SELECT COUNT(DISTINCT f) FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE p.wrestler = :wrestler AND p.isActive = true AND f.isActive = true
      """)
  long countActiveFeudsForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find largest active feuds by participant count. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      ORDER BY (SELECT COUNT(p) FROM FeudParticipant p WHERE p.feud = f AND p.isActive = true) DESC
      """)
  List<MultiWrestlerFeud> findLargestFeuds(Pageable pageable);

  /** Find feuds that involve wrestlers from multiple factions. */
  @Query(
      """
      SELECT DISTINCT f FROM MultiWrestlerFeud f
      JOIN f.participants p
      WHERE f.isActive = true AND p.isActive = true
      AND (SELECT COUNT(DISTINCT p2.wrestler.faction) FROM FeudParticipant p2
           WHERE p2.feud = f AND p2.isActive = true AND p2.wrestler.faction IS NOT NULL) > 1
      """)
  List<MultiWrestlerFeud> findInterFactionFeuds();

  /** Find feuds involving only wrestlers without factions. */
  @Query(
      """
      SELECT f FROM MultiWrestlerFeud f
      WHERE f.isActive = true
      AND NOT EXISTS (SELECT p FROM FeudParticipant p
                      WHERE p.feud = f AND p.isActive = true AND p.wrestler.faction IS NOT NULL)
      """)
  List<MultiWrestlerFeud> findIndependentWrestlerFeuds();
}
