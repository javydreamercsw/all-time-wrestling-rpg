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
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  private PermissionService permissionService;
  private SecurityUtils securityUtils;
  private SecurityContext originalContext;

  @BeforeEach
  void setUp() {
    originalContext = SecurityContextHolder.getContext();
    SecurityContextHolder.clearContext();
    permissionService = mock(PermissionService.class);
    securityUtils = new SecurityUtils(permissionService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.setContext(originalContext);
  }

  private void setMockUser(String username, String role) {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getUsername()).thenReturn(username);
    when(user.getAuthorities())
        .thenAnswer(invocation -> Collections.singletonList(new SimpleGrantedAuthority(role)));

    Authentication auth =
        new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
  }

  @Test
  void testHasRole() {
    setMockUser("testuser", "ROLE_ADMIN");
    assertThat(securityUtils.hasRole(RoleName.ADMIN)).isTrue();
    assertThat(securityUtils.hasRole(RoleName.PLAYER)).isFalse();
  }

  @Test
  void testIsAdmin() {
    setMockUser("testuser", "ROLE_ADMIN");
    assertThat(securityUtils.isAdmin()).isTrue();
    assertThat(securityUtils.isBooker()).isFalse();
  }

  @Test
  void testIsAuthenticated() {
    setMockUser("testuser", "ROLE_PLAYER");
    assertThat(securityUtils.isAuthenticated()).isTrue();

    SecurityContextHolder.clearContext();
    assertThat(securityUtils.isAuthenticated()).isFalse();
  }

  @Test
  void testGetCurrentUsername() {
    setMockUser("testuser", "ROLE_PLAYER");
    assertThat(securityUtils.getCurrentUsername()).isEqualTo("testuser");

    SecurityContextHolder.clearContext();
    assertThat(securityUtils.getCurrentUsername()).isEqualTo("anonymous");
  }

  @Test
  void testGetCurrentAccountId() {
    CustomUserDetails user = mock(CustomUserDetails.class);
    when(user.getId()).thenReturn(123L);

    Authentication auth =
        new UsernamePasswordAuthenticationToken(user, "password", Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    assertThat(securityUtils.getCurrentAccountId()).contains(123L);

    SecurityContextHolder.clearContext();
    assertThat(securityUtils.getCurrentAccountId()).isEmpty();
  }

  @Test
  void testCanCreate() {
    setMockUser("testuser", "ROLE_ADMIN");
    assertThat(securityUtils.canCreate()).isTrue();

    setMockUser("testuser", "ROLE_VIEWER");
    assertThat(securityUtils.canCreate()).isFalse();
  }

  @Test
  void testCanEdit() {
    setMockUser("testuser", "ROLE_ADMIN");
    assertThat(securityUtils.canEdit()).isTrue();

    setMockUser("testuser", "ROLE_PLAYER");
    assertThat(securityUtils.canEdit()).isTrue();

    setMockUser("testuser", "ROLE_VIEWER");
    assertThat(securityUtils.canEdit()).isFalse();
  }

  @Test
  void testCanDelete() {
    setMockUser("testuser", "ROLE_BOOKER");
    assertThat(securityUtils.canDelete()).isTrue();

    setMockUser("testuser", "ROLE_PLAYER");
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
    setMockUser("testuser", "ROLE_ADMIN");
    Object target = new Object();
    assertThat(securityUtils.canEdit(target)).isTrue();

    setMockUser("testuser", "ROLE_PLAYER");
    when(permissionService.isOwner(target)).thenReturn(true);
    assertThat(securityUtils.canEdit(target)).isTrue();

    when(permissionService.isOwner(target)).thenReturn(false);
    assertThat(securityUtils.canEdit(target)).isFalse();
  }

  @Test
  void testCanDeleteTarget() {
    setMockUser("testuser", "ROLE_BOOKER");
    Object target = new Object();
    assertThat(securityUtils.canDelete(target)).isTrue();

    setMockUser("testuser", "ROLE_PLAYER");
    assertThat(securityUtils.canDelete(target)).isFalse();
  }

  @Test
  void testLogout() {
    setMockUser("testuser", "ROLE_PLAYER");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

    securityUtils.logout();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
