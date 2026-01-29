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
package com.github.javydreamercsw.management.service.league;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeagueService {

  private final LeagueRepository leagueRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;

  @Transactional
  public League createLeague(String name, Account commissioner) {
    League league = new League();
    league.setName(name);
    league.setCommissioner(commissioner);
    league.setStatus(League.LeagueStatus.PRE_DRAFT);
    league = leagueRepository.save(league);

    // Add commissioner as a member with COMMISSIONER role
    addMember(league, commissioner, LeagueMembership.LeagueRole.COMMISSIONER);

    return league;
  }

  @Transactional
  public void addMember(League league, Account member, LeagueMembership.LeagueRole role) {
    if (leagueMembershipRepository.findByLeagueAndMember(league, member).isPresent()) {
      return; // Already a member
    }

    LeagueMembership membership = new LeagueMembership();
    membership.setLeague(league);
    membership.setMember(member);
    membership.setRole(role);
    leagueMembershipRepository.save(membership);
  }

  @Transactional
  public void addPlayer(League league, Account player) {
    addMember(league, player, LeagueMembership.LeagueRole.PLAYER);
  }

  public List<League> getLeaguesForUser(Account user) {
    return leagueMembershipRepository.findByMember(user).stream()
        .map(LeagueMembership::getLeague)
        .collect(Collectors.toList());
  }
}
