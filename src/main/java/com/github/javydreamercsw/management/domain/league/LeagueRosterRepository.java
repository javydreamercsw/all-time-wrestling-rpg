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
package com.github.javydreamercsw.management.domain.league;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface LeagueRosterRepository extends JpaRepository<LeagueRoster, Long> {
  List<LeagueRoster> findByLeagueAndOwner(League league, Account owner);

  List<LeagueRoster> findByLeague(League league);

  @Query(
      "SELECT r FROM LeagueRoster r LEFT JOIN FETCH r.wrestler w"
          + " LEFT JOIN FETCH w.wrestlerStates WHERE r.league = :league")
  List<LeagueRoster> findByLeagueWithWrestlerDetails(@Param("league") League league);

  Optional<LeagueRoster> findByLeagueAndWrestler(League league, Wrestler wrestler);
}
