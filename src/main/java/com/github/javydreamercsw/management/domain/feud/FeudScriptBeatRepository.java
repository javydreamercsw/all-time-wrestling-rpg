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
package com.github.javydreamercsw.management.domain.feud;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeudScriptBeatRepository extends JpaRepository<FeudScriptBeat, Long> {

  @Query(
      "SELECT b FROM FeudScriptBeat b WHERE b.targetShow.id = :showId"
          + " AND b.beatStatus = 'PENDING'"
          + " AND b.script.status = 'ACTIVE'"
          + " ORDER BY b.script.id, b.beatOrder")
  List<FeudScriptBeat> findPendingBeatsForShow(@Param("showId") Long showId);

  Optional<FeudScriptBeat> findByActualSegment(Segment segment);

  /**
   * Find pending beats whose arc wrestlers appear in the given ID set — rivalry arcs (both
   * wrestlers present) and multi-wrestler feud arcs (any active member present; the service layer
   * filters for full coverage). Ordered by beat order so the first match is the arc's next beat.
   */
  @Query(
      "SELECT DISTINCT b FROM FeudScriptBeat b JOIN b.script s LEFT JOIN s.rivalry r LEFT JOIN"
          + " s.feud f LEFT JOIN f.participants fp WHERE b.beatStatus = 'PENDING' AND s.status ="
          + " 'ACTIVE' AND (   (r IS NOT NULL AND r.wrestler1.id IN :wrestlerIds AND r.wrestler2.id"
          + " IN :wrestlerIds)   OR (f IS NOT NULL AND fp.isActive = true AND fp.wrestler.id IN"
          + " :wrestlerIds)) ORDER BY b.beatOrder ASC")
  List<FeudScriptBeat> findPendingBeatsForWrestlers(@Param("wrestlerIds") List<Long> wrestlerIds);

  /**
   * Next pending beat (lowest beatOrder) of every ACTIVE script, regardless of target show. Beats
   * without a targetShow never match {@link #findPendingBeatsForShow}, so planning falls back to
   * this to keep arcs moving.
   */
  @Query(
      value =
          "SELECT b.* FROM feud_script_beat b"
              + " JOIN feud_script s ON s.feud_script_id = b.script_id"
              + " WHERE s.status = 'ACTIVE'"
              + " AND b.beat_status = 'PENDING'"
              + " AND b.beat_order = (SELECT MIN(b2.beat_order) FROM feud_script_beat b2"
              + "   WHERE b2.script_id = b.script_id AND b2.beat_status = 'PENDING')",
      nativeQuery = true)
  List<FeudScriptBeat> findNextPendingBeatPerActiveScript();
}
