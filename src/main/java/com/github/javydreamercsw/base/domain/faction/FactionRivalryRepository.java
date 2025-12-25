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
package com.github.javydreamercsw.base.domain.faction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactionRivalryRepository
    extends JpaRepository<FactionRivalry, Long>, JpaSpecificationExecutor<FactionRivalry> {

  Optional<FactionRivalry> findByExternalId(String externalId);

  // If you don't need a total row count, Slice is better than Page.
  Page<FactionRivalry> findAllBy(Pageable pageable);

  @Query("SELECT fr FROM FactionRivalry fr JOIN FETCH fr.faction1 JOIN FETCH fr.faction2")
  Page<FactionRivalry> findAllWithFactions(Pageable pageable);

  /** Find active faction rivalries. */
  List<FactionRivalry> findByIsActiveTrue();

  /** Find ended faction rivalries. */
  List<FactionRivalry> findByIsActiveFalse();

  /** Find rivalry between two specific factions (regardless of order). */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE ((fr.faction1 = :faction1 AND fr.faction2 = :faction2)
          OR (fr.faction1 = :faction2 AND fr.faction2 = :faction1))
      AND fr.isActive = true
      """)
  Optional<FactionRivalry> findActiveRivalryBetween(
      @Param("faction1") Faction faction1, @Param("faction2") Faction faction2);

  /** Find all rivalries involving a specific faction. */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE (fr.faction1 = :faction OR fr.faction2 = :faction)
      AND fr.isActive = true
      """)
  List<FactionRivalry> findActiveRivalriesForFaction(@Param("faction") Faction faction);

  /** Find rivalries that require matches at next show (10+ heat). */
  @Query("SELECT fr FROM FactionRivalry fr WHERE fr.isActive = true AND fr.heat >= 10")
  List<FactionRivalry> findRivalriesRequiringMatches();

  /** Find rivalries that can attempt resolution (20+ heat). */
  @Query("SELECT fr FROM FactionRivalry fr WHERE fr.isActive = true AND fr.heat >= 20")
  List<FactionRivalry> findRivalriesEligibleForResolution();

  /** Find rivalries requiring rule matches (30+ heat). */
  @Query("SELECT fr FROM FactionRivalry fr WHERE fr.isActive = true AND fr.heat >= 30")
  List<FactionRivalry> findRivalriesRequiringStipulationMatches();

  /** Find rivalries by heat level range. */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE fr.isActive = true
      AND fr.heat >= :minHeat
      AND fr.heat <= :maxHeat
      """)
  List<FactionRivalry> findByHeatRange(
      @Param("minHeat") int minHeat, @Param("maxHeat") int maxHeat);

  /** Find the hottest active faction rivalries. */
  @Query("SELECT fr FROM FactionRivalry fr WHERE fr.isActive = true ORDER BY fr.heat DESC")
  List<FactionRivalry> findHottestRivalries(Pageable pageable);

  /** Check if two factions have any rivalry history (active or ended). */
  @Query(
      """
      SELECT COUNT(fr) > 0 FROM FactionRivalry fr
      WHERE (fr.faction1 = :faction1 AND fr.faction2 = :faction2)
         OR (fr.faction1 = :faction2 AND fr.faction2 = :faction1)
      """)
  boolean hasRivalryHistory(
      @Param("faction1") Faction faction1, @Param("faction2") Faction faction2);

  /** Get rivalry count for a faction. */
  @Query(
      """
      SELECT COUNT(fr) FROM FactionRivalry fr
      WHERE (fr.faction1 = :faction OR fr.faction2 = :faction)
      AND fr.isActive = true
      """)
  long countActiveRivalriesForFaction(@Param("faction") Faction faction);

  /** Find rivalries involving stables (3+ members). */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE fr.isActive = true
      AND (SIZE(fr.faction1.members) >= 3 OR SIZE(fr.faction2.members) >= 3)
      """)
  List<FactionRivalry> findRivalriesInvolvingStables();

  /** Find tag team rivalries (both factions have exactly 2 members). */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE fr.isActive = true
      AND SIZE(fr.faction1.members) = 2
      AND SIZE(fr.faction2.members) = 2
      """)
  List<FactionRivalry> findTagTeamRivalries();

  /** Find recent faction rivalries (within specified days). */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE fr.startedDate >= :sinceDate
      ORDER BY fr.startedDate DESC
      """)
  List<FactionRivalry> findRecentRivalries(@Param("sinceDate") java.time.Instant sinceDate);

  /** Count total wrestlers involved in faction rivalries. */
  @Query(
      """
      SELECT SUM(SIZE(fr.faction1.members) + SIZE(fr.faction2.members))
      FROM FactionRivalry fr
      WHERE fr.isActive = true
      """)
  Long countTotalWrestlersInRivalries();

  /** Find rivalries with the most wrestlers involved. */
  @Query(
      """
      SELECT fr FROM FactionRivalry fr
      WHERE fr.isActive = true
      ORDER BY (SIZE(fr.faction1.members) + SIZE(fr.faction2.members)) DESC
      """)
  List<FactionRivalry> findRivalriesWithMostWrestlers(Pageable pageable);
}
