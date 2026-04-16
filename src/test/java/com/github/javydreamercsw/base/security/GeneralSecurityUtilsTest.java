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

import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class GeneralSecurityUtilsTest {

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testRunAsAdmin() {
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());

    GeneralSecurityUtils.runAsAdmin(
        () -> {
          var auth = SecurityContextHolder.getContext().getAuthentication();
          Assertions.assertNotNull(auth);
          Assertions.assertTrue(
              auth.getAuthorities().stream()
                  .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN")));
          return null;
        });

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void testRunAsAdminRunnable() {
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());

    GeneralSecurityUtils.runAsAdmin(
        (Runnable)
            () -> {
              var auth = SecurityContextHolder.getContext().getAuthentication();
              Assertions.assertNotNull(auth);
              Assertions.assertTrue(
                  auth.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
            });

    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void testRunAs() {
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());

    String result =
        GeneralSecurityUtils.runAs(
            () -> {
              var auth = SecurityContextHolder.getContext().getAuthentication();
              Assertions.assertNotNull(auth);
              Assertions.assertTrue(
                  auth.getAuthorities().stream()
                      .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_PLAYER")));
              return "success";
            },
            "testuser",
            "password",
            "PLAYER");

    Assertions.assertEquals("success", result);
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void testRunAsInvalidRole() {
    GeneralSecurityUtils.runAs(
        () -> {
          var auth = SecurityContextHolder.getContext().getAuthentication();
          Assertions.assertNotNull(auth);
          // Should still set context but log warning (internal check)
          return null;
        },
        "testuser",
        "password",
        "INVALID_ROLE");
  }

  @Test
  void testRunWithContext() {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    String result =
        GeneralSecurityUtils.runWithContext(
            context,
            () -> {
              Assertions.assertSame(context, SecurityContextHolder.getContext());
              return "done";
            });

    Assertions.assertEquals("done", result);
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void testContextRestoration() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          var initialAuth = SecurityContextHolder.getContext().getAuthentication();
          Assertions.assertNotNull(initialAuth);

          GeneralSecurityUtils.runAs(
              () -> {
                var innerAuth = SecurityContextHolder.getContext().getAuthentication();
                Assertions.assertNotNull(innerAuth);
                Assertions.assertTrue(
                    innerAuth.getAuthorities().stream()
                        .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_PLAYER")));
                return null;
              },
              "player",
              "pass",
              "PLAYER");

          // Should be back to admin
          var restoredAuth = SecurityContextHolder.getContext().getAuthentication();
          Assertions.assertSame(initialAuth, restoredAuth);
          return null;
        });
  }
}
