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
package com.github.javydreamercsw.management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AccountServiceTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testCreateAndReadAccount() {
    Account account =
        accountService.createAccount("test_user", "Password123!", "test@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    Account foundAccount = accountService.get(account.getId()).orElse(null);
    assertNotNull(foundAccount);
    assertEquals("test_user", foundAccount.getUsername());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testUpdateAccount() {
    Account account =
        accountService.createAccount(
            "update_user", "Password123!", "update@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    account.setEmail("new_email@user.com");
    Account updatedAccount = accountService.update(account);
    assertEquals("new_email@user.com", updatedAccount.getEmail());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testDeleteAccount() {
    Account account =
        accountService.createAccount(
            "delete_user", "Password123!", "delete@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    accountService.delete(account.getId());
    assertFalse(accountService.get(account.getId()).isPresent());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testFindByUsername() {
    accountService.createAccount("find_user", "Password123!", "find@user.com", RoleName.PLAYER);
    assertTrue(accountService.findByUsername("find_user").isPresent());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testCreateAccountWithInvalidPassword_tooShort() {
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                accountService.createAccount(
                    "invalid_pass_short", "short", "a@b.com", RoleName.PLAYER));
    assertEquals("Password is not valid.", exception.getMessage());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testUpdateAccountWithInvalidPassword_noUppercase() {
    Account account =
        accountService.createAccount(
            "test_user_valid", "Password123!", "valid@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    account.setPassword("password123!");
    IllegalArgumentException exception =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> accountService.update(account));
    assertEquals("Password is not valid.", exception.getMessage());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  public void testUpdateAccountWithValidPassword() {
    Account account =
        accountService.createAccount(
            "test_user_update", "Password123!", "update@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());
    String oldEncodedPassword = account.getPassword();

    account.setPassword("NewPass456$");
    Account updatedAccount = accountService.update(account);
    assertNotEquals(oldEncodedPassword, updatedAccount.getPassword());
    assertTrue(passwordEncoder.matches("NewPass456$", updatedAccount.getPassword()));
  }
}
