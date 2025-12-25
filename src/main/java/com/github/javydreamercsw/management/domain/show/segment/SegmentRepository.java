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
package com.github.javydreamercsw.management.domain.show.segment;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentRepository
    extends JpaRepository<Segment, Long>, JpaSpecificationExecutor<Segment> {

  @Query("SELECT s FROM Segment s JOIN FETCH s.show")
  List<Segment> findAllWithShow();

  // If you don't need a total row count, Slice is better than Page.
  Page<Segment> findAllBy(Pageable pageable);

  /** Find all segments for a specific show. */
  @Query(
      """
      SELECT s FROM Segment s
      JOIN FETCH s.show
      WHERE s.show = :show
      ORDER BY s.segmentOrder ASC
      """)
  List<Segment> findByShow(@Param("show") Show show);

  /** Find all segments where a wrestler participated. */
  @Query(
      value =
          """
          SELECT s FROM Segment s
          JOIN FETCH s.show
          JOIN s.participants p
          WHERE p.wrestler = :wrestler
          ORDER BY s.segmentDate DESC
          """,
      countQuery =
          """
          SELECT COUNT(s) FROM Segment s
          JOIN s.participants p
          WHERE p.wrestler = :wrestler
          """)
  Page<Segment> findByWrestlerParticipation(
      @Param("wrestler") Wrestler wrestler, Pageable pageable);

  /** Find all segments won by a specific wrestler. */
  @Query(
      """
      SELECT s FROM Segment s
      JOIN s.participants p
      WHERE p.wrestler = :wrestler AND p.isWinner = true
      ORDER BY s.segmentDate DESC
      """)
  List<Segment> findByWinner(@Param("wrestler") Wrestler wrestler);

  /** Find recent segments between two wrestlers. */
  @Query(
      """
      SELECT s FROM Segment s
      JOIN s.participants p1
      JOIN s.participants p2
      WHERE p1.wrestler = :wrestler1 AND p2.wrestler = :wrestler2
      AND p1.id != p2.id
      ORDER BY s.segmentDate DESC
      """)
  List<Segment> findSegmentsBetween(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find NPC-generated segments. */
  List<Segment> findByIsNpcGeneratedTrue();

  /** Find title segments. */
  List<Segment> findByIsTitleSegmentTrue();

  /** Find segments after a specific date. */
  List<Segment> findBySegmentDateAfter(Instant date);

  /** Find segments between two dates. */
  List<Segment> findBySegmentDateBetween(Instant startDate, Instant endDate);

  /** Count wins for a wrestler. */
  @Query(
      """
      SELECT COUNT(s) FROM Segment s
      JOIN s.participants p
      WHERE p.wrestler = :wrestler AND p.isWinner = true
      """)
  long countWinsByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Count total segments for a wrestler. */
  @Query(
      """
      SELECT COUNT(s) FROM Segment s
      JOIN s.participants p
      WHERE p.wrestler = :wrestler
      """)
  long countSegmentsByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Count total match segments for a wrestler, excluding 'Promo' segments. */
  @Query(
      """
      SELECT COUNT(s) FROM Segment s
      JOIN s.participants p
      WHERE p.wrestler = :wrestler
      AND s.segmentType.name <> 'Promo'
      """)
  long countMatchSegmentsByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Check if a segment result exists by external ID. */
  boolean existsByExternalId(String externalId);

  /** Find a segment result by external ID. */
  Optional<Segment> findByExternalId(String externalId);

  /** Find all external IDs. */
  @Query("SELECT s.externalId FROM Segment s WHERE s.externalId IS NOT NULL")
  List<String> findAllExternalIds();

  List<Segment> findByShowOrderBySegmentOrderAsc(Show show);

  @Query(
      """
      SELECT s FROM Segment s
      JOIN s.participants p
      JOIN s.show sh
      JOIN sh.season se
      WHERE p.wrestler = :wrestler AND se = :season
      ORDER BY s.segmentDate DESC
      """)
  List<Segment> findByWrestlerParticipationAndSeason(
      @Param("wrestler") Wrestler wrestler, @Param("season") Season season);

  @Query(
      """
      SELECT s FROM Segment s
      JOIN FETCH s.show
      JOIN s.participants p
      WHERE p.wrestler = :wrestler
      ORDER BY s.segmentDate DESC
      """)
  List<Segment> findByWrestlerParticipationWithShow(@Param("wrestler") Wrestler wrestler);
}
