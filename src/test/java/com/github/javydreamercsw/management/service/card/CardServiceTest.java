package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "data.initializer.enabled=false")
class CardServiceTest {

  @Autowired private CardService cardService;

  @Autowired private CardSetRepository cardSetRepository;

  @Test
  void testCardService() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Test Set");
    cardSet.setSetCode("TS");
    cardSetRepository.save(cardSet);

    Card card1 = cardService.createCard("Card 1");
    assertNotNull(card1);
    assertNotNull(card1.getCreationDate());
    assertEquals("Card 1", card1.getName());
    assertEquals("Strike", card1.getType());
    assertEquals(cardSet.getId(), card1.getSet().getId());

    Card card2 = cardService.createCard("Card 2");
    assertNotNull(card2);
    assertNotNull(card2.getCreationDate());
    assertEquals("Card 2", card2.getName());
    assertEquals("Strike", card2.getType());
    assertEquals(cardSet.getId(), card2.getSet().getId());

    assertEquals(2, cardService.list(org.springframework.data.domain.Pageable.unpaged()).size());

    assertEquals(2, cardService.count());

    Card card = cardService.list(org.springframework.data.domain.Pageable.unpaged()).get(0);
    card.setName("Updated Card");
    cardService.save(card);

    assertEquals(
        "Updated Card",
        cardService.list(org.springframework.data.domain.Pageable.unpaged()).get(0).getName());

    assertEquals(2, cardService.findAll().size());

    Optional<Card> foundCard = cardService.findByNumberAndSet(card1.getNumber(), cardSet.getName());
    assertTrue(foundCard.isPresent());
    assertEquals(card1.getId(), foundCard.get().getId());

    cardService.delete(card1.getId());
    assertEquals(1, cardService.count());
  }
}
