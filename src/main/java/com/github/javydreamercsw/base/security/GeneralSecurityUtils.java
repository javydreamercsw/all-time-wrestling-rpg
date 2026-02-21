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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

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
   * Runs the given {@link Supplier} with the credentials and roles provided.
   *
   * @param <T> The type of the result.
   * @param supplier The supplier to run.
   * @return The result of the supplier.
   */
  public static <T> T runAs(
      @NonNull Supplier<T> supplier,
      @NonNull String username,
      @NonNull String password,
      @NonNull String role) {
    SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
    SecurityContext originalContext = strategy.getContext();
    try {
      SecurityContext context = strategy.createEmptyContext();

      // Create a mock account and role for the principal
      Account account = new Account(username, password, username + "@example.com");
      // Set ID to 1L to match WithCustomMockUserSecurityContextFactory
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

      // Spring Security 6 works best when authorities are consistent with UserDetails
      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      // Add the raw role as well for compatibility with different annotation styles
      authorities.add(new SimpleGrantedAuthority(role));

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, password, authorities);
      context.setAuthentication(authentication);

      log.debug(
          "Setting SecurityContext for user '{}' with role '{}' in thread '{}'",
          username,
          role,
          Thread.currentThread().getName());
      strategy.setContext(context);
      SecurityContextHolder.setContext(context);

      // Verification log to catch immediate failure
      Authentication currentAuth = strategy.getContext().getAuthentication();
      if (currentAuth == null) {
        log.error(
            "CRITICAL: Failed to set SecurityContext for user '{}' in thread '{}'",
            username,
            Thread.currentThread().getName());
      } else {
        log.debug(
            "SecurityContext successfully set for '{}' with authorities: {}",
            username,
            currentAuth.getAuthorities());
      }

      return supplier.get();
    } finally {
      if (originalContext != null && originalContext.getAuthentication() != null) {
        log.trace(
            "Restoring original SecurityContext to thread '{}'", Thread.currentThread().getName());
        strategy.setContext(originalContext);
        SecurityContextHolder.setContext(originalContext);
      } else {
        log.trace("Clearing SecurityContext for thread '{}'", Thread.currentThread().getName());
        strategy.clearContext();
        SecurityContextHolder.clearContext();
      }
    }
  }
}
