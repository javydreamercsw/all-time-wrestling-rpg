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

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

class FactionServiceIT extends ManagementIntegrationTest {

  @Autowired private FactionService factionService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private AccountInitializer accountInitializer;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;
  private Faction faction;

  @BeforeEach
  void setUp() {
    databaseCleaner.clearRepositories();
    accountInitializer.init();

    Account booker = accountRepository.findByUsername("booker").orElseThrow();
    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker T");
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.save(bookerWrestler);

    Account player = accountRepository.findByUsername("player").orElseThrow();
    playerWrestler = new Wrestler();
    playerWrestler.setName("Player One");
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.save(playerWrestler);

    faction = new Faction();
    faction.setName("Test Faction");
    faction.setDescription("Test Description");
    factionRepository.save(faction);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateFaction() {
    Optional<Faction> createdFaction =
        factionService.createFaction("Admin Faction", "Admin Description", bookerWrestler.getId());
    Assertions.assertTrue(createdFaction.isPresent());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateFaction() {
    Optional<Faction> createdFaction =
        factionService.createFaction(
            "Booker Faction", "Booker Description", bookerWrestler.getId());
    Assertions.assertTrue(createdFaction.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateFaction() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            factionService.createFaction(
                "Player Faction", "Player Description", playerWrestler.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanAddMember() {
    Optional<Faction> updatedFaction =
        factionService.addMemberToFaction(faction.getId(), playerWrestler.getId());
    Assertions.assertTrue(updatedFaction.isPresent());
    Assertions.assertTrue(updatedFaction.get().hasMember(playerWrestler));
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
    TestUtils.runAsAdmin(
        () -> factionService.addMemberToFaction(faction.getId(), playerWrestler.getId()));
    Optional<Faction> updatedFaction =
        factionService.removeMemberFromFaction(faction.getId(), playerWrestler.getId(), "Test");
    Assertions.assertTrue(updatedFaction.isPresent());
    Assertions.assertFalse(updatedFaction.get().hasMember(playerWrestler));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotRemoveMember() {
    TestUtils.runAsAdmin(
        () -> factionService.addMemberToFaction(faction.getId(), playerWrestler.getId()));
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
    TestUtils.runAsAdmin(
        () -> factionService.addMemberToFaction(faction.getId(), playerWrestler.getId()));
    Optional<Faction> updatedFaction =
        factionService.changeFactionLeader(faction.getId(), playerWrestler.getId());
    Assertions.assertTrue(updatedFaction.isPresent());
    Assertions.assertEquals(playerWrestler, updatedFaction.get().getLeader());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotChangeLeader() {
    TestUtils.runAsAdmin(
        () -> factionService.addMemberToFaction(faction.getId(), playerWrestler.getId()));
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
    Assertions.assertFalse(disbandedFaction.get().getIsActive());
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
    factionService.findAllWithMembers();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanFindAllWithMembersAndTeams() {
    factionService.findAllWithMembersAndTeams();
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
    factionService.getFactionByIdWithMembers(faction.getId());
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

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanHaveRivalry() {
    Faction other = new Faction();
    other.setName("Other Faction");
    factionRepository.save(other);
    factionService.canHaveRivalry(faction.getId(), other.getId());
    // No exception means success
  }
}
