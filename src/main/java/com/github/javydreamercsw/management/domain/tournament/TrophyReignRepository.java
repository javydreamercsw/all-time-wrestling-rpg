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
package com.github.javydreamercsw.management.domain.tournament;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrophyReignRepository extends JpaRepository<TrophyReign, Long> {

  /** Idempotency guard for the completion-time stamp: one reign per tournament edition. */
  boolean existsByTournamentId(Long tournamentId);

  /** The reign stamped for one tournament edition, if any. */
  Optional<TrophyReign> findByTournamentId(Long tournamentId);

  /** Detail-view lineage for a trophy: newest edition first. */
  List<TrophyReign> findByTrophyNameOrderByYearWonDesc(String trophyName);

  /** Narration honors for a singles winner. */
  List<TrophyReign> findByWinnerWrestlerId(Long winnerWrestlerId);

  /** Narration honors for a team winner (members expand in the service). */
  @Query(
      """
      SELECT r FROM TrophyReign r JOIN r.winnerTeam t JOIN t.members m\
       WHERE m.wrestler.id = :wrestlerId\
      """)
  List<TrophyReign> findByTeamMemberWrestlerId(@Param("wrestlerId") Long wrestlerId);

  /** Every reign in a universe, newest first (admin/history views). */
  List<TrophyReign> findByUniverseIdOrderByYearWonDesc(Long universeId);
}
