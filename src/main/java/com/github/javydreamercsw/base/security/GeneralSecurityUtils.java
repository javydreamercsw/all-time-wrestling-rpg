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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** A utility class for general security-related operations. */
@Slf4j
public final class GeneralSecurityUtils {

  private GeneralSecurityUtils() {
    // private constructor to prevent instantiation
  }

  /**
   * Run a task as an admin user.
   *
   * @param task The task to run.
   */
  public static void runAsAdmin(@NonNull Runnable task) {
    runAs(task, "system", "password", "ROLE_ADMIN", "ROLE_SYSTEM");
  }

  /**
   * Run a task as an admin user.
   *
   * @param <T> The return type of the task.
   * @param supplier The task to run.
   * @return The result of the task.
   */
  public static <T> T runAsAdmin(@NonNull Supplier<T> supplier) {
    return runAs(supplier, "system", "password", "ROLE_ADMIN", "ROLE_SYSTEM");
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
      @NonNull Supplier<T> supplier,
      @NonNull String username,
      @NonNull String password,
      @NonNull String... roles) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    Authentication currentAuth = originalContext.getAuthentication();

    // If already authenticated as the requested user, just run the supplier
    if (currentAuth != null && currentAuth.isAuthenticated()) {
      String currentUsername = null;
      Object principal = currentAuth.getPrincipal();
      if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
        currentUsername = ud.getUsername();
      } else if (principal instanceof com.github.javydreamercsw.base.domain.account.Account a) {
        currentUsername = a.getUsername();
      } else if (principal instanceof String s) {
        currentUsername = s;
      }

      if (username.equals(currentUsername)) {
        log.trace("Already authenticated as '{}', skipping context switch", username);
        return supplier.get();
      }
    }

    try {
      SecurityContext context = SecurityContextHolder.createEmptyContext();

      // Create a mock account and roles for the principal
      Account account = new Account(username, password, username + "@example.com");
      account.setId(-1L); // Use a non-null ID for mock accounts

      Set<Role> accountRoles = new HashSet<>();
      Set<SimpleGrantedAuthority> authorities = new HashSet<>();

      for (String role : roles) {
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        try {
          RoleName roleName = RoleName.valueOf(cleanRole);
          accountRoles.add(new Role(roleName, roleName.name()));
        } catch (IllegalArgumentException e) {
          log.warn("Invalid role provided to runAs: {}", role);
        }

        authorities.add(new SimpleGrantedAuthority(role));
        if (!role.startsWith("ROLE_")) {
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
      }
      account.setRoles(accountRoles);

      CustomUserDetails principal = new CustomUserDetails(account, null);

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(principal, password, authorities);
      context.setAuthentication(authentication);

      log.debug(
          "Setting SecurityContext for user '{}' with authorities '{}' in thread '{}'",
          username,
          authorities,
          Thread.currentThread().getName());
      SecurityContextHolder.setContext(context);
      setTestSecurityContext(context);

      return supplier.get();
    } finally {
      if (originalContext != null && originalContext.getAuthentication() != null) {
        log.debug(
            "Restoring original SecurityContext to thread '{}'", Thread.currentThread().getName());
        SecurityContextHolder.setContext(originalContext);
        setTestSecurityContext(originalContext);
      } else {
        log.debug("Clearing SecurityContext for thread '{}'", Thread.currentThread().getName());
        SecurityContextHolder.clearContext();
        clearTestSecurityContext();
      }
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
      @NonNull Runnable task,
      @NonNull String username,
      @NonNull String password,
      @NonNull String... roles) {
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
  public static <T> T runWithContext(SecurityContext context, Supplier<T> supplier) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      SecurityContextHolder.setContext(context);
      setTestSecurityContext(context);
      return supplier.get();
    } finally {
      if (originalContext != null && originalContext.getAuthentication() != null) {
        log.debug(
            "Restoring original SecurityContext to thread '{}'", Thread.currentThread().getName());
        SecurityContextHolder.setContext(originalContext);
        setTestSecurityContext(originalContext);
      } else {
        log.debug("Clearing SecurityContext for thread '{}'", Thread.currentThread().getName());
        SecurityContextHolder.clearContext();
        clearTestSecurityContext();
      }
    }
  }

  /**
   * Use reflection to set TestSecurityContextHolder if it's available on the classpath (i.e.,
   * during tests).
   *
   * @param context The SecurityContext to set.
   */
  private static void setTestSecurityContext(SecurityContext context) {
    try {
      Class<?> testHolderClass =
          Class.forName("org.springframework.security.test.context.TestSecurityContextHolder");
      java.lang.reflect.Method setContextMethod =
          testHolderClass.getMethod("setContext", SecurityContext.class);
      setContextMethod.invoke(null, context);
      log.trace("TestSecurityContextHolder set via reflection");
    } catch (ClassNotFoundException e) {
      // Not on classpath, normal for production
    } catch (Exception e) {
      log.warn("Failed to set TestSecurityContextHolder via reflection", e);
    }
  }

  /**
   * Use reflection to clear TestSecurityContextHolder if it's available on the classpath (i.e.,
   * during tests).
   */
  private static void clearTestSecurityContext() {
    try {
      Class<?> testHolderClass =
          Class.forName("org.springframework.security.test.context.TestSecurityContextHolder");
      java.lang.reflect.Method clearContextMethod = testHolderClass.getMethod("clearContext");
      clearContextMethod.invoke(null);
      log.trace("TestSecurityContextHolder cleared via reflection");
    } catch (ClassNotFoundException e) {
      // Not on classpath, normal for production
    } catch (Exception e) {
      log.warn("Failed to clear TestSecurityContextHolder via reflection", e);
    }
  }
}
