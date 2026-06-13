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
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("managementAccountService")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  @Lazy private final PasswordEncoder passwordEncoder;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseMembershipRepository universeMembershipRepository;

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public Account createAccount(
      final String username, final String password, final String email, final RoleName roleName) {
    return createAccountInternal(username, password, email, roleName);
  }

  /** Creates a PLAYER account without requiring an authenticated session (invite-link flow). */
  public Account createPlayerAccountForInvite(
      final String username, final String password, final String email) {
    return createAccountInternal(username, password, email, RoleName.PLAYER);
  }

  private Account createAccountInternal(
      final String username, final String password, final String email, final RoleName roleName) {
    if (!CustomPasswordValidator.isValid(password)) {
      throw new IllegalArgumentException("Password is not valid.");
    }
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
    Account account = new Account(username, passwordEncoder.encode(password), email);
    account.setRoles(new java.util.HashSet<>(Set.of(role)));
    Account saved = accountRepository.save(account);
    log.info("[AUDIT] Account created: username={} role={}", username, roleName);
    return saved;
  }

  public Optional<Account> get(final Long id) {
    return accountRepository.findById(id);
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or (hasAuthority('ROLE_PLAYER')\
       and #entity.id == authentication.principal.id)\
      """)
  public Account update(final Account entity) {
    Account original =
        accountRepository
            .findById(entity.getId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found for update."));

    String incomingPassword = entity.getPassword();
    String originalEncodedPassword = original.getPassword();

    boolean alreadyEncoded =
        incomingPassword.startsWith("$2a$")
            || incomingPassword.startsWith("$2b$")
            || incomingPassword.startsWith("$2y$");
    if (alreadyEncoded) {
      // Password is a bcrypt hash — keep it as-is (unchanged or pre-encoded by a trusted caller).
      entity.setPassword(incomingPassword);
    } else if (passwordEncoder.matches(incomingPassword, originalEncodedPassword)) {
      // Caller passed the correct plaintext password unchanged; keep stored hash.
      entity.setPassword(originalEncodedPassword);
    } else {
      if (!CustomPasswordValidator.isValid(incomingPassword)) {
        throw new IllegalArgumentException("Password is not valid.");
      }
      log.warn("[AUDIT] Password changed for account id={}", entity.getId());
      entity.setPassword(passwordEncoder.encode(incomingPassword));
    }

    return accountRepository.save(entity);
  }

  public void delete(final Long id) {
    accountRepository
        .findById(id)
        .ifPresent(
            account -> {
              account.setEnabled(false);
              accountRepository.save(account);
            });
  }

  public void hardDelete(final Long id) {
    accountRepository.deleteById(id);
  }

  public void enable(final Long id) {
    accountRepository
        .findById(id)
        .ifPresent(
            account -> {
              account.setEnabled(true);
              accountRepository.save(account);
            });
  }

  public Page<Account> list(final Pageable pageable) {
    return accountRepository.findAll(pageable);
  }

  public List<Account> findAll() {
    return accountRepository.findAll();
  }

  public int count() {
    return (int) accountRepository.count();
  }

  public Optional<Account> findByUsername(final String username) {
    return accountRepository.findByUsername(username);
  }

  public Optional<Account> findByEmail(final String email) {
    return accountRepository.findByEmail(email);
  }

  public boolean canDelete(final Account account) {
    return wrestlerRepository.findByAccountId(account.getId()).isEmpty();
  }

  public Role getRole(final RoleName roleName) {
    return roleRepository
        .findByName(roleName)
        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Account setLockExpiredAndSave(final Account account) {
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

  // Package-private method for internal use by trusted services (e.g., PasswordResetService)
  // Bypasses regular @PreAuthorize checks.
  void updateAccountPasswordInternal(final Account account, final String newEncodedPassword) {
    Account existingAccount =
        accountRepository
            .findById(account.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Account not found for internal update."));
    existingAccount.setPassword(newEncodedPassword);
    accountRepository.save(existingAccount);
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or (hasAuthority('ROLE_PLAYER')\
       and #accountId == authentication.principal.id)\
      """)
  public Account updateThemePreference(final Long accountId, final String themePreference) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    account.setThemePreference(themePreference);
    return accountRepository.save(account);
  }

  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or (hasAuthority('ROLE_PLAYER')\
       and #accountId == authentication.principal.id)\
      """)
  public Account setActiveWrestlerId(final Long accountId, final Long wrestlerId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    account.setActiveWrestlerId(wrestlerId);
    Account saved = accountRepository.save(account);

    // Ensure the wrestler is linked back to this account so findByAccountId queries work.
    // This matters for wrestlers created without an account (e.g. tutorial-seeded wrestlers).
    if (wrestlerId != null) {
      wrestlerRepository
          .findById(wrestlerId)
          .filter(w -> !saved.equals(w.getAccount()))
          .ifPresent(
              w -> {
                w.setAccount(saved);
                wrestlerRepository.save(w);
              });
    }

    // Refresh SecurityContext so the in-memory CustomUserDetails reflects the new active wrestler
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth != null
        && currentAuth.getPrincipal() instanceof CustomUserDetails details
        && details.getId() != null
        && details.getId().equals(accountId)) {
      Wrestler newWrestler =
          wrestlerId != null ? wrestlerRepository.findById(wrestlerId).orElse(null) : null;
      CustomUserDetails refreshed = new CustomUserDetails(saved, newWrestler);
      Authentication newAuth =
          new UsernamePasswordAuthenticationToken(
              refreshed, currentAuth.getCredentials(), currentAuth.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    return saved;
  }
}
