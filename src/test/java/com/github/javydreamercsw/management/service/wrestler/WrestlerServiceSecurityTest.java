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
package com.github.javydreamercsw.management.service.wrestler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.WithMockCustomUser;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ContextConfiguration(classes = TestSecurityConfig.class)
@Transactional
class WrestlerServiceSecurityTest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler ownedWrestler;
  private Wrestler otherWrestler;
  private Account ownerAccount;

  @BeforeEach
  void setUp() {
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

    ownerAccount = new Account();
    ownerAccount.setUsername("owner");
    ownerAccount.setPassword(passwordEncoder.encode("password"));
    ownerAccount.setRoles(Set.of(playerRole));
    ownerAccount.setEmail("owner@test.com");
    accountRepository.save(ownerAccount);

    ownedWrestler = new Wrestler();
    ownedWrestler.setName("Owned Wrestler");
    ownedWrestler.setIsPlayer(true);
    ownedWrestler.setAccount(ownerAccount);
    wrestlerRepository.save(ownedWrestler);

    otherWrestler = new Wrestler();
    otherWrestler.setName("Other Wrestler");
    wrestlerRepository.save(otherWrestler);
  }

  // --- Save Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanSaveAnyWrestler() {
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanSaveAnyWrestler() {
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
    assertDoesNotThrow(() -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanSaveOwnedWrestler() {
    assertNotNull(ownedWrestler);
    assertDoesNotThrow(() -> wrestlerService.save(ownedWrestler));
  }

  @Test
  @WithMockCustomUser(username = "not_owner", roles = "PLAYER")
  void testPlayerCannotSaveOtherWrestler() {
    assertNotNull(ownedWrestler);
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(ownedWrestler));
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotSaveWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.save(ownedWrestler));
  }

  // --- Delete Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanDeleteWrestler() {
    assertDoesNotThrow(() -> wrestlerService.delete(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanDeleteWrestler() {
    assertDoesNotThrow(() -> wrestlerService.delete(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "PLAYER")
  void testPlayerCannotDeleteWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.delete(ownedWrestler));
    assertThrows(AccessDeniedException.class, () -> wrestlerService.delete(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotDeleteWrestler() {
    assertThrows(AccessDeniedException.class, () -> wrestlerService.delete(ownedWrestler));
  }
}
