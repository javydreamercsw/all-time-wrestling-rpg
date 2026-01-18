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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

@Component
public class WithCustomMockUserSecurityContextFactory
    implements WithSecurityContextFactory<WithCustomMockUser> {

  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private Clock clock;
  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private RoleRepository roleRepository;

  @Override
  public SecurityContext createSecurityContext(WithCustomMockUser customUser) {
    String username = customUser.username();
    String[] roles = customUser.roles();

    Account account;
    // Try to find existing account to avoid unique constraint violations
    Optional<Account> existingAccount = accountRepository.findByUsername(username);

    if (existingAccount.isPresent()) {
      account = existingAccount.get();
    } else {
      // Create transient account (don't save to DB)
      account = new Account();
      account.setId(1L); // Mock ID
      account.setUsername(username);
      account.setPassword(passwordEncoder.encode("ValidPassword1!"));
      account.setEmail(username + "@test.com");
      account.setEnabled(true);
      account.setAccountNonExpired(true);
      account.setAccountNonLocked(true);
      account.setCredentialsNonExpired(true);

      Set<Role> assignedRoles =
          Arrays.stream(roles)
              .map(RoleName::valueOf)
              .map(
                  roleName -> {
                    Optional<Role> dbRole = roleRepository.findByName(roleName);
                    if (dbRole.isPresent()) {
                      return dbRole.get();
                    }
                    Role newRole = new Role();
                    newRole.setId((long) roleName.ordinal() + 100); // Mock ID (avoid 0)
                    newRole.setName(roleName);
                    newRole.setDescription(roleName + " role");
                    return newRole;
                  })
              .collect(Collectors.toSet());
      account.setRoles(assignedRoles);
    }

    Wrestler wrestler;
    Optional<Wrestler> existingWrestler = wrestlerRepository.findByAccount(account);
    if (existingWrestler.isPresent()) {
      wrestler = existingWrestler.get();
    } else {
      // Create transient wrestler (don't save to DB)
      wrestler = new Wrestler();
      wrestler.setId(1L); // Mock ID
      wrestler.setName(account.getUsername() + " Wrestler");
      wrestler.setIsPlayer(true);
      wrestler.setAccount(account);
      wrestler.setCreationDate(Instant.now(clock));
      wrestler.setExternalId("wrestler-" + account.getUsername());
    }

    CustomUserDetails principal = new CustomUserDetails(account, wrestler);

    // Ensure all roles are correctly mapped to authorities
    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
    for (String role : roles) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      authorities.add(new SimpleGrantedAuthority(role));
    }

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "ValidPassword1!", authorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
