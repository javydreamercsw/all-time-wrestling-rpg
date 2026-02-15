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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDateTime;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Custom UserDetailsService that loads user details from the database. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;
  private final WrestlerRepository wrestlerRepository;
  private final AccountUnlockService accountUnlockService; // Inject new service

  @Override
  @Transactional
  public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    Account account =
        accountRepository
            .findByUsernameWithRoles(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("No user found with username: " + username));

    // Check if account lock has expired and unlock if necessary
    if (!account.isAccountNonLocked() && account.isLockExpired()) {
      account = accountUnlockService.unlockAndReloadAccount(account.getUsername()); // Pass username
      log.info("Account lock expired and reset for user: {}", username);
    }

    // Find the wrestler associated with the account
    Wrestler wrestler = wrestlerRepository.findByAccount(account).orElse(null);

    return new CustomUserDetails(account, wrestler); // Pass reloaded account to constructor
  }

  /**
   * Record a failed login attempt for the user.
   *
   * @param username the username
   */
  @Transactional
  public void recordFailedLoginAttempt(@NonNull String username) {
    accountRepository
        .findByUsername(username)
        .ifPresent(
            account -> {
              account.incrementFailedAttempts();
              log.warn(
                  "Failed login attempt #{} for user: {}",
                  account.getFailedLoginAttempts(),
                  username);

              // Lock account after 5 failed attempts
              if (account.getFailedLoginAttempts() >= 5) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(15);
                account.lockUntil(lockUntil);
                log.warn("Account locked until {} for user: {}", lockUntil, username);
              }

              accountRepository.save(account);
            });
  }

  /**
   * Reset failed login attempts after successful login.
   *
   * @param username the username
   */
  @Transactional
  public void recordSuccessfulLogin(@NonNull String username) {
    accountRepository
        .findByUsername(username)
        .ifPresent(
            account -> {
              account.resetFailedAttempts();
              account.setLastLogin(LocalDateTime.now());
              accountRepository.save(account);
              log.debug("Recorded successful login for user: {}", username);
            });
  }
}
