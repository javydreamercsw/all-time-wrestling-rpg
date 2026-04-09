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
package com.github.javydreamercsw.management.domain.relationship;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WrestlerRelationshipRepository extends JpaRepository<WrestlerRelationship, Long> {

  @Query(
      "SELECT r FROM WrestlerRelationship r JOIN FETCH r.wrestler1 JOIN FETCH r.wrestler2"
          + " WHERE r.wrestler1 = :wrestler OR r.wrestler2 = :wrestler")
  List<WrestlerRelationship> findAllByWrestler(@Param("wrestler") Wrestler wrestler);

  @Query(
      "SELECT r FROM WrestlerRelationship r WHERE (r.wrestler1 = :w1 AND r.wrestler2 = :w2) OR"
          + " (r.wrestler1 = :w2 AND r.wrestler2 = :w1)")
  List<WrestlerRelationship> findBetweenWrestlers(
      @Param("w1") Wrestler w1, @Param("w2") Wrestler w2);

  @Query("SELECT r FROM WrestlerRelationship r JOIN FETCH r.wrestler1 JOIN FETCH r.wrestler2")
  List<WrestlerRelationship> findAllWithWrestlers();
}
