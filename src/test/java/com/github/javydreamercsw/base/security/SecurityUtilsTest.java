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
package com.github.javydreamercsw.base.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  private PermissionService permissionService;
  private AuthenticationContext authenticationContext;
  private SecurityUtils securityUtils;
  private SecurityContext originalContext;

  @BeforeEach
  void setUp() {
    originalContext = SecurityContextHolder.getContext();
    SecurityContextHolder.clearContext();
    permissionService = mock(PermissionService.class);
    authenticationContext = mock(AuthenticationContext.class);
    securityUtils = new SecurityUtils(permissionService, authenticationContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.setContext(originalContext);
  }

  @Test
  void testHasRole() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.hasRole(RoleName.ADMIN)).isTrue();
    assertThat(securityUtils.hasRole(RoleName.PLAYER)).isFalse();
  }

  @Test
  void testIsAdmin() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.isAdmin()).isTrue();
    assertThat(securityUtils.isBooker()).isFalse();
  }

  @Test
  void testIsAuthenticated() {
    when(authenticationContext.isAuthenticated()).thenReturn(true);
    assertThat(securityUtils.isAuthenticated()).isTrue();

    when(authenticationContext.isAuthenticated()).thenReturn(false);
    assertThat(securityUtils.isAuthenticated()).isFalse();
  }

  @Test
  void testGetCurrentUsername() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getUsername()).thenReturn("testuser");
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.getCurrentUsername()).isEqualTo("testuser");

    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.empty());
    assertThat(securityUtils.getCurrentUsername()).isEqualTo("anonymous");
  }

  @Test
  void testGetCurrentAccountId() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getId()).thenReturn(123L);
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.getCurrentAccountId()).contains(123L);

    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.empty());
    assertThat(securityUtils.getCurrentAccountId()).isEmpty();
  }

  @Test
  void testCanCreate() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.canCreate()).isTrue();

    CustomUserDetails user2 = mock(CustomUserDetails.class);
    when(user2.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user2));
    assertThat(securityUtils.canCreate()).isFalse();
  }

  @Test
  void testCanEdit() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.canEdit()).isTrue();

    CustomUserDetails user2 = mock(CustomUserDetails.class);
    when(user2.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user2));
    assertThat(securityUtils.canEdit()).isTrue();

    CustomUserDetails user3 = mock(CustomUserDetails.class);
    when(user3.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user3));
    assertThat(securityUtils.canEdit()).isFalse();
  }

  @Test
  void testCanDelete() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_BOOKER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    assertThat(securityUtils.canDelete()).isTrue();

    CustomUserDetails user2 = mock(CustomUserDetails.class);
    when(user2.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user2));
    assertThat(securityUtils.canDelete()).isFalse();
  }

  @Test
  void testIsOwner() {
    Object target = new Object();
    when(permissionService.isOwner(target)).thenReturn(true);
    assertThat(securityUtils.isOwner(target)).isTrue();

    when(permissionService.isOwner(target)).thenReturn(false);
    assertThat(securityUtils.isOwner(target)).isFalse();
  }

  @Test
  void testCanEditTarget() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    Object target = new Object();
    assertThat(securityUtils.canEdit(target)).isTrue();

    CustomUserDetails user2 = mock(CustomUserDetails.class);
    when(user2.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user2));
    when(permissionService.isOwner(target)).thenReturn(true);
    assertThat(securityUtils.canEdit(target)).isTrue();

    when(permissionService.isOwner(target)).thenReturn(false);
    assertThat(securityUtils.canEdit(target)).isFalse();
  }

  @Test
  void testCanDeleteTarget() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_BOOKER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user));

    Object target = new Object();
    assertThat(securityUtils.canDelete(target)).isTrue();

    CustomUserDetails user2 = mock(CustomUserDetails.class);
    when(user2.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(user2));
    assertThat(securityUtils.canDelete(target)).isFalse();
  }

  @Test
  void playerCanDeleteOwnContent() {
    CustomUserDetails player = mock(CustomUserDetails.class);
    when(player.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(player));

    Object ownedTarget = new Object();
    when(permissionService.isOwner(ownedTarget)).thenReturn(true);
    assertThat(securityUtils.canDelete(ownedTarget)).isTrue();

    Object unownedTarget = new Object();
    when(permissionService.isOwner(unownedTarget)).thenReturn(false);
    assertThat(securityUtils.canDelete(unownedTarget)).isFalse();
  }

  @Test
  void viewerCannotDeleteEvenOwnContent() {
    CustomUserDetails viewer = mock(CustomUserDetails.class);
    when(viewer.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER")));
    when(authenticationContext.getAuthenticatedUser(Object.class)).thenReturn(Optional.of(viewer));

    Object target = new Object();
    when(permissionService.isOwner(target)).thenReturn(true);
    assertThat(securityUtils.canDelete(target)).isFalse();
  }

  @Test
  void testGetAuthenticatedUserWithAnonymousUser() {
    // Simulate anonymous user with a String principal
    when(authenticationContext.getAuthenticatedUser(Object.class))
        .thenReturn(Optional.of("anonymousUser"));

    Optional<CustomUserDetails> result = securityUtils.getAuthenticatedUser();

    assertThat(result).isEmpty();
  }

  @Test
  void testLogout() {
    securityUtils.logout();
    verify(authenticationContext).logout();
  }
}
