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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;

class DeckCardServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private DeckCardService deckCardService;
  @Autowired private DeckCardRepository deckCardRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;

  private Wrestler getWrestler(String username) {
    return wrestlerRepository
        .findByAccountUsername(username)
        .orElseThrow(
            () -> new IllegalStateException("Wrestler for user " + username + " not found"));
  }

  private Deck getOrCreateDeck(Wrestler wrestler) {
    List<Deck> decks = deckRepository.findByWrestler(wrestler);
    if (decks.isEmpty()) {
      Deck newDeck = new Deck();
      newDeck.setWrestler(wrestler);
      newDeck.setCreationDate(clock.instant());
      return deckRepository.save(newDeck);
    }
    return decks.get(0);
  }

  private CardSet getOrCreateCardSet() {
    return cardSetRepository
        .findByName("Test Set")
        .orElseGet(
            () -> {
              CardSet newCardSet = new CardSet();
              newCardSet.setName("Test Set");
              newCardSet.setCreationDate(clock.instant());
              newCardSet.setCode("TEST_SET_CODE");
              return cardSetRepository.save(newCardSet);
            });
  }

  private Card getOrCreateCard(CardSet cardSet) {
    return cardRepository
        .findByName("Test Card")
        .orElseGet(
            () -> {
              Card newCard = new Card();
              newCard.setName("Test Card");
              newCard.setSet(cardSet);
              newCard.setType("Test");
              newCard.setTarget(1);
              newCard.setStamina(1);
              newCard.setDamage(1);
              newCard.setMomentum(1);
              newCard.setCreationDate(clock.instant());
              return cardRepository.save(newCard);
            });
  }

  private DeckCard getOrCreateDeckCard(Deck deck, Card card, CardSet cardSet) {
    return deckCardRepository
        .findByDeckAndCardAndSet(deck, card, cardSet)
        .orElseGet(
            () -> {
              DeckCard newDeckCard = new DeckCard();
              newDeckCard.setDeck(deck);
              newDeckCard.setCard(card);
              newDeckCard.setSet(cardSet);
              return deckCardRepository.save(newDeckCard);
            });
  }

  // --- Save DeckCard Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanSaveAnyDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    Deck otherDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherDeckCard = getOrCreateDeckCard(otherDeck, card, cardSet);

    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(otherDeckCard));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanSaveAnyDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    Deck otherDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherDeckCard = getOrCreateDeckCard(otherDeck, card, cardSet);

    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(otherDeckCard));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanSaveOwnedDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertDoesNotThrow(() -> deckCardService.save(ownedDeckCard));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCannotSaveOtherDeckCard() {
    Deck currentPlayerDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard currentPlayerDeckCard = getOrCreateDeckCard(currentPlayerDeck, card, cardSet);

    Deck otherPlayerDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherPlayerDeckCard = getOrCreateDeckCard(otherPlayerDeck, card, cardSet);

    assertThrows(AccessDeniedException.class, () -> deckCardService.save(otherPlayerDeckCard));
    assertDoesNotThrow(() -> deckCardService.save(currentPlayerDeckCard));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotSaveDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertThrows(AccessDeniedException.class, () -> deckCardService.save(ownedDeckCard));
  }

  // --- Delete DeckCard Method Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanDeleteAnyDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    Deck otherDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherDeckCard = getOrCreateDeckCard(otherDeck, card, cardSet);

    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(otherDeckCard));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanDeleteAnyDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    Deck otherDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherDeckCard = getOrCreateDeckCard(otherDeck, card, cardSet);

    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(otherDeckCard));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanDeleteOwnedDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);
    // Players can delete their own deck cards
    assertDoesNotThrow(() -> deckCardService.delete(ownedDeckCard));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCannotDeleteOtherDeckCard() {
    Deck currentPlayerDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard currentPlayerDeckCard = getOrCreateDeckCard(currentPlayerDeck, card, cardSet);

    Deck otherPlayerDeck = getOrCreateDeck(getWrestler("not_owner"));
    DeckCard otherPlayerDeckCard = getOrCreateDeckCard(otherPlayerDeck, card, cardSet);

    assertThrows(AccessDeniedException.class, () -> deckCardService.delete(otherPlayerDeckCard));
    assertDoesNotThrow(() -> deckCardService.delete(currentPlayerDeckCard));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotDeleteDeckCard() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertThrows(AccessDeniedException.class, () -> deckCardService.delete(ownedDeckCard));
  }

  // --- Read Operations Tests ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanReadDeckCards() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanReadDeckCards() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanReadDeckCards() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    Deck otherDeck = getOrCreateDeck(getWrestler("not_owner"));
    CardSet otherCardSet = getOrCreateCardSet();
    Card otherCard = getOrCreateCard(otherCardSet);
    DeckCard otherDeckCard = getOrCreateDeckCard(otherDeck, otherCard, otherCardSet);

    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                otherDeck.getId(), otherCard.getId(), otherCardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCanReadDeckCards() {
    Deck ownedDeck = getOrCreateDeck(getWrestler("owner"));
    CardSet cardSet = getOrCreateCardSet();
    Card card = getOrCreateCard(cardSet);
    DeckCard ownedDeckCard = getOrCreateDeckCard(ownedDeck, card, cardSet);

    assertDoesNotThrow(
        () ->
            deckCardService.findByDeckIdAndCardIdAndSetId(
                ownedDeck.getId(), card.getId(), cardSet.getId()));
    assertDoesNotThrow(() -> deckCardService.findAll());
    assertDoesNotThrow(() -> deckCardService.findByDeck(ownedDeck));
  }
}
