/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeckSyncTest {

  @Mock private DeckService deckService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CardSetRepository cardSetRepository;
  @Mock private CardService cardService;

  private DeckSync skipSync;
  private DeckSync fullSync;

  private Wrestler jeffHardy() {
    Wrestler w = new Wrestler();
    w.setId(99L);
    w.setName("Eddie Guerrero");
    return w;
  }

  private CardSet eddieSet() {
    CardSet set = new CardSet();
    set.setId(1L);
    set.setCode("EDDIE");
    return set;
  }

  private Card eddieCard(int number) {
    CardSet set = eddieSet();
    Card c = new Card();
    c.setId((long) number);
    c.setName("EDDIE card " + number);
    c.setNumber(number);
    c.setSet(set);
    return c;
  }

  @Test
  void sync_topUpCreatesDeckForWrestlerWithoutOneWhenSkipFlagSet() {
    // Simulates a flag-protected install: decks exist for everyone except Eddie Guerrero. Top-up
    // must build exactly his deck from decks.json.
    skipSync =
        new DeckSync(
            true,
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper());
    when(deckService.count()).thenReturn(35L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    // Jeff has no deck yet.
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of());
    Deck newDeck = new Deck();
    newDeck.setWrestler(jeff);
    when(deckService.createDeck(jeff)).thenReturn(newDeck);

    // EDDIE cards exist in the catalog (CardSync top-up ran first in production ordering).
    CardSet set = eddieSet();
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(eddieCard(1)));

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Deck>> captor = ArgumentCaptor.forClass(List.class);
    verify(deckService, atLeastOnce()).saveAll(captor.capture());
    List<Deck> saved =
        captor.getAllValues().stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toList());
    assertFalse(saved.isEmpty(), "Eddie's new deck must be saved");
  }

  @Test
  void sync_topUpNeverModifiesExistingDeckWhenSkipFlagSet() {
    skipSync =
        new DeckSync(
            true,
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper());
    when(deckService.count()).thenReturn(1L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    // Eddie already has a deck: under the skip gate it must never be re-saved, even if the seed
    // file lists different cards.
    Deck existingDeck = new Deck();
    existingDeck.setWrestler(jeff);
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of(existingDeck));

    skipSync.sync();

    verify(deckService, never()).saveAll(any());
  }

  @Test
  void sync_fullModeReconcilesExistingDeck() {
    fullSync =
        new DeckSync(
            false,
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper());
    when(deckService.count()).thenReturn(1L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    Deck existingDeck = new Deck();
    existingDeck.setWrestler(jeff);
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of(existingDeck));

    CardSet set = eddieSet();
    Card card = eddieCard(1);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card));

    fullSync.sync();

    // Full mode reconciles: Eddie's deck gains the seeded JFH #1 card.
    verify(deckService, atLeastOnce()).saveAll(any());
    assertFalse(existingDeck.getCards().isEmpty());
    assertEquals(1, existingDeck.getCards().size());
  }
}
