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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.TestCustomUserDetailsService;
import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckCardServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private DeckCardService deckCardService;
  @Autowired private DeckCardRepository deckCardRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private TestCustomUserDetailsService userDetailsService;

  private Account ownerAccount;
  private Account notOwnerAccount;
  private Account adminAccount;
  private Account bookerAccount;
  private Account viewerAccount;

  private Wrestler ownedWrestler;
  private Wrestler unownedWrestler;

  private Deck ownedDeck;
  private Deck unownedDeck;

  private Card card;
  private CardSet cardSet;

  private DeckCard ownedDeckCard;
  private DeckCard unownedDeckCard;

  @BeforeEach
  protected void setup() {
    super.setup();
    ownerAccount = createTestAccount("owner", RoleName.PLAYER);
    ownedWrestler = createTestWrestler("owner");
    ownedWrestler.setAccount(ownerAccount);
    wrestlerRepository.save(ownedWrestler);
    ownedDeck = new Deck();
    ownedDeck.setWrestler(ownedWrestler);
    deckRepository.save(ownedDeck);

    notOwnerAccount = createTestAccount("not_owner", RoleName.PLAYER);
    unownedWrestler = createTestWrestler("not_owner");
    unownedWrestler.setAccount(notOwnerAccount);
    wrestlerRepository.save(unownedWrestler);
    unownedDeck = new Deck();
    unownedDeck.setWrestler(unownedWrestler);
    deckRepository.save(unownedDeck);

    adminAccount = createTestAccount("admin", RoleName.ADMIN);
    bookerAccount = createTestAccount("booker", RoleName.BOOKER);
    viewerAccount = createTestAccount("viewer", RoleName.VIEWER);

    cardSet = new CardSet();
    cardSet.setName("Test Set");
    cardSetRepository.save(cardSet);

    card = new Card();
    card.setName("Test Card");
    card.setSet(cardSet);
    card.setType("Test");
    card.setTarget(1);
    card.setStamina(1);
    card.setDamage(1);
    card.setMomentum(1);
    cardRepository.save(card);

    ownedDeckCard = new DeckCard();
    ownedDeckCard.setDeck(ownedDeck);
    ownedDeckCard.setCard(card);
    ownedDeckCard.setSet(cardSet);
    deckCardRepository.save(ownedDeckCard);

    unownedDeckCard = new DeckCard();
    unownedDeckCard.setDeck(unownedDeck);
    unownedDeckCard.setCard(card);
    unownedDeckCard.setSet(cardSet);
    deckCardRepository.save(unownedDeckCard);
  }

  // --- Save DeckCard Method Tests ---

  @Test
  void testAdminCanSaveAnyDeckCard() {
    login("admin");
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(unownedDeckCard));
  }

  @Test
  void testBookerCanSaveAnyDeckCard() {
    login("booker");
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(unownedDeckCard));
  }

  @Test
  void testPlayerCanSaveOwnedDeckCard() {
    login("owner");
    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
  }

  @Test
  void testPlayerCannotSaveOtherDeckCard() {
    login("owner");
    assertThrows(AuthorizationDeniedException.class, () -> deckCardService.save(unownedDeckCard));
  }

  @Test
  void testViewerCannotSaveDeckCard() {
    login("viewer");
    assertThrows(AuthorizationDeniedException.class, () -> deckCardService.save(ownedDeckCard));
  }

  // --- Delete DeckCard Method Tests ---

  @Test
  void testAdminCanDeleteAnyDeckCard() {
    login("admin");
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(unownedDeckCard));
  }

  @Test
  void testBookerCanDeleteAnyDeckCard() {
    login("booker");
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(unownedDeckCard));
  }

  @Test
  void testPlayerCanDeleteOwnedDeckCard() {
    login("owner");
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
  }

  @Test
  void testPlayerCannotDeleteOtherDeckCard() {
    login("owner");
    assertThrows(AuthorizationDeniedException.class, () -> deckCardService.delete(unownedDeckCard));
  }

  @Test
  void testViewerCannotDeleteDeckCard() {
    login("viewer");
    assertThrows(AuthorizationDeniedException.class, () -> deckCardService.delete(ownedDeckCard));
  }

  // --- Read Operations Tests ---

  @Test
  void testAdminCanReadDeckCards() {
    login("admin");
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  void testBookerCanReadDeckCards() {
    login("booker");
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  void testPlayerCanReadDeckCards() {
    login("owner");
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                unownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  void testViewerCanReadDeckCards() {
    login("viewer");
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }
}
