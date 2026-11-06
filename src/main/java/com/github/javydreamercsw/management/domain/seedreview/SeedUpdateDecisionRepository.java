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
package com.github.javydreamercsw.management.domain.seedreview;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence for pending/applied seed-update decisions (seed-update review feature). */
@Repository
public interface SeedUpdateDecisionRepository extends JpaRepository<SeedUpdateDecision, Long> {

  /** All decisions in a given status, oldest first (stable ordering for the review queue). */
  List<SeedUpdateDecision> findByStatusOrderByIdAsc(String status);

  /** Number of decisions in a given status (e.g. badge counts for the admin dashboard). */
  long countByStatus(String status);

  /**
   * At-most-one open decision per {@code (domainKey, entityKey, fieldName)} coordinate: callers
   * look up the PENDING row before inserting. H2/MySQL lack partial indexes, so the PENDING-only
   * uniqueness is enforced here (query-level check) plus service-layer dedupe instead of by the
   * database. Returns empty when no PENDING row exists for the coordinate.
   */
  Optional<SeedUpdateDecision> findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
      String domainKey, String entityKey, String fieldName, String status);
}
