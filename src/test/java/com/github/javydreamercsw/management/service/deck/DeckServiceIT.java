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
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DirtiesContext
class DeckServiceIT extends ManagementIntegrationTest {

  @Autowired private DeckService deckService;

  private String bookerUsername;
  private String playerUsername;
  private String viewerUsername;
  private String adminUsername;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    bookerUsername = "deck_booker_" + suffix;
    playerUsername = "deck_player_" + suffix;
    viewerUsername = "deck_viewer_" + suffix;
    adminUsername = "deck_admin_" + suffix;

    Account booker = createTestAccount(bookerUsername, RoleName.BOOKER);
    Account player = createTestAccount(playerUsername, RoleName.PLAYER);
    createTestAccount(viewerUsername, RoleName.VIEWER);
    createTestAccount(adminUsername, RoleName.ADMIN);

    // Ensure accounts are flushed to DB so PermissionService can find them
    accountRepository.flush();

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker " + suffix);
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(bookerWrestler);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player " + suffix);
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(playerWrestler);
  }

  @Test
  void testAdminCanCreateDeck() {
    loginAs(adminUsername);
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  void testPlayerCanCreateTheirOwnDeck() {
    loginAs(playerUsername);
    Deck deck = deckService.createDeck(playerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testPlayerCannotCreateDeckForSomeoneElse() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.createDeck(bookerWrestler));
  }

  @Test
  @WithCustomMockUser(
      username = "viewer",
      roles = {"VIEWER"})
  void testAuthenticatedCannotListDecksIfNotAdmin() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.list(Pageable.unpaged()));
  }

  @Test
  void testAdminCanListDecks() {
    loginAs(adminUsername);
    deckService.list(Pageable.unpaged());
  }

  @Test
  @WithCustomMockUser(
      username = "viewer",
      roles = {"VIEWER"})
  void testAuthenticatedCannotCountDecksIfNotAdmin() {
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.count());
  }

  @Test
  void testAdminCanCountDecks() {
    loginAs(adminUsername);
    deckService.count();
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testAuthenticatedCannotFindAllDecksIfNotAdmin() {
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.findAll());
  }

  @Test
  void testAdminCanFindAllDecks() {
    loginAs(adminUsername);
    deckService.findAll();
  }

  @Test
  void testAuthenticatedCanFindById() {
    loginAs(playerUsername);
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    Assertions.assertNotNull(deck.getId());
    deckService.findById(deck.getId());
  }

  @Test
  void testAuthenticatedCanFindByWrestler() {
    loginAs(playerUsername);
    deckService.findByWrestler(playerWrestler);
  }

  @Test
  void testPlayerCanSaveTheirOwnDeck() {
    loginAs(playerUsername);
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.save(deck);
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testPlayerCannotSaveSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.save(finalDeck));
  }

  @Test
  void testPlayerCanDeleteTheirOwnDeck() {
    loginAs(bookerUsername);
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  void testPlayerCanDeleteTheirOwnDeckPlayer() {
    loginAs(playerUsername);
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testPlayerCannotDeleteSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.delete(finalDeck));
  }
}
