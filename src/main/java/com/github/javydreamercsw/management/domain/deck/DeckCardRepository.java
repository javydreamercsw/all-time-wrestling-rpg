package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.management.domain.card.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCardRepository extends JpaRepository<DeckCard, Long> {
  Optional<DeckCard> findByDeckAndCard(Deck deck, Card card);

  Optional<DeckCard> findByDeckIdAndCardIdAndSetId(Long deckId, Long cardId, Long setId);
}
