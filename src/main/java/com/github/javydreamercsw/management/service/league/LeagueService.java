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
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
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
  private final LeagueRosterRepository leagueRosterRepository;
  private final UniverseContextService universeContextService;

  @Transactional
  public League createLeague(
      final String name,
      final Account commissioner,
      final int maxPicks,
      final Set<Wrestler> excluded,
      final boolean commissionerPlays) {
    return createLeague(
        name,
        commissioner,
        maxPicks,
        excluded,
        commissionerPlays,
        java.math.BigDecimal.ZERO,
        null,
        100);
  }

  @Transactional
  public League createLeague(
      final String name,
      final Account commissioner,
      final int maxPicks,
      final Set<Wrestler> excluded,
      final boolean commissionerPlays,
      final java.math.BigDecimal budget,
      final Integer durationWeeks,
      final int lockerRoomMorale) {
    League league = new League();
    league.setName(name);
    league.setCommissioner(commissioner);
    league.setMaxPicksPerPlayer(maxPicks);
    league.setExcludedWrestlers(excluded);
    league.setStatus(League.LeagueStatus.PRE_DRAFT);
    league.setBudget(budget != null ? budget : java.math.BigDecimal.ZERO);
    league.setDurationWeeks(durationWeeks);
    league.setLockerRoomMorale(lockerRoomMorale);

    // Set universe from context if available
    universeContextService.getCurrentUniverse().ifPresent(league::setUniverse);

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
      final Long id,
      final String name,
      final int maxPicks,
      final Set<Wrestler> excluded,
      final boolean commissionerPlays) {
    League existing =
        leagueRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("League not found: " + id));
    return updateLeague(
        id,
        name,
        maxPicks,
        excluded,
        commissionerPlays,
        existing.getBudget(),
        existing.getDurationWeeks(),
        existing.getLockerRoomMorale() != null ? existing.getLockerRoomMorale() : 100,
        existing.getStatus());
  }

  @Transactional
  public League updateLeague(
      final Long id,
      final String name,
      final int maxPicks,
      final Set<Wrestler> excluded,
      final boolean commissionerPlays,
      final java.math.BigDecimal budget,
      final Integer durationWeeks,
      final int lockerRoomMorale,
      final League.LeagueStatus status) {
    League league =
        leagueRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("League not found: " + id));

    league.setName(name);
    league.setMaxPicksPerPlayer(maxPicks);
    league.setExcludedWrestlers(excluded);
    league.setBudget(budget != null ? budget : java.math.BigDecimal.ZERO);
    league.setDurationWeeks(durationWeeks);
    league.setLockerRoomMorale(lockerRoomMorale);
    if (status != null) {
      league.setStatus(status);
    }

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
  public void deleteLeague(final Long id) {
    League league =
        leagueRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("League not found: " + id));

    if (league.getUniverse() != null && showRepository.existsByUniverse(league.getUniverse())) {
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

  public List<League> getLeaguesForUser(final Account user) {
    return leagueMembershipRepository.findByMember(user).stream()
        .map(LeagueMembership::getLeague)
        .collect(Collectors.toList());
  }

  public Optional<League> getLeagueById(final Long id) {
    return leagueRepository.findById(id);
  }

  public Optional<League> getLeagueWithExcludedWrestlers(final Long id) {
    return leagueRepository.findByIdWithExcludedWrestlers(id);
  }

  public List<LeagueRoster> getRoster(final Long id) {
    return leagueRepository
        .findById(id)
        .map(leagueRosterRepository::findByLeague)
        .orElse(java.util.List.of());
  }
}
