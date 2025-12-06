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

import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeckCardService {
  private final DeckCardRepository deckCardRepository;

  public DeckCardService(DeckCardRepository deckCardRepository) {
    this.deckCardRepository = deckCardRepository;
  }

  public DeckCard save(DeckCard dc) {
    return deckCardRepository.save(dc);
  }

  public void delete(DeckCard dc) {
    deckCardRepository.delete(dc);
  }

  public Optional<DeckCard> findByDeckIdAndCardIdAndSetId(Long deckId, Long cardId, Long setId) {

    return deckCardRepository.findByDeckIdAndCardIdAndSetId(deckId, cardId, setId);
  }

  public Iterable<DeckCard> findAll() {

    return deckCardRepository.findAll();
  }

  public List<DeckCard> findByDeck(com.github.javydreamercsw.management.domain.deck.Deck deck) {
    return deckCardRepository.findByDeck(deck);
  }
}
