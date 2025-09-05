package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CardSetService {

  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private Clock clock;

  public void createCardSet(@NonNull String name) {
    CardSet card = new CardSet();
    card.setName(name);
    save(card);
  }

  public List<CardSet> list(@NonNull Pageable pageable) {
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
