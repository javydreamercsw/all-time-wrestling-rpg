package com.github.javydreamercsw.management.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CardSetTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private CardSetRepository repository;

  @Test
  void testGettersAndSetters() {
    CardSet cardSet = new CardSet();
    assertNull(cardSet.getId());
    assertNull(cardSet.getName());
    assertNull(cardSet.getCreationDate());

    cardSet.setId(1L);
    cardSet.setName("Set");
    Instant now = Instant.now();
    cardSet.setSetCode("S");
    cardSet.setCreationDate(now);

    assertEquals(1L, cardSet.getId());
    assertEquals("Set", cardSet.getName());
    assertEquals("S", cardSet.getSetCode());
    assertEquals(now, cardSet.getCreationDate());
  }

  @Test
  void testEnsureDefaults() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    cardSet.setSetCode("S");
    repository.save(cardSet);
    assertNotNull(cardSet.getCreationDate());
  }

  @Test
  void testEqualsAndHashCode() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("S1");
    cardSet1.setSetCode("S1");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("S2");
    cardSet2.setSetCode("S2");
    repository.save(cardSet2);

    assertNotNull(cardSet1.getId());
    assertTrue(repository.findById(cardSet1.getId()).isPresent());
    assertEquals(cardSet1, repository.findById(cardSet1.getId()).get());
  }

  @Test
  void testNameNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setSetCode("S");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet));
  }

  @Test
  void testSetCodeNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet));
  }

  @Test
  void testSetCodeSize() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    cardSet.setSetCode("S123");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet));
  }

  @Test
  void testNameUnique() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("Set");
    cardSet1.setSetCode("S1");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("Set");
    cardSet2.setSetCode("S2");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet2));
  }

  @Test
  void testSetCodeUnique() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("Set1");
    cardSet1.setSetCode("S");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("Set2");
    cardSet2.setSetCode("S");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet2));
  }
}
