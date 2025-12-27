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
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeckServiceTest {

  @Autowired private DeckService deckService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private RoleRepository roleRepository;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
    accountRepository.deleteAll();
    roleRepository.deleteAll();

    Role bookerRole = new Role(RoleName.BOOKER, "Booker role");
    roleRepository.save(bookerRole);
    Account booker = new Account("booker", "password", "booker@test.com");
    booker.setRoles(Collections.singleton(bookerRole));
    accountRepository.save(booker);

    bookerWrestler = new Wrestler();
    bookerWrestler.setName("Booker T");
    bookerWrestler.setAccount(booker);
    bookerWrestler.setIsPlayer(true);
    wrestlerRepository.save(bookerWrestler);

    Role playerRole = new Role(RoleName.PLAYER, "Player role");
    roleRepository.save(playerRole);
    Account player = new Account("player", "password", "player@test.com");
    player.setRoles(Collections.singleton(playerRole));
    accountRepository.save(player);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Player One");
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.save(playerWrestler);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateDeck() {
    Deck deck = deckService.createDeck(bookerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanCreateTheirOwnDeck() {
    Deck deck = deckService.createDeck(playerWrestler);
    Assertions.assertNotNull(deck);
    Assertions.assertNotNull(deck.getId());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateDeckForSomeoneElse() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> deckService.createDeck(bookerWrestler));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanListDecks() {
    deckService.list(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanCountDecks() {
    deckService.count();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindAllDecks() {
    deckService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindById() {
    Deck deck = deckService.createDeck(playerWrestler);
    deckService.findById(deck.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindByWrestler() {
    deckService.findByWrestler(playerWrestler);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanSaveTheirOwnDeck() {
    Deck deck = deckService.createDeck(playerWrestler);
    deckService.save(deck);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotSaveSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.save(deck));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCanDeleteTheirOwnDeck() {
    Deck deck = deckService.createDeck(playerWrestler);
    deckService.delete(deck);
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotDeleteSomeoneElsesDeck() {
    Deck deck = new Deck();
    deck.setWrestler(bookerWrestler);
    Assertions.assertThrows(AccessDeniedException.class, () -> deckService.delete(deck));
  }
}
