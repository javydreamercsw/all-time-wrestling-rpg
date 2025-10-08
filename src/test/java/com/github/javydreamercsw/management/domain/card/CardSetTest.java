package com.github.javydreamercsw.management.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

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

    assertEquals(cardSet1, cardSet1);
    assertFalse(cardSet1.equals(cardSet2));
    assertTrue(cardSet1.equals(repository.findById(cardSet1.getId()).get()));
  }

  @Test
  void testSetCodeNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    assertThrows(Exception.class, () -> repository.save(cardSet));
  }

  @Test
  void testSetCodeSize() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    cardSet.setSetCode("S123");
    assertThrows(Exception.class, () -> repository.save(cardSet));
  }

  @Test
  void testNameNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setSetCode("S1");
    assertThrows(Exception.class, () -> repository.save(cardSet));
  }
}
