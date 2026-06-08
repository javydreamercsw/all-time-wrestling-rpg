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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest.RequestStatus;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequestRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinRequestServiceTest {

  @Mock private UniverseJoinRequestRepository requestRepository;
  @Mock private UniverseMembershipService membershipService;
  @Mock private InviteService inviteService;

  @InjectMocks private JoinRequestService service;

  private Universe universe;
  private Account admin;
  private Account requester;
  private UniverseInvite invite;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    admin = new Account();
    admin.setId(1L);
    admin.setUsername("admin");

    requester = new Account();
    requester.setId(2L);
    requester.setUsername("player1");

    invite = new UniverseInvite();
    invite.setId("tok-123");
    invite.setUniverse(universe);
    invite.setType(InviteType.COMMUNITY);

    when(requestRepository.save(any(UniverseJoinRequest.class)))
        .thenAnswer(
            inv -> {
              UniverseJoinRequest r = inv.getArgument(0);
              if (r.getId() == null) r.setId(99L);
              return r;
            });
  }

  // ── submitRequest ─────────────────────────────────────────────────────────

  @Test
  void submitRequest_newRequest_createsPending() {
    when(requestRepository.findByUniverseAndAccountAndStatus(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(requestRepository.findByUniverseAndAccountAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());

    UniverseJoinRequest result =
        service.submitRequest(invite, "Player One", "p@test.com", requester);

    assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
    assertThat(result.getRequesterName()).isEqualTo("Player One");
    assertThat(result.getUniverse()).isEqualTo(universe);
    verify(inviteService).recordUse(invite);
  }

  @Test
  void submitRequest_anonymous_noBlockCheck() {
    UniverseJoinRequest result = service.submitRequest(invite, "Anonymous", null, null);

    assertThat(result.getAccount()).isNull();
    assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
  }

  @Test
  void submitRequest_blockedAccount_throws() {
    UniverseJoinRequest blocked = new UniverseJoinRequest();
    blocked.setStatus(RequestStatus.BLOCKED);
    when(requestRepository.findByUniverseAndAccountAndStatus(
            universe, requester, RequestStatus.BLOCKED))
        .thenReturn(Optional.of(blocked));

    assertThatThrownBy(() -> service.submitRequest(invite, "Player One", null, requester))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("blocked");
  }

  @Test
  void submitRequest_alreadyPending_throws() {
    when(requestRepository.findByUniverseAndAccountAndStatus(any(), any(), any()))
        .thenReturn(Optional.empty());
    UniverseJoinRequest pending = new UniverseJoinRequest();
    pending.setStatus(RequestStatus.PENDING);
    when(requestRepository.findByUniverseAndAccountAndStatusIn(
            universe, requester, List.of(RequestStatus.PENDING)))
        .thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> service.submitRequest(invite, "Player One", null, requester))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already pending");
  }

  // ── approveRequest ────────────────────────────────────────────────────────

  @Test
  void approveRequest_pending_approvesAndAddsMember() {
    UniverseJoinRequest request = pendingRequest();
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    service.approveRequest(1L, admin);

    assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    assertThat(request.getResolvedBy()).isEqualTo(admin);
    assertThat(request.getResolvedAt()).isNotNull();
    verify(membershipService)
        .addMember(
            universe,
            requester,
            com.github.javydreamercsw.management.domain.universe.UniverseMembership
                .UniverseMemberRole.MEMBER);
  }

  @Test
  void approveRequest_notPending_throws() {
    UniverseJoinRequest request = pendingRequest();
    request.setStatus(RequestStatus.REJECTED);
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    assertThatThrownBy(() -> service.approveRequest(1L, admin))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not PENDING");
  }

  @Test
  void approveRequest_noLinkedAccount_doesNotCallAddMember() {
    UniverseJoinRequest request = pendingRequest();
    request.setAccount(null);
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    service.approveRequest(1L, admin);

    assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    verify(membershipService, never()).addMember(any(), any(), any());
  }

  // ── rejectRequest ─────────────────────────────────────────────────────────

  @Test
  void rejectRequest_pending_setsRejectedWithNotes() {
    UniverseJoinRequest request = pendingRequest();
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    service.rejectRequest(1L, admin, "Not eligible yet");

    assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    assertThat(request.getNotes()).isEqualTo("Not eligible yet");
    assertThat(request.getResolvedBy()).isEqualTo(admin);
  }

  @Test
  void rejectRequest_notPending_throws() {
    UniverseJoinRequest request = pendingRequest();
    request.setStatus(RequestStatus.APPROVED);
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    assertThatThrownBy(() -> service.rejectRequest(1L, admin, null))
        .isInstanceOf(IllegalStateException.class);
  }

  // ── blockRequester ────────────────────────────────────────────────────────

  @Test
  void blockRequester_setsBlocked() {
    UniverseJoinRequest request = pendingRequest();
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    service.blockRequester(1L, admin, "Spam");

    assertThat(request.getStatus()).isEqualTo(RequestStatus.BLOCKED);
    assertThat(request.getNotes()).isEqualTo("Spam");
  }

  @Test
  void blockRequester_canBlockAlreadyRejected() {
    UniverseJoinRequest request = pendingRequest();
    request.setStatus(RequestStatus.REJECTED);
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    // block doesn't require PENDING — works on any status
    service.blockRequester(1L, admin, "Persistent bad actor");

    assertThat(request.getStatus()).isEqualTo(RequestStatus.BLOCKED);
  }

  // ── getPendingRequests ────────────────────────────────────────────────────

  @Test
  void getPendingRequests_delegatesToRepository() {
    UniverseJoinRequest r = pendingRequest();
    when(requestRepository.findPendingByUniverse(universe)).thenReturn(List.of(r));

    List<UniverseJoinRequest> result = service.getPendingRequests(universe);

    assertThat(result).containsExactly(r);
  }

  // ── linkAccount ───────────────────────────────────────────────────────────

  @Test
  void linkAccount_setsAccountOnRequest() {
    UniverseJoinRequest request = pendingRequest();
    request.setAccount(null);
    when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

    service.linkAccount(1L, requester);

    ArgumentCaptor<UniverseJoinRequest> captor = ArgumentCaptor.forClass(UniverseJoinRequest.class);
    verify(requestRepository).save(captor.capture());
    assertThat(captor.getValue().getAccount()).isEqualTo(requester);
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private UniverseJoinRequest pendingRequest() {
    UniverseJoinRequest r = new UniverseJoinRequest();
    r.setId(1L);
    r.setUniverse(universe);
    r.setAccount(requester);
    r.setRequesterName("Player One");
    r.setStatus(RequestStatus.PENDING);
    return r;
  }
}
