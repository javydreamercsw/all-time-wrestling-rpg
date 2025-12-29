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
import com.github.javydreamercsw.base.domain.account.PasswordResetToken;
import com.github.javydreamercsw.base.domain.account.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;

  @Qualifier("managementAccountService") private final AccountService accountService;

  private final PasswordEncoder passwordEncoder;

  public String createPasswordResetTokenForUser(Account account) {
    String token = UUID.randomUUID().toString();
    PasswordResetToken myToken = new PasswordResetToken(token, account);
    tokenRepository.save(myToken);
    return token;
  }

  public boolean validatePasswordResetToken(String token) {
    return tokenRepository.findByToken(token).map(t -> !isTokenExpired(t)).orElse(false);
  }

  private boolean isTokenExpired(PasswordResetToken passToken) {
    return passToken.getExpiryDate().isBefore(LocalDateTime.now());
  }

  public void resetPassword(String token, String newPassword) {
    if (!validatePasswordResetToken(token)) {
      throw new IllegalArgumentException("Invalid or expired password reset token.");
    }
    tokenRepository
        .findByToken(token)
        .ifPresentOrElse(
            t -> {
              Account account = t.getAccount();
              // Encode the new password before passing it to the internal update method
              String encodedPassword = passwordEncoder.encode(newPassword);
              accountService.updateAccountPasswordInternal(account, encodedPassword);
              tokenRepository.delete(t);
            },
            () -> {
              throw new IllegalArgumentException("Invalid or expired password reset token.");
            });
  }
}
