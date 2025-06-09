package com.github.javydreamercsw.management.service.deck;

import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DeckCardService {

  private final DeckCardRepository deckCardRepository;
  private final Clock clock;

  DeckCardService(DeckCardRepository deckCardRepository, Clock clock) {
    this.deckCardRepository = deckCardRepository;
    this.clock = clock;
  }

  public List<DeckCard> list(Pageable pageable) {
    return deckCardRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return deckCardRepository.count();
  }

  public DeckCard save(@NonNull DeckCard deckCard) {
    return deckCardRepository.saveAndFlush(deckCard);
  }

  public List<DeckCard> findAll() {
    return deckCardRepository.findAll();
  }

  public void delete(DeckCard dc) {
    deckCardRepository.delete(dc);
  }
}
