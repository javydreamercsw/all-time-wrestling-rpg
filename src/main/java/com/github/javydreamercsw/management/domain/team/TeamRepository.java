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
package com.github.javydreamercsw.management.domain.team;

import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Team> findAllBy(Pageable pageable);

  /** Find team by name. */
  Optional<Team> findByName(String name);

  /** Find team by external ID (for Notion sync). */
  Optional<Team> findByExternalId(String externalId);

  /** Check if team name exists. */
  boolean existsByName(String name);

  /** Find all active teams. */
  List<Team> findByStatus(TeamStatus status);

  /** Find all teams associated with a faction. */
  List<Team> findByFaction(Faction faction);

  /** Find teams where a specific wrestler is a member. */
  @Query("SELECT t FROM Team t WHERE t.wrestler1 = :wrestler OR t.wrestler2 = :wrestler")
  List<Team> findByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find active teams where a specific wrestler is a member. */
  @Query(
      "SELECT t FROM Team t WHERE (t.wrestler1 = :wrestler OR t.wrestler2 = :wrestler) AND t.status"
          + " = :status")
  List<Team> findByWrestlerAndStatus(
      @Param("wrestler") Wrestler wrestler, @Param("status") TeamStatus status);

  /** Find team by both wrestlers (regardless of order). */
  @Query(
      "SELECT t FROM Team t WHERE (t.wrestler1 = :wrestler1 AND t.wrestler2 = :wrestler2) OR"
          + " (t.wrestler1 = :wrestler2 AND t.wrestler2 = :wrestler1)")
  Optional<Team> findByBothWrestlers(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find active team by both wrestlers (regardless of order). */
  @Query(
      "SELECT t FROM Team t WHERE ((t.wrestler1 = :wrestler1 AND t.wrestler2 = :wrestler2) OR"
          + " (t.wrestler1 = :wrestler2 AND t.wrestler2 = :wrestler1)) AND t.status = 'ACTIVE'")
  Optional<Team> findActiveTeamByBothWrestlers(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Count active teams. */
  long countByStatus(TeamStatus status);

  /** Find teams by faction and status. */
  List<Team> findByFactionAndStatus(Faction faction, TeamStatus status);

  @Query(
      value =
          "SELECT t FROM Team t"
              + " LEFT JOIN FETCH t.wrestler1"
              + " LEFT JOIN FETCH t.wrestler2"
              + " LEFT JOIN FETCH t.faction",
      countQuery = "SELECT count(t) FROM Team t")
  @Override
  Page<Team> findAll(Pageable pageable);

  @Query(
      "SELECT t FROM Team t"
          + " LEFT JOIN FETCH t.wrestler1"
          + " LEFT JOIN FETCH t.wrestler2"
          + " LEFT JOIN FETCH t.faction")
  @Override
  List<Team> findAll();
}
