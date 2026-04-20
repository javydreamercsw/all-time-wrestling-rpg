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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class WithCustomMockUserSecurityContextFactory
    implements WithSecurityContextFactory<WithCustomMockUser> {

  @Autowired(required = false)
  private PasswordEncoder passwordEncoder;

  @Autowired(required = false)
  private AccountRepository accountRepository;

  @Autowired(required = false)
  private RoleRepository roleRepository;

  @Autowired(required = false)
  private WrestlerRepository wrestlerRepository;

  @Autowired(required = false)
  private WrestlerStateRepository wrestlerStateRepository;

  @Autowired(required = false)
  private UniverseRepository universeRepository;

  @Autowired(required = false)
  private EntityManager entityManager;

  @Autowired(required = false)
  private TransactionTemplate transactionTemplate;

  @Override
  public SecurityContext createSecurityContext(WithCustomMockUser customUser) {
    String username = customUser.username();
    String[] roles = customUser.roles();

    if (transactionTemplate != null && entityManager != null) {
      return transactionTemplate.execute(
          status -> {
            // 1. Try to find real account
            Account account = accountRepository.findByUsername(username).orElse(null);

            // 2. If not found, create saved account
            if (account == null) {
              account = new Account();
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

              // Find real roles
              Set<Role> assignedRoles = new HashSet<>();
              for (String roleNameStr : roles) {
                RoleName roleName = RoleName.valueOf(roleNameStr);
                roleRepository.findByName(roleName).ifPresent(assignedRoles::add);
              }
              account.setRoles(assignedRoles);
              account = accountRepository.saveAndFlush(account);
            }

            // 3. Handle Wrestler
            Wrestler wrestler =
                wrestlerRepository.findByAccount(account).stream().findFirst().orElse(null);

            if (wrestler == null) {
              wrestler = new Wrestler();
              wrestler.setName(account.getUsername() + " Wrestler");
              wrestler.setIsPlayer(true);
              wrestler.setAccount(account);
              wrestler.setCreationDate(Instant.now());
              wrestler.setExternalId("wrestler-" + account.getUsername());

              // We need a universe for a real wrestler
              Universe universe = universeRepository.findAll().stream().findFirst().orElse(null);
              if (universe == null) {
                universe =
                    universeRepository.saveAndFlush(
                        Universe.builder()
                            .name("Default Universe")
                            .type(Universe.UniverseType.GLOBAL)
                            .build());
              }
              wrestler = wrestlerRepository.saveAndFlush(wrestler);

              com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
                  com.github.javydreamercsw.management.domain.wrestler.WrestlerState.builder()
                      .wrestler(wrestler)
                      .universe(universe)
                      .fans(0L)
                      .tier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.ROOKIE)
                      .currentHealth(15)
                      .bumps(0)
                      .morale(100)
                      .build();
              wrestlerStateRepository.saveAndFlush(state);
            }

            return finishContext(username, roles, account, wrestler);
          });
    } else {
      // Fallback for unit tests without DB
      Account account = new Account();
      account.setId(1L);
      account.setUsername(username);
      account.setPassword("password");
      account.setEmail(username + "@test.com");

      Wrestler wrestler = new Wrestler();
      wrestler.setId(1L);
      wrestler.setName(username + " Wrestler");

      return finishContext(username, roles, account, wrestler);
    }
  }

  private SecurityContext finishContext(
      String username, String[] roles, Account account, Wrestler wrestler) {
    CustomUserDetails principal = new CustomUserDetails(account, wrestler);

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
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
