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
package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.management.domain.card.Card;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DeckCardRepository extends JpaRepository<DeckCard, Long> {
  Optional<DeckCard> findByDeckAndCard(Deck deck, Card card);

  Optional<DeckCard> findByDeckIdAndCardIdAndSetId(Long deckId, Long cardId, Long setId);

  @Modifying
  @Transactional
  @Query("DELETE FROM DeckCard dc WHERE dc.deck = :deck")
  void deleteAllByDeck(Deck deck);

  List<DeckCard> findByDeck(Deck deck);
}
