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

import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;

class DeckServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private DeckService deckService;
  @Autowired private DeckRepository deckRepository;

  private Wrestler getOwnerWrestler() {
    return wrestlerRepository
        .findByAccountUsername("owner")
        .orElseThrow(() -> new IllegalStateException("Owner wrestler not found"));
  }

  private Wrestler getOtherWrestler() {
    return wrestlerRepository
        .findByAccountUsername("not_owner")
        .orElseThrow(() -> new IllegalStateException("Other wrestler not found"));
  }

  private Deck getOwnedDeck() {
    Wrestler ownerWrestler = getOwnerWrestler();
    List<Deck> decks = deckRepository.findByWrestler(ownerWrestler);
    if (decks.isEmpty()) {
      Deck newDeck = new Deck();
      newDeck.setWrestler(ownerWrestler);
      newDeck.setCreationDate(clock.instant());
      return deckRepository.save(newDeck);
    }
    return decks.get(0);
  }

  private Deck getOtherDeck() {
    Wrestler otherWrestler = getOtherWrestler();
    List<Deck> decks = deckRepository.findByWrestler(otherWrestler);
    if (decks.isEmpty()) {
      Deck newDeck = new Deck();
      newDeck.setWrestler(otherWrestler);
      newDeck.setCreationDate(clock.instant());
      return deckRepository.save(newDeck);
    }
    return decks.get(0);
  }

  // --- Create Deck Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanCreateDeckForAnyWrestler() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Admin Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertDoesNotThrow(() -> deckService.createDeck(newWrestler));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanCreateDeckForAnyWrestler() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Booker Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertDoesNotThrow(() -> deckService.createDeck(newWrestler));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanCreateDeckForOwnedWrestler() {
    assertDoesNotThrow(() -> deckService.createDeck(getOwnerWrestler()));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotCreateDeckForOtherWrestler() {
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(getOwnerWrestler()));
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(getOtherWrestler()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotCreateDeck() {
    Wrestler newWrestler = new Wrestler();
    newWrestler.setName("Viewer Created Wrestler");
    wrestlerRepository.save(newWrestler);
    assertThrows(AccessDeniedException.class, () -> deckService.createDeck(newWrestler));
  }

  // --- Save Deck Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanSaveAnyDeck() {
    assertDoesNotThrow(() -> deckService.save(getOwnedDeck()));
    assertDoesNotThrow(() -> deckService.save(getOtherDeck()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanSaveAnyDeck() {
    assertDoesNotThrow(() -> deckService.save(getOwnedDeck()));
    assertDoesNotThrow(() -> deckService.save(getOtherDeck()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanSaveOwnedDeck() {
    Deck ownedDeck = getOwnedDeck();
    assertNotNull(ownedDeck);
    assertDoesNotThrow(() -> deckService.save(ownedDeck));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotSaveOtherDeck() {
    Deck ownedDeck = getOwnedDeck();
    Deck otherDeck = getOtherDeck();
    assertNotNull(ownedDeck);
    assertThrows(AccessDeniedException.class, () -> deckService.save(ownedDeck));
    assertThrows(AccessDeniedException.class, () -> deckService.save(otherDeck));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotSaveDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.save(getOwnedDeck()));
  }

  // --- Delete Deck Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanDeleteDeck() {
    assertDoesNotThrow(() -> deckService.delete(getOtherDeck()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanDeleteDeck() {
    assertDoesNotThrow(() -> deckService.delete(getOtherDeck()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCannotDeleteDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.delete(getOwnedDeck()));
    assertThrows(AccessDeniedException.class, () -> deckService.delete(getOtherDeck()));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotDeleteDeck() {
    assertThrows(AccessDeniedException.class, () -> deckService.delete(getOwnedDeck()));
  }

  // --- FindById Method Tests (Read Operations) ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(getOwnedDeck().getId()));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(getOwnedDeck().getId()));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(getOwnedDeck().getId()));
    assertDoesNotThrow(
        () -> deckService.findById(getOtherDeck().getId())); // Player can view all decks
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindDeckById() {
    assertDoesNotThrow(() -> deckService.findById(getOwnedDeck().getId()));
  }

  // --- FindAll Method Tests (Read Operations) ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanFindAllDecks() {
    assertDoesNotThrow(() -> deckService.findAll());
  }
}
