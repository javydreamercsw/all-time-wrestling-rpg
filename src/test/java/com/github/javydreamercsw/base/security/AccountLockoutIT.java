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

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
public class AccountLockoutIT extends AbstractIntegrationTest {

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService
        .findByUsername("viewer")
        .ifPresent(
            account -> {
              account.resetFailedAttempts();
              accountService.update(account);
            });
  }

  @Test
  public void testAccountLockout() {
    String username = "viewer";
    String correctPassword = "viewer123";
    String wrongPassword = "wrongpassword";

    // 4 failed attempts, should not lock the account
    for (int i = 0; i < 4; i++) {
      assertThrows(
          BadCredentialsException.class,
          () ->
              authenticationManager.authenticate(
                  new UsernamePasswordAuthenticationToken(username, wrongPassword)));
    }

    // 5th failed attempt should lock the account, but still throw BadCredentialsException
    assertThrows(
        BadCredentialsException.class,
        () ->
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, wrongPassword)));

    // Account should be locked, so it will throw LockedException
    assertThrows(
        LockedException.class,
        () ->
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, correctPassword)));

    Account lockedAccount = accountService.findByUsername(username).get();
    assertFalse(lockedAccount.isAccountNonLocked());
    assertNotNull(lockedAccount.getLockedUntil());
  }
}
