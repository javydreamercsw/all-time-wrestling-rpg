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
package com.github.javydreamercsw.management.domain.outcome;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutcomeMatrixEntryRepository extends JpaRepository<OutcomeMatrixEntry, Long> {

  Optional<OutcomeMatrixEntry> findByMatrixAndDiceRoll(OutcomeMatrix matrix, int diceRoll);

  List<OutcomeMatrixEntry> findByMatrix(OutcomeMatrix matrix);

  List<OutcomeMatrixEntry> findByMatrixOrderByDiceRollAsc(OutcomeMatrix matrix);

  /**
   * Every entry with its matrix and redirect target eagerly loaded — the seed-provenance manifest
   * generator and the version-change detection pass re-hash rows outside a Hibernate session
   * ({@code matrix} and {@code redirectToMatrix} are LAZY and the redirect target's name is part of
   * the entry domain's fingerprint).
   */
  @Query(
      "SELECT e FROM OutcomeMatrixEntry e JOIN FETCH e.matrix LEFT JOIN FETCH e.redirectToMatrix")
  List<OutcomeMatrixEntry> findAllWithMatrixAndRedirect();
}
