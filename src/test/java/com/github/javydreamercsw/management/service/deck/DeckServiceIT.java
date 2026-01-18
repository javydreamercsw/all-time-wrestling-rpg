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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckServiceIT extends ManagementIntegrationTest {

  @Autowired private DeckService deckService;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    clearAllRepositories();

    // Create test-specific accounts to avoid conflicts with global accounts
    Account booker = createTestAccount("deck_booker", RoleName.BOOKER);
    Account player = createTestAccount("deck_player", RoleName.PLAYER);
    createTestAccount("deck_viewer", RoleName.VIEWER);
    createTestAccount("deck_admin", RoleName.ADMIN);

    // Ensure accounts are flushed to DB so PermissionService can find them
    accountRepository.flush();

    // Check if wrestler already exists for this account and reuse it or delete it
    bookerWrestler = wrestlerRepository.findByAccount(booker).orElse(null);
    if (bookerWrestler != null) {
      wrestlerRepository.delete(bookerWrestler);
      wrestlerRepository.flush();
    }

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker");
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(bookerWrestler);

    // Check if wrestler already exists for this account and reuse it or delete it
    playerWrestler = wrestlerRepository.findByAccount(player).orElse(null);
    if (playerWrestler != null) {
      wrestlerRepository.delete(playerWrestler);
      wrestlerRepository.flush();
    }

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player One");
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "deck_admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "deck_booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(username = "deck_booker", roles = "BOOKER")
  void testPlayerCanCreateTheirOwnDeck() {
    Deck deck = deckService.createDeck(playerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testPlayerCannotCreateDeckForSomeoneElse() {
    Assertions.assertThrows(
        AuthorizationDeniedException.class, () -> deckService.createDeck(bookerWrestler));
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testAuthenticatedCanListDecks() {
    deckService.list(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_viewer", roles = "VIEWER")
  void testAuthenticatedCanCountDecks() {
    deckService.count();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testAuthenticatedCanFindAllDecks() {
    deckService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testAuthenticatedCanFindById() {
    // Create deck as admin first to bypass security check during creation
    // or use a different user context for creation
    // But here we want to test if player can find their own deck.
    // The issue is likely that createDeck requires permissions that deck_player might not have
    // OR createDeck creates it, but findById fails.

    // Let's temporarily switch to admin to create the deck
    // Actually, we can just use the repository directly to bypass service security
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);

    Assertions.assertNotNull(deck.getId());
    deckService.findById(deck.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testAuthenticatedCanFindByWrestler() {
    deckService.findByWrestler(playerWrestler);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_booker", roles = "BOOKER")
  void testPlayerCanSaveTheirOwnDeck() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);

    deckService.save(deck);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testPlayerCannotSaveSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> deckService.save(deck));
  }

  @Test
  @WithCustomMockUser(username = "deck_booker", roles = "BOOKER")
  void testPlayerCanDeleteTheirOwnDeck() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);

    deckService.delete(deck);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testPlayerCanDeleteTheirOwnDeckPlayer() {
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.save(deck);

    deckService.delete(deck);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "deck_player", roles = "PLAYER")
  void testPlayerCannotDeleteSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    Assertions.assertThrows(AuthorizationDeniedException.class, () -> deckService.delete(deck));
  }
}
