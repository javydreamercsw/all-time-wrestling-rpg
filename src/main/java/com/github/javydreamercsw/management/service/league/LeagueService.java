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
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeagueService {

  private final LeagueRepository leagueRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;
  private final ShowRepository showRepository;

  @Transactional
  public League createLeague(
      String name,
      Account commissioner,
      int maxPicks,
      Set<Wrestler> excluded,
      boolean commissionerPlays) {
    League league = new League();
    league.setName(name);
    league.setCommissioner(commissioner);
    league.setMaxPicksPerPlayer(maxPicks);
    league.setExcludedWrestlers(excluded);
    league.setStatus(League.LeagueStatus.PRE_DRAFT);
    league = leagueRepository.save(league);

    // Add commissioner
    addMember(
        league,
        commissioner,
        commissionerPlays
            ? LeagueMembership.LeagueRole.COMMISSIONER_PLAYER
            : LeagueMembership.LeagueRole.COMMISSIONER);

    return league;
  }

  @Transactional
  public League updateLeague(
      Long id, String name, int maxPicks, Set<Wrestler> excluded, boolean commissionerPlays) {
    League league =
        leagueRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("League not found: " + id));

    league.setName(name);
    league.setMaxPicksPerPlayer(maxPicks);
    league.setExcludedWrestlers(excluded);

    // Update commissioner role if needed
    Optional<LeagueMembership> commMembership =
        leagueMembershipRepository.findByLeagueAndMember(league, league.getCommissioner());

    commMembership.ifPresent(
        m -> {
          m.setRole(
              commissionerPlays
                  ? LeagueMembership.LeagueRole.COMMISSIONER_PLAYER
                  : LeagueMembership.LeagueRole.COMMISSIONER);
          leagueMembershipRepository.save(m);
        });

    return leagueRepository.save(league);
  }

  @Transactional
  public void deleteLeague(Long id) {
    League league =
        leagueRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("League not found: " + id));

    if (showRepository.existsByLeague(league)) {
      throw new IllegalStateException("Cannot delete league with associated shows.");
    }

    // Delete memberships first
    List<LeagueMembership> memberships = leagueMembershipRepository.findByLeague(league);
    leagueMembershipRepository.deleteAll(memberships);

    // Delete league
    leagueRepository.delete(league);
  }

  @Transactional
  public void addMember(League league, Account member, LeagueMembership.LeagueRole role) {
    Optional<LeagueMembership> existing =
        leagueMembershipRepository.findByLeagueAndMember(league, member);
    if (existing.isPresent()) {
      existing.get().setRole(role);
      leagueMembershipRepository.save(existing.get());
      return;
    }

    LeagueMembership membership = new LeagueMembership();
    membership.setLeague(league);
    membership.setMember(member);
    membership.setRole(role);
    leagueMembershipRepository.save(membership);
  }

  @Transactional
  public void addPlayer(League league, Account player) {
    // Only add as PLAYER if they aren't already a COMMISSIONER or COMMISSIONER_PLAYER
    Optional<LeagueMembership> existing =
        leagueMembershipRepository.findByLeagueAndMember(league, player);
    if (existing.isPresent()) {
      if (existing.get().getRole() == LeagueMembership.LeagueRole.COMMISSIONER) {
        existing.get().setRole(LeagueMembership.LeagueRole.COMMISSIONER_PLAYER);
        leagueMembershipRepository.save(existing.get());
      }
      return;
    }
    addMember(league, player, LeagueMembership.LeagueRole.PLAYER);
  }

  @Transactional
  public void removeMember(League league, Account member) {
    leagueMembershipRepository
        .findByLeagueAndMember(league, member)
        .ifPresent(
            m -> {
              if (m.getMember().equals(league.getCommissioner())) {
                throw new IllegalStateException("Cannot remove commissioner from league.");
              }
              leagueMembershipRepository.delete(m);
            });
  }

  public List<League> getLeaguesForUser(Account user) {
    return leagueMembershipRepository.findByMember(user).stream()
        .map(LeagueMembership::getLeague)
        .collect(Collectors.toList());
  }

  public Optional<League> getLeagueById(Long id) {
    return leagueRepository.findById(id);
  }

  public Optional<League> getLeagueWithExcludedWrestlers(Long id) {
    return leagueRepository.findByIdWithExcludedWrestlers(id);
  }
}
