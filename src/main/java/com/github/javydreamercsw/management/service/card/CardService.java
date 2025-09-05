package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
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
public class CardService {

  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private Clock clock;

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

    // Set default CardSet (use the first available one)
    CardSet defaultSet =
        cardSetRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No CardSet available for default"));
    card.setSet(defaultSet);

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

  public Optional<Card> findByNumberAndSet(Integer number, String setName) {
    return cardRepository.findByNumberAndSetName(number, setName);
  }
}
