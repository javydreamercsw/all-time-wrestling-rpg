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
package com.github.javydreamercsw.base;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInitializer implements Initializable {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  @Lazy private final PasswordEncoder passwordEncoder;

  @Value("${data.initializer.enabled:true}")
  private boolean enabled;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void init() {
    if (enabled) {
      log.info("Initializing accounts...");
      // Create roles if they don't exist
      Map<RoleName, Role> roles =
          roleRepository.findAll().stream().collect(Collectors.toMap(Role::getName, role -> role));

      for (RoleName roleName : RoleName.values()) {
        if (!roles.containsKey(roleName)) {
          Role role = new Role(roleName, roleName.name() + " role");
          roleRepository.save(role);
          roles.put(roleName, role);
          log.info("Created role: {}", roleName);
        }
      }

      // Create default accounts
      createAccount("admin", "admin123", Set.of(roles.get(RoleName.ADMIN)));
      createAccount("booker", "booker123", Set.of(roles.get(RoleName.BOOKER)));
      createAccount("player", "player123", Set.of(roles.get(RoleName.PLAYER)));
      createAccount("viewer", "viewer123", Set.of(roles.get(RoleName.VIEWER)));
      log.info("Account initialization complete.");
    }
  }

  private void createAccount(
      @NonNull String username, @NonNull String password, @NonNull Set<Role> roles) {
    if (accountRepository.findByUsername(username).isEmpty()) {
      Account account =
          new Account(username, passwordEncoder.encode(password), username + "@example.com");
      account.setRoles(roles);
      accountRepository.save(account);
      log.info("Created account: {}", username);
    }
  }
}
