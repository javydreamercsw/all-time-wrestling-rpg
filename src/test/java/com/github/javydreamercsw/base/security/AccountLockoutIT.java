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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.AccountService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class AccountLockoutIT {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired private CustomUserDetailsService userDetailsService;
  @Autowired private AccountService accountService;
  @Autowired private AccountRepository accountRepository;

  private Account testAccount;

  @BeforeEach
  void setUp() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          testAccount =
              accountService.createAccount(
                  "locktest-" + UUID.randomUUID(),
                  "LockPass123!",
                  "lock-" + UUID.randomUUID() + "@test.com",
                  RoleName.PLAYER);
        });
    // Reload to ensure testAccount is a managed entity for the test method's transaction
    testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
  }

  @Test
  void testFailedLoginAttemptsLeadToLockout() {
    for (int i = 0; i < 5; i++) {
      userDetailsService.recordFailedLoginAttempt(testAccount.getUsername());
      testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
    }

    assertFalse(testAccount.isAccountNonLocked());
    assertTrue(testAccount.getFailedLoginAttempts() >= 5);
  }

  @Test
  void testLockedAccountCannotLogin() {
    // Lock the account via failed attempts
    for (int i = 0; i < 5; i++) {
      userDetailsService.recordFailedLoginAttempt(testAccount.getUsername());
      testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
    }

    // Manually set lockedUntil to ensure it's in the future and account remains locked
    // This is done in the test's transaction, so changes are part of it.
    testAccount.setLockedUntil(LocalDateTime.now().plusHours(1));
    accountRepository.save(testAccount);
    testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();

    // Now, loading the user should return a UserDetails with accountNonLocked=false
    UserDetails userDetails = userDetailsService.loadUserByUsername(testAccount.getUsername());
    assertFalse(userDetails.isAccountNonLocked());
  }

  @Test
  void testSuccessfulLoginResetsFailedAttempts() {
    for (int i = 0; i < 3; i++) {
      userDetailsService.recordFailedLoginAttempt(testAccount.getUsername());
      testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
    }

    userDetailsService.recordSuccessfulLogin(testAccount.getUsername());
    testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();

    assertTrue(testAccount.isAccountNonLocked());
    assertEquals(0, testAccount.getFailedLoginAttempts());
  }

  @Test
  void testLockExpiresAndAccountBecomesUnlocked() {
    for (int i = 0; i < 5; i++) {
      userDetailsService.recordFailedLoginAttempt(testAccount.getUsername());
      testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
    }

    // Manually set lock to past and save
    testAccount = accountService.setLockExpiredAndSave(testAccount);
    testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();

    // Reload user details, which should trigger unlock logic in CustomUserDetailsService
    UserDetails userDetails = userDetailsService.loadUserByUsername(testAccount.getUsername());
    assertTrue(userDetails.isAccountNonLocked());

    // Verify account state after unlock
    testAccount = accountRepository.findByUsername(testAccount.getUsername()).orElseThrow();
    assertTrue(testAccount.isAccountNonLocked());
    assertEquals(0, testAccount.getFailedLoginAttempts());
  }
}
