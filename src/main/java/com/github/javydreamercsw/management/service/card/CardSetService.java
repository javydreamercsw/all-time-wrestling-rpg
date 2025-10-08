package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
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

  public CardSet createCardSet(@NonNull String name, @NonNull String setCode) {
    CardSet card = new CardSet();
    card.setName(name);
    card.setSetCode(setCode);
    return save(card);
  }

  public List<CardSet> list(@NonNull Pageable pageable) {
    return cardSetRepository.findAll(pageable).toList();
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

  public Optional<CardSet> findBySetCode(@NonNull String setCode) {
    return cardSetRepository.findBySetCode(setCode);
  }
}
