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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private WrestlerRepository wrestlerRepository;

  @InjectMocks private AccountService accountService;

  @Test
  void testUpdateThemePreference() {
    Long accountId = 1L;
    String newTheme = "dark";
    Account account = new Account();
    account.setId(accountId);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    accountService.updateThemePreference(accountId, newTheme);

    verify(accountRepository).save(account);
    assert "dark".equals(account.getThemePreference());
  }
}
