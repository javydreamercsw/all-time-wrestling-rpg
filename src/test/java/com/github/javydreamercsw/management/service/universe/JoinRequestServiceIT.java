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
package com.github.javydreamercsw.management.service.universe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest.RequestStatus;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequestRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import com.github.javydreamercsw.management.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link JoinRequestService}. Exercises real H2 schema — verifies the full
 * submit → approve → member-added flow, the block guard, and the PENDING dedup guard.
 */
class JoinRequestServiceIT extends ManagementIntegrationTest {

  @Autowired private JoinRequestService joinRequestService;
  @Autowired private InviteService inviteService;
  @Autowired private UniverseService universeService;
  @Autowired private UniverseMembershipService membershipService;
  @Autowired private AccountService accountService;
  @Autowired private UniverseJoinRequestRepository requestRepository;
  @Autowired private UniverseMembershipRepository membershipRepository;

  private Universe universe;
  private Account admin;
  private Account requester;
  private UniverseInvite invite;

  @BeforeEach
  void setUp() {
    universe =
        universeService.save(
            Universe.builder()
                .name("IT Test Universe " + System.nanoTime())
                .type(UniverseType.GLOBAL)
                .build());

    admin =
        accountService.createAccount(
            "it_admin_" + System.nanoTime(), "P@ssw0rd!", "admin@test.com", RoleName.ADMIN);
    membershipService.addMember(universe, admin, UniverseMembership.UniverseMemberRole.OWNER);

    requester =
        accountService.createAccount(
            "it_player_" + System.nanoTime(), "P@ssw0rd!", "player@test.com", RoleName.PLAYER);

    invite = inviteService.generateInvite(universe, InviteType.COMMUNITY, admin);
  }

  @AfterEach
  void cleanupTestUniverses() {
    // Remove memberships and requests before deleting the universe (FK constraints)
    universeService.findAll().stream()
        .filter(u -> u.getName().startsWith("IT Test Universe"))
        .forEach(
            u -> {
              requestRepository.findAllByUniverse(u).forEach(requestRepository::delete);
              membershipRepository.findByUniverse(u).forEach(membershipRepository::delete);
              universeService.delete(u.getId());
            });
  }

  @Test
  void submitAndApprove_addsRequesterAsMember() {
    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "Requester", null, requester);

    assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    assertThat(membershipService.isMember(universe, requester)).isFalse();

    joinRequestService.approveRequest(request.getId(), admin);

    assertThat(requestRepository.findById(request.getId()))
        .isPresent()
        .hasValueSatisfying(r -> assertThat(r.getStatus()).isEqualTo(RequestStatus.APPROVED));
    assertThat(membershipService.isMember(universe, requester)).isTrue();
  }

  @Test
  void submitAndReject_doesNotAddMember_allowsReRequest() {
    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "Requester", null, requester);

    joinRequestService.rejectRequest(request.getId(), admin, "Not yet");

    assertThat(requestRepository.findById(request.getId()).get().getStatus())
        .isEqualTo(RequestStatus.REJECTED);
    assertThat(membershipService.isMember(universe, requester)).isFalse();

    // Re-request should succeed after rejection
    UniverseJoinRequest reRequest =
        joinRequestService.submitRequest(invite, "Requester", null, requester);
    assertThat(reRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
  }

  @Test
  void submitAndBlock_preventsReRequest() {
    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "Requester", null, requester);

    joinRequestService.blockRequester(request.getId(), admin, "Spam");

    assertThatThrownBy(() -> joinRequestService.submitRequest(invite, "Requester", null, requester))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("blocked");
  }

  @Test
  void submitTwice_community_existingPending_returnsExisting() {
    UniverseJoinRequest first =
        joinRequestService.submitRequest(invite, "Requester", null, requester);

    UniverseJoinRequest second =
        joinRequestService.submitRequest(invite, "Requester", null, requester);

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(requestRepository.findAllByUniverse(universe)).hasSize(1);
  }

  @Test
  void submitTwice_targeted_withSameAccountPending_throws() {
    UniverseInvite targeted = inviteService.generateInvite(universe, InviteType.TARGETED, admin);
    joinRequestService.submitRequest(targeted, "Requester", null, requester);

    UniverseInvite targeted2 = inviteService.generateInvite(universe, InviteType.TARGETED, admin);
    assertThatThrownBy(
            () -> joinRequestService.submitRequest(targeted2, "Requester", null, requester))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already pending");
  }

  @Test
  void approveRequest_nullAccount_throws() {
    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "Anonymous", "anon@test.com", null);
    assertThat(request.getAccount()).isNull();

    assertThatThrownBy(() -> joinRequestService.approveRequest(request.getId(), admin))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no account is linked");
    assertThat(membershipService.isMember(universe, requester)).isFalse();
  }

  @Test
  void approveRequest_alreadyMember_stillApproves() {
    membershipService.addMember(
        universe,
        requester,
        com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole
            .MEMBER);

    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "Requester", null, requester);
    joinRequestService.approveRequest(request.getId(), admin);

    assertThat(requestRepository.findById(request.getId()).orElseThrow().getStatus())
        .isEqualTo(RequestStatus.APPROVED);
    assertThat(membershipService.isMember(universe, requester)).isTrue();
  }

  @Test
  void anonymousSubmit_thenLinkAccount_thenApprove_addsMember() {
    UniverseJoinRequest request =
        joinRequestService.submitRequest(invite, "New Player", "new@test.com", null);
    assertThat(request.getAccount()).isNull();

    joinRequestService.linkAccount(request.getId(), requester);
    joinRequestService.approveRequest(request.getId(), admin);

    assertThat(membershipService.isMember(universe, requester)).isTrue();
  }
}
