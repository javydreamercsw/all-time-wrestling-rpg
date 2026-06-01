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
package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.management.domain.universe.Universe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WrestlerStateRepository extends JpaRepository<WrestlerState, Long> {
  Optional<WrestlerState> findByWrestlerAndUniverse(Wrestler wrestler, Universe universe);

  @Query(
      """
      SELECT s FROM WrestlerState s LEFT JOIN FETCH s.wrestler LEFT JOIN FETCH s.manager\
       WHERE s.wrestler.id = :wrestlerId AND s.universe.id = :universeId\
      """)
  Optional<WrestlerState> findByWrestlerIdAndUniverseId(
      @Param("wrestlerId") Long wrestlerId, @Param("universeId") Long universeId);

  List<WrestlerState> findByWrestlerIsPlayerTrueAndUniverseId(Long universeId);

  List<WrestlerState> findByWrestlerIsPlayerFalseAndUniverseId(Long universeId);

  List<WrestlerState> findByUniverseId(Long universeId);

  @Query("SELECT s FROM WrestlerState s LEFT JOIN FETCH s.wrestler")
  List<WrestlerState> findAllWithWrestler();

  List<WrestlerState> findByUniverseIdAndTier(
      Long universeId, com.github.javydreamercsw.base.domain.wrestler.WrestlerTier tier);

  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE WrestlerState s SET s.fans = 0, s.tier ="
          + " com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.ROOKIE WHERE"
          + " s.universe.id = :universeId")
  int resetFansAndTierByUniverseId(@Param("universeId") Long universeId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE WrestlerState s SET s.physicalCondition = 100 WHERE s.universe.id = :universeId")
  int resetPhysicalConditionByUniverseId(@Param("universeId") Long universeId);
}
