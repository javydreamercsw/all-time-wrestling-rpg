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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating a security context for @WithCustomMockUser. This factory is designed to be
 * fast and avoid complex database operations that might interfere with test setup/cleanup.
 */
@Component
@Slf4j
public class WithCustomMockUserSecurityContextFactory
    implements WithSecurityContextFactory<WithCustomMockUser> {

  @Autowired(required = false)
  private AccountRepository accountRepository;

  @Autowired(required = false)
  private WrestlerRepository wrestlerRepository;

  @Override
  public SecurityContext createSecurityContext(WithCustomMockUser customUser) {
    String username = customUser.username();
    String[] roles = customUser.roles();

    log.debug("Creating security context for user: {}", username);

    Account account = null;
    Wrestler wrestler = null;

    // Try to find real account if repository is available
    if (accountRepository != null) {
      try {
        account = accountRepository.findByUsername(username).orElse(null);
        if (account != null && wrestlerRepository != null) {
          wrestler = wrestlerRepository.findByAccount(account).stream().findFirst().orElse(null);
        }
      } catch (Exception e) {
        log.trace("Could not load real account for mock context: {}", e.getMessage());
      }
    }

    // If not found, create a mock account object (not persisted)
    if (account == null) {
      account = new Account(username, "password", username + "@test.com");
      account.setEnabled(true);
      account.setAccountNonExpired(true);
      account.setAccountNonLocked(true);
      account.setCredentialsNonExpired(true);

      Set<Role> mockRoles = new HashSet<>();
      for (String roleNameStr : roles) {
        String cleanRoleName = roleNameStr;
        if (cleanRoleName.startsWith("ROLE_")) {
          cleanRoleName = cleanRoleName.substring(5);
        }
        try {
          RoleName rn = RoleName.valueOf(cleanRoleName);
          mockRoles.add(new Role(rn, rn.name()));
        } catch (IllegalArgumentException e) {
          // Ignore
        }
      }
      account.setRoles(mockRoles);
    }

    CustomUserDetails principal = new CustomUserDetails(account, wrestler);
    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
    for (String roleName : roles) {
      authorities.add(new SimpleGrantedAuthority(roleName));
      if (!roleName.startsWith("ROLE_")) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
      }
    }

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "password", authorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
