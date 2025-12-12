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
package com.github.javydreamercsw.management.domain.faction;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactionRepository
    extends JpaRepository<Faction, Long>, JpaSpecificationExecutor<Faction> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Faction> findAllBy(Pageable pageable);

  /** Find faction by name. */
  Optional<Faction> findByName(String name);

  /** Find faction by external ID (e.g., Notion page ID). */
  Optional<Faction> findByExternalId(String externalId);

  /** Check if faction name exists. */
  boolean existsByName(String name);

  /** Find active factions. */
  List<Faction> findByIsActiveTrue();

  /** Find active or inactive factions. */
  List<Faction> findByIsActive(boolean active);

  /** Find disbanded factions. */
  List<Faction> findByIsActiveFalse();

  /** Find all factions with members and teams eagerly loaded for UI display. */
  @Query(
      """
      SELECT DISTINCT f FROM Faction f
      LEFT JOIN FETCH f.members m
      LEFT JOIN FETCH f.teams t
      ORDER BY f.name
      """)
  List<Faction> findAllWithMembersAndTeams();

  /** Find factions led by a specific wrestler. */
  List<Faction> findByLeader(Wrestler leader);

  /** Find active factions led by a specific wrestler. */
  List<Faction> findByIsActiveTrueAndLeader(Wrestler leader);

  /** Find factions with a specific member. */
  @Query(
      """
      SELECT f FROM Faction f
      JOIN f.members m
      WHERE m = :wrestler AND f.isActive = true
      """)
  Optional<Faction> findActiveFactionByMember(@Param("wrestler") Wrestler wrestler);

  /** Find all factions (active and inactive) with a specific member. */
  @Query(
      """
      SELECT f FROM Faction f
      JOIN f.members m
      WHERE m = :wrestler
      """)
  List<Faction> findFactionsByMember(@Param("wrestler") Wrestler wrestler);

  /** Find factions by member count range. */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      AND SIZE(f.members) >= :minMembers
      AND SIZE(f.members) <= :maxMembers
      """)
  List<Faction> findByMemberCountRange(
      @Param("minMembers") int minMembers, @Param("maxMembers") int maxMembers);

  /** Find singles factions (1 member). */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      AND SIZE(f.members) = 1
      """)
  List<Faction> findSinglesFactions();

  /** Find tag team factions (2 members). */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      AND SIZE(f.members) = 2
      """)
  List<Faction> findTagTeamFactions();

  /** Find stable factions (3+ members). */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      AND SIZE(f.members) >= 3
      """)
  List<Faction> findStableFactions();

  /** Find factions with active rivalries. */
  @Query(
      """
      SELECT DISTINCT f FROM Faction f
      WHERE f.isActive = true
      AND (EXISTS (SELECT fr FROM FactionRivalry fr WHERE fr.faction1 = f AND fr.isActive = true)
           OR EXISTS (SELECT fr FROM FactionRivalry fr WHERE fr.faction2 = f AND fr.isActive = true))
      """)
  List<Faction> findFactionsWithActiveRivalries();

  /** Find factions without any active rivalries. */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      AND NOT EXISTS (SELECT fr FROM FactionRivalry fr WHERE (fr.faction1 = f OR fr.faction2 = f) AND fr.isActive = true)
      """)
  List<Faction> findFactionsWithoutActiveRivalries();

  /** Count total active factions. */
  long countByIsActiveTrue();

  /** Find largest active factions. */
  @Query(
      """
      SELECT f FROM Faction f
      WHERE f.isActive = true
      ORDER BY SIZE(f.members) DESC
      """)
  List<Faction> findLargestFactions(Pageable pageable);

  /** Find all factions with members and leader eagerly loaded for UI display. */
  @Query(
      """
      SELECT DISTINCT f FROM Faction f
      LEFT JOIN FETCH f.members m
      LEFT JOIN FETCH f.leader
      ORDER BY f.name
      """)
  List<Faction> findAllWithMembers();

  /** Find all factions with teams eagerly loaded for UI display. */
  @Query(
      """
      SELECT DISTINCT f FROM Faction f
      LEFT JOIN FETCH f.teams t
      ORDER BY f.name
      """)
  List<Faction> findAllWithTeams();

  /** Find faction by ID with members and leader eagerly loaded. */
  @Query(
      """
      SELECT f FROM Faction f
      LEFT JOIN FETCH f.members m
      LEFT JOIN FETCH f.leader
      WHERE f.id = :id
      """)
  Optional<Faction> findByIdWithMembers(@Param("id") Long id);

  /** Find newest active factions. */
  List<Faction> findByIsActiveTrueOrderByFormedDateDesc();

  /** Find oldest active factions. */
  List<Faction> findByIsActiveTrueOrderByFormedDateAsc();
}
