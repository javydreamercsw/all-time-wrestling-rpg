package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import java.time.Clock;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CardService {

  private final CardRepository cardRepository;
  private final Clock clock;

  CardService(CardRepository cardRepository, Clock clock) {
    this.cardRepository = cardRepository;
    this.clock = clock;
  }

  public void createCard(@NonNull String name) {
    Card card = new Card();
    card.setName(name);
    // Set default values
    card.setDamage(1);
    card.setMomentum(1);
    card.setTarget(1);
    card.setStamina(1);
    card.setSignature(false);
    card.setFinisher(false);
    card.setType("TBD");
    save(card);
  }

  public List<Card> list(Pageable pageable) {
    return cardRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return cardRepository.count();
  }

  public Card save(@NonNull Card card) {
    card.setCreationDate(clock.instant());
    return cardRepository.saveAndFlush(card);
  }

  public List<Card> findAll() {
    return cardRepository.findAll();
  }
}
