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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;

/**
 * Enhanced integration test base class for management-related tests. Provides automatic database
 * reset and base data initialization before each test.
 */
@Slf4j
public abstract class ManagementIntegrationTest extends AbstractMockUserIntegrationTest {
  @Autowired protected CacheManager cacheManager;

  @BeforeEach
  public void prepareTestEnvironment() {
    log.info("Resetting database for test: {}", this.getClass().getSimpleName());

    // Re-initialize base data (universe, segment types, etc.) so IDs are always valid
    // Run as admin to avoid AccessDeniedException in security tests
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
    try {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "system",
                  "system",
                  java.util.List.of(
                      new SimpleGrantedAuthority("ROLE_ADMIN"),
                      new SimpleGrantedAuthority("ADMIN"),
                      new SimpleGrantedAuthority("BOOKER"))));
      dataInitializer.init();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(originalAuth);
    }

    // Refresh security context to ensure the principal has persistent entities
    refreshSecurityContext();

    // If no authentication was established (e.g. first run with empty DB or not using
    // @WithMockUser), log in as the default admin
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      log.info("No security context found, logging in as default admin...");
      accountRepository.findByUsername("admin").ifPresent(this::login);
    }
  }

  /** Refreshes the current security context by re-loading the authenticated user from the DB. */
  protected void refreshSecurityContext() {
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth != null && currentAuth.getPrincipal() instanceof Account accountPrincipal) {
      accountRepository
          .findByUsername(accountPrincipal.getUsername())
          .ifPresent(
              refreshedAccount -> {
                log.debug(
                    "Refreshing security context for user: {}", refreshedAccount.getUsername());
                login(refreshedAccount);
              });
    }
  }

  protected void login(Account account) {
    var principal = account;
    var authorities =
        account.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
            .toList();

    var authentication =
        new UsernamePasswordAuthenticationToken(principal, account.getPassword(), authorities);

    SecurityContextHolder.getContext().setAuthentication(authentication);
    TestSecurityContextHolder.setAuthentication(authentication);
  }

  protected void clearCache() {
    cacheManager
        .getCacheNames()
        .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
  }

  protected void loginAs(String username) {
    var accountOpt = accountRepository.findByUsername(username);
    if (accountOpt.isPresent()) {
      login(accountOpt.get());
    } else {
      throw new RuntimeException("loginAs: Account not found: " + username);
    }
  }

  protected void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }

  @org.junit.jupiter.api.AfterEach
  public void tearDown() throws Exception {
    clearSecurityContext();
    clearCache();
  }
}
