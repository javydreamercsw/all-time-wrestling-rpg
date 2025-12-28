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
package com.github.javydreamercsw.management.service.feud;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MultiWrestlerFeudServiceIT extends AbstractIntegrationTest {
  @Autowired private MultiWrestlerFeudService multiWrestlerFeudService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private MultiWrestlerFeudRepository feudRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private Wrestler bookerWrestler;

  @BeforeEach
  void setUp() {
    factionRepository.deleteAll();
    feudRepository.deleteAll();
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
    accountRepository.deleteAll();
    roleRepository.deleteAll();

    // Create roles
    Role adminRole = new Role(RoleName.ADMIN, "Admin role");
    roleRepository.save(adminRole);
    Role bookerRole = new Role(RoleName.BOOKER, "Booker role");
    roleRepository.save(bookerRole);
    Role playerRole = new Role(RoleName.PLAYER, "Player role");
    roleRepository.save(playerRole);

    // Create accounts with ValidPassword1!
    Account adminAccount =
        new Account("admin", passwordEncoder.encode("ValidPassword1!"), "admin@test.com");
    adminAccount.setRoles(Collections.singleton(adminRole));
    accountRepository.save(adminAccount);

    Account bookerAccount =
        new Account("booker", passwordEncoder.encode("ValidPassword1!"), "booker@test.com");
    bookerAccount.setRoles(Collections.singleton(bookerRole));
    accountRepository.save(bookerAccount);

    // Create distinct player accounts for each wrestler
    Account playerAccount1 =
        new Account("player1", passwordEncoder.encode("ValidPassword1!"), "player1@test.com");
    playerAccount1.setRoles(Collections.singleton(playerRole));
    accountRepository.save(playerAccount1);

    Account playerAccount2 =
        new Account("player2", passwordEncoder.encode("ValidPassword1!"), "player2@test.com");
    playerAccount2.setRoles(Collections.singleton(playerRole));
    accountRepository.save(playerAccount2);

    Account playerAccount3 =
        new Account("player3", passwordEncoder.encode("ValidPassword1!"), "player3@test.com");
    playerAccount3.setRoles(Collections.singleton(playerRole));
    accountRepository.save(playerAccount3);

    // Create wrestlers using the service with admin privileges and distinct accounts
    TestUtils.runAsAdmin(
        () -> {
          wrestler1 =
              wrestlerService.createWrestler(
                  "Wrestler One", true, "Desc1", WrestlerTier.ROOKIE, playerAccount1);
          wrestler2 =
              wrestlerService.createWrestler(
                  "Wrestler Two", true, "Desc2", WrestlerTier.ROOKIE, playerAccount2);
          wrestler3 =
              wrestlerService.createWrestler(
                  "Wrestler Three", true, "Desc3", WrestlerTier.ROOKIE, playerAccount3);
          bookerWrestler =
              wrestlerService.createWrestler(
                  "Booker Wrestler", false, "DescB", WrestlerTier.ROOKIE, bookerAccount);
        });
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateFeud() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId()));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertEquals(2, feud.get().getParticipants().size());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateFeud() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId()));
    Assertions.assertTrue(feud.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateFeud() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            multiWrestlerFeudService.createFeud(
                "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId())));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanAddParticipant() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId()));
    Assertions.assertTrue(feud.isPresent());
    Optional<MultiWrestlerFeud> updatedFeud =
        multiWrestlerFeudService.addParticipant(
            feud.get().getId(), wrestler2.getId(), FeudRole.PROTAGONIST);
    Assertions.assertTrue(updatedFeud.isPresent());
    Assertions.assertEquals(2, updatedFeud.get().getParticipants().size());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotAddParticipant() {
    Optional<MultiWrestlerFeud> feud =
        TestUtils.runAsAdmin(
            () ->
                multiWrestlerFeudService.createFeud(
                    "Test Feud", "Description", "", List.of(wrestler1.getId())));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            multiWrestlerFeudService.addParticipant(
                feud.get().getId(), wrestler2.getId(), FeudRole.PROTAGONIST));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanRemoveParticipant() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId()));
    Assertions.assertTrue(feud.isPresent());
    Optional<MultiWrestlerFeud> updatedFeud =
        multiWrestlerFeudService.removeParticipant(
            Objects.requireNonNull(feud.get().getId()), wrestler2.getId(), "Reason");
    Assertions.assertTrue(updatedFeud.isPresent());
    Assertions.assertEquals(1, updatedFeud.get().getActiveParticipantCount());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotRemoveParticipant() {
    Optional<MultiWrestlerFeud> feud =
        TestUtils.runAsAdmin(
            () ->
                multiWrestlerFeudService.createFeud(
                    "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId())));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            multiWrestlerFeudService.removeParticipant(
                feud.get().getId(), wrestler2.getId(), "Reason"));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotRemoveOwnWrestlerFromFeud() {
    // Create a feud as admin with player1's wrestler (wrestler1) and another wrestler (wrestler2)
    Optional<MultiWrestlerFeud> feud =
        TestUtils.runAsAdmin(
            () ->
                multiWrestlerFeudService.createFeud(
                    "Player Feud",
                    "Description",
                    "",
                    List.of(wrestler1.getId(), wrestler2.getId())));
    Assertions.assertTrue(feud.isPresent());

    // Attempt to remove wrestler1 (owned by player1) from the feud as player1
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            multiWrestlerFeudService.removeParticipant(
                feud.get().getId(), wrestler1.getId(), "Player trying to remove own wrestler"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanEndFeud() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId()));
    Assertions.assertTrue(feud.isPresent());
    Optional<MultiWrestlerFeud> updatedFeud =
        multiWrestlerFeudService.endFeud(feud.get().getId(), "Ended for testing");
    Assertions.assertTrue(updatedFeud.isPresent());
    Assertions.assertFalse(updatedFeud.get().getIsActive());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotEndFeud() {
    Optional<MultiWrestlerFeud> feud =
        TestUtils.runAsAdmin(
            () ->
                multiWrestlerFeudService.createFeud(
                    "Test Feud", "Description", "", List.of(wrestler1.getId())));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> multiWrestlerFeudService.endFeud(feud.get().getId(), "Ended for testing"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanDeleteFeud() {
    final Long[] feudId = new Long[1];
    TestUtils.runAsAdmin(
        () -> {
          Optional<MultiWrestlerFeud> createdFeud =
              multiWrestlerFeudService.createFeud(
                  "Test Feud", "Description", "", List.of(wrestler1.getId()));
          createdFeud.ifPresent(f -> feudId[0] = f.getId());
        });
    multiWrestlerFeudService.deleteFeud(feudId[0]);
    Assertions.assertTrue(feudRepository.findById(feudId[0]).isEmpty());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotDeleteFeud() {
    final Long[] feudId = new Long[1];
    TestUtils.runAsAdmin(
        () -> {
          Optional<MultiWrestlerFeud> createdFeud =
              multiWrestlerFeudService.createFeud(
                  "Test Feud", "Description", "", List.of(wrestler1.getId()));
          createdFeud.ifPresent(f -> feudId[0] = f.getId());
        });
    Assertions.assertThrows(
        AccessDeniedException.class, () -> multiWrestlerFeudService.deleteFeud(feudId[0]));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetAllFeuds() {
    TestUtils.runAsAdmin(
        () ->
            multiWrestlerFeudService.createFeud(
                "Test Feud", "Description", "", List.of(wrestler1.getId())));
    multiWrestlerFeudService.getAllFeuds(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetFeudById() {
    final Long[] feudId = new Long[1];
    TestUtils.runAsAdmin(
        () -> {
          Optional<MultiWrestlerFeud> createdFeud =
              multiWrestlerFeudService.createFeud(
                  "Test Feud", "Description", "", List.of(wrestler1.getId()));
          createdFeud.ifPresent(f -> feudId[0] = f.getId());
        });
    multiWrestlerFeudService.getFeudById(feudId[0]);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetFeudsForWrestler() {
    TestUtils.runAsAdmin(
        () ->
            multiWrestlerFeudService.createFeud(
                "Test Feud", "Description", "", List.of(wrestler1.getId())));
    multiWrestlerFeudService.getActiveFeudsForWrestler(wrestler1.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetActiveFeuds() {
    TestUtils.runAsAdmin(
        () ->
            multiWrestlerFeudService.createFeud(
                "Test Feud", "Description", "", List.of(wrestler1.getId())));
    multiWrestlerFeudService.getActiveFeuds();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanAddHeat() {
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId()));
    Assertions.assertTrue(feud.isPresent());
    Optional<MultiWrestlerFeud> updatedFeud =
        multiWrestlerFeudService.addHeat(feud.get().getId(), 10, "Heated rivalry");
    Assertions.assertTrue(updatedFeud.isPresent());
    Assertions.assertEquals(10, updatedFeud.get().getHeat());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotAddHeat() {
    Optional<MultiWrestlerFeud> feud =
        TestUtils.runAsAdmin(
            () ->
                multiWrestlerFeudService.createFeud(
                    "Test Feud", "Description", "", List.of(wrestler1.getId())));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> multiWrestlerFeudService.addHeat(feud.get().getId(), 10, "Heated rivalry"));
  }
}
