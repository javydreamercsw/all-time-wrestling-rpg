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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
class DeckServiceIT extends ManagementIntegrationTest {

  @Autowired private DeckService deckService;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    // Create fixed accounts for this test class
    Account booker = createTestAccount("deck_booker", RoleName.BOOKER);
    Account player = createTestAccount("deck_player", RoleName.PLAYER);
    createTestAccount("deck_viewer", RoleName.VIEWER);
    createTestAccount("deck_admin", RoleName.ADMIN);

    // Ensure accounts are flushed to DB so PermissionService can find them
    accountRepository.flush();

    // Re-create wrestlers to ensure clean state
    wrestlerRepository.findByAccount(booker).forEach(w -> wrestlerRepository.delete(w));
    wrestlerRepository.findByAccount(player).forEach(w -> wrestlerRepository.delete(w));
    wrestlerRepository.flush();

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker");
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(bookerWrestler);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player One");
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_admin",
      roles = {"ADMIN"})
  void testAdminCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCanCreateTheirOwnDeck() {
    Deck deck = deckService.createDeck(playerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCannotCreateDeckForSomeoneElse() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.createDeck(bookerWrestler));
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testAuthenticatedCanListDecks() {
    deckService.list(Pageable.unpaged());
  }

  @Test
  @WithCustomMockUser(
      username = "deck_viewer",
      roles = {"VIEWER"})
  void testAuthenticatedCanCountDecks() {
    deckService.count();
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testAuthenticatedCanFindAllDecks() {
    deckService.findAll();
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testAuthenticatedCanFindById() {
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    Assertions.assertNotNull(deck.getId());
    deckService.findById(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testAuthenticatedCanFindByWrestler() {
    deckService.findByWrestler(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCanSaveTheirOwnDeck() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.save(deck);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCannotSaveSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.save(finalDeck));
  }

  @Test
  @WithCustomMockUser(
      username = "deck_booker",
      roles = {"BOOKER"})
  void testPlayerCanDeleteTheirOwnDeck() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCanDeleteTheirOwnDeckPlayer() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_player",
      roles = {"PLAYER"})
  void testPlayerCannotDeleteSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.delete(finalDeck));
  }
}
