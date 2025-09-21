package com.github.javydreamercsw.management.domain.storyline;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorylineBranchRepository
    extends JpaRepository<StorylineBranch, Long>, JpaSpecificationExecutor<StorylineBranch> {

  // If you don't need a total row count, Slice is better than Page.
  Page<StorylineBranch> findAllBy(Pageable pageable);

  /** Find active storyline branches. */
  List<StorylineBranch> findByIsActiveTrue();

  /** Find completed storyline branches. */
  List<StorylineBranch> findByIsActiveFalse();

  /** Find branches by type. */
  List<StorylineBranch> findByBranchType(StorylineBranchType branchType);

  /** Find active branches by type. */
  List<StorylineBranch> findByIsActiveTrueAndBranchType(StorylineBranchType branchType);

  /** Find branches that have been activated. */
  List<StorylineBranch> findByActivatedDateIsNotNull();

  /** Find branches that have not been activated yet. */
  List<StorylineBranch> findByActivatedDateIsNull();

  /** Find branches that have been completed. */
  List<StorylineBranch> findByCompletedDateIsNotNull();

  /** Find branches triggered by a specific segment. */
  List<StorylineBranch> findByTriggeringSegment(Segment segmentResult);

  /** Find branches by priority range. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.priority >= :minPriority
      AND sb.priority <= :maxPriority
      ORDER BY sb.priority DESC
      """)
  List<StorylineBranch> findByPriorityRange(
      @Param("minPriority") int minPriority, @Param("maxPriority") int maxPriority);

  /** Find highest priority active branches. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      ORDER BY sb.priority DESC
      """)
  List<StorylineBranch> findHighestPriorityBranches(Pageable pageable);

  /** Find branches ready to activate (all conditions met). */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.activatedDate IS NULL
      AND NOT EXISTS (
          SELECT c FROM StorylineBranchCondition c
          WHERE c.storylineBranch = sb
          AND c.isConditionMet = false
      )
      ORDER BY sb.priority DESC
      """)
  List<StorylineBranch> findBranchesReadyToActivate();

  /** Find branches waiting for conditions. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.activatedDate IS NULL
      AND EXISTS (
          SELECT c FROM StorylineBranchCondition c
          WHERE c.storylineBranch = sb
          AND c.isConditionMet = false
      )
      """)
  List<StorylineBranch> findBranchesWaitingForConditions();

  /** Find activated branches with pending effects. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.activatedDate IS NOT NULL
      AND sb.completedDate IS NULL
      AND EXISTS (
          SELECT e FROM StorylineBranchEffect e
          WHERE e.storylineBranch = sb
          AND e.isExecuted = false
      )
      """)
  List<StorylineBranch> findActivatedBranchesWithPendingEffects();

  /** Find branches created within a time period. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.creationDate >= :startDate
      AND sb.creationDate <= :endDate
      ORDER BY sb.creationDate DESC
      """)
  List<StorylineBranch> findBranchesCreatedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /** Find recent branches (within specified days). */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.creationDate >= :sinceDate
      ORDER BY sb.creationDate DESC
      """)
  List<StorylineBranch> findRecentBranches(@Param("sinceDate") Instant sinceDate);

  /** Find branches activated within a time period. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.activatedDate >= :startDate
      AND sb.activatedDate <= :endDate
      ORDER BY sb.activatedDate DESC
      """)
  List<StorylineBranch> findBranchesActivatedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /** Find branches completed within a time period. */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.completedDate >= :startDate
      AND sb.completedDate <= :endDate
      ORDER BY sb.completedDate DESC
      """)
  List<StorylineBranch> findBranchesCompletedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /** Count active branches by type. */
  long countByIsActiveTrueAndBranchType(StorylineBranchType branchType);

  /** Count branches ready to activate. */
  @Query(
      """
      SELECT COUNT(sb) FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.activatedDate IS NULL
      AND NOT EXISTS (
          SELECT c FROM StorylineBranchCondition c
          WHERE c.storylineBranch = sb
          AND c.isConditionMet = false
      )
      """)
  long countBranchesReadyToActivate();

  /** Find branches with specific condition types. */
  @Query(
      """
      SELECT DISTINCT sb FROM StorylineBranch sb
      JOIN sb.conditions c
      WHERE sb.isActive = true
      AND c.conditionType = :conditionType
      """)
  List<StorylineBranch> findBranchesWithConditionType(@Param("conditionType") String conditionType);

  /** Find branches with specific effect types. */
  @Query(
      """
      SELECT DISTINCT sb FROM StorylineBranch sb
      JOIN sb.effects e
      WHERE sb.isActive = true
      AND e.effectType = :effectType
      """)
  List<StorylineBranch> findBranchesWithEffectType(@Param("effectType") String effectType);

  /** Find segment outcome branches. */
  List<StorylineBranch> findByBranchTypeAndIsActiveTrue(StorylineBranchType branchType);

  /** Find expired branches (created long ago but never activated). */
  @Query(
      """
      SELECT sb FROM StorylineBranch sb
      WHERE sb.isActive = true
      AND sb.activatedDate IS NULL
      AND sb.creationDate < :expirationDate
      """)
  List<StorylineBranch> findExpiredBranches(@Param("expirationDate") Instant expirationDate);
}
