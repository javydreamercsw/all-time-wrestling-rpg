package com.github.javydreamercsw.management.domain.deck;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeckCardRepository
    extends JpaRepository<DeckCard, Long>, JpaSpecificationExecutor<DeckCard> {

  // If you don't need a total row count, Slice is better than Page.
  Page<DeckCard> findAllBy(Pageable pageable);

  Optional<DeckCard> findByDeckIdAndCardIdAndSetId(Long deckId, Long cardId, Long setId);
}
