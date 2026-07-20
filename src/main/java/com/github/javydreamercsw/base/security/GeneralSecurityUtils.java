/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.server.VaadinSession;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** A utility class for general security-related operations. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class GeneralSecurityUtils {

  /**
   * The strategy instance captured before Vaadin's SpringSecurityAutoConfiguration replaces the
   * global with VaadinAwareSecurityContextHolderStrategy. Spring Security's
   * AuthorizationManagerBeforeMethodInterceptor captures the global strategy at bean-creation time
   * and holds that reference forever. After Vaadin replaces the global, runAsAdmin() sets context
   * on the VaadinAware strategy but @PreAuthorize reads from this earlier instance — causing
   * AuthenticationCredentialsNotFoundException on ForkJoinPool threads. We set context on BOTH
   * strategies to close the gap.
   *
   * <p>Set by Application's static initializer immediately after setStrategyName().
   */
  static volatile SecurityContextHolderStrategy methodSecurityStrategy;

  /**
   * Called by Application's static initializer to record the strategy instance that Spring
   * Security's method-security interceptors will capture at bean-creation time.
   *
   * @param strategy the active strategy immediately after setStrategyName(), before Vaadin replaces
   *     it
   */
  public static void setMethodSecurityStrategy(final SecurityContextHolderStrategy strategy) {
    methodSecurityStrategy = strategy;
  }

  /**
   * Run a task as an admin user.
   *
   * @param task The task to run.
   */
  public static void runAsAdmin(@NonNull final Runnable task) {
    runAs(task, "system", "password", "ROLE_ADMIN", "ROLE_SYSTEM", "ROLE_BOOKER");
  }

  /**
   * Run a task as an admin user.
   *
   * @param <T> The return type of the task.
   * @param supplier The task to run.
   * @return The result of the task.
   */
  public static <T> T runAsAdmin(@NonNull final Supplier<T> supplier) {
    return runAs(supplier, "system", "password", "ROLE_ADMIN", "ROLE_SYSTEM", "ROLE_BOOKER");
  }

  /**
   * Runs a task as an admin user on a background thread (via {@code ForkJoinPool.commonPool()}),
   * safely for {@code @PreAuthorize}-guarded calls.
   *
   * <p>{@code CompletableFuture.supplyAsync(() -> runAsAdmin(...))} is unreliable because {@code
   * VaadinAwareSecurityContextHolderStrategy.getContext()} prefers the VaadinSession's HTTP session
   * attribute over the ThreadLocal. A ForkJoinPool worker can accumulate a stale VaadinSession from
   * prior Vaadin request-handling work on the same thread. When that happens, {@code setContext()}
   * silently writes to the ThreadLocal, but {@code getContext()} reads the HTTP session and finds
   * the wrong (or absent) authentication — causing {@code
   * AuthenticationCredentialsNotFoundException} even though the code passes locally.
   *
   * <p>This method clears any stale VaadinSession from the worker thread before delegating to
   * {@link #runAsAdmin(Supplier)}, forcing {@code VaadinAwareSecurityContextHolderStrategy} into
   * ThreadLocal mode for the duration of the call.
   *
   * @param <T> The return type of the task.
   * @param supplier The task to run.
   * @return A {@link CompletableFuture} completing with the task's result.
   */
  public static <T> CompletableFuture<T> runAsAdminAsync(@NonNull final Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(
        () -> {
          // Clear any stale VaadinSession this ForkJoinPool worker may have accumulated.
          // VaadinAwareSecurityContextHolderStrategy.getContext() prefers the HTTP session
          // attribute over the ThreadLocal, so a stale session would shadow the admin context
          // we're about to set. With no VaadinSession, the strategy falls back to ThreadLocal.
          VaadinSession.setCurrent(null);
          return runAsAdmin(supplier);
        });
  }

  /**
   * Runs a task as an admin user on a background thread. See {@link #runAsAdminAsync(Supplier)} for
   * why this is necessary instead of {@code CompletableFuture.runAsync(() -> runAsAdmin(...))}.
   *
   * @param task The task to run.
   * @return A {@link CompletableFuture} completing when the task finishes.
   */
  public static CompletableFuture<Void> runAsAdminAsync(@NonNull final Runnable task) {
    return runAsAdminAsync(
        () -> {
          task.run();
          return null;
        });
  }

  /**
   * Run a task with the given user and roles.
   *
   * @param <T> The return type of the supplier.
   * @param supplier The supplier to run.
   * @param username The username to use.
   * @param password The password to use.
   * @param roles The roles to use.
   * @return The result of the supplier.
   */
  public static <T> T runAs(
      @NonNull final Supplier<T> supplier,
      @NonNull final String username,
      @NonNull final String password,
      @NonNull final String... roles) {
    SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
    SecurityContext originalContext = strategy.getContext();
    Authentication originalAuth = originalContext.getAuthentication();

    // Re-entrancy check: if already authenticated as the requested user, just run the supplier.
    if (originalAuth != null
        && originalAuth.isAuthenticated()
        && username.equals(originalAuth.getName())) {
      return supplier.get();
    }

    // VaadinAwareSecurityContextHolderStrategy reads from VaadinSession's HTTP session
    // BEFORE the ThreadLocal, so we must update both to make privilege elevation visible.
    Object originalSessionCtx = null;
    VaadinSession vaadinSession = null;
    try {
      vaadinSession = VaadinSession.getCurrent();
    } catch (Exception ignored) {
      // No Vaadin context available
    }
    if (vaadinSession != null) {
      try {
        originalSessionCtx =
            vaadinSession
                .getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      } catch (IllegalStateException ignored) {
        vaadinSession = null;
      }
    }

    try {
      SecurityContext newContext = strategy.createEmptyContext();

      // Create a mock account and roles for the principal
      Account account = new Account(username, password, username + "@example.com");
      account.setId(-1L);

      Set<Role> accountRoles = new HashSet<>();
      Set<SimpleGrantedAuthority> authorities = new HashSet<>();

      for (String role : roles) {
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        try {
          RoleName roleName = RoleName.valueOf(cleanRole);
          accountRoles.add(new Role(roleName, roleName.name()));
        } catch (IllegalArgumentException e) {
          log.trace("Non-enum role provided: {}", role);
        }

        authorities.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
      }
      account.setRoles(accountRoles);

      CustomUserDetails principal = new CustomUserDetails(account, null);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(principal, password, authorities);

      newContext.setAuthentication(authentication);

      // Establish the new context globally/thread-locally
      strategy.setContext(newContext);
      // Also propagate to the strategy that @PreAuthorize interceptors captured at bean-creation
      // time (before Vaadin replaced the global). Without this, interceptors on ForkJoinPool
      // worker threads see an empty SecurityContext even though VaadinAware has admin context.
      if (methodSecurityStrategy != null && methodSecurityStrategy != strategy) {
        methodSecurityStrategy.setContext(newContext);
      }
      setTestSecurityContext(newContext);
      if (vaadinSession != null) {
        try {
          vaadinSession
              .getSession()
              .setAttribute(
                  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, newContext);
        } catch (IllegalStateException ignored) {
          // Session invalidated between the check and here
        }
      }

      log.debug(
          "Establishing system authority for user '{}' with authorities: {}",
          username,
          authorities);

      return supplier.get();
    } finally {
      // Restore the original context
      strategy.setContext(originalContext);
      if (methodSecurityStrategy != null && methodSecurityStrategy != strategy) {
        methodSecurityStrategy.setContext(originalContext);
      }
      setTestSecurityContext(originalContext);
      if (vaadinSession != null) {
        try {
          vaadinSession
              .getSession()
              .setAttribute(
                  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                  originalSessionCtx);
        } catch (IllegalStateException ignored) {
          // Session invalidated
        }
      }

      log.trace("Restored original SecurityContext");
    }
  }

  /**
   * Run a task with the given user and roles.
   *
   * @param task The task to run.
   * @param username The username to use.
   * @param password The password to use.
   * @param roles The roles to use.
   */
  public static void runAs(
      @NonNull final Runnable task,
      @NonNull final String username,
      @NonNull final String password,
      @NonNull final String... roles) {
    runAs(
        () -> {
          task.run();
          return null;
        },
        username,
        password,
        roles);
  }

  /**
   * Captures the current authenticated {@link SecurityContext} for propagation onto a background
   * thread.
   *
   * <p>When the application uses {@link
   * com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy}, the context may live
   * in the HTTP session rather than the thread-local. If the thread-local context has no
   * authentication (e.g. during a WebSocket push event before the holder strategy is fully
   * synchronised), this method falls back to reading the context directly from the current {@link
   * VaadinSession}'s underlying HTTP session attribute so that callers can still propagate a valid
   * context.
   *
   * @return The best available {@link SecurityContext}, never {@code null}.
   */
  public static SecurityContext captureCurrentContext() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    if (ctx.getAuthentication() == null) {
      VaadinSession vaadinSession = null;
      try {
        vaadinSession = VaadinSession.getCurrent();
      } catch (Exception ignored) {
        // No Vaadin context — fall through
      }
      if (vaadinSession != null) {
        try {
          Object sessionAttr =
              vaadinSession
                  .getSession()
                  .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
          if (sessionAttr instanceof SecurityContext sc && sc.getAuthentication() != null) {
            log.debug(
                "captureCurrentContext: falling back to HTTP session attribute on thread={}",
                Thread.currentThread().getName());
            return sc;
          }
        } catch (IllegalStateException ignored) {
          // Session already invalidated
        }
      }
      log.warn(
          "captureCurrentContext: no authenticated context available — thread={}, strategy={}",
          Thread.currentThread().getName(),
          SecurityContextHolder.getContextHolderStrategy().getClass().getSimpleName());
    }
    return ctx;
  }

  /**
   * Run a task within a specific security context.
   *
   * <p>Intended for propagating an authenticated context captured on a Vaadin request/push thread
   * onto a background thread (e.g. inside {@code CompletableFuture.runAsync}).
   *
   * @param <T> The return type.
   * @param context The security context.
   * @param supplier The task.
   * @return The result.
   */
  public static <T> T runWithContext(final SecurityContext context, final Supplier<T> supplier) {
    SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
    SecurityContext originalContext = strategy.getContext();

    // VaadinAwareSecurityContextHolderStrategy reads from VaadinSession's HTTP session
    // BEFORE the ThreadLocal, so we must update both — otherwise a stale/absent session
    // attribute on this thread (e.g. a reused ForkJoinPool.commonPool worker) shadows the
    // context we're propagating and @PreAuthorize sees no Authentication.
    Object originalSessionCtx = null;
    VaadinSession vaadinSession = null;
    try {
      vaadinSession = VaadinSession.getCurrent();
    } catch (Exception ignored) {
      // No Vaadin context available
    }
    if (vaadinSession != null) {
      try {
        originalSessionCtx =
            vaadinSession
                .getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      } catch (IllegalStateException ignored) {
        vaadinSession = null;
      }
    }

    try {
      strategy.setContext(context);
      if (methodSecurityStrategy != null && methodSecurityStrategy != strategy) {
        methodSecurityStrategy.setContext(context);
      }
      setTestSecurityContext(context);
      if (vaadinSession != null) {
        try {
          vaadinSession
              .getSession()
              .setAttribute(
                  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        } catch (IllegalStateException ignored) {
          // Session invalidated between the check and here
        }
      }
      return supplier.get();
    } finally {
      strategy.setContext(originalContext);
      if (methodSecurityStrategy != null && methodSecurityStrategy != strategy) {
        methodSecurityStrategy.setContext(originalContext);
      }
      setTestSecurityContext(originalContext);
      if (vaadinSession != null) {
        try {
          vaadinSession
              .getSession()
              .setAttribute(
                  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                  originalSessionCtx);
        } catch (IllegalStateException ignored) {
          // Session invalidated
        }
      }
    }
  }

  /**
   * Use reflection to set TestSecurityContextHolder if it's available on the classpath (i.e.,
   * during tests).
   *
   * @param context The SecurityContext to set.
   */
  private static void setTestSecurityContext(final SecurityContext context) {
    try {
      Class<?> testHolderClass =
          Class.forName("org.springframework.security.test.context.TestSecurityContextHolder");
      java.lang.reflect.Method setContextMethod =
          testHolderClass.getMethod("setContext", SecurityContext.class);
      setContextMethod.invoke(null, context);
    } catch (ClassNotFoundException e) {
      // Not on classpath, normal for production
    } catch (Exception e) {
      log.warn("Failed to set TestSecurityContextHolder via reflection", e);
    }
  }
}
