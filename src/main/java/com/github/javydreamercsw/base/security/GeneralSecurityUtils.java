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
import java.util.HashSet;
import java.util.Set;
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

/** A utility class for general security-related operations. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class GeneralSecurityUtils {

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

        authorities.add(new SimpleGrantedAuthority(cleanRole));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
      }
      account.setRoles(accountRoles);

      CustomUserDetails principal = new CustomUserDetails(account, null);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(principal, password, authorities);

      newContext.setAuthentication(authentication);

      // Establish the new context globally/thread-locally
      strategy.setContext(newContext);
      setTestSecurityContext(newContext);

      log.debug(
          "Establishing system authority for user '{}' with authorities: {}",
          username,
          authorities);

      return supplier.get();
    } finally {
      // Restore the original context
      strategy.setContext(originalContext);
      setTestSecurityContext(originalContext);

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
   * Run a task within a specific security context.
   *
   * @param <T> The return type.
   * @param context The security context.
   * @param supplier The task.
   * @return The result.
   */
  public static <T> T runWithContext(final SecurityContext context, final Supplier<T> supplier) {
    SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
    SecurityContext originalContext = strategy.getContext();
    try {
      strategy.setContext(context);
      setTestSecurityContext(context);
      return supplier.get();
    } finally {
      strategy.setContext(originalContext);
      setTestSecurityContext(originalContext);
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
