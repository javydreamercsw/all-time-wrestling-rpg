/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.PasswordResetToken;
import com.github.javydreamercsw.base.domain.account.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private AccountService accountService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private PasswordResetService passwordResetService;

  private Account testAccount;

  @BeforeEach
  void setUp() {
    testAccount = new Account("testuser", "encoded-password", "test@example.com");
    testAccount.setId(1L);
  }

  @Test
  void createPasswordResetTokenForUser_savesAndReturnsToken() {
    when(tokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String token = passwordResetService.createPasswordResetTokenForUser(testAccount);

    assertThat(token).isNotNull().isNotEmpty();
    verify(tokenRepository).save(any(PasswordResetToken.class));
  }

  @Test
  void validatePasswordResetToken_validToken_returnsTrue() {
    PasswordResetToken resetToken = new PasswordResetToken("valid-token", testAccount);

    when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));

    boolean result = passwordResetService.validatePasswordResetToken("valid-token");

    assertThat(result).isTrue();
  }

  @Test
  void validatePasswordResetToken_expiredToken_returnsFalse() {
    PasswordResetToken expiredToken = mock(PasswordResetToken.class);
    when(expiredToken.getExpiryDate()).thenReturn(LocalDateTime.now().minusHours(1));
    when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

    boolean result = passwordResetService.validatePasswordResetToken("expired-token");

    assertThat(result).isFalse();
  }

  @Test
  void validatePasswordResetToken_unknownToken_returnsFalse() {
    when(tokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

    boolean result = passwordResetService.validatePasswordResetToken("unknown-token");

    assertThat(result).isFalse();
  }

  @Test
  void resetPassword_validToken_updatesPassword() {
    PasswordResetToken resetToken = new PasswordResetToken("valid-token", testAccount);

    when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("newPassword")).thenReturn("encoded-new-password");
    doNothing()
        .when(accountService)
        .updateAccountPasswordInternal(any(Account.class), any(String.class));
    doNothing().when(tokenRepository).delete(resetToken);

    passwordResetService.resetPassword("valid-token", "newPassword");

    verify(passwordEncoder).encode("newPassword");
    verify(accountService).updateAccountPasswordInternal(testAccount, "encoded-new-password");
    verify(tokenRepository).delete(resetToken);
  }

  @Test
  void resetPassword_expiredToken_throwsException() {
    PasswordResetToken expiredToken = mock(PasswordResetToken.class);
    when(expiredToken.getExpiryDate()).thenReturn(LocalDateTime.now().minusHours(1));
    when(tokenRepository.findByToken("expired-token"))
        .thenReturn(Optional.of(expiredToken))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPassword"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid or expired password reset token");

    verify(accountService, never()).updateAccountPasswordInternal(any(), any());
  }

  @Test
  void resetPassword_unknownToken_throwsException() {
    when(tokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passwordResetService.resetPassword("unknown", "newPassword"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
