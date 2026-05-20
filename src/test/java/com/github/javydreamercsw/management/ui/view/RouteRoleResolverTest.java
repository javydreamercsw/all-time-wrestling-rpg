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
package com.github.javydreamercsw.management.ui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class RouteRoleResolverTest {

  // ── Test view stubs ────────────────────────────────────────────────────────

  @Route("admin-only")
  @RolesAllowed(RoleName.ADMIN_ROLE)
  static class AdminOnlyView {}

  @Route("admin-or-booker")
  @RolesAllowed({RoleName.ADMIN_ROLE, RoleName.BOOKER_ROLE})
  static class AdminOrBookerView {}

  @Route("permit-all")
  @PermitAll
  static class PermitAllView {}

  @Route("deny-all")
  @DenyAll
  static class DenyAllView {}

  @Route("unannotated")
  static class UnannotatedView {}

  // ── Test setup ─────────────────────────────────────────────────────────────

  @Mock private ApplicationContext applicationContext;

  private RouteRoleResolver resolver;

  @BeforeEach
  void setUp() {
    when(applicationContext.getBeanNamesForAnnotation(Route.class))
        .thenReturn(
            new String[] {
              "adminView", "adminOrBookerView", "permitAllView", "denyAllView", "unannotatedView"
            });
    when(applicationContext.getType("adminView")).thenReturn((Class) AdminOnlyView.class);
    when(applicationContext.getType("adminOrBookerView"))
        .thenReturn((Class) AdminOrBookerView.class);
    when(applicationContext.getType("permitAllView")).thenReturn((Class) PermitAllView.class);
    when(applicationContext.getType("denyAllView")).thenReturn((Class) DenyAllView.class);
    when(applicationContext.getType("unannotatedView")).thenReturn((Class) UnannotatedView.class);

    resolver = new RouteRoleResolver(applicationContext);
    resolver.buildCache();
  }

  // ── resolveRoles ──────────────────────────────────────────────────────────

  @Test
  void resolveRoles_adminOnlyView_returnsAdminRole() {
    Optional<Set<RoleName>> result = resolver.resolveRoles("admin-only");

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly(RoleName.ADMIN);
  }

  @Test
  void resolveRoles_multiRoleView_returnsAllRoles() {
    Optional<Set<RoleName>> result = resolver.resolveRoles("admin-or-booker");

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.BOOKER);
  }

  @Test
  void resolveRoles_permitAllView_returnsEmptySet() {
    Optional<Set<RoleName>> result = resolver.resolveRoles("permit-all");

    assertThat(result).isPresent();
    assertThat(result.get()).isEmpty();
  }

  @Test
  void resolveRoles_unannotatedView_returnsEmpty() {
    // No annotation → resolver has no opinion; MenuService falls back to MenuItem explicit roles
    Optional<Set<RoleName>> result = resolver.resolveRoles("unannotated");

    assertThat(result).isEmpty();
  }

  @Test
  void resolveRoles_unknownPath_returnsEmpty() {
    Optional<Set<RoleName>> result = resolver.resolveRoles("path-that-does-not-exist");

    assertThat(result).isEmpty();
  }

  @Test
  void resolveRoles_nullPath_returnsEmpty() {
    Optional<Set<RoleName>> result = resolver.resolveRoles(null);

    assertThat(result).isEmpty();
  }

  // ── isDeniedAll ───────────────────────────────────────────────────────────

  @Test
  void isDeniedAll_denyAllView_returnsTrue() {
    assertThat(resolver.isDeniedAll("deny-all")).isTrue();
  }

  @Test
  void isDeniedAll_adminOnlyView_returnsFalse() {
    assertThat(resolver.isDeniedAll("admin-only")).isFalse();
  }

  @Test
  void isDeniedAll_unknownPath_returnsFalse() {
    assertThat(resolver.isDeniedAll("nonexistent")).isFalse();
  }

  @Test
  void isDeniedAll_nullPath_returnsFalse() {
    assertThat(resolver.isDeniedAll(null)).isFalse();
  }
}
