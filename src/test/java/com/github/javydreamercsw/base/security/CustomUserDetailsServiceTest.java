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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Unit tests for {@link CustomUserDetailsService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomUserDetailsServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private AccountUnlockService accountUnlockService;

  @InjectMocks private CustomUserDetailsService service;

  // ---------------------------------------------------------------------------
  // loadUserByUsername
  // ---------------------------------------------------------------------------

  @Test
  void loadUserByUsername_userFoundSuccessfully() {
    Account account = new Account("testuser", "password123", "test@example.com");
    account.setId(1L);

    when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findAllByAccount(account)).thenReturn(Collections.emptyList());

    UserDetails result = service.loadUserByUsername("testuser");

    assertThat(result).isInstanceOf(CustomUserDetails.class);
    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  @Test
  void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
    when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("unknown");
  }

  @Test
  void loadUserByUsername_systemUser_createdInline() {
    // "system" is not in the repository
    when(accountRepository.findByUsername("system")).thenReturn(Optional.empty());
    when(wrestlerRepository.findAllByAccount(any(Account.class)))
        .thenReturn(Collections.emptyList());

    UserDetails result = service.loadUserByUsername("system");

    assertThat(result).isInstanceOf(CustomUserDetails.class);
    assertThat(result.getUsername()).isEqualTo("system");
    // system user has no DB id so save is never called
    verify(accountRepository, never()).save(any());
  }

  @Test
  void loadUserByUsername_lockedAccountWithExpiredLock_unlocks() {
    Account lockedAccount = new Account("lockeduser", "password123", "locked@example.com");
    lockedAccount.setId(2L);
    // Lock was set in the past — it has expired
    lockedAccount.lockUntil(LocalDateTime.now().minusMinutes(1));

    Account unlockedAccount = new Account("lockeduser", "password123", "locked@example.com");
    unlockedAccount.setId(2L);

    when(accountRepository.findByUsername("lockeduser")).thenReturn(Optional.of(lockedAccount));
    when(accountUnlockService.unlockAndReloadAccount("lockeduser")).thenReturn(unlockedAccount);
    when(wrestlerRepository.findAllByAccount(any(Account.class)))
        .thenReturn(Collections.emptyList());

    UserDetails result = service.loadUserByUsername("lockeduser");

    assertThat(result.getUsername()).isEqualTo("lockeduser");
    verify(accountUnlockService).unlockAndReloadAccount("lockeduser");
  }

  @Test
  void loadUserByUsername_accountNotLocked_doesNotCallUnlockService() {
    Account account = new Account("normaluser", "password123", "normal@example.com");
    account.setId(3L);
    // account is not locked at all

    when(accountRepository.findByUsername("normaluser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findAllByAccount(account)).thenReturn(Collections.emptyList());

    service.loadUserByUsername("normaluser");

    verify(accountUnlockService, never()).unlockAndReloadAccount(any());
  }

  @Test
  void loadUserByUsername_accountWithWrestlerViaActiveWrestlerId() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(10L);

    Account account = new Account("playeruser", "password123", "player@example.com");
    account.setId(4L);
    account.setActiveWrestlerId(10L);

    when(accountRepository.findByUsername("playeruser")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(wrestler));

    UserDetails result = service.loadUserByUsername("playeruser");

    assertThat(result).isInstanceOf(CustomUserDetails.class);
    CustomUserDetails details = (CustomUserDetails) result;
    assertThat(details.getWrestler()).isEqualTo(wrestler);
  }

  @Test
  void loadUserByUsername_accountWithWrestlerViaFallback_setsActiveWrestlerId() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(20L);

    Account account = new Account("playeruser2", "password123", "player2@example.com");
    account.setId(5L);
    // No active wrestler id set

    when(accountRepository.findByUsername("playeruser2")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findAllByAccount(account)).thenReturn(List.of(wrestler));
    when(accountRepository.save(account)).thenReturn(account);

    UserDetails result = service.loadUserByUsername("playeruser2");

    assertThat(result).isInstanceOf(CustomUserDetails.class);
    CustomUserDetails details = (CustomUserDetails) result;
    assertThat(details.getWrestler()).isEqualTo(wrestler);
    assertThat(account.getActiveWrestlerId()).isEqualTo(20L);
    verify(accountRepository).save(account);
  }

  @Test
  void loadUserByUsername_activeWrestlerIdNotFound_fallsBackToList() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(30L);

    Account account = new Account("playeruser3", "password123", "player3@example.com");
    account.setId(6L);
    account.setActiveWrestlerId(99L); // id that doesn't exist

    when(accountRepository.findByUsername("playeruser3")).thenReturn(Optional.of(account));
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findAllByAccount(account)).thenReturn(List.of(wrestler));
    when(accountRepository.save(account)).thenReturn(account);

    UserDetails result = service.loadUserByUsername("playeruser3");

    CustomUserDetails details = (CustomUserDetails) result;
    assertThat(details.getWrestler()).isEqualTo(wrestler);
  }

  // ---------------------------------------------------------------------------
  // recordFailedLoginAttempt
  // ---------------------------------------------------------------------------

  @Test
  void recordFailedLoginAttempt_userFound_incrementsAttempts() {
    Account account = new Account("failuser", "password123", "fail@example.com");
    account.setId(7L);

    when(accountRepository.findByUsername("failuser")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.recordFailedLoginAttempt("failuser");

    assertThat(account.getFailedLoginAttempts()).isEqualTo(1);
    verify(accountRepository).save(account);
  }

  @Test
  void recordFailedLoginAttempt_after5Failures_locksAccount() {
    Account account = new Account("lockme", "password123", "lockme@example.com");
    account.setId(8L);
    // Pre-set to 4 so the next increment triggers the lock
    account.setFailedLoginAttempts(4);

    when(accountRepository.findByUsername("lockme")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.recordFailedLoginAttempt("lockme");

    assertThat(account.getFailedLoginAttempts()).isEqualTo(5);
    assertThat(account.isAccountNonLocked()).isFalse();
    assertThat(account.getLockedUntil()).isNotNull();
    assertThat(account.getLockedUntil()).isAfter(LocalDateTime.now());
    verify(accountRepository).save(account);
  }

  @Test
  void recordFailedLoginAttempt_userNotFound_noOp() {
    when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    service.recordFailedLoginAttempt("ghost");

    verify(accountRepository, never()).save(any());
  }

  // ---------------------------------------------------------------------------
  // recordSuccessfulLogin
  // ---------------------------------------------------------------------------

  @Test
  void recordSuccessfulLogin_resetsAttemptsAndSetsLastLogin() {
    Account account = new Account("successuser", "password123", "success@example.com");
    account.setId(9L);
    account.setFailedLoginAttempts(3);

    when(accountRepository.findByUsername("successuser")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.recordSuccessfulLogin("successuser");

    assertThat(account.getFailedLoginAttempts()).isZero();
    assertThat(account.getLastLogin()).isNotNull();
    verify(accountRepository).save(account);
  }

  @Test
  void recordSuccessfulLogin_userNotFound_noOp() {
    when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    service.recordSuccessfulLogin("ghost");

    verify(accountRepository, never()).save(any());
  }
}
