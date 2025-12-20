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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.WithMockCustomUser;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
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
class DeckCardServiceSecurityTest {

  @Autowired private DeckCardService deckCardService;
  @Autowired private DeckCardRepository deckCardRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private Clock clock;

  private DeckCard ownedDeckCard;
  private DeckCard otherDeckCard;
  private Deck ownedDeck;
  private Deck otherDeck;
  private Card testCard;
  private CardSet testCardSet;

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

    Account ownerAccount = new Account();
    ownerAccount.setUsername("owner");
    ownerAccount.setPassword(passwordEncoder.encode("password"));
    ownerAccount.setRoles(Set.of(playerRole));
    ownerAccount.setEmail("owner@test.com");
    accountRepository.save(ownerAccount);

    testCardSet = new CardSet();
    testCardSet.setName("Test Set");
    testCardSet.setCreationDate(clock.instant());
    testCardSet.setSetCode("TEST_SET_CODE");
    cardSetRepository.save(testCardSet);

    testCard = new Card();
    testCard.setName("Test Card");
    testCard.setSet(testCardSet);
    testCard.setType("Test");
    testCard.setTarget(1);
    testCard.setStamina(1);
    testCard.setDamage(1);
    testCard.setMomentum(1);
    testCard.setCreationDate(clock.instant());
    cardRepository.save(testCard);

    // Create an owned wrestler, deck, and deck card associated with it
    Wrestler ownedWrestler = new Wrestler();
    ownedWrestler.setName("Owned Wrestler");
    ownedWrestler.setIsPlayer(true);
    ownedWrestler.setAccount(ownerAccount);
    ownedWrestler.setCreationDate(clock.instant());
    wrestlerRepository.save(ownedWrestler);

    ownedDeck = new Deck();
    ownedDeck.setWrestler(ownedWrestler);
    ownedDeck.setCreationDate(clock.instant());
    deckRepository.save(ownedDeck);

    ownedDeckCard = new DeckCard();
    ownedDeckCard.setDeck(ownedDeck);
    ownedDeckCard.setCard(testCard);
    ownedDeckCard.setSet(testCardSet);
    deckCardRepository.save(ownedDeckCard);

    // Create another wrestler, deck, and deck card not owned by the test user
    Wrestler otherWrestler = new Wrestler();
    otherWrestler.setName("Other Wrestler");
    otherWrestler.setCreationDate(clock.instant());
    wrestlerRepository.save(otherWrestler);

    otherDeck = new Deck();
    otherDeck.setWrestler(otherWrestler);
    otherDeck.setCreationDate(clock.instant());
    deckRepository.save(otherDeck);

    otherDeckCard = new DeckCard();
    otherDeckCard.setDeck(otherDeck);
    otherDeckCard.setCard(testCard);
    otherDeckCard.setSet(testCardSet);
    deckCardRepository.save(otherDeckCard);
  }

  // --- Save DeckCard Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanSaveAnyDeckCard() {
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanSaveAnyDeckCard() {
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanSaveOwnedDeckCard() {
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
  }

  @Test
  @WithMockCustomUser(username = "not_owner", roles = "PLAYER")
  void testPlayerCannotSaveOtherDeckCard() {
    assertThrows(AccessDeniedException.class, () -> deckCardService.save(ownedDeckCard));
    assertThrows(AccessDeniedException.class, () -> deckCardService.save(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotSaveDeckCard() {
    assertThrows(AccessDeniedException.class, () -> deckCardService.save(ownedDeckCard));
  }

  // --- Delete DeckCard Method Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanDeleteAnyDeckCard() {
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanDeleteAnyDeckCard() {
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanDeleteOwnedDeckCard() {
    // Players can delete their own deck cards
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
  }

  @Test
  @WithMockCustomUser(username = "not_owner", roles = "PLAYER")
  void testPlayerCannotDeleteOtherDeckCard() {
    assertThrows(AccessDeniedException.class, () -> deckCardService.delete(ownedDeckCard));
    assertThrows(AccessDeniedException.class, () -> deckCardService.delete(otherDeckCard));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCannotDeleteDeckCard() {
    assertThrows(AccessDeniedException.class, () -> deckCardService.delete(ownedDeckCard));
  }

  // --- Read Operations Tests ---

  @Test
  @WithMockCustomUser(roles = "ADMIN")
  void testAdminCanReadDeckCards() {
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), testCard.getId(), testCardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithMockCustomUser(roles = "BOOKER")
  void testBookerCanReadDeckCards() {
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), testCard.getId(), testCardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithMockCustomUser(username = "owner", roles = "PLAYER")
  void testPlayerCanReadDeckCards() {
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), testCard.getId(), testCardSet.getId()));
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                otherDeck.getId(), testCard.getId(), testCardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithMockCustomUser(roles = "VIEWER")
  void testViewerCanReadDeckCards() {
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), testCard.getId(), testCardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }
}
