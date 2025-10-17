package com.github.javydreamercsw.management.service.deck;

import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
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
}
