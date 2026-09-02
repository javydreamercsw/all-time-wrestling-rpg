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

import com.github.javydreamercsw.management.domain.title.Title;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WrestlerTitleCooldownRepository
    extends JpaRepository<WrestlerTitleCooldown, Long> {

  Optional<WrestlerTitleCooldown> findByWrestlerStateAndTitle(
      WrestlerState wrestlerState, Title title);

  Optional<WrestlerTitleCooldown> findByWrestlerState_IdAndTitle_Id(
      Long wrestlerStateId, Long titleId);

  @Modifying
  @Query(
      "UPDATE WrestlerTitleCooldown c SET c.defenseCountAtChallenge = :count"
          + " WHERE c.wrestlerState.id = :stateId AND c.title.id = :titleId")
  int updateDefenseCount(
      @Param("stateId") Long stateId, @Param("titleId") Long titleId, @Param("count") long count);
}
