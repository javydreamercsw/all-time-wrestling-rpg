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
package com.github.javydreamercsw.base.test;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
public class TestUserInitializer implements CommandLineRunner {

  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private Clock clock;

  @Override
  @Transactional // Ensure transactions are handled for data persistence
  public void run(String... args) throws Exception {
    // Create roles if they don't exist
    Role adminRole =
        roleRepository
            .findByName(RoleName.ADMIN)
            .orElseGet(
                () -> {
                  Role newRole = new Role();
                  newRole.setName(RoleName.ADMIN);
                  newRole.setDescription("Admin role");
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

    Role viewerRole =
        roleRepository
            .findByName(RoleName.VIEWER)
            .orElseGet(
                () -> {
                  Role newRole = new Role();
                  newRole.setName(RoleName.VIEWER);
                  newRole.setDescription("Viewer role");
                  return roleRepository.save(newRole);
                });

    // Create test accounts if they don't exist
    if (accountRepository.findByUsername("admin").isEmpty()) {
      Account adminAccount = new Account();
      adminAccount.setUsername("admin");
      adminAccount.setPassword(passwordEncoder.encode("password"));
      adminAccount.setRoles(Set.of(adminRole));
      adminAccount.setEmail("admin@test.com");
      accountRepository.save(adminAccount);
    }

    if (accountRepository.findByUsername("booker").isEmpty()) {
      Account bookerAccount = new Account();
      bookerAccount.setUsername("booker");
      bookerAccount.setPassword(passwordEncoder.encode("password"));
      bookerAccount.setRoles(Set.of(bookerRole));
      bookerAccount.setEmail("booker@test.com");
      accountRepository.save(bookerAccount);
    }

    if (accountRepository.findByUsername("owner").isEmpty()) {
      Account ownerAccount = new Account();
      ownerAccount.setUsername("owner");
      ownerAccount.setPassword(passwordEncoder.encode("password"));
      ownerAccount.setRoles(Set.of(playerRole));
      ownerAccount.setEmail("owner@test.com");
      accountRepository.save(ownerAccount);

      Wrestler ownedWrestler = new Wrestler();
      ownedWrestler.setName("Owned Wrestler");
      ownedWrestler.setIsPlayer(true);
      ownedWrestler.setAccount(ownerAccount);
      ownedWrestler.setCreationDate(clock.instant());
      ownedWrestler.setExternalId("wrestler-owner");
      wrestlerRepository.save(ownedWrestler);
    }

    if (accountRepository.findByUsername("not_owner").isEmpty()) {
      Account otherAccount = new Account();
      otherAccount.setUsername("not_owner");
      otherAccount.setPassword(passwordEncoder.encode("password"));
      otherAccount.setRoles(Set.of(playerRole));
      otherAccount.setEmail("other@test.com");
      accountRepository.save(otherAccount);

      Wrestler otherWrestler = new Wrestler();
      otherWrestler.setName("Other Wrestler");
      otherWrestler.setIsPlayer(true);
      otherWrestler.setAccount(otherAccount);
      otherWrestler.setCreationDate(clock.instant());
      otherWrestler.setExternalId("wrestler-not_owner");
      wrestlerRepository.save(otherWrestler);
    }

    if (accountRepository.findByUsername("viewer").isEmpty()) {
      Account viewerAccount = new Account();
      viewerAccount.setUsername("viewer");
      viewerAccount.setPassword(passwordEncoder.encode("password"));
      viewerAccount.setRoles(Set.of(viewerRole));
      viewerAccount.setEmail("viewer@test.com");
      accountRepository.save(viewerAccount);
    }
  }
}
