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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountUnlockService {

  private final AccountRepository accountRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Account unlockAndReloadAccount(final String username) {
    return accountRepository
        .findByUsername(username)
        .map(
            acc -> {
              acc.resetFailedAttempts();
              Account saved = accountRepository.save(acc);
              log.warn("[AUDIT] Account unlocked (lock expired): username={}", username);
              return saved;
            })
        .orElseThrow(() -> new RuntimeException("Account not found for unlock: " + username));
  }
}
