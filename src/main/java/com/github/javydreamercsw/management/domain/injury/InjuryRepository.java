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
package com.github.javydreamercsw.management.domain.injury;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InjuryRepository
    extends JpaRepository<Injury, Long>, JpaSpecificationExecutor<Injury> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Injury> findAllBy(Pageable pageable);

  /** Find all injuries for a specific wrestler. */
  List<Injury> findByWrestler(Wrestler wrestler);

  /** Find active injuries for a specific wrestler. */
  @Query("SELECT i FROM Injury i WHERE i.wrestler = :wrestler AND i.isActive = true")
  List<Injury> findActiveInjuriesForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find healed injuries for a specific wrestler. */
  @Query("SELECT i FROM Injury i WHERE i.wrestler = :wrestler AND i.isActive = false")
  List<Injury> findHealedInjuriesForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find injuries by severity. */
  List<Injury> findBySeverity(InjurySeverity severity);

  /** Find injury by external ID (e.g., Notion page ID). */
  java.util.Optional<Injury> findByExternalId(String externalId);

  /** Check if injury exists by external ID. */
  boolean existsByExternalId(String externalId);

  /** Find all active injuries. */
  @Query("SELECT i FROM Injury i WHERE i.isActive = true")
  List<Injury> findAllActiveInjuries();

  /** Get total health penalty for a wrestler from active injuries. */
  @Query(
      """
      SELECT COALESCE(SUM(i.healthPenalty), 0)
      FROM Injury i
      WHERE i.wrestler = :wrestler AND i.isActive = true
      """)
  Integer getTotalHealthPenaltyForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Count active injuries for a wrestler. */
  @Query(
      """
      SELECT COUNT(i)
      FROM Injury i
      WHERE i.wrestler = :wrestler AND i.isActive = true
      """)
  long countActiveInjuriesForWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find wrestlers with active injuries. */
  @Query(
      """
      SELECT DISTINCT i.wrestler
      FROM Injury i
      WHERE i.isActive = true
      """)
  List<Wrestler> findWrestlersWithActiveInjuries();

  /** Find injuries that can be healed (active injuries). */
  @Query("SELECT i FROM Injury i WHERE i.isActive = true ORDER BY i.injuryDate ASC")
  List<Injury> findHealableInjuries();

  /** Find the most severe active injury for a wrestler. */
  @Query(
      """
      SELECT i FROM Injury i
      WHERE i.wrestler = :wrestler AND i.isActive = true
      ORDER BY i.severity DESC, i.healthPenalty DESC
      LIMIT 1
      """)
  java.util.Optional<Injury> findMostSevereActiveInjuryForWrestler(
      @Param("wrestler") Wrestler wrestler);
}
