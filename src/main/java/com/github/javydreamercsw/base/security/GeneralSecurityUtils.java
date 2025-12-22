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

import java.util.Collections;
import java.util.function.Supplier;
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
   * Runs the given {@link Runnable} with the admin role.
   *
   * @param runnable The runnable to run.
   */
  public static void runAsAdmin(Runnable runnable) {
    runAsAdmin(
        () -> {
          runnable.run();
          return null;
        });
  }

  /**
   * Runs the given {@link Supplier} with the admin role.
   *
   * @param <T> The type of the result.
   * @param supplier The supplier to run.
   * @return The result of the supplier.
   */
  public static <T> T runAsAdmin(Supplier<T> supplier) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(
              "admin",
              "password",
              Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      return supplier.get();
    } finally {
      SecurityContextHolder.setContext(originalContext);
    }
  }
}
