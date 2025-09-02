package com.github.javydreamercsw.management.domain.show;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Show> findAllBy(Pageable pageable);

  Optional<Show> findByName(String name);

  Optional<Show> findByExternalId(String externalId);

  // ==================== CALENDAR-SPECIFIC QUERIES ====================

  /**
   * Find shows within a date range, ordered by show date.
   *
   * @param startDate Start date (inclusive)
   * @param endDate End date (inclusive)
   * @return List of shows in the date range
   */
  List<Show> findByShowDateBetweenOrderByShowDate(LocalDate startDate, LocalDate endDate);

  /**
   * Find upcoming shows from a given date onwards.
   *
   * @param date Start date
   * @param pageable Pagination information
   * @return List of upcoming shows
   */
  List<Show> findByShowDateGreaterThanEqualOrderByShowDate(LocalDate date, Pageable pageable);

  /**
   * Find shows on a specific date.
   *
   * @param date The date to search for
   * @return List of shows on that date
   */
  List<Show> findByShowDateOrderByCreationDate(LocalDate date);

  /**
   * Find shows with no scheduled date.
   *
   * @return List of unscheduled shows
   */
  List<Show> findByShowDateIsNullOrderByCreationDate();

  @Query(
      """
      SELECT s FROM Show s
      LEFT JOIN FETCH s.season
      LEFT JOIN FETCH s.template t
      LEFT JOIN FETCH t.showType
      WHERE s.showDate >= :date
      ORDER BY s.showDate ASC
      """)
  List<Show> findUpcomingWithRelationships(LocalDate date, Pageable pageable);

  /**
   * Find all shows with eagerly loaded relationships for export purposes. This prevents
   * LazyInitializationException when accessing Season and ShowTemplate outside of transaction.
   *
   * @return List of all shows with eagerly loaded relationships
   */
  @Query(
      """
      SELECT s FROM Show s
      LEFT JOIN FETCH s.season
      LEFT JOIN FETCH s.template
      LEFT JOIN FETCH s.type
      ORDER BY s.creationDate DESC
      """)
  List<Show> findAllWithRelationships();
}
