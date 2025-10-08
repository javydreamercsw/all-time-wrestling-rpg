package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "data.initializer.enabled=false")
class CardSetServiceTest {

  @Autowired private CardSetService cardSetService;

  @Autowired private CardSetRepository cardSetRepository;

  @Test
  void testCardSetServiceFlow() {
    CardSet cardSet1 = cardSetService.createCardSet("Test Set 1", "TS1");
    assertNotNull(cardSet1);
    assertNotNull(cardSet1.getCreationDate());
    assertEquals("Test Set 1", cardSet1.getName());
    assertEquals("TS1", cardSet1.getSetCode());

    CardSet cardSet2 = cardSetService.createCardSet("Test Set 2", "TS2");
    assertNotNull(cardSet2);
    assertNotNull(cardSet2.getCreationDate());
    assertEquals("Test Set 2", cardSet2.getName());
    assertEquals("TS2", cardSet2.getSetCode());

    assertEquals(2, cardSetService.list(org.springframework.data.domain.Pageable.unpaged()).size());
    assertEquals(2, cardSetService.count());

    cardSet1.setName("Updated Set 1");
    cardSetService.save(cardSet1);

    assertEquals("Updated Set 1", cardSetService.findBySetCode("TS1").get().getName());

    assertEquals(2, cardSetService.findAll().size());

    Optional<CardSet> foundCardSet = cardSetService.findBySetCode("TS1");
    assertTrue(foundCardSet.isPresent());
    assertEquals("Updated Set 1", foundCardSet.get().getName());
  }
}
