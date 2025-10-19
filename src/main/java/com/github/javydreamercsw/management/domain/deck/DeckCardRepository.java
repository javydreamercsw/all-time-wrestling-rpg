package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.management.domain.card.Card;
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
}
