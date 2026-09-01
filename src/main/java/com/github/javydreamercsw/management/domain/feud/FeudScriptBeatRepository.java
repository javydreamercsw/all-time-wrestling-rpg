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

  /** Find the first pending beat whose rivalry wrestlers are both present in the given ID set. */
  @Query(
      "SELECT b FROM FeudScriptBeat b"
          + " JOIN b.script s"
          + " JOIN s.rivalry r"
          + " WHERE b.beatStatus = 'PENDING'"
          + " AND s.status = 'ACTIVE'"
          + " AND r.wrestler1.id IN :wrestlerIds"
          + " AND r.wrestler2.id IN :wrestlerIds"
          + " ORDER BY b.beatOrder ASC")
  List<FeudScriptBeat> findPendingBeatsForWrestlers(@Param("wrestlerIds") List<Long> wrestlerIds);
}
