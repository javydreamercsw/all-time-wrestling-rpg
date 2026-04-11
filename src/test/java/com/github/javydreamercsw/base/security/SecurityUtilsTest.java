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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class SecurityUtilsTest {

  private AuthenticationContext authenticationContext;
  private PermissionService permissionService;
  private SecurityUtils securityUtils;

  @BeforeEach
  void setUp() {
    authenticationContext = mock(AuthenticationContext.class);
    permissionService = mock(PermissionService.class);
    securityUtils = new SecurityUtils(authenticationContext, permissionService);
  }

  @Test
  void testHasRole() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.hasRole(RoleName.ADMIN)).isTrue();
    assertThat(securityUtils.hasRole(RoleName.PLAYER)).isFalse();
  }

  @Test
  void testIsAdmin() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

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
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.getCurrentUsername()).isEqualTo("testuser");

    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.empty());
    assertThat(securityUtils.getCurrentUsername()).isEqualTo("anonymous");
  }

  @Test
  void testGetCurrentAccountId() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getId()).thenReturn(123L);
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.getCurrentAccountId()).contains(123L);

    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.empty());
    assertThat(securityUtils.getCurrentAccountId()).isEmpty();
  }

  @Test
  void testCanCreate() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.canCreate()).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER")));
    assertThat(securityUtils.canCreate()).isFalse();
  }

  @Test
  void testCanEdit() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.canEdit()).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    assertThat(securityUtils.canEdit()).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER")));
    assertThat(securityUtils.canEdit()).isFalse();
  }

  @Test
  void testCanDelete() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_BOOKER")));
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    assertThat(securityUtils.canDelete()).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
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
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    Object target = new Object();
    assertThat(securityUtils.canEdit(target)).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
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
    when(authenticationContext.getAuthenticatedUser(CustomUserDetails.class))
        .thenReturn(Optional.of(user));

    Object target = new Object();
    assertThat(securityUtils.canDelete(target)).isTrue();

    when(user.getAuthorities())
        .thenAnswer(
            invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_PLAYER")));
    assertThat(securityUtils.canDelete(target)).isFalse();
  }

  @Test
  void testLogout() {
    securityUtils.logout();
    org.mockito.Mockito.verify(authenticationContext).logout();
  }
}
