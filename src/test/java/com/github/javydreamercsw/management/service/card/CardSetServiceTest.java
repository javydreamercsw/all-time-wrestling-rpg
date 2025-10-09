package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "data.initializer.enabled=false")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CardSetServiceTest {

  @Autowired private CardSetService cardSetService;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private CardRepository cardRepository;

  private static int setCodeCounter;

  @BeforeAll
  static void setupAll() {
    setCodeCounter = 0;
  }

  private String generateUniqueSetCode() {
    return String.format("T%02d", setCodeCounter++);
  }

  @BeforeEach
  void setUp() {

    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
  }

  @Test
  void testCardSetServiceFlow() {

    String setCode1 = generateUniqueSetCode();

    String setName1 = "Test Set " + UUID.randomUUID();

    CardSet cardSet1 = cardSetService.createCardSet(setName1, setCode1);

    assertNotNull(cardSet1);

    assertNotNull(cardSet1.getCreationDate());

    assertEquals(setName1, cardSet1.getName());

    assertEquals(setCode1, cardSet1.getSetCode());

    String setCode2 = generateUniqueSetCode();

    String setName2 = "Test Set " + UUID.randomUUID();

    CardSet cardSet2 = cardSetService.createCardSet(setName2, setCode2);

    assertNotNull(cardSet2);

    assertNotNull(cardSet2.getCreationDate());

    assertEquals(setName2, cardSet2.getName());

    assertEquals(setCode2, cardSet2.getSetCode());

    assertEquals(2, cardSetService.list(Pageable.unpaged()).size());

    assertEquals(2, cardSetService.count());

    cardSet1.setName("Updated Set 1");
    cardSetService.save(cardSet1);

    assertEquals("Updated Set 1", cardSetService.findBySetCode(setCode1).get().getName());

    assertEquals(2, cardSetService.findAll().size());

    Optional<CardSet> foundCardSet = cardSetService.findBySetCode(setCode1);
    assertTrue(foundCardSet.isPresent());
    assertEquals("Updated Set 1", foundCardSet.get().getName());
  }

  @Test
  void testCreateCardSet() {
    String setCode = generateUniqueSetCode();
    CardSet cardSet = cardSetService.createCardSet("Test Set", setCode);
    assertNotNull(cardSet);
    assertNotNull(cardSet.getCreationDate());
    assertEquals("Test Set", cardSet.getName());
    assertEquals(setCode, cardSet.getSetCode());
  }

  @Test
  void testListAndCount() {
    cardSetService.createCardSet("Test Set " + UUID.randomUUID(), generateUniqueSetCode());
    cardSetService.createCardSet("Test Set " + UUID.randomUUID(), generateUniqueSetCode());

    assertEquals(2, cardSetService.list(Pageable.unpaged()).size());
    assertEquals(2, cardSetService.count());
  }

  @Test
  void testSave() {
    String setCode = generateUniqueSetCode();
    CardSet cardSet = cardSetService.createCardSet("Test Set", setCode);
    cardSet.setName("Updated Set");
    cardSetService.save(cardSet);

    assertEquals("Updated Set", cardSetService.findBySetCode(setCode).get().getName());
  }

  @Test
  void testFindAll() {
    int initialSize = cardSetService.findAll().size();
    cardSetService.createCardSet("Test Set " + UUID.randomUUID(), generateUniqueSetCode());
    cardSetService.createCardSet("Test Set " + UUID.randomUUID(), generateUniqueSetCode());

    assertEquals(initialSize + 2, cardSetService.findAll().size());
  }

  @Test
  void testFindBySetCode() {
    String setCode = generateUniqueSetCode();
    cardSetService.createCardSet("Test Set " + UUID.randomUUID(), setCode);

    Assertions.assertTrue(cardSetService.findBySetCode(setCode).isPresent());
  }

  @Test
  void testCreateCardSetWithDuplicateName() {
    String name = "Duplicate Name";
    String setCode1 = generateUniqueSetCode();
    cardSetService.createCardSet(name, setCode1);

    String setCode2 = generateUniqueSetCode();
    Assertions.assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> cardSetService.createCardSet(name, setCode2));
  }

  @Test
  void testCreateCardSetWithDuplicateSetCode() {
    String setCode = generateUniqueSetCode();
    String name1 = "Name 1 " + UUID.randomUUID();
    cardSetService.createCardSet(name1, setCode);

    String name2 = "Name 2 " + UUID.randomUUID();
    Assertions.assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> cardSetService.createCardSet(name2, setCode));
  }
}
