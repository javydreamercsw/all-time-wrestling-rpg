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
package com.github.javydreamercsw.management.service.faction;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

class FactionServiceIT extends ManagementIntegrationTest {

  @Autowired private FactionService factionService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;
  private Faction faction;
  private Account bookerAccount;
  private Account playerAccount;
  private Universe universe;

  @BeforeEach
  void setUp() {
    universe =
        universeRepository
            .findById(1L)
            .orElseGet(
                () -> {
                  Universe u = new Universe();
                  u.setName("Test Universe");
                  return universeRepository.save(u);
                });

    // Roles should be present in the DB from migrations or a general test data setup.
    Role bookerRole =
        roleRepository
            .findByName(RoleName.BOOKER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.BOOKER, "Booker role")));
    Role playerRole =
        roleRepository
            .findByName(RoleName.PLAYER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.PLAYER, "Player role")));

    String uuid1 = UUID.randomUUID().toString();
    bookerAccount =
        new Account(
            "bk-" + uuid1, passwordEncoder.encode("password"), "booker-" + uuid1 + "@test.com");
    bookerAccount.setRoles(Set.of(bookerRole, playerRole));
    accountRepository.save(bookerAccount);

    String uuid2 = UUID.randomUUID().toString();
    playerAccount =
        new Account(
            "pl-" + uuid2, passwordEncoder.encode("password"), "player-" + uuid2 + "@test.com");
    playerAccount.setRoles(Set.of(playerRole));
    accountRepository.save(playerAccount);

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker T");
    bookerWrestler.setAccount(bookerAccount);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.save(bookerWrestler);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player One");
    playerWrestler.setAccount(playerAccount);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.save(playerWrestler);

    faction = new Faction();
    faction.setName("Test Faction " + UUID.randomUUID());
    faction.setDescription("Test Description");
    faction.setUniverse(universe);
    factionRepository.save(faction);

    // Ensure state exists for members
    wrestlerService.getOrCreateState(bookerWrestler.getId(), universe.getId());
    wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    // Rely on the robust cleanup in the parent class
    super.tearDown();
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateFaction() {
    Optional<Faction> createdFaction =
        factionService.createFaction(
            "Admin Faction", "Admin Description", bookerWrestler.getId(), universe.getId());
    Assertions.assertTrue(createdFaction.isPresent());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateFaction() {
    Optional<Faction> createdFaction =
        factionService.createFaction(
            "Booker Faction", "Booker Description", bookerWrestler.getId(), universe.getId());
    Assertions.assertTrue(createdFaction.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateFaction() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            factionService.createFaction(
                "Player Faction", "Player Description", playerWrestler.getId(), universe.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanAddMember() {
    Optional<Faction> updatedFaction =
        factionService.addMemberToFaction(faction.getId(), playerWrestler.getId());
    Assertions.assertTrue(updatedFaction.isPresent());
    WrestlerState state =
        wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
    Assertions.assertTrue(updatedFaction.get().hasMember(state.getWrestler()));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotAddMember() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> factionService.addMemberToFaction(faction.getId(), playerWrestler.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanRemoveMember() {
    WrestlerState state =
        wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
    faction.addMember(state);
    factionRepository.save(faction);

    Optional<Faction> updatedFaction =
        factionService.removeMemberFromFaction(faction.getId(), playerWrestler.getId(), "Test");
    Assertions.assertTrue(updatedFaction.isPresent());
    Assertions.assertFalse(updatedFaction.get().hasMember(state.getWrestler()));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotRemoveMember() {
    WrestlerState state =
        wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
    faction.addMember(state);
    factionRepository.save(faction);

    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            factionService.removeMemberFromFaction(
                faction.getId(), playerWrestler.getId(), "Test"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanChangeLeader() {
    WrestlerState state =
        wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
    faction.addMember(state);
    factionRepository.save(faction);

    Optional<Faction> updatedFaction =
        factionService.changeFactionLeader(faction.getId(), playerWrestler.getId());
    Assertions.assertTrue(updatedFaction.isPresent());
    Assertions.assertEquals(playerWrestler, updatedFaction.get().getLeader());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotChangeLeader() {
    WrestlerState state =
        wrestlerService.getOrCreateState(playerWrestler.getId(), universe.getId());
    faction.addMember(state);
    factionRepository.save(faction);

    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> factionService.changeFactionLeader(faction.getId(), playerWrestler.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanDisbandFaction() {
    Optional<Faction> disbandedFaction = factionService.disbandFaction(faction.getId(), "Test");
    Assertions.assertTrue(disbandedFaction.isPresent());
    Assertions.assertFalse(disbandedFaction.get().isActive());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotDisbandFaction() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> factionService.disbandFaction(faction.getId(), "Test"));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindAll() {
    factionService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetAllFactions() {
    factionService.getAllFactions();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindAllWithMembers() {
    factionService.findAllByUniverse(universe.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanFindAllWithMembersAndTeams() {
    factionService.findAllByUniverse(universe.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetAllFactionsWithPageable() {
    factionService.getAllFactions(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetFactionById() {
    factionService.getFactionById(faction.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetFactionByIdWithMembers() {
    factionService.getFactionById(faction.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetFactionByName() {
    factionService.getFactionByName(faction.getName());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetFactionForWrestler() {
    factionService.getFactionForWrestler(playerWrestler.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetFactionsWithActiveRivalries() {
    factionService.getFactionsWithActiveRivalries();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetFactionsByType() {
    factionService.getFactionsByType("singles");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetLargestFactions() {
    factionService.getLargestFactions(5);
    // No exception means success
  }
}
