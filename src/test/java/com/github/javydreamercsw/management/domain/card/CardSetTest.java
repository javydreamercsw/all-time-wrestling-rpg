/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.AbstractJpaTest;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CardSetTest extends AbstractJpaTest {

  @Autowired private TestEntityManager entityManager;
  @Autowired private CardSetRepository repository;

  @AfterEach
  void tearDown() {
    entityManager.clear();
  }

  @Test
  void testGettersAndSetters() {
    CardSet cardSet = new CardSet();
    assertNull(cardSet.getId());
    assertNull(cardSet.getName());
    assertNull(cardSet.getCreationDate());

    cardSet.setId(1L);
    cardSet.setName("Set");
    Instant now = Instant.now();
    cardSet.setCode("S");
    cardSet.setCreationDate(now);

    assertEquals(1L, cardSet.getId());
    assertEquals("Set", cardSet.getName());
    assertEquals("S", cardSet.getCode());
    assertEquals(now, cardSet.getCreationDate());
  }

  @Test
  void testEnsureDefaults() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    cardSet.setCode("S");
    repository.save(cardSet);
    assertNotNull(cardSet.getCreationDate());
  }

  @Test
  void testEqualsAndHashCode() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("S1");
    cardSet1.setCode("S1");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("S2");
    cardSet2.setCode("S2");
    repository.save(cardSet2);

    assertNotNull(cardSet1.getId());
    assertTrue(repository.findById(cardSet1.getId()).isPresent());
    assertEquals(cardSet1, repository.findById(cardSet1.getId()).get());
  }

  @Test
  void testNameNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setCode("S");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet));
  }

  @Test
  void testSetCodeNotNull() {
    CardSet cardSet = new CardSet();
    cardSet.setName("Set");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet));
  }

  @Test
  void testNameUnique() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("Set");
    cardSet1.setCode("S1");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("Set");
    cardSet2.setCode("S2");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet2));
  }

  @Test
  void testSetCodeUnique() {
    CardSet cardSet1 = new CardSet();
    cardSet1.setName("Set1");
    cardSet1.setCode("S");
    repository.save(cardSet1);

    CardSet cardSet2 = new CardSet();
    cardSet2.setName("Set2");
    cardSet2.setCode("S");
    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(cardSet2));
  }
}
