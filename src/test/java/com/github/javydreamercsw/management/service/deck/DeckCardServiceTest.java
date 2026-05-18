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
package com.github.javydreamercsw.management.service.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeckCardServiceTest {

  @Mock private DeckCardRepository deckCardRepository;

  @InjectMocks private DeckCardService deckCardService;

  private Deck deck;
  private Card card;
  private CardSet cardSet;
  private DeckCard deckCard;

  @BeforeEach
  void setUp() {
    deck = new Deck();
    deck.setId(1L);

    card = new Card();
    card.setId(10L);
    card.setName("Suplex");

    cardSet = new CardSet();
    cardSet.setId(2L);
    cardSet.setCode("BASE");

    deckCard = new DeckCard();
    deckCard.setId(100L);
    deckCard.setDeck(deck);
    deckCard.setCard(card);
    deckCard.setSet(cardSet);
    deckCard.setAmount(2);
  }

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  void save_persistsDeckCard() {
    when(deckCardRepository.save(deckCard)).thenReturn(deckCard);

    DeckCard result = deckCardService.save(deckCard);

    assertThat(result).isSameAs(deckCard);
    verify(deckCardRepository).save(deckCard);
  }

  // -------------------------------------------------------------------------
  // delete
  // -------------------------------------------------------------------------

  @Test
  void delete_delegatesToRepository() {
    deckCardService.delete(deckCard);

    verify(deckCardRepository).delete(deckCard);
  }

  // -------------------------------------------------------------------------
  // findByDeckIdAndCardIdAndSetId
  // -------------------------------------------------------------------------

  @Test
  void findByDeckIdAndCardIdAndSetId_found() {
    when(deckCardRepository.findByDeckIdAndCardIdAndSetId(1L, 10L, 2L))
        .thenReturn(Optional.of(deckCard));

    Optional<DeckCard> result = deckCardService.findByDeckIdAndCardIdAndSetId(1L, 10L, 2L);

    assertThat(result).isPresent().contains(deckCard);
    verify(deckCardRepository).findByDeckIdAndCardIdAndSetId(1L, 10L, 2L);
  }

  @Test
  void findByDeckIdAndCardIdAndSetId_notFound_returnsEmpty() {
    when(deckCardRepository.findByDeckIdAndCardIdAndSetId(99L, 99L, 99L))
        .thenReturn(Optional.empty());

    Optional<DeckCard> result = deckCardService.findByDeckIdAndCardIdAndSetId(99L, 99L, 99L);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findAll
  // -------------------------------------------------------------------------

  @Test
  void findAll_returnsAllDeckCards() {
    when(deckCardRepository.findAll()).thenReturn(List.of(deckCard));

    Iterable<DeckCard> result = deckCardService.findAll();

    assertThat(result).contains(deckCard);
    verify(deckCardRepository).findAll();
  }

  // -------------------------------------------------------------------------
  // findByDeck
  // -------------------------------------------------------------------------

  @Test
  void findByDeck_returnsDeckCards() {
    when(deckCardRepository.findByDeck(deck)).thenReturn(List.of(deckCard));

    List<DeckCard> result = deckCardService.findByDeck(deck);

    assertThat(result).hasSize(1).contains(deckCard);
    verify(deckCardRepository).findByDeck(deck);
  }

  @Test
  void findByDeck_returnsEmptyList_whenNone() {
    when(deckCardRepository.findByDeck(deck)).thenReturn(List.of());

    List<DeckCard> result = deckCardService.findByDeck(deck);

    assertThat(result).isEmpty();
  }
}
