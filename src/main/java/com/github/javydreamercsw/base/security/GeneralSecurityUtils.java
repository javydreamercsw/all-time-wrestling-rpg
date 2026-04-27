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
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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
    Authentication currentAuth = originalContext.getAuthentication();

    // If already authenticated as the requested user, just run the supplier
    if (currentAuth != null && currentAuth.getName().equals(username)) {
      log.trace("Already authenticated as '{}', skipping context switch", username);
      return supplier.get();
    }

    try {
      SecurityContext context = SecurityContextHolder.createEmptyContext();

      // Create a mock account and role for the principal
      Account account = new Account(username, password, username + "@example.com");
      RoleName rn = RoleName.valueOf(role.replace("ROLE_", ""));
      Role r = new Role(rn, rn.name());
      account.setRoles(Collections.singleton(r));

      // Try to reload real account if possible to have a valid ID and more data
      try {
        ApplicationContext appCtx =
            com.github.javydreamercsw.base.config.ApplicationContextProvider
                .getApplicationContext();
        if (appCtx != null) {
          AccountRepository repo = appCtx.getBean(AccountRepository.class);
          account = repo.findByUsername(username).orElse(account);
        }
      } catch (Exception e) {
        // Fallback to mock
      }

      CustomUserDetails principal = new CustomUserDetails(account, null);

      Set<SimpleGrantedAuthority> authorities = new HashSet<>();
      authorities.add(new SimpleGrantedAuthority(role));
      if (!role.startsWith("ROLE_")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(principal, password, authorities);
      context.setAuthentication(authentication);

      log.debug(
          "Setting temporary SecurityContext for user '{}' with authorities: {}",
          username,
          authorities);
      SecurityContextHolder.setContext(context);

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
