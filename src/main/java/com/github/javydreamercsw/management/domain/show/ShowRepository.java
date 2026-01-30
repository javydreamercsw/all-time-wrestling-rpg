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
package com.github.javydreamercsw.management.domain.show;

import com.github.javydreamercsw.management.domain.league.League;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Show> findAllBy(Pageable pageable);

  List<Show> findByName(String name);

  Optional<Show> findByExternalId(String externalId);

  Optional<Show> findByNameAndShowDate(String name, LocalDate showDate);

  boolean existsByLeague(League league);

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
      ORDER BY s.showDate DESC
      """)
  List<Show> findAllWithRelationships();

  /** Find all external IDs. */
  @Query("SELECT s.externalId FROM Show s WHERE s.externalId IS NOT NULL")
  List<String> findAllExternalIds();

  @Query(
      value =
          """
          SELECT DISTINCT s.show_id, s.show_date FROM show s
          JOIN segment seg ON seg.show_id = s.show_id
          JOIN segment_participant sp ON sp.segment_id = seg.segment_id
          WHERE s.show_date >= :date AND sp.wrestler_id = :wrestlerId
          ORDER BY s.show_date ASC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<Object[]> findUpcomingShowIdsAndDatesForWrestler(
      LocalDate date, Long wrestlerId, int limit, int offset);

  @Query(
      """
      SELECT s FROM Show s
      LEFT JOIN FETCH s.season
      LEFT JOIN FETCH s.template t
      LEFT JOIN FETCH t.showType
      WHERE s.id IN :showIds
      """)
  List<Show> findByIdsWithRelationships(List<Long> showIds, Sort sort);
}
