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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired private AccountService accountService;

  @Test
  public void testCreateAndReadAccount() {
    Account account =
        accountService.createAccount("test_user", "password", "test@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    Account foundAccount = accountService.get(account.getId()).orElse(null);
    assertNotNull(foundAccount);
    assertEquals("test_user", foundAccount.getUsername());
  }

  @Test
  public void testUpdateAccount() {
    Account account =
        accountService.createAccount("update_user", "password", "update@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    account.setEmail("new_email@user.com");
    Account updatedAccount = accountService.update(account);
    assertEquals("new_email@user.com", updatedAccount.getEmail());
  }

  @Test
  public void testDeleteAccount() {
    Account account =
        accountService.createAccount("delete_user", "password", "delete@user.com", RoleName.PLAYER);
    assertNotNull(account.getId());

    accountService.delete(account.getId());
    assertFalse(accountService.get(account.getId()).isPresent());
  }

  @Test
  public void testFindByUsername() {
    accountService.createAccount("find_user", "password", "find@user.com", RoleName.PLAYER);
    assertTrue(accountService.findByUsername("find_user").isPresent());
  }
}
