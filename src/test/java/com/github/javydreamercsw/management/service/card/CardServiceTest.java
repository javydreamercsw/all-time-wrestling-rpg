package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "data.initializer.enabled=false")
class CardServiceTest {

  @Autowired private CardService cardService;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  private CardSet cardSet;

  @BeforeEach
  void setUp() {
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
    cardSet = new CardSet();
    cardSet.setName("Test Set");
    cardSet.setSetCode("TS");
    cardSetRepository.save(cardSet);
  }

  @Test
  void testCreateCard() {
    Card card = cardService.createCard("Card 1");
    assertNotNull(card);
    assertNotNull(card.getCreationDate());
    assertEquals("Card 1", card.getName());
    assertEquals("Strike", card.getType());
    assertEquals(cardSet.getId(), card.getSet().getId());
    assertEquals(1, card.getNumber());
  }

  @Test
  void testCreateCardNoSet() {
    cardSetRepository.deleteAll();
    assertThrows(IllegalStateException.class, () -> cardService.createCard("Card 1"));
  }

  @Test
  void testListAndCount() {
    cardService.createCard("Card 1");
    cardService.createCard("Card 2");

    assertEquals(2, cardService.list(org.springframework.data.domain.Pageable.unpaged()).size());
    assertEquals(2, cardService.count());
  }

  @Test
  void testSave() {
    Card card = cardService.createCard("Card 1");
    card.setName("Updated Card");
    cardService.save(card);

    assertEquals(
        "Updated Card",
        cardService.list(org.springframework.data.domain.Pageable.unpaged()).get(0).getName());
  }

  @Test
  void testFindAll() {
    cardService.createCard("Card 1");
    cardService.createCard("Card 2");

    assertEquals(2, cardService.findAll().size());
  }

  @Test
  void testFindByNumberAndSet() {
    Card card = cardService.createCard("Card 1");

    Optional<Card> foundCard = cardService.findByNumberAndSet(card.getNumber(), cardSet.getName());
    assertTrue(foundCard.isPresent());
    assertEquals(card.getId(), foundCard.get().getId());
  }

  @Test
  void testDelete() {
    Card card = cardService.createCard("Card 1");
    assertEquals(1, cardService.count());

    cardService.delete(card.getId());
    assertEquals(0, cardService.count());
  }
}
