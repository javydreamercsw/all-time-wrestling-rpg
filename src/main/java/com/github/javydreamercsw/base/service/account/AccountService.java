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
package com.github.javydreamercsw.base.service.account;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("baseAccountService")
@Transactional
public class AccountService {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public List<Account> findAll() {
    return accountRepository.findAll();
  }

  /**
   * Finds all accounts that are not currently assigned to any wrestler.
   *
   * @return A list of accounts not linked to any wrestler.
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public List<Account> findAllNonPlayerAccounts() {
    List<Account> allAccounts = accountRepository.findAll();
    List<Account> accountsWithWrestlers =
        wrestlerRepository.findAll().stream()
            .map(Wrestler::getAccount)
            .filter(Objects::nonNull)
            .toList();

    allAccounts.removeAll(accountsWithWrestlers);
    return allAccounts;
  }
}
