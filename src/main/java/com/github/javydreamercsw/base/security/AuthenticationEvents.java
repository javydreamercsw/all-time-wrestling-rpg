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

import com.github.javydreamercsw.management.service.AccountService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEvents {

  public static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int LOCK_TIME_DURATION = 15; // in minutes

  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @EventListener
  public void onSuccess(AuthenticationSuccessEvent success) {
    String username = success.getAuthentication().getName();
    accountService
        .findByUsername(username)
        .ifPresent(
            account -> {
              if (account.getFailedLoginAttempts() > 0) {
                account.resetFailedAttempts();
                accountService.update(account);
              }
            });
  }

  @EventListener
  public void onFailure(AuthenticationFailureBadCredentialsEvent failure) {
    String username = failure.getAuthentication().getName();
    accountService
        .findByUsername(username)
        .ifPresent(
            account -> {
              account.incrementFailedAttempts();
              if (account.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                account.lockUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION));
              }
              accountService.update(account);
            });
  }
}
