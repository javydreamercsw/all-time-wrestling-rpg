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
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

class MultiWrestlerFeudServiceIT extends ManagementIntegrationTest {
  @Autowired private MultiWrestlerFeudService multiWrestlerFeudService;
  @Autowired private MultiWrestlerFeudRepository feudRepository;
  @Autowired private WrestlerService wrestlerService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    // Do NOT delete accounts to avoid breaking other tests running in parallel

    // Create test-specific accounts
    createTestAccount("feud_admin", RoleName.ADMIN);
    Account bookerAccount = createTestAccount("feud_booker", RoleName.BOOKER);
    Account playerAccount1 = createTestAccount("feud_player1", RoleName.PLAYER);
    Account playerAccount2 = createTestAccount("feud_player2", RoleName.PLAYER);
    Account playerAccount3 = createTestAccount("feud_player3", RoleName.PLAYER);

    // Create wrestlers using the service with admin privileges and distinct accounts
    TestUtils.runAsAdmin(
        () -> {
          wrestler1 =
              wrestlerService.createWrestler(
                  "Wrestler One", true, "Desc1", WrestlerTier.ROOKIE, playerAccount1);
          wrestler2 =
              wrestlerService.createWrestler(
                  "Wrestler Two", true, "Desc2", WrestlerTier.ROOKIE, playerAccount2);
          wrestlerService.createWrestler(
              "Wrestler Three", true, "Desc3", WrestlerTier.ROOKIE, playerAccount3);
          wrestlerService.createWrestler(
              "Booker Wrestler", false, "DescB", WrestlerTier.ROOKIE, bookerAccount);
        });
  }

  @Test
  @WithCustomMockUser(
      username = "feud_admin",
      roles = {"ADMIN"})
  void testAdminCanCreateFeud() {
    Assertions.assertNotNull(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId()));
    Assertions.assertTrue(feud.isPresent());
    Assertions.assertEquals(2, feud.get().getParticipants().size());
  }

  @Test
  @WithCustomMockUser(
      username = "feud_booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateFeud() {
    Assertions.assertNotNull(wrestler1.getId());
    Assertions.assertNotNull(wrestler2.getId());
    Optional<MultiWrestlerFeud> feud =
        multiWrestlerFeudService.createFeud(
            "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId()));
    Assertions.assertTrue(feud.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
  void testPlayerCannotCreateFeud() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            multiWrestlerFeudService.createFeud(
                "Test Feud", "Description", "", List.of(wrestler1.getId(), wrestler2.getId())));
  }

  @Test
  @WithCustomMockUser(
      username = "feud_admin",
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
      username = "feud_admin",
      roles = {"ADMIN"})
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
      username = "feud_admin",
      roles = {"ADMIN"})
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
  @WithCustomMockUser(username = "feud_admin", roles = "ADMIN")
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
      username = "feud_admin",
      roles = {"ADMIN"})
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
  @WithCustomMockUser(username = "feud_player1", roles = "PLAYER")
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
