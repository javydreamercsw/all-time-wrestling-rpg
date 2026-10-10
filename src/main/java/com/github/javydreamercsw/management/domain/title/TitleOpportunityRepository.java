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
package com.github.javydreamercsw.management.domain.title;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleOpportunityRepository extends JpaRepository<TitleOpportunity, Long> {

  /**
   * Grant idempotency: one opportunity per awarding tournament (re-runs are no-ops). Explicit JPQL
   * because the entity's session-safe {@code getEarnedFromTournamentId()} getter shadows the
   * property path — a derived {@code existsByEarnedFromTournamentId} fails to resolve
   * (UnknownPathException against the real metamodel).
   */
  @Query("SELECT COUNT(t) > 0 FROM TitleOpportunity t WHERE t.earnedFromTournament.id = :id")
  boolean existsByEarnedFromTournamentId(@Param("id") Long tournamentId);

  /** One-HELD guard and badge lookup for a wrestler's current case. */
  Optional<TitleOpportunity> findFirstByWrestlerIdAndStatus(
      Long wrestlerId, TitleOpportunityStatus status);

  /** All currently HELD opportunities (booker dashboard panel, ATW-3fhh). */
  List<TitleOpportunity> findByStatus(TitleOpportunityStatus status);

  /** Career-view history, newest first. */
  List<TitleOpportunity> findByWrestlerIdOrderByEarnedAtDesc(Long wrestlerId);

  /** Expiry sweep. */
  List<TitleOpportunity> findByStatusAndExpiryDateBefore(
      TitleOpportunityStatus status, LocalDate gameDate);

  /** Opportunities that cashed in against the given title — reference check for repairs. */
  List<TitleOpportunity> findByCashedAgainstTitleId(Long titleId);

  /**
   * Detached-safe full fetch for the CRUD view (ATW-jpki): holder, universe and tournament load
   * inside the transaction so the dialog and grid can read them.
   */
  @Query("SELECT o FROM TitleOpportunity o LEFT JOIN FETCH o.wrestler LEFT JOIN FETCH o.universe")
  List<TitleOpportunity> findAllWithDetails();

  /** Newest first, for the CRUD grid (ATW-jpki). */
  List<TitleOpportunity> findAllByOrderByEarnedAtDesc();

  /**
   * Detached-safe single fetch with holder resolved — the CRUD dialog reads the holder after the
   * transaction (ATW-jpki).
   */
  @Query("SELECT o FROM TitleOpportunity o LEFT JOIN FETCH o.wrestler WHERE o.id = :id")
  Optional<TitleOpportunity> findByIdWithDetails(@Param("id") Long id);
}
