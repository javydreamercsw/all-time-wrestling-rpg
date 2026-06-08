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
package com.github.javydreamercsw.management.domain.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest.RequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UniverseJoinRequestRepository extends JpaRepository<UniverseJoinRequest, Long> {

  @Query(
      "SELECT r FROM UniverseJoinRequest r WHERE r.universe = :universe"
          + " AND r.status = 'PENDING' ORDER BY r.requestedAt ASC")
  List<UniverseJoinRequest> findPendingByUniverse(@Param("universe") Universe universe);

  @Query(
      "SELECT r FROM UniverseJoinRequest r WHERE r.universe = :universe"
          + " ORDER BY r.requestedAt DESC")
  List<UniverseJoinRequest> findAllByUniverse(@Param("universe") Universe universe);

  /** Checks whether an account is blocked from a universe — prevents re-requests. */
  Optional<UniverseJoinRequest> findByUniverseAndAccountAndStatus(
      Universe universe, Account account, RequestStatus status);

  /** Finds any existing PENDING request from the same account to avoid duplicates. */
  Optional<UniverseJoinRequest> findByUniverseAndAccountAndStatusIn(
      Universe universe, Account account, List<RequestStatus> statuses);

  /** Counts pending requests across all universes — used for admin badge. */
  @Query(
      "SELECT COUNT(r) FROM UniverseJoinRequest r"
          + " JOIN UniverseMembership m ON m.universe = r.universe"
          + " WHERE r.status = 'PENDING' AND m.account = :admin")
  long countPendingForAdmin(@Param("admin") Account admin);
}
