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

    // Setup under admin context to ensure permissions
    loginAs("admin");

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
  void testAdminCanCreateDeck() {
    loginAs("admin");
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  void testPlayerCanCreateTheirOwnDeck() {
    loginAs("player");
    Deck deck = deckService.createDeck(playerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  void testPlayerCannotCreateDeckForSomeoneElse() {
    loginAs("player");
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.createDeck(bookerWrestler));
  }

  @Test
  void testAuthenticatedCannotListDecksIfNotAdmin() {
    loginAs("viewer");
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.list(Pageable.unpaged()));
  }

  @Test
  void testAdminCanListDecks() {
    loginAs("admin");
    deckService.list(Pageable.unpaged());
  }

  @Test
  void testAuthenticatedCannotCountDecksIfNotAdmin() {
    loginAs("viewer");
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.count());
  }

  @Test
  void testAdminCanCountDecks() {
    loginAs("admin");
    deckService.count();
  }

  @Test
  void testAuthenticatedCannotFindAllDecksIfNotAdmin() {
    loginAs("player");
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.findAll());
  }

  @Test
  void testAdminCanFindAllDecks() {
    loginAs("admin");
    deckService.findAll();
  }

  @Test
  void testAuthenticatedCanFindById() {
    loginAs("player");
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    Assertions.assertNotNull(deck.getId());
    deckService.findById(deck.getId());
  }

  @Test
  void testAuthenticatedCanFindByWrestler() {
    loginAs("player");
    deckService.findByWrestler(playerWrestler);
  }

  @Test
  void testPlayerCanSaveTheirOwnDeck() {
    loginAs("player");
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.save(deck);
  }

  @Test
  void testPlayerCannotSaveSomeoneElsesDeck() {
    loginAs("player");
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.save(finalDeck));
  }

  @Test
  void testPlayerCanDeleteTheirOwnDeck() {
    loginAs("booker");
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  void testPlayerCanDeleteTheirOwnDeckPlayer() {
    loginAs("player");
    // Use repository to create initial deck
    Deck deck = new Deck();
    deck.setWrestler(playerWrestler);
    deck.setCreationDate(Instant.now());
    deck = deckRepository.saveAndFlush(deck);

    deckService.delete(deck);
  }

  @Test
  void testPlayerCannotDeleteSomeoneElsesDeck() {
    loginAs("player");
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    deck.setCreationDate(Instant.now());
    final Deck finalDeck = deckRepository.saveAndFlush(deck);

    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.delete(finalDeck));
  }
}
