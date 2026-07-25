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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;

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
                      .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
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
  void testRunWithContextRestoresContextOnException() {
    SecurityContext before = SecurityContextHolder.getContext();
    SecurityContext context = new SecurityContextImpl();

    Assertions.assertThrows(
        RuntimeException.class,
        () ->
            GeneralSecurityUtils.runWithContext(
                context,
                () -> {
                  throw new RuntimeException("test error");
                }));

    // Original context must be restored even after the supplier threw
    Assertions.assertSame(before, SecurityContextHolder.getContext());
  }

  @Test
  void testCaptureCurrentContext_whenAuthenticated() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          SecurityContext captured = GeneralSecurityUtils.captureCurrentContext();
          Assertions.assertNotNull(captured.getAuthentication());
          return null;
        });
  }

  @Test
  void testCaptureCurrentContext_whenNotAuthenticated_returnsEmptyContext() {
    // No Vaadin context in unit tests → falls through to the log.warn path and returns empty ctx
    SecurityContext captured = GeneralSecurityUtils.captureCurrentContext();
    Assertions.assertNotNull(captured);
    Assertions.assertNull(captured.getAuthentication());
  }

  @Test
  void testRunAsAdminAsync_supplierRunsOnBackgroundThread() throws Exception {
    AtomicReference<String> threadName = new AtomicReference<>();
    String result =
        GeneralSecurityUtils.runAsAdminAsync(
                () -> {
                  threadName.set(Thread.currentThread().getName());
                  return "async-result";
                })
            .get();

    Assertions.assertEquals("async-result", result);
    Assertions.assertNotEquals(Thread.currentThread().getName(), threadName.get());
  }

  @Test
  void testRunAsAdminAsync_runnableCompletesSuccessfully() throws Exception {
    AtomicBoolean ran = new AtomicBoolean(false);
    GeneralSecurityUtils.runAsAdminAsync((Runnable) () -> ran.set(true)).get();
    Assertions.assertTrue(ran.get());
  }

  @Test
  void testSetMethodSecurityStrategy_propagatesToSecondStrategy() {
    SecurityContextHolderStrategy saved = GeneralSecurityUtils.methodSecurityStrategy;
    SecurityContextHolderStrategy secondStrategy = simpleStrategy();
    GeneralSecurityUtils.setMethodSecurityStrategy(secondStrategy);
    try {
      GeneralSecurityUtils.runAsAdmin(
          () -> {
            Authentication auth = secondStrategy.getContext().getAuthentication();
            Assertions.assertNotNull(auth, "methodSecurityStrategy must see admin context");
            Assertions.assertTrue(
                auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
            return null;
          });
      // After runAs the second strategy context is restored to its original (empty) state
      Assertions.assertNull(secondStrategy.getContext().getAuthentication());
    } finally {
      GeneralSecurityUtils.methodSecurityStrategy = saved;
    }
  }

  @Test
  void testRunWithContext_propagatesToMethodSecurityStrategy() {
    SecurityContextHolderStrategy saved = GeneralSecurityUtils.methodSecurityStrategy;
    SecurityContextHolderStrategy secondStrategy = simpleStrategy();
    GeneralSecurityUtils.setMethodSecurityStrategy(secondStrategy);
    try {
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      GeneralSecurityUtils.runWithContext(
          ctx,
          () -> {
            Assertions.assertSame(ctx, secondStrategy.getContext());
            return null;
          });
      // Restored after the call
      Assertions.assertNotSame(ctx, secondStrategy.getContext());
    } finally {
      GeneralSecurityUtils.methodSecurityStrategy = saved;
    }
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

  private static SecurityContextHolderStrategy simpleStrategy() {
    java.util.concurrent.atomic.AtomicReference<SecurityContext> holder =
        new java.util.concurrent.atomic.AtomicReference<>(
            SecurityContextHolder.createEmptyContext());
    return new SecurityContextHolderStrategy() {
      @Override
      public void clearContext() {
        holder.set(SecurityContextHolder.createEmptyContext());
      }

      @Override
      public SecurityContext getContext() {
        return holder.get();
      }

      @Override
      public void setContext(final SecurityContext context) {
        holder.set(context);
      }

      @Override
      public SecurityContext createEmptyContext() {
        return SecurityContextHolder.createEmptyContext();
      }
    };
  }
}
