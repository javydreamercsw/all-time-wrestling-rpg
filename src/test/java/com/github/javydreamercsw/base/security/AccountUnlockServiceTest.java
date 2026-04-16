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
package com.github.javydreamercsw.base.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountUnlockServiceTest {

  private AccountRepository accountRepository;
  private AccountUnlockService accountUnlockService;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepository.class);
    accountUnlockService = new AccountUnlockService(accountRepository);
  }

  @Test
  void testUnlockAndReloadAccount() {
    Account account = new Account("testuser", "password", "test@example.com");
    account.setFailedLoginAttempts(5);
    account.setAccountNonLocked(false);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Account unlockedAccount = accountUnlockService.unlockAndReloadAccount("testuser");

    assertThat(unlockedAccount.getFailedLoginAttempts()).isZero();
    assertThat(unlockedAccount.isAccountNonLocked()).isTrue();
    verify(accountRepository).save(account);
  }

  @Test
  void testUnlockAndReloadAccountNotFound() {
    when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountUnlockService.unlockAndReloadAccount("unknown"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Account not found");
  }
}
