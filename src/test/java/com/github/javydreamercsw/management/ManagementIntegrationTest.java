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
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;

/**
 * Enhanced integration test base class for management-related tests. Provides automatic database
 * reset and base data initialization before each test.
 */
@Slf4j
public abstract class ManagementIntegrationTest extends AbstractMockUserIntegrationTest {

  @Autowired protected CacheManager cacheManager;
  @Autowired protected WrestlerRepository wrestlerRepository;

  @BeforeEach
  public void prepareTestEnvironment() {
    org.mockito.MockitoAnnotations.openMocks(this);
    log.info("Preparing test environment for: {}", this.getClass().getSimpleName());

    // If we have an authentication, try to refresh it
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      refreshSecurityContext();
    }

    // If STILL no authentication was established (e.g. not using @WithMockUser),
    // log in as the default admin for convenience in normal tests.
    // BUT only if we didn't just clear it due to a refresh failure.
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      log.info("No security context found, logging in as default admin...");
      accountRepository.findByUsername("admin").ifPresent(this::login);
    }
  }

  /** Refreshes the current security context by re-loading the authenticated user from the DB. */
  protected void refreshSecurityContext() {
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth == null) {
      return;
    }

    final String username;
    if (currentAuth.getPrincipal() instanceof Account accountPrincipal) {
      username = accountPrincipal.getUsername();
    } else if (currentAuth.getPrincipal() instanceof CustomUserDetails userDetails) {
      username = userDetails.getUsername();
    } else if (currentAuth.getPrincipal()
        instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
      username = userDetails.getUsername();
    } else {
      username = null;
    }

    if (username != null) {
      accountRepository
          .findByUsername(username)
          .ifPresentOrElse(
              refreshedAccount -> {
                log.info(
                    "Refreshing security context for user: {}", refreshedAccount.getUsername());

                // Force reload of wrestler if available
                Wrestler wrestler = null;
                if (refreshedAccount.getActiveWrestlerId() != null) {
                  wrestler =
                      wrestlerRepository
                          .findById(refreshedAccount.getActiveWrestlerId())
                          .orElse(null);
                }

                // Re-login to refresh the principal and authorities with persistent entities
                login(refreshedAccount);
              },
              () -> {
                log.warn("Account not found during refresh: {}, clearing context", username);
                clearSecurityContext();
              });
    }
  }

  protected void login(@NonNull final Account account) {
    log.info("Logging in as user: {}", account.getUsername());
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    Wrestler wrestler = wrestlers.isEmpty() ? null : wrestlers.getFirst();

    var principal = new CustomUserDetails(account, wrestler);
    Set<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
        new java.util.HashSet<>();
    for (com.github.javydreamercsw.base.domain.account.Role role : account.getRoles()) {
      authorities.add(
          new org.springframework.security.core.authority.SimpleGrantedAuthority(
              role.getName().name()));
      authorities.add(
          new org.springframework.security.core.authority.SimpleGrantedAuthority(
              "ROLE_" + role.getName().name()));
    }

    var authentication =
        new UsernamePasswordAuthenticationToken(principal, "password", authorities);

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

  @org.junit.jupiter.api.AfterEach
  public void tearDown() {
    clearSecurityContext();
    clearCache();
  }
}
