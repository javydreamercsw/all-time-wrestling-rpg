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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.account.PasswordResetToken;
import com.github.javydreamercsw.management.domain.account.PasswordResetTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;
  private final AccountService accountService;

  public PasswordResetService(
      PasswordResetTokenRepository tokenRepository,
      @Qualifier("managementAccountService") AccountService accountService) {
    this.tokenRepository = tokenRepository;
    this.accountService = accountService;
  }

  /**
   * Creates a password reset token for the given user. If a token already exists for the user, it
   * will be replaced.
   *
   * @param account The account to create the token for.
   * @return The created token.
   */
  public String createPasswordResetTokenForUser(Account account) {
    // Invalidate previous tokens
    tokenRepository.findByAccount(account).ifPresent(tokenRepository::delete);

    String token = UUID.randomUUID().toString();
    PasswordResetToken myToken = new PasswordResetToken(token, account);
    tokenRepository.save(myToken);
    return token;
  }

  public Optional<PasswordResetToken> getPasswordResetToken(String token) {
    return tokenRepository.findByToken(token);
  }

  /**
   * Resets the password for the given token.
   *
   * @param token The password reset token.
   * @param newPassword The new password.
   * @throws IllegalArgumentException if the token is invalid or expired.
   */
  public void resetPassword(String token, String newPassword) {
    Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(token);
    if (resetToken.isPresent() && !resetToken.get().isExpired()) {
      Account account = resetToken.get().getAccount();
      account.setPassword(newPassword); // Password should be encoded by the AccountService
      accountService.update(account);
      tokenRepository.delete(resetToken.get());
    } else {
      throw new IllegalArgumentException("Invalid or expired password reset token.");
    }
  }
}
