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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInviteRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequestRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UniverseAuthorizationServiceTest {

  @Mock private UniverseMembershipRepository membershipRepository;
  @Mock private UniverseInviteRepository inviteRepository;
  @Mock private UniverseJoinRequestRepository joinRequestRepository;

  @InjectMocks private UniverseAuthorizationService service;

  private Universe universe;
  private Account account;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    account = new Account();
    account.setId(42L);
    account.setUsername("testuser");
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(Account acct) {
    CustomUserDetails details = new CustomUserDetails(acct);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
  }

  // --- isOwner ---

  @Test
  void isOwner_returnsTrueWhenCurrentUserIsOwner() {
    authenticateAs(account);
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(true);

    assertThat(service.isOwner(universe)).isTrue();
  }

  @Test
  void isOwner_returnsFalseWhenCurrentUserIsMemberNotOwner() {
    authenticateAs(account);
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(false);

    assertThat(service.isOwner(universe)).isFalse();
  }

  @Test
  void isOwner_returnsFalseWhenNotAuthenticated() {
    // No security context set up
    assertThat(service.isOwner(universe)).isFalse();
  }

  @Test
  void isOwner_returnsFalseWhenNoMembershipExists() {
    authenticateAs(account);
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(false);

    assertThat(service.isOwner(universe)).isFalse();
  }

  // --- isOwnerOfInvite ---

  @Test
  void isOwnerOfInvite_returnsTrueWhenInviteBelongsToOwnedUniverse() {
    authenticateAs(account);
    UniverseInvite invite = new UniverseInvite();
    invite.setId("token-abc");
    invite.setUniverse(universe);
    when(inviteRepository.findById("token-abc")).thenReturn(Optional.of(invite));
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(true);

    assertThat(service.isOwnerOfInvite("token-abc")).isTrue();
  }

  @Test
  void isOwnerOfInvite_returnsFalseWhenTokenNotFound() {
    authenticateAs(account);
    when(inviteRepository.findById("bad-token")).thenReturn(Optional.empty());

    assertThat(service.isOwnerOfInvite("bad-token")).isFalse();
  }

  @Test
  void isOwnerOfInvite_returnsFalseWhenUserIsNotOwnerOfInviteUniverse() {
    authenticateAs(account);
    UniverseInvite invite = new UniverseInvite();
    invite.setId("token-xyz");
    invite.setUniverse(universe);
    when(inviteRepository.findById("token-xyz")).thenReturn(Optional.of(invite));
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(false);

    assertThat(service.isOwnerOfInvite("token-xyz")).isFalse();
  }

  // --- isOwnerOfRequest ---

  @Test
  void isOwnerOfRequest_returnsTrueWhenRequestBelongsToOwnedUniverse() {
    authenticateAs(account);
    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(99L);
    request.setUniverse(universe);
    when(joinRequestRepository.findById(99L)).thenReturn(Optional.of(request));
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(true);

    assertThat(service.isOwnerOfRequest(99L)).isTrue();
  }

  @Test
  void isOwnerOfRequest_returnsFalseWhenRequestNotFound() {
    authenticateAs(account);
    when(joinRequestRepository.findById(404L)).thenReturn(Optional.empty());

    assertThat(service.isOwnerOfRequest(404L)).isFalse();
  }

  @Test
  void isOwnerOfRequest_returnsFalseWhenUserIsNotOwnerOfRequestUniverse() {
    authenticateAs(account);
    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(55L);
    request.setUniverse(universe);
    when(joinRequestRepository.findById(55L)).thenReturn(Optional.of(request));
    when(membershipRepository.existsByAccount_IdAndUniverseAndRole(
            42L, universe, UniverseMemberRole.OWNER))
        .thenReturn(false);

    assertThat(service.isOwnerOfRequest(55L)).isFalse();
  }
}
