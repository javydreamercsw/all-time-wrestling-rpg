/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.tournament;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

  List<TournamentMatch> findByRoundId(Long roundId);

  List<TournamentMatch> findByRoundIdAndWinnerIsNull(Long roundId);

  /** The bracket match a booked segment fed (reverse of TournamentMatch.segment). */
  @Query(
      "SELECT m FROM TournamentMatch m JOIN FETCH m.round r JOIN FETCH r.tournament"
          + " WHERE m.segment.id = :segmentId")
  Optional<TournamentMatch> findBySegmentId(@Param("segmentId") Long segmentId);

  /**
   * Orphan candidates for the startup detector: match segments living on a show that hosts a
   * tournament round, referenced by no bracket match. A hit is usually the runner's
   * compensation-deletion failure path (REQUIRES_NEW insert committed, outer rollback) — logged,
   * not deleted, so a human decides. Cancelled segments are never candidates (deliberate, not
   * orphans).
   */
  @Query(
      "SELECT s FROM Segment s WHERE s.show.id IN (SELECT r.show.id FROM TournamentRound r WHERE"
          + " r.show IS NOT NULL) AND s.status != 'CANCELLED'"
          + " AND NOT EXISTS (SELECT m FROM TournamentMatch m WHERE m.segment = s)")
  List<Segment> findOrphanCandidatesOnTournamentShows();
}
