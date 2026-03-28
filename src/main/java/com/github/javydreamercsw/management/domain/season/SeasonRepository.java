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
package com.github.javydreamercsw.management.domain.season;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository
    extends JpaRepository<Season, Long>, JpaSpecificationExecutor<Season> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Season> findAllBy(Pageable pageable);

  Optional<Season> findByName(String name);

  Optional<Season> findByExternalId(String externalId);

  /** Find the currently active season. There should only be one active season at a time. */
  @Query("SELECT s FROM Season s WHERE s.isActive = true")
  Optional<Season> findActiveSeason();

  /** Find the most recent season (by creation date). */
  @Query("SELECT s FROM Season s ORDER BY s.creationDate DESC LIMIT 1")
  Optional<Season> findLatestSeason();

  /** Search seasons by name or description. */
  @Query(
      "SELECT s FROM Season s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR"
          + " s.description LIKE CONCAT('%', :searchTerm, '%')")
  Page<Season> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
      String searchTerm, Pageable pageable);

  /**
   * Find all seasons where a wrestler participated in at least one segment.
   *
   * @param wrestler the wrestler to search for
   * @return list of seasons
   */
  @Query(
      "SELECT DISTINCT se FROM Segment s "
          + "JOIN s.participants p "
          + "JOIN s.show sh "
          + "JOIN sh.season se "
          + "WHERE p.wrestler = :wrestler")
  List<Season> findByWrestler(@Param("wrestler") Wrestler wrestler);
}
