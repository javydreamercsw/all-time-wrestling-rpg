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
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  @Lazy private final PasswordEncoder passwordEncoder;

  public Account createAccount(String username, String password, String email, RoleName roleName) {
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
    Account account = new Account(username, passwordEncoder.encode(password), email);
    account.setRoles(Set.of(role));
    return accountRepository.save(account);
  }

  public Optional<Account> get(Long id) {
    return accountRepository.findById(id);
  }

  public Account update(Account entity) {
    // Hash password if it has been changed
    if (entity.getId() != null) {
      accountRepository
          .findById(entity.getId())
          .ifPresent(
              original -> {
                if (!entity.getPassword().equals(original.getPassword())) {
                  entity.setPassword(passwordEncoder.encode(entity.getPassword()));
                }
              });
    } else {
      entity.setPassword(passwordEncoder.encode(entity.getPassword()));
    }
    return accountRepository.save(entity);
  }

  public void delete(Long id) {
    accountRepository.deleteById(id);
  }

  public Page<Account> list(Pageable pageable) {
    return accountRepository.findAll(pageable);
  }

  public int count() {
    return (int) accountRepository.count();
  }

  public Optional<Account> findByUsername(String username) {
    return accountRepository.findByUsername(username);
  }

  public Optional<Account> findByEmail(String email) {
    return accountRepository.findByEmail(email);
  }
}
