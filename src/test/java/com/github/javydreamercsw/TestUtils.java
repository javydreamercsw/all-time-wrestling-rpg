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
package com.github.javydreamercsw;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.Collections;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class TestUtils {
  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name, long fans) {
    Wrestler wrestler = createWrestler(name);
    wrestler.setFans(fans);
    return wrestler;
  }

  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName(name);
    wrestler.setExternalId(java.util.UUID.randomUUID().toString());
    wrestler.setDescription("Test Wrestler");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setLowHealth(4);
    wrestler.setLowStamina(2);
    wrestler.setTier(WrestlerTier.ROOKIE);
    wrestler.setCreationDate(Instant.now());
    wrestler.setFans(10_00L); // Default fan count
    return wrestler;
  }

  public static void runAsAdmin(Runnable runnable) {
    // Save the current security context
    SecurityContext originalContext = SecurityContextHolder.getContext();

    try {
      // Create a mock admin user
      Account account = new Account("admin", "admin", "admin@localhost.com");
      Role adminRole = new Role(RoleName.ADMIN, "Admin role");
      account.setRoles(Collections.singleton(adminRole));
      CustomUserDetails userDetails = new CustomUserDetails(account, null);

      // Create an authentication token
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      // Set the authentication in the security context
      SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
      securityContext.setAuthentication(authentication);
      SecurityContextHolder.setContext(securityContext);

      // Run the code as the admin user
      runnable.run();
    } finally {
      // Restore the original security context
      SecurityContextHolder.setContext(originalContext);
    }
  }

  public static <T> T runAsAdmin(java.util.concurrent.Callable<T> callable) {
    SecurityContext originalContext = SecurityContextHolder.getContext();
    try {
      Account account = new Account("admin", "admin", "admin@localhost.com");
      Role adminRole = new Role(RoleName.ADMIN, "Admin role");
      account.setRoles(Collections.singleton(adminRole));
      CustomUserDetails userDetails = new CustomUserDetails(account, null);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
      securityContext.setAuthentication(authentication);
      SecurityContextHolder.setContext(securityContext);
      return callable.call();
    } catch (Exception e) {
      throw new RuntimeException("Error running callable as admin", e);
    } finally {
      SecurityContextHolder.setContext(originalContext);
    }
  }
}
