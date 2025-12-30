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
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.transaction.annotation.Transactional;

public class WithCustomMockUserSecurityContextFactory
    implements WithSecurityContextFactory<WithCustomMockUser> {

  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private Clock clock;

  @Override
  @Transactional
  public SecurityContext createSecurityContext(WithCustomMockUser customUser) {
    String username = customUser.username();
    String[] roles = customUser.roles();

    // Find or create account
    Account account =
        accountRepository
            .findByUsername(username)
            .orElseGet(
                () -> {
                  Account newAccount = new Account();
                  newAccount.setUsername(username);
                  newAccount.setPassword(
                      passwordEncoder.encode("ValidPassword1!")); // Dummy password
                  newAccount.setEmail(username + "@test.com");
                  newAccount.setEnabled(true);
                  newAccount.setAccountNonExpired(true);
                  newAccount.setAccountNonLocked(true);
                  newAccount.setCredentialsNonExpired(true);

                  Set<Role> assignedRoles =
                      Arrays.stream(roles)
                          .map(RoleName::valueOf)
                          .map(
                              roleName ->
                                  roleRepository
                                      .findByName(roleName)
                                      .orElseGet(
                                          () -> {
                                            Role newRole = new Role();
                                            newRole.setName(roleName);
                                            newRole.setDescription(roleName + " role");
                                            return roleRepository.save(newRole);
                                          }))
                          .collect(Collectors.toSet());
                  newAccount.setRoles(assignedRoles);
                  return accountRepository.save(newAccount);
                });

    // Create or update wrestler for the account
    Wrestler wrestler =
        wrestlerRepository
            .findByAccount(account)
            .orElseGet(
                () -> {
                  Wrestler newWrestler = new Wrestler();
                  newWrestler.setName(account.getUsername() + " Wrestler");
                  newWrestler.setIsPlayer(true);
                  newWrestler.setAccount(account);
                  newWrestler.setCreationDate(Instant.now(clock));
                  newWrestler.setExternalId("wrestler-" + account.getUsername());
                  return wrestlerRepository.save(newWrestler);
                });

    CustomUserDetails principal = new CustomUserDetails(account, wrestler);

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            principal, "ValidPassword1!", principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
