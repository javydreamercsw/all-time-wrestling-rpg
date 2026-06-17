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
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
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
  @Mock private UniverseContextService universeContextService;

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

  private UniverseMembership membershipWith(UniverseMemberRole role) {
    UniverseMembership m = new UniverseMembership();
    m.setUniverse(universe);
    m.setAccount(account);
    m.setRole(role);
    return m;
  }

  private void stubMembership(UniverseMemberRole role) {
    when(membershipRepository.findByAccount_IdAndUniverse(42L, universe))
        .thenReturn(Optional.of(membershipWith(role)));
  }

  private void stubNoMembership() {
    when(membershipRepository.findByAccount_IdAndUniverse(42L, universe))
        .thenReturn(Optional.empty());
  }

  // --- hasRole: hierarchy ---

  @Test
  void hasRole_ownerSatisfiesAllRoles() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.OWNER);

    assertThat(service.hasRole(universe, "OWNER")).isTrue();
    assertThat(service.hasRole(universe, "ADMIN")).isTrue();
    assertThat(service.hasRole(universe, "BOOKER")).isTrue();
    assertThat(service.hasRole(universe, "PLAYER")).isTrue();
    assertThat(service.hasRole(universe, "MEMBER")).isTrue();
  }

  @Test
  void hasRole_adminSatisfiesAdminAndBelow() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.hasRole(universe, "OWNER")).isFalse();
    assertThat(service.hasRole(universe, "ADMIN")).isTrue();
    assertThat(service.hasRole(universe, "BOOKER")).isTrue();
    assertThat(service.hasRole(universe, "PLAYER")).isTrue();
    assertThat(service.hasRole(universe, "MEMBER")).isTrue();
  }

  @Test
  void hasRole_bookerSatisfiesBookerAndBelow() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.BOOKER);

    assertThat(service.hasRole(universe, "OWNER")).isFalse();
    assertThat(service.hasRole(universe, "ADMIN")).isFalse();
    assertThat(service.hasRole(universe, "BOOKER")).isTrue();
    assertThat(service.hasRole(universe, "PLAYER")).isTrue();
    assertThat(service.hasRole(universe, "MEMBER")).isTrue();
  }

  @Test
  void hasRole_memberSatisfiesOnlyMember() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.MEMBER);

    assertThat(service.hasRole(universe, "OWNER")).isFalse();
    assertThat(service.hasRole(universe, "ADMIN")).isFalse();
    assertThat(service.hasRole(universe, "BOOKER")).isFalse();
    assertThat(service.hasRole(universe, "PLAYER")).isFalse();
    assertThat(service.hasRole(universe, "MEMBER")).isTrue();
  }

  @Test
  void hasRole_returnsFalseWhenNotAuthenticated() {
    assertThat(service.hasRole(universe, "MEMBER")).isFalse();
  }

  @Test
  void hasRole_returnsFalseWhenNoMembership() {
    authenticateAs(account);
    stubNoMembership();

    assertThat(service.hasRole(universe, "MEMBER")).isFalse();
  }

  @Test
  void hasRole_returnsFalseForUnknownRoleName() {
    authenticateAs(account);
    // No membership stub needed — invalid role name short-circuits before repo lookup
    assertThat(service.hasRole(universe, "SUPERADMIN")).isFalse();
  }

  // --- isOwner ---

  @Test
  void isOwner_returnsTrueWhenCurrentUserIsOwner() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.OWNER);

    assertThat(service.isOwner(universe)).isTrue();
  }

  @Test
  void isOwner_returnsFalseWhenCurrentUserIsAdmin() {
    authenticateAs(account);
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.isOwner(universe)).isFalse();
  }

  @Test
  void isOwner_returnsFalseWhenNotAuthenticated() {
    assertThat(service.isOwner(universe)).isFalse();
  }

  // --- hasRoleForInvite ---

  @Test
  void hasRoleForInvite_returnsTrueWhenUserHasRequiredRoleInInviteUniverse() {
    authenticateAs(account);
    UniverseInvite invite = new UniverseInvite();
    invite.setId("token-abc");
    invite.setUniverse(universe);
    when(inviteRepository.findById("token-abc")).thenReturn(Optional.of(invite));
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.hasRoleForInvite("token-abc", "ADMIN")).isTrue();
  }

  @Test
  void hasRoleForInvite_returnsFalseWhenTokenNotFound() {
    authenticateAs(account);
    when(inviteRepository.findById("bad-token")).thenReturn(Optional.empty());

    assertThat(service.hasRoleForInvite("bad-token", "ADMIN")).isFalse();
  }

  @Test
  void isOwnerOfInvite_returnsTrueWhenUserIsOwnerOfInviteUniverse() {
    authenticateAs(account);
    UniverseInvite invite = new UniverseInvite();
    invite.setId("token-xyz");
    invite.setUniverse(universe);
    when(inviteRepository.findById("token-xyz")).thenReturn(Optional.of(invite));
    stubMembership(UniverseMemberRole.OWNER);

    assertThat(service.isOwnerOfInvite("token-xyz")).isTrue();
  }

  @Test
  void isOwnerOfInvite_returnsFalseWhenUserIsAdminNotOwner() {
    authenticateAs(account);
    UniverseInvite invite = new UniverseInvite();
    invite.setId("token-xyz");
    invite.setUniverse(universe);
    when(inviteRepository.findById("token-xyz")).thenReturn(Optional.of(invite));
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.isOwnerOfInvite("token-xyz")).isFalse();
  }

  // --- hasRoleForRequest ---

  @Test
  void hasRoleForRequest_returnsTrueWhenUserHasRequiredRoleInRequestUniverse() {
    authenticateAs(account);
    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(99L);
    request.setUniverse(universe);
    when(joinRequestRepository.findById(99L)).thenReturn(Optional.of(request));
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.hasRoleForRequest(99L, "ADMIN")).isTrue();
  }

  @Test
  void hasRoleForRequest_returnsFalseWhenRequestNotFound() {
    authenticateAs(account);
    when(joinRequestRepository.findById(404L)).thenReturn(Optional.empty());

    assertThat(service.hasRoleForRequest(404L, "ADMIN")).isFalse();
  }

  @Test
  void isOwnerOfRequest_returnsTrueWhenUserIsOwnerOfRequestUniverse() {
    authenticateAs(account);
    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(55L);
    request.setUniverse(universe);
    when(joinRequestRepository.findById(55L)).thenReturn(Optional.of(request));
    stubMembership(UniverseMemberRole.OWNER);

    assertThat(service.isOwnerOfRequest(55L)).isTrue();
  }

  @Test
  void isOwnerOfRequest_returnsFalseWhenUserIsAdminNotOwner() {
    authenticateAs(account);
    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(55L);
    request.setUniverse(universe);
    when(joinRequestRepository.findById(55L)).thenReturn(Optional.of(request));
    stubMembership(UniverseMemberRole.ADMIN);

    assertThat(service.isOwnerOfRequest(55L)).isFalse();
  }

  // --- hasRoleInCurrentUniverse ---

  @Test
  void hasRoleInCurrentUniverse_returnsTrueWhenUserHasRequiredRoleInActiveUniverse() {
    authenticateAs(account);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
    stubMembership(UniverseMemberRole.BOOKER);

    assertThat(service.hasRoleInCurrentUniverse("BOOKER")).isTrue();
  }

  @Test
  void hasRoleInCurrentUniverse_returnsTrueForOwnerWhenBookerRequired() {
    authenticateAs(account);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
    stubMembership(UniverseMemberRole.OWNER);

    assertThat(service.hasRoleInCurrentUniverse("BOOKER")).isTrue();
  }

  @Test
  void hasRoleInCurrentUniverse_returnsFalseWhenMemberBelowRequired() {
    authenticateAs(account);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
    stubMembership(UniverseMemberRole.MEMBER);

    assertThat(service.hasRoleInCurrentUniverse("BOOKER")).isFalse();
  }

  @Test
  void hasRoleInCurrentUniverse_returnsFalseWhenNoActiveUniverse() {
    authenticateAs(account);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());

    assertThat(service.hasRoleInCurrentUniverse("BOOKER")).isFalse();
  }
}
