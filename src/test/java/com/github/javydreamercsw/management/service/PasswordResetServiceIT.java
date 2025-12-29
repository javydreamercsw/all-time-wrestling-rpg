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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.PasswordResetToken;
import com.github.javydreamercsw.base.domain.account.PasswordResetTokenRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
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
public class PasswordResetServiceIT {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired private PasswordResetService passwordResetService;
  @Autowired private AccountService accountService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private PasswordResetTokenRepository tokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Account testAccount;

  @BeforeEach
  void setUp() {
    testAccount =
        accountService.createAccount(
            "resetuser", "ResetPass123!", "reset@test.com", RoleName.PLAYER);
    accountRepository.save(testAccount);
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testCreatePasswordResetTokenForUser() {
    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);
    assertNotNull(token);
    assertTrue(tokenRepository.findByToken(token).isPresent());
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testValidatePasswordResetToken_valid() {
    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);
    assertTrue(passwordResetService.validatePasswordResetToken(token));
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testValidatePasswordResetToken_invalid() {
    assertFalse(passwordResetService.validatePasswordResetToken("invalid_token"));
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testValidatePasswordResetToken_expired() {
    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);
    PasswordResetToken resetToken = tokenRepository.findByToken(token).get();
    resetToken.setExpiryDate(LocalDateTime.now().minusHours(1));
    tokenRepository.save(resetToken);

    assertFalse(passwordResetService.validatePasswordResetToken(token));
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testResetPassword_validToken() {
    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);
    String newPassword = "NewResetPass456!";
    passwordResetService.resetPassword(token, newPassword);

    Account updatedAccount = accountRepository.findByUsername(testAccount.getUsername()).get();
    assertTrue(passwordEncoder.matches(newPassword, updatedAccount.getPassword()));
    assertFalse(tokenRepository.findByToken(token).isPresent()); // Token should be deleted
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testResetPassword_invalidToken() {
    String newPassword = "NewResetPass456!";
    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword("invalid_token", newPassword));
  }

  @Test
  @WithCustomMockUser(roles = "ADMIN")
  void testResetPassword_expiredToken() {
    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);
    PasswordResetToken resetToken = tokenRepository.findByToken(token).get();
    resetToken.setExpiryDate(LocalDateTime.now().minusHours(1));
    tokenRepository.save(resetToken);

    String newPassword = "NewResetPass456!";
    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword(token, newPassword));
  }
}
