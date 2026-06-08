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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseMembershipRepository universeMembershipRepository;

  @InjectMocks private AccountService accountService;

  private Account account;
  private Role adminRole;

  @BeforeEach
  void setUp() {
    account = new Account("testUser", "$2a$10$encodedPassword", "test@example.com");
    account.setId(1L);

    adminRole = new Role(RoleName.ADMIN, "ADMIN");

    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void testUpdateThemePreference() {
    accountService.updateThemePreference(1L, "dark");

    verify(accountRepository).save(account);
    assertThat(account.getThemePreference()).isEqualTo("dark");
  }

  @Test
  void testUpdateThemePreference_accountNotFound_throws() {
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.updateThemePreference(99L, "dark"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account not found");
  }

  @Test
  void testGet_returnsAccount() {
    Optional<Account> result = accountService.get(1L);

    assertThat(result).isPresent().contains(account);
  }

  @Test
  void testGet_missingId_returnsEmpty() {
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Account> result = accountService.get(99L);

    assertThat(result).isEmpty();
  }

  @Test
  void testDelete_softDeletes_setsEnabledFalse() {
    accountService.delete(1L);

    assertThat(account.isEnabled()).isFalse();
    verify(accountRepository).save(account);
  }

  @Test
  void testDelete_nonExistentId_doesNothing() {
    accountService.delete(99L);

    verify(accountRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void testEnable_setsEnabledTrue() {
    account.setEnabled(false);

    accountService.enable(1L);

    assertThat(account.isEnabled()).isTrue();
    verify(accountRepository).save(account);
  }

  @Test
  void testEnable_nonExistentId_doesNothing() {
    accountService.enable(99L);

    verify(accountRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void testList_returnsPaged() {
    Page<Account> page = new PageImpl<>(List.of(account));
    when(accountRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<Account> result = accountService.list(Pageable.unpaged());

    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  void testFindAll() {
    when(accountRepository.findAll()).thenReturn(List.of(account));

    List<Account> result = accountService.findAll();

    assertThat(result).hasSize(1);
  }

  @Test
  void testCount() {
    when(accountRepository.count()).thenReturn(5L);

    assertThat(accountService.count()).isEqualTo(5);
  }

  @Test
  void testFindByUsername_found() {
    when(accountRepository.findByUsername("testUser")).thenReturn(Optional.of(account));

    Optional<Account> result = accountService.findByUsername("testUser");

    assertThat(result).isPresent();
  }

  @Test
  void testFindByEmail_found() {
    when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(account));

    Optional<Account> result = accountService.findByEmail("test@example.com");

    assertThat(result).isPresent();
  }

  @Test
  void testCanDelete_noWrestlers_returnsTrue() {
    when(wrestlerRepository.findByAccountId(1L)).thenReturn(List.of());

    assertThat(accountService.canDelete(account)).isTrue();
  }

  @Test
  void testCanDelete_withWrestler_returnsFalse() {
    Wrestler wrestler = new Wrestler();
    when(wrestlerRepository.findByAccountId(1L)).thenReturn(List.of(wrestler));

    assertThat(accountService.canDelete(account)).isFalse();
  }

  @Test
  void testGetRole_found() {
    when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));

    Role result = accountService.getRole(RoleName.ADMIN);

    assertThat(result).isSameAs(adminRole);
  }

  @Test
  void testGetRole_notFound_throws() {
    when(roleRepository.findByName(RoleName.PLAYER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.getRole(RoleName.PLAYER))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Role not found");
  }

  @Test
  void testSetActiveWrestlerId() {
    accountService.setActiveWrestlerId(1L, 42L);

    verify(accountRepository).save(account);
    assertThat(account.getActiveWrestlerId()).isEqualTo(42L);
  }

  @Test
  void testSetActiveWrestlerId_accountNotFound_throws() {
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.setActiveWrestlerId(99L, 42L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testSetLockExpiredAndSave() {
    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

    Account result = accountService.setLockExpiredAndSave(account);

    verify(accountRepository).save(account);
    assertThat(account.getLockedUntil()).isBefore(LocalDateTime.now());
  }

  @Test
  void testSetLockExpiredAndSave_accountNotFound_throws() {
    Account missing = new Account("missing", "pass", "m@x.com");
    missing.setId(99L);
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.setLockExpiredAndSave(missing))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void testUpdate_samePassword_keepsOriginalEncoded() {
    account.setPassword("$2a$10$originalEncoded");
    Account incoming = new Account("testUser", "plaintext", "test@example.com");
    incoming.setId(1L);
    when(passwordEncoder.matches("plaintext", "$2a$10$originalEncoded")).thenReturn(true);

    accountService.update(incoming);

    verify(accountRepository).save(incoming);
    assertThat(incoming.getPassword()).isEqualTo("$2a$10$originalEncoded");
  }

  @Test
  void testUpdate_newPlaintextPassword_encodesAndUpdates() {
    account.setPassword("$2a$10$originalEncoded");
    Account incoming = new Account("testUser", "NewPass1!", "test@example.com");
    incoming.setId(1L);
    when(passwordEncoder.matches("NewPass1!", "$2a$10$originalEncoded")).thenReturn(false);
    when(passwordEncoder.encode("NewPass1!")).thenReturn("$2a$10$newEncoded");

    accountService.update(incoming);

    assertThat(incoming.getPassword()).isEqualTo("$2a$10$newEncoded");
  }

  @Test
  void testUpdate_accountNotFound_throws() {
    Account incoming = new Account("testUser", "password", "test@example.com");
    incoming.setId(99L);
    when(accountRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.update(incoming))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Account not found");
  }
}
