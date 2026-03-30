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

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    Account booker = accountRepository.findByUsername("booker").orElseThrow();
    Account player = accountRepository.findByUsername("player").orElseThrow();

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker " + suffix);
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    bookerWrestler = wrestlerRepository.saveAndFlush(bookerWrestler);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player " + suffix);
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    playerWrestler = wrestlerRepository.saveAndFlush(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void testAdminCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "player",
      roles = {"PLAYER"})
  void testPlayerCanCreateTheirOwnDeck() {
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
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void testAdminCanListDecks() {
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
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void testAdminCanCountDecks() {
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
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void testAdminCanFindAllDecks() {
    deckService.findAll();
  }

  @Test
  @WithCustomMockUser(
      username = "player",
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
      username = "player",
      roles = {"PLAYER"})
  void testAuthenticatedCanFindByWrestler() {
    deckService.findByWrestler(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "player",
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
  @WithCustomMockUser(
      username = "booker",
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
      username = "player",
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
