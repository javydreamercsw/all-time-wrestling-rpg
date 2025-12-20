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
package com.github.javydreamercsw.management.service.deck;

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
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Clock;
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
class DeckServiceSecurityTest {

  @Autowired private DeckService deckService;
  @Autowired private DeckRepository deckRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private Clock clock;

  private Deck ownedDeck;
  private Deck otherDeck;
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

    // Create an owned wrestler and an owned deck associated with it
    ownedWrestler = new Wrestler();
    ownedWrestler.setName("Owned Wrestler");
    ownedWrestler.setIsPlayer(true);
    ownedWrestler.setAccount(ownerAccount);
    ownedWrestler.setCreationDate(clock.instant());
    wrestlerRepository.save(ownedWrestler);

    ownedDeck = new Deck();
    ownedDeck.setWrestler(ownedWrestler);
    ownedDeck.setCreationDate(clock.instant());
    deckRepository.save(ownedDeck);

    // Create another wrestler and deck that are not owned by the test user
    otherWrestler = new Wrestler();
    otherWrestler.setName("Other Wrestler");
    otherWrestler.setCreationDate(clock.instant());
    wrestlerRepository.save(otherWrestler);

    otherDeck = new Deck();
    otherDeck.setWrestler(otherWrestler);
    otherDeck.setCreationDate(clock.instant());
    deckRepository.save(otherDeck);
  }

  // --- Create Deck Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanCreateDeckForAnyWrestler() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Admin Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertDoesNotThrow(() -> deckService.createDeck(newWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanCreateDeckForAnyWrestler() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Booker Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertDoesNotThrow(() -> deckService.createDeck(newWrestler));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanCreateDeckForOwnedWrestler() {
    assertDoesNotThrow(() -> deckService.createDeck(ownedWrestler));
  }

  @Test
  @WithMockCustomUser(username = "not_owner", roles = "PLAYER")
  void testPlayerCannotCreateDeckForOtherWrestler() {
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(ownedWrestler));
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(otherWrestler));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotCreateDeck() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Viewer Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(newWrestler));
  }

  // --- Save Deck Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanSaveAnyDeck() {
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
    assertDoesNotThrow(() -> deckService.save(otherDeck));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanSaveAnyDeck() {
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
    assertDoesNotThrow(() -> deckService.save(otherDeck));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanSaveOwnedDeck() {
    assertNotNull(ownedDeck);
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
  }

  @Test
  @WithMockCustomUser(username = "not_owner", roles = "PLAYER")
  void testPlayerCannotSaveOtherDeck() {
    assertNotNull(ownedDeck);
    assertThrows(AccessDeniedException.class, () -> deckService.save(ownedDeck));
    assertThrows(AccessDeniedException.class, () -> deckService.save(otherDeck));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotSaveDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.save(ownedDeck));
  }

  // --- Delete Deck Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanDeleteDeck() {
    assertDoesNotThrow(() -> deckService.delete(otherDeck));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanDeleteDeck() {
    assertDoesNotThrow(() -> deckService.delete(otherDeck));
  }

  @Test
  @WithMockCustomUser(roles = "PLAYER")
  void testPlayerCannotDeleteDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.delete(ownedDeck));
    assertThrows(AccessDeniedException.class, () -> deckService.delete(otherDeck));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotDeleteDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.delete(ownedDeck));
  }

  // --- FindById Method Tests (Read Operations) ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
    assertDoesNotThrow(() -> deckService.findById(otherDeck.getId())); // Player can view all decks
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
  }

  // --- FindAll Method Tests (Read Operations) ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }
}
