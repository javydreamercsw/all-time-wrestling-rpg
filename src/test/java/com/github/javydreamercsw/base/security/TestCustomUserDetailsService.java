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
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("test")
public class TestCustomUserDetailsService implements UserDetailsService {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private Clock clock;

  @Override
  public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    return accountRepository
        .findByUsername(username)
        .map(
            account -> {
              Wrestler wrestler = findOrCreateWrestlerForAccount(account);
              return new TestCustomUserDetails(account, wrestler);
            })
        .orElseGet(
            () -> {
              // Create default roles if they don't exist
              Role playerRole =
                  roleRepository
                      .findByName(RoleName.PLAYER)
                      .orElseGet(
                          () -> {
                            Role newRole = new Role();
                            newRole.setName(RoleName.PLAYER);
                            newRole.setDescription("Player role");
                            return roleRepository.save(newRole);
                          });

              Role adminRole =
                  roleRepository
                      .findByName(RoleName.ADMIN)
                      .orElseGet(
                          () -> {
                            Role newRole = new Role();
                            newRole.setName(RoleName.ADMIN);
                            newRole.setDescription("Administrator role");
                            return roleRepository.save(newRole);
                          });

              Role bookerRole =
                  roleRepository
                      .findByName(RoleName.BOOKER)
                      .orElseGet(
                          () -> {
                            Role newRole = new Role();
                            newRole.setName(RoleName.BOOKER);
                            newRole.setDescription("Booker role");
                            return roleRepository.save(newRole);
                          });

              Set<Role> rolesToAssign;
              if ("admin".equals(username)) {
                rolesToAssign = Set.of(adminRole, playerRole); // Admin also has player privileges
              } else if ("booker".equals(username)) {
                rolesToAssign = Set.of(bookerRole, playerRole); // Booker also has player privileges
              } else if ("not_owner".equals(username)) {
                rolesToAssign = Set.of(playerRole);
              } else {
                rolesToAssign = Set.of(playerRole);
              }

              // Create default accounts if they don't exist
              Account account = new Account();
              account.setUsername(username);
              account.setPassword(passwordEncoder.encode("password")); // Default password for tests
              account.setRoles(rolesToAssign);
              account.setEmail(username + "@test.com");
              accountRepository.save(account);

              Wrestler wrestler = findOrCreateWrestlerForAccount(account);

              return new TestCustomUserDetails(account, wrestler);
            });
  }

  private Wrestler findOrCreateWrestlerForAccount(@NonNull Account account) {
    return wrestlerRepository
        .findByAccount(account)
        .orElseGet(
            () -> {
              // Check if a wrestler with the external ID already exists
              String externalId = "wrestler-" + account.getUsername();
              return wrestlerRepository
                  .findByExternalId(externalId)
                  .orElseGet(
                      () -> {
                        Wrestler wrestler = new Wrestler();
                        wrestler.setName(account.getUsername() + " Wrestler");
                        wrestler.setIsPlayer(true);
                        wrestler.setAccount(account);
                        wrestler.setCreationDate(clock.instant());
                        wrestler.setExternalId(externalId);
                        return wrestlerRepository.save(wrestler);
                      });
            });
  }

  private static class TestCustomUserDetails extends CustomUserDetails {
    public TestCustomUserDetails(Account account, Wrestler wrestler) {
      super(account, wrestler);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return getAccount().getRoles().stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
          .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
      return getAccount().getPassword();
    }

    @Override
    public String getUsername() {
      return getAccount().getUsername();
    }
  }
}
