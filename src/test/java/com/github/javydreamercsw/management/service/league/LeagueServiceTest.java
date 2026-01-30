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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

  @Mock private LeagueRepository leagueRepository;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;

  @InjectMocks private LeagueService leagueService;

  @Test
  void testCreateLeague() {
    Account commissioner = new Account();
    commissioner.setId(1L);
    commissioner.setUsername("commish");

    when(leagueRepository.save(any(League.class)))
        .thenAnswer(
            i -> {
              League l = i.getArgument(0);
              l.setId(100L);
              return l;
            });

    League league =
        leagueService.createLeague("Test League", commissioner, 1, Collections.emptySet(), false);

    assertThat(league).isNotNull();
    assertThat(league.getName()).isEqualTo("Test League");
    assertThat(league.getCommissioner()).isEqualTo(commissioner);
    assertThat(league.getStatus()).isEqualTo(League.LeagueStatus.PRE_DRAFT);

    verify(leagueMembershipRepository).save(any(LeagueMembership.class));
  }

  @Test
  void testAddPlayer() {
    League league = new League();
    league.setId(100L);

    Account player = new Account();
    player.setId(2L);

    when(leagueMembershipRepository.findByLeagueAndMember(league, player))
        .thenReturn(Optional.empty());

    leagueService.addPlayer(league, player);

    verify(leagueMembershipRepository).save(any(LeagueMembership.class));
  }

  @Test
  void testGetLeaguesForUser() {
    Account user = new Account();
    League league = new League();
    LeagueMembership membership = new LeagueMembership();
    membership.setLeague(league);
    membership.setMember(user);

    when(leagueMembershipRepository.findByMember(user)).thenReturn(List.of(membership));

    List<League> result = leagueService.getLeaguesForUser(user);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(league);
  }
}
