package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CardSetService {

  private final CardSetRepository cardSetRepository;
  private final Clock clock;

  CardSetService(CardSetRepository cardSetRepository, Clock clock) {
    this.cardSetRepository = cardSetRepository;
    this.clock = clock;
  }

  public void createCard(@NonNull String name) {
    CardSet card = new CardSet();
    card.setName(name);
    save(card);
  }

  public List<CardSet> list(Pageable pageable) {
    return cardSetRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return cardSetRepository.count();
  }

  public CardSet save(@NonNull CardSet card) {
    card.setCreationDate(clock.instant());
    return cardSetRepository.saveAndFlush(card);
  }

  public List<CardSet> findAll() {
    return cardSetRepository.findAll();
  }
}
