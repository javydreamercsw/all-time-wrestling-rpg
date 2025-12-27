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
import com.github.javydreamercsw.base.security.CustomPasswordValidator;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("managementAccountService")
@Transactional
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  @Lazy private final PasswordEncoder passwordEncoder;
  private final WrestlerRepository wrestlerRepository;

  public Account createAccount(String username, String password, String email, RoleName roleName) {
    if (!CustomPasswordValidator.isValid(password)) {
      throw new IllegalArgumentException("Password is not valid.");
    }
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
    Account account = new Account(username, passwordEncoder.encode(password), email);
    account.setRoles(new java.util.HashSet<>(Set.of(role)));
    return accountRepository.save(account);
  }

  public Optional<Account> get(Long id) {
    return accountRepository.findById(id);
  }

  public Account update(Account entity) {
    Account original =
        accountRepository
            .findById(entity.getId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found for update."));

    String incomingPassword = entity.getPassword();
    String originalEncodedPassword = original.getPassword();

    // Check if a new plaintext password is provided (not starting with $2a$ and doesn't match
    // original encoded)
    if (!incomingPassword.startsWith("$2a$")
        && !passwordEncoder.matches(incomingPassword, originalEncodedPassword)) {
      if (!CustomPasswordValidator.isValid(incomingPassword)) {
        throw new IllegalArgumentException("Password is not valid.");
      }
      entity.setPassword(passwordEncoder.encode(incomingPassword));
    }
    // Check if a new encoded password is provided (starts with $2a$ and is different from original
    // encoded)
    else if (incomingPassword.startsWith("$2a$")
        && !incomingPassword.equals(originalEncodedPassword)) {
      // Assuming if an encoded password is provided directly, it's intended to be set.
      // No plaintext validation needed for already encoded passwords.
      entity.setPassword(incomingPassword);
    }
    // Otherwise, password hasn't changed or is the same encoded password, keep original encoded
    // password
    else {
      entity.setPassword(originalEncodedPassword);
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

  public boolean canDelete(Account account) {
    return wrestlerRepository.findByAccountId(account.getId()).isEmpty();
  }

  public Role getRole(RoleName roleName) {
    return roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Account setLockExpiredAndSave(Account account) {
    Account reloadedAccount =
        accountRepository
            .findById(account.getId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Account not found for setLockExpiredAndSave: " + account.getUsername()));
    reloadedAccount.setLockedUntil(java.time.LocalDateTime.now().minusMinutes(1));
    return accountRepository.save(reloadedAccount);
  }
}
