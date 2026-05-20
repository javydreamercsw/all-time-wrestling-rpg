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
package com.github.javydreamercsw.management.domain.universe;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniverseWrestlerExclusionRepository
    extends JpaRepository<UniverseWrestlerExclusion, Long> {

  List<UniverseWrestlerExclusion> findByUniverse(Universe universe);

  boolean existsByUniverseAndWrestler(Universe universe, Wrestler wrestler);

  void deleteByUniverseAndWrestler(Universe universe, Wrestler wrestler);

  /**
   * Returns the IDs of wrestlers excluded from the given universe. Using an ID-only projection
   * avoids loading full Wrestler entities and keeps this query cheap.
   */
  @Query("SELECT e.wrestler.id FROM UniverseWrestlerExclusion e WHERE e.universe.id = :universeId")
  Set<Long> findExcludedWrestlerIdsByUniverseId(@Param("universeId") Long universeId);
}
