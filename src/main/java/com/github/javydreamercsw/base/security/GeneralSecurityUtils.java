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
import java.util.Collections;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** A utility class for general security-related operations. */
@Slf4j
public final class GeneralSecurityUtils {

  private GeneralSecurityUtils() {
    // private constructor to prevent instantiation
  }

  /**
   * Runs the given {@link Supplier} with the admin role.
   *
   * @param <T> The type of the result.
   * @param supplier The supplier to run.
   * @return The result of the supplier.
   */
  public static <T> T runAsAdmin(@NonNull Supplier<T> supplier) {
    return runAs(supplier, "admin", "password", "ADMIN");
  }

  public static void runAsAdmin(@NonNull Runnable runnable) {
    runAsAdmin(
        (Supplier<Object>)
            () -> {
              runnable.run();
              return null;
            });
  }

  /**
   * Runs the given {@link Supplier} with the provided {@link SecurityContext}.
   *
   * @param <T> The type of the result.
   * @param context The security context to use.
   * @param supplier The supplier to run.
   * @return The result of the supplier.
   */
  public static <T> T runWithContext(
      @NonNull SecurityContext context, @NonNull Supplier<T> supplier) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      log.debug(
          "Setting provided SecurityContext in thread '{}'", Thread.currentThread().getName());
      SecurityContextHolder.setContext(context);
      return supplier.get();
    } finally {
      log.debug(
          "Restoring original SecurityContext to thread '{}'", Thread.currentThread().getName());
      SecurityContextHolder.setContext(originalContext);
    }
  }

  /**
   * Runs the given {@link Supplier} with the credentials and roles provided.
   *
   * @param <T> The type of the result.
   * @param supplier The supplier to run.
   * @param username The username to use.
   * @param password The password to use.
   * @param role The role to use.
   * @return The result of the supplier.
   */
  public static <T> T runAs(
      @NonNull Supplier<T> supplier,
      @NonNull String username,
      @NonNull String password,
      @NonNull String role) {
    SecurityContext originalContext = SecurityContextHolder.getContext();

    try {
      SecurityContext context = SecurityContextHolder.createEmptyContext();

      // Create a mock account and role for the principal
      Account account = new Account(username, password, username + "@example.com");
      account.setId(1L);
      try {
        RoleName roleName = RoleName.valueOf(role);
        Role r = new Role(roleName, roleName.name() + " role");
        r.setId((long) roleName.ordinal() + 100);
        account.setRoles(Collections.singleton(r));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid role provided: {}", role);
      }

      CustomUserDetails userDetails = new CustomUserDetails(account, null);
      java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
          authorities = userDetails.getAuthorities();

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, password, authorities);
      context.setAuthentication(authentication);

      log.debug(
          "Setting temporary SecurityContext for user '{}' with authorities: {}",
          username,
          authorities);
      SecurityContextHolder.setContext(context);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Also update TestSecurityContextHolder via reflection if it exists
      try {
        Class<?> testHolderClass =
            Class.forName("org.springframework.security.test.context.TestSecurityContextHolder");
        java.lang.reflect.Method setContextMethod =
            testHolderClass.getMethod("setContext", SecurityContext.class);
        setContextMethod.invoke(null, context);
      } catch (Exception e) {
        // Ignore
      }

      return supplier.get();
    } finally {
      log.debug(
          "Restoring original SecurityContext to thread '{}'", Thread.currentThread().getName());
      SecurityContextHolder.setContext(originalContext);

      // Also restore TestSecurityContextHolder
      try {
        Class<?> testHolderClass =
            Class.forName("org.springframework.security.test.context.TestSecurityContextHolder");
        java.lang.reflect.Method setContextMethod =
            testHolderClass.getMethod("setContext", SecurityContext.class);
        setContextMethod.invoke(null, originalContext);
      } catch (Exception e) {
        // Ignore
      }
    }
  }
}
