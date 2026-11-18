/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.seedreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.AbstractJpaTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Persistence round-trip for the seed_update_decision table (seed-update review feature, Task 2).
 * Verifies the entity maps to the Flyway-created schema and the repository's derived queries behave
 * as the later review-workflow tasks expect.
 */
class SeedUpdateDecisionTest extends AbstractJpaTest {

  @Autowired private SeedUpdateDecisionRepository repository;
  @Autowired private EntityManager entityManager;

  @AfterEach
  public void tearDown() {
    entityManager.clear();
    super.tearDown();
  }

  private void detachAll() {
    entityManager.clear();
  }

  private SeedUpdateDecision fieldConflictRow() {
    SeedUpdateDecision d = new SeedUpdateDecision();
    d.setDomainKey("wrestler");
    d.setEntityKey("wrestler:bret-hart");
    d.setEntityDisplayName("Bret Hart");
    d.setFieldName("catchphrase");
    d.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    d.setCurrentValue("The Excellence of Execution");
    d.setNewValue("The Best There Is");
    d.setNewFingerprint("fp-new-0001");
    d.setStatus(SeedUpdateDecision.STATUS_PENDING);
    d.setAppVersion("2.13.0");
    d.setCreatedAt(Instant.now());
    return d;
  }

  private SeedUpdateDecision removalRow() {
    SeedUpdateDecision d = new SeedUpdateDecision();
    d.setDomainKey("npc");
    d.setEntityKey("npc:samael");
    d.setEntityDisplayName("Samael");
    d.setFieldName(null); // NULL for removal decisions
    d.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    d.setCurrentValue(null); // NULL for removals
    d.setNewValue(null); // NULL for removals
    d.setNewFingerprint("fp-new-0002");
    d.setStatus(SeedUpdateDecision.STATUS_PENDING);
    d.setAppVersion("2.13.0");
    d.setCreatedAt(Instant.now());
    return d;
  }

  @Test
  void testFieldConflictRoundTrip() {
    SeedUpdateDecision saved = repository.saveAndFlush(fieldConflictRow());

    assertNotNull(saved.getId());
    detachAll();

    Optional<SeedUpdateDecision> reloaded = repository.findById(saved.getId());
    assertTrue(reloaded.isPresent());
    SeedUpdateDecision found = reloaded.get();

    assertEquals("wrestler", found.getDomainKey());
    assertEquals("wrestler:bret-hart", found.getEntityKey());
    assertEquals("Bret Hart", found.getEntityDisplayName());
    assertEquals("catchphrase", found.getFieldName());
    assertEquals(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT, found.getDecisionType());
    assertEquals("The Excellence of Execution", found.getCurrentValue());
    assertEquals("The Best There Is", found.getNewValue());
    assertEquals("fp-new-0001", found.getNewFingerprint());
    assertEquals(SeedUpdateDecision.STATUS_PENDING, found.getStatus());
    assertEquals("2.13.0", found.getAppVersion());
    assertNotNull(found.getCreatedAt());
  }

  @Test
  void testRemovalRoundTripWithNullables() {
    SeedUpdateDecision saved = repository.saveAndFlush(removalRow());

    assertNotNull(saved.getId());
    detachAll();

    Optional<SeedUpdateDecision> reloaded = repository.findById(saved.getId());
    assertTrue(reloaded.isPresent());
    SeedUpdateDecision found = reloaded.get();

    assertEquals(SeedUpdateDecision.DECISION_TYPE_REMOVAL, found.getDecisionType());
    assertNull(found.getFieldName());
    assertNull(found.getCurrentValue());
    assertNull(found.getNewValue());
    assertEquals(SeedUpdateDecision.STATUS_PENDING, found.getStatus());
  }

  @Test
  void testFindByStatusOrderByIdAsc() {
    SeedUpdateDecision first = repository.saveAndFlush(fieldConflictRow());
    SeedUpdateDecision second = repository.saveAndFlush(removalRow());
    second.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP);
    repository.saveAndFlush(second);

    detachAll();

    List<SeedUpdateDecision> pending =
        repository.findByStatusOrderByIdAsc(SeedUpdateDecision.STATUS_PENDING);
    assertEquals(1, pending.size());
    assertEquals(first.getId(), pending.get(0).getId());

    List<SeedUpdateDecision> appliedKeep =
        repository.findByStatusOrderByIdAsc(SeedUpdateDecision.STATUS_APPLIED_KEEP);
    assertEquals(1, appliedKeep.size());
    assertEquals(second.getId(), appliedKeep.get(0).getId());
  }

  @Test
  void testCountByStatus() {
    repository.saveAndFlush(fieldConflictRow());
    repository.saveAndFlush(removalRow());

    assertEquals(2, repository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertEquals(0, repository.countByStatus(SeedUpdateDecision.STATUS_APPLIED_NEW));
  }

  @Test
  void testFindPendingByDomainEntityField() {
    SeedUpdateDecision saved = repository.saveAndFlush(fieldConflictRow());
    repository.saveAndFlush(removalRow());

    detachAll();

    Optional<SeedUpdateDecision> hit =
        repository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            "wrestler", "wrestler:bret-hart", "catchphrase", SeedUpdateDecision.STATUS_PENDING);
    assertTrue(hit.isPresent());
    assertEquals(saved.getId(), hit.get().getId());

    // Same coordinates but a non-PENDING status: not found. Only PENDING rows are deduped
    // against by the service layer (the unique constraint is PENDING-only by design).
    Optional<SeedUpdateDecision> miss =
        repository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            "wrestler", "wrestler:bret-hart", "catchphrase", SeedUpdateDecision.STATUS_APPLIED_NEW);
    assertTrue(miss.isEmpty());
  }
}
