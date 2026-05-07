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
package com.github.javydreamercsw.management.domain.wrestler;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface WrestlerRepository
    extends JpaRepository<Wrestler, Long>, JpaSpecificationExecutor<Wrestler> {

  List<Wrestler> findByAccount(com.github.javydreamercsw.base.domain.account.Account account);

  List<Wrestler> findAllByAccount(com.github.javydreamercsw.base.domain.account.Account account);

  // If you don't need a total row count, Slice is better than Page.
  @Query(
      value =
          "SELECT DISTINCT w FROM Wrestler w LEFT JOIN FETCH w.alignments LEFT JOIN FETCH"
              + " w.wrestlerStates ws LEFT JOIN FETCH ws.faction",
      countQuery = "SELECT count(DISTINCT w) FROM Wrestler w")
  Page<Wrestler> findAllBy(Pageable pageable);

  @Query(
      "SELECT DISTINCT w FROM Wrestler w LEFT JOIN FETCH w.decks LEFT JOIN FETCH w.alignments LEFT"
          + " JOIN FETCH w.reigns LEFT JOIN FETCH w.wrestlerStates ws LEFT JOIN FETCH ws.faction"
          + " WHERE w.name = :name")
  Optional<Wrestler> findByName(@Param("name") String name);

  @Query("SELECT w FROM Wrestler w LEFT JOIN FETCH w.statuses WHERE w.id = :id")
  Optional<Wrestler> findByIdWithStatuses(@Param("id") Long id);

  Optional<Wrestler> findByExternalId(String externalId);

  @Query(
      "SELECT DISTINCT w FROM Wrestler w JOIN w.wrestlerStates ws WHERE ws.fans BETWEEN :minFans"
          + " AND :maxFans")
  List<Wrestler> findByFansBetween(@Param("minFans") long minFans, @Param("maxFans") long maxFans);

  @Query("SELECT DISTINCT w FROM Wrestler w JOIN w.wrestlerStates ws WHERE ws.fans >= :minFans")
  List<Wrestler> findByFansGreaterThanEqual(@Param("minFans") long minFans);

  @Query(
      "SELECT DISTINCT w FROM Wrestler w LEFT JOIN FETCH w.alignments LEFT JOIN FETCH"
          + " w.wrestlerStates ws LEFT JOIN FETCH ws.faction WHERE w.active = true")
  List<Wrestler> findAllByActiveTrue();

  List<Wrestler> findAllByGenderAndActive(
      com.github.javydreamercsw.base.domain.wrestler.Gender gender, boolean active);

  List<Wrestler> findByAccountUsername(String username);

  List<Wrestler> findByAccountId(Long accountId);

  @Query("SELECT DISTINCT w.id FROM Wrestler w")
  List<Long> findAllIds();

  @Query("SELECT DISTINCT w FROM Wrestler w LEFT JOIN FETCH w.wrestlerStates ws WHERE w.id = :id")
  Optional<Wrestler> findByIdWithStates(@Param("id") Long id);

  @Query(
      "SELECT DISTINCT w FROM Wrestler w JOIN SegmentParticipant sp ON sp.wrestler = w WHERE"
          + " sp.segment = :segment")
  List<Wrestler> findAllBySegment(
      @Param("segment") com.github.javydreamercsw.management.domain.show.segment.Segment segment);
}
