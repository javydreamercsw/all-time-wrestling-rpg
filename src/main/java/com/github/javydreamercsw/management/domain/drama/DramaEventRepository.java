package com.github.javydreamercsw.management.domain.drama;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DramaEventRepository
    extends JpaRepository<DramaEvent, Long>, JpaSpecificationExecutor<DramaEvent> {

  // If you don't need a total row count, Slice is better than Page.
  Page<DramaEvent> findAllBy(Pageable pageable);

  /** Find all drama events for a specific wrestler (as primary or secondary participant). */
  @Query(
      """
      SELECT de FROM DramaEvent de
      WHERE de.primaryWrestler = :wrestler OR de.secondaryWrestler = :wrestler
      ORDER BY de.eventDate DESC
      """)
  List<DramaEvent> findByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find drama events for a wrestler with pagination. */
  @Query(
      """
      SELECT de FROM DramaEvent de
      WHERE de.primaryWrestler = :wrestler OR de.secondaryWrestler = :wrestler
      ORDER BY de.eventDate DESC
      """)
  Page<DramaEvent> findByWrestler(@Param("wrestler") Wrestler wrestler, Pageable pageable);

  /** Find drama events between two specific wrestlers. */
  @Query(
      """
      SELECT de FROM DramaEvent de
      WHERE (de.primaryWrestler = :wrestler1 AND de.secondaryWrestler = :wrestler2)
         OR (de.primaryWrestler = :wrestler2 AND de.secondaryWrestler = :wrestler1)
      ORDER BY de.eventDate DESC
      """)
  List<DramaEvent> findBetweenWrestlers(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find unprocessed drama events. */
  List<DramaEvent> findByIsProcessedFalseOrderByEventDateAsc();

  /** Find drama events by type. */
  List<DramaEvent> findByEventTypeOrderByEventDateDesc(DramaEventType eventType);

  /** Find drama events by severity. */
  List<DramaEvent> findBySeverityOrderByEventDateDesc(DramaEventSeverity severity);

  /** Find recent drama events (within specified days). */
  @Query(
      """
      SELECT de FROM DramaEvent de
      WHERE de.eventDate >= :since
      ORDER BY de.eventDate DESC
      """)
  List<DramaEvent> findRecentEvents(@Param("since") Instant since);

  /** Find drama events that created rivalries. */
  List<DramaEvent> findByRivalryCreatedTrueOrderByEventDateDesc();

  /** Find drama events that ended rivalries. */
  List<DramaEvent> findByRivalryEndedTrueOrderByEventDateDesc();

  /** Find drama events that caused injuries. */
  List<DramaEvent> findByInjuryCausedTrueOrderByEventDateDesc();

  /** Find drama events with positive fan impact. */
  @Query("SELECT de FROM DramaEvent de WHERE de.fanImpact > 0 ORDER BY de.fanImpact DESC")
  List<DramaEvent> findPositiveFanImpactEvents();

  /** Find drama events with negative fan impact. */
  @Query("SELECT de FROM DramaEvent de WHERE de.fanImpact < 0 ORDER BY de.fanImpact ASC")
  List<DramaEvent> findNegativeFanImpactEvents();

  /** Find drama events that affected heat (positive or negative). */
  @Query(
      "SELECT de FROM DramaEvent de WHERE de.heatImpact IS NOT NULL AND de.heatImpact != 0 ORDER BY"
          + " de.eventDate DESC")
  List<DramaEvent> findHeatAffectingEvents();

  /** Count drama events for a wrestler in a time period. */
  @Query(
      """
      SELECT COUNT(de) FROM DramaEvent de
      WHERE (de.primaryWrestler = :wrestler OR de.secondaryWrestler = :wrestler)
        AND de.eventDate BETWEEN :startDate AND :endDate
      """)
  long countEventsForWrestlerInPeriod(
      @Param("wrestler") Wrestler wrestler,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Find the most recent drama event for a wrestler. */
  @Query(
      value =
          """
          SELECT * FROM drama_event de
          WHERE de.primary_wrestler_id = :#{#wrestler.id} OR de.secondary_wrestler_id = :#{#wrestler.id}
          ORDER BY de.event_date DESC
          LIMIT 1
          """,
      nativeQuery = true)
  DramaEvent findMostRecentForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find drama events by multiple types. */
  List<DramaEvent> findByEventTypeInOrderByEventDateDesc(List<DramaEventType> eventTypes);

  /** Find drama events by multiple severities. */
  List<DramaEvent> findBySeverityInOrderByEventDateDesc(List<DramaEventSeverity> severities);

  /** Find multi-wrestler drama events. */
  @Query(
      "SELECT de FROM DramaEvent de WHERE de.secondaryWrestler IS NOT NULL ORDER BY de.eventDate"
          + " DESC")
  List<DramaEvent> findMultiWrestlerEvents();

  /** Find single-wrestler drama events. */
  @Query(
      "SELECT de FROM DramaEvent de WHERE de.secondaryWrestler IS NULL ORDER BY de.eventDate DESC")
  List<DramaEvent> findSingleWrestlerEvents();

  @Transactional
  void deleteByPrimaryWrestlerOrSecondaryWrestler(
      Wrestler primaryWrestler, Wrestler secondaryWrestler);
}
