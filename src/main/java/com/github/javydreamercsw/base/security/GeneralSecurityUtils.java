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
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** A utility class for general security-related operations. */
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
  public static <T> T runAsAdmin(Supplier<T> supplier) {
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

  public static <T> T runAsAdmin(@NonNull Callable<T> callable) {
    return runAsAdmin((Callable<T>) () -> callable.call());
  }

  /**
   * Runs the given {@link Supplier} with the credentials and roles provided.
   *
   * @param <T> The type of the result.
   * @param supplier The supplier to run.
   * @return The result of the supplier.
   */
  public static <T> T runAs(
      Supplier<T> supplier,
      @NonNull String username,
      @NonNull String password,
      @NonNull String role) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      SecurityContext context = SecurityContextHolder.createEmptyContext();

      // Create a mock account and role for the principal
      Account account = new Account(username, password, username + "@example.com");
      try {
        RoleName roleName = RoleName.valueOf(role);
        Role r = new Role(roleName, roleName.name() + " role");
        account.setRoles(Collections.singleton(r));
      } catch (IllegalArgumentException e) {
        // Fallback for custom roles not in enum if needed, or just skip
      }

      CustomUserDetails userDetails = new CustomUserDetails(account, null);

      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      authorities.add(new SimpleGrantedAuthority(role));

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, password, authorities);
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      return supplier.get();
    } finally {
      SecurityContextHolder.setContext(originalContext);
    }
  }
}
