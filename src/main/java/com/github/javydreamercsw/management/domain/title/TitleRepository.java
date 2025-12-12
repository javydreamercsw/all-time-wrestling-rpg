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
package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleRepository
    extends JpaRepository<Title, Long>, JpaSpecificationExecutor<Title> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Title> findAllBy(Pageable pageable);

  Optional<Title> findByName(String name);

  List<Title> findByTier(WrestlerTier tier);

  List<Title> findByIsActiveTrue();

  List<Title> findByIsActiveTrueAndTier(WrestlerTier tier);

  /** Find titles currently held by a specific wrestler. */
  @Query(
      "SELECT DISTINCT tr.title FROM TitleReign tr WHERE tr.endDate IS NULL AND :wrestler MEMBER OF"
          + " tr.champions")
  List<Title> findTitlesHeldByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Find active titles of a specific tier. */
  @Query("SELECT t FROM Title t JOIN FETCH t.champion WHERE t.tier = :tier AND t.isActive = true")
  List<Title> findActiveTitlesByTier(@Param("tier") WrestlerTier tier);

  /** Get vacant titles. */
  @Query("SELECT t FROM Title t WHERE t.champion IS EMPTY AND t.isActive = true")
  List<Title> findVacantActiveTitles();

  /** Check if a title with the given name already exists. */
  boolean existsByName(String name);

  /** Find a title by its external ID (Notion page ID). */
  Optional<Title> findByExternalId(String externalId);

  /** Find titles that a wrestler is eligible to challenge for. */
  @Query(
      """
      SELECT t FROM Title t
      WHERE t.isActive = true
      AND (
          (t.tier = 'ROOKIE' AND :fanCount >= 0) OR
          (t.tier = 'TAG_TEAM' AND :fanCount >= 40000) OR
          (t.tier = 'EXTREME' AND :fanCount >= 25000) OR
          (t.tier = 'WORLD' AND :fanCount >= 100000)
      )
      """)
  List<Title> findEligibleTitlesForFanCount(@Param("fanCount") Long fanCount);
}
