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
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Factory for creating a security context for @WithCustomMockUser. This factory is designed to
 * create transient mock accounts and wrestlers for testing purposes.
 *
 * <p>NOTE: Do NOT add @Component annotation. This class must be registered as a bean explicitly
 * in test configuration classes to ensure Spring's @WithSecurityContext annotation processing can
 * find it at test class load time.
 */
@Slf4j
public class WithCustomMockUserSecurityContextFactory
    implements WithSecurityContextFactory<WithCustomMockUser> {

  @Autowired(required = false)
  private PasswordEncoder passwordEncoder;

  @Override
  public SecurityContext createSecurityContext(WithCustomMockUser customUser) {
    String username = customUser.username();
    String[] roles = customUser.roles();

    log.debug(
        "Creating security context for user: {} with roles: {}",
        username,
        Arrays.toString(roles));

    // Create transient account (don't save to DB)
    Account account = new Account();
    account.setId(1L); // Default Mock ID
    account.setUsername(username);
    if (passwordEncoder != null) {
      account.setPassword(passwordEncoder.encode("ValidPassword1!"));
    } else {
      account.setPassword("encoded_password");
    }
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
                  Role newRole = new Role();
                  newRole.setId((long) roleName.ordinal() + 100); // Transient ID
                  newRole.setName(roleName);
                  newRole.setDescription(roleName + " role");
                  return newRole;
                })
            .collect(Collectors.toSet());
    account.setRoles(assignedRoles);

    // Create transient wrestler (don't save to DB)
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L); // Default Mock ID
    wrestler.setName(account.getUsername() + " Wrestler");
    wrestler.setIsPlayer(true);
    wrestler.setAccount(account);
    wrestler.setCreationDate(Instant.now());
    wrestler.setExternalId("wrestler-" + account.getUsername());

    CustomUserDetails principal = new CustomUserDetails(account, wrestler);

    // Ensure all roles are correctly mapped to authorities
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (String role : roles) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      authorities.add(new SimpleGrantedAuthority(role));
    }

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "ValidPassword1!", authorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    log.debug(
        "Security context created for user: {} with {} authorities",
        username,
        authorities.size());
    return context;
  }
}
