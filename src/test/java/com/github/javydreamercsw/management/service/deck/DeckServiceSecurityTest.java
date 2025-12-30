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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.TestCustomUserDetailsService;
import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private DeckService deckService;
  @Autowired private DeckRepository deckRepository;
  @Autowired private TestCustomUserDetailsService userDetailsService;

  private Wrestler ownedWrestler;
  private Wrestler unownedWrestler;

  private Deck ownedDeck;
  private Deck unownedDeck;

  @BeforeEach
  protected void setup() {
    super.setup();
    Account ownerAccount = createTestAccount("owner", RoleName.PLAYER);
    ownedWrestler = createTestWrestler("owner");
    ownedWrestler.setAccount(ownerAccount);
    wrestlerRepository.save(ownedWrestler);
    ownedDeck = new Deck();
    ownedDeck.setWrestler(ownedWrestler);
    deckRepository.save(ownedDeck);

    Account notOwnerAccount = createTestAccount("not_owner", RoleName.PLAYER);
    unownedWrestler = createTestWrestler("not_owner");
    unownedWrestler.setAccount(notOwnerAccount);
    wrestlerRepository.save(unownedWrestler);
    unownedDeck = new Deck();
    unownedDeck.setWrestler(unownedWrestler);
    deckRepository.save(unownedDeck);

    createTestAccount("admin", RoleName.ADMIN);
    createTestAccount("booker", RoleName.BOOKER);
    createTestAccount("viewer", RoleName.VIEWER);
  }

  // --- Create Deck Method Tests ---

  @Test
  void testAdminCanCreateDeckForAnyWrestler() {
    login("admin");
    assertDoesNotThrow(() -> deckService.createDeck(ownedWrestler));
  }

  @Test
  void testBookerCanCreateDeckForAnyWrestler() {
    login("booker");
    assertDoesNotThrow(() -> deckService.createDeck(ownedWrestler));
  }

  @Test
  void testPlayerCanCreateDeckForOwnedWrestler() {
    login("owner");
    assertDoesNotThrow(() -> deckService.createDeck(ownedWrestler));
  }

  @Test
  void testPlayerCannotCreateDeckForOtherWrestler() {
    login("owner");
    assertThrows(AuthorizationDeniedException.class, () -> deckService.createDeck(unownedWrestler));
  }

  @Test
  void testViewerCannotCreateDeck() {
    login("viewer");
    assertThrows(AuthorizationDeniedException.class, () -> deckService.createDeck(ownedWrestler));
  }

  // --- Save Deck Method Tests ---

  @Test
  void testAdminCanSaveAnyDeck() {
    login("admin");
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
    assertDoesNotThrow(() -> deckService.save(unownedDeck));
  }

  @Test
  void testBookerCanSaveAnyDeck() {
    login("booker");
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
    assertDoesNotThrow(() -> deckService.save(unownedDeck));
  }

  @Test
  void testPlayerCanSaveOwnedDeck() {
    login("owner");
    assertNotNull(ownedDeck);
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
  }

  @Test
  void testPlayerCannotSaveOtherDeck() {
    login("owner");
    assertNotNull(ownedDeck);
    assertThrows(AuthorizationDeniedException.class, () -> deckService.save(unownedDeck));
  }

  @Test
  void testViewerCannotSaveDeck() {
    login("viewer");
    assertThrows(AuthorizationDeniedException.class, () -> deckService.save(ownedDeck));
  }

  // --- Delete Deck Method Tests ---

  @Test
  void testAdminCanDeleteDeck() {
    login("admin");
    assertDoesNotThrow(() -> deckService.delete(unownedDeck));
  }

  @Test
  void testBookerCanDeleteDeck() {
    login("booker");
    assertDoesNotThrow(() -> deckService.delete(unownedDeck));
  }

  @Test
  void testPlayerCanDeleteOwnDeckButNotOthers() {
    login("owner");
    assertDoesNotThrow(() -> deckService.delete(ownedDeck));
    assertThrows(AuthorizationDeniedException.class, () -> deckService.delete(unownedDeck));
  }

  @Test
  void testViewerCannotDeleteDeck() {
    login("viewer");
    assertThrows(AuthorizationDeniedException.class, () -> deckService.delete(ownedDeck));
  }

  // --- FindById Method Tests (Read Operations) ---

  @Test
  void testAdminCanFindDeckById() {
    login("admin");
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
  }

  @Test
  void testBookerCanFindDeckById() {
    login("booker");
    assertDoesNotThrow(() -> deckService.findById(ownedDeck.getId()));
  }

  @Test
  void testPlayerCanFindDeckById() {
    login("owner");
    assertDoesNotThrow(
        () -> {
          assert ownedDeck.getId() != null;
          return deckService.findById(ownedDeck.getId());
        });
    assertDoesNotThrow(
        () -> {
          assert unownedDeck.getId() != null;
          return deckService.findById(unownedDeck.getId());
        });
  }

  @Test
  void testViewerCanFindDeckById() {
    login("viewer");
    assertDoesNotThrow(
        () -> {
          assert ownedDeck.getId() != null;
          return deckService.findById(ownedDeck.getId());
        });
  }

  // --- FindAll Method Tests (Read Operations) ---

  @Test
  void testAdminCanFindAllDecks() {
    login("admin");
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  void testBookerCanFindAllDecks() {
    login("booker");
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  void testPlayerCanFindAllDecks() {
    login("owner");
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  void testViewerCanFindAllDecks() {
    login("viewer");
    assertDoesNotThrow(() -> deckService.findAll());
  }
}
