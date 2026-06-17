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
package com.github.javydreamercsw.management.service.tutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeagueTutorialDefinitionTest {

  @Mock private LeagueService leagueService;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;
  @Mock private DraftRepository draftRepository;
  @Mock private DraftPickRepository draftPickRepository;
  @Mock private InviteService inviteService;
  @Mock private UniverseMembershipRepository universeMembershipRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseService universeService;

  private LeagueTutorialDefinition definition;
  private Account account;

  @BeforeEach
  void setUp() {
    definition =
        new LeagueTutorialDefinition(
            leagueService,
            leagueMembershipRepository,
            draftRepository,
            draftPickRepository,
            inviteService,
            universeMembershipRepository,
            universeContextService,
            universeService);
    account = new Account();
    account.setId(1L);
    account.setUsername("test-user");
  }

  // --- Step 1: commissioner check ---

  @Test
  @DisplayName("Step 1 validate passes when account is COMMISSIONER")
  void step1_validate_commissionerRole_passes() {
    LeagueMembership m = new LeagueMembership();
    m.setRole(LeagueMembership.LeagueRole.COMMISSIONER);
    when(leagueMembershipRepository.findByMember(account)).thenReturn(List.of(m));

    String result = definition.getSteps().get(0).validate(account);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Step 1 validate passes when account is COMMISSIONER_PLAYER")
  void step1_validate_commissionerPlayerRole_passes() {
    LeagueMembership m = new LeagueMembership();
    m.setRole(LeagueMembership.LeagueRole.COMMISSIONER_PLAYER);
    when(leagueMembershipRepository.findByMember(account)).thenReturn(List.of(m));

    String result = definition.getSteps().get(0).validate(account);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Step 1 validate fails when account has no commissioner membership")
  void step1_validate_noCommissionerRole_fails() {
    when(leagueMembershipRepository.findByMember(account)).thenReturn(List.of());

    String result = definition.getSteps().get(0).validate(account);

    assertThat(result).isNotNull();
  }

  // --- Step 2: invite sent + member accepted ---

  @Test
  @DisplayName("Step 2 validate fails when tutorial universe not found")
  void step2_validate_noUniverse_fails() {
    when(universeService.findByName("Tutorial – test-user")).thenReturn(Optional.empty());

    String result = definition.getSteps().get(1).validate(account);

    assertThat(result).contains("Tutorial universe not found");
  }

  @Test
  @DisplayName("Step 2 validate fails when no invite has been sent")
  void step2_validate_noInviteSent_fails() {
    Universe universe = new Universe();
    when(universeService.findByName("Tutorial – test-user")).thenReturn(Optional.of(universe));
    when(inviteService.listActiveInvites(universe)).thenReturn(List.of());

    String result = definition.getSteps().get(1).validate(account);

    assertThat(result).contains("No invite has been sent");
  }

  @Test
  @DisplayName("Step 2 validate fails when invite sent but no one has accepted yet")
  void step2_validate_inviteSentNoMembers_fails() {
    Universe universe = new Universe();
    when(universeService.findByName("Tutorial – test-user")).thenReturn(Optional.of(universe));
    when(inviteService.listActiveInvites(universe)).thenReturn(List.of(new UniverseInvite()));
    UniverseMembership selfMember = new UniverseMembership();
    when(universeMembershipRepository.findByUniverse(universe)).thenReturn(List.of(selfMember));

    String result = definition.getSteps().get(1).validate(account);

    assertThat(result).contains("Waiting for a player");
  }

  @Test
  @DisplayName("Step 2 validate passes when invite sent and at least one other player joined")
  void step2_validate_inviteSentAndMemberJoined_passes() {
    Universe universe = new Universe();
    when(universeService.findByName("Tutorial – test-user")).thenReturn(Optional.of(universe));
    when(inviteService.listActiveInvites(universe)).thenReturn(List.of(new UniverseInvite()));
    when(universeMembershipRepository.findByUniverse(universe))
        .thenReturn(List.of(new UniverseMembership(), new UniverseMembership()));

    String result = definition.getSteps().get(1).validate(account);

    assertThat(result).isNull();
  }

  // --- Step 3: draft started ---

  @Test
  @DisplayName("Step 3 validate passes when a draft exists for the league")
  void step3_validate_draftExists_passes() {
    when(draftRepository.existsByLeague_Commissioner(account)).thenReturn(true);

    String result = definition.getSteps().get(2).validate(account);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Step 3 validate fails when no draft has been started")
  void step3_validate_noDraft_fails() {
    when(draftRepository.existsByLeague_Commissioner(account)).thenReturn(false);

    String result = definition.getSteps().get(2).validate(account);

    assertThat(result).contains("No draft found");
  }

  // --- Step 4: draft pick made ---

  @Test
  @DisplayName("Step 4 validate passes when account has made a draft pick")
  void step4_validate_pickExists_passes() {
    when(draftPickRepository.existsByUser(account)).thenReturn(true);

    String result = definition.getSteps().get(3).validate(account);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Step 4 validate fails when account has not made any pick")
  void step4_validate_noPick_fails() {
    when(draftPickRepository.existsByUser(account)).thenReturn(false);

    String result = definition.getSteps().get(3).validate(account);

    assertThat(result).contains("haven't made a pick");
  }
}
