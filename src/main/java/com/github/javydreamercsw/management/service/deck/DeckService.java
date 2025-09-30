package com.github.javydreamercsw.management.service.deck;

import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DeckService {

  private final DeckRepository deckRepository;
  private final Clock clock;

  DeckService(DeckRepository deckRepository, Clock clock) {
    this.deckRepository = deckRepository;
    this.clock = clock;
  }

  public Deck createDeck(@NonNull Wrestler wrestler) {
    Deck deck = new Deck();
    deck.setWrestler(wrestler);
    save(deck);
    return deck;
  }

  public List<Deck> list(Pageable pageable) {
    return deckRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return deckRepository.count();
  }

  public Deck save(@NonNull Deck deck) {
    deck.setCreationDate(clock.instant());
    return deckRepository.saveAndFlush(deck);
  }

  public List<Deck> findAll() {
    return deckRepository.findAll();
  }

  public Deck findById(@NonNull Long id) {
    return deckRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Deck with id " + id + " not found"));
  }

  public List<Deck> findByWrestler(Wrestler wrestler) {
    return deckRepository.findByWrestler(wrestler);
  }
}
