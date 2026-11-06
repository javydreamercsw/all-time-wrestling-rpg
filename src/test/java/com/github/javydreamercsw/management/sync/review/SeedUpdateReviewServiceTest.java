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
package com.github.javydreamercsw.management.sync.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.migration.DataMigrationHistory;
import com.github.javydreamercsw.management.migration.DataMigrationHistoryRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

/**
 * Contract tests for the seed-update review service (seed-update review feature, Task 4).
 *
 * <p>Runs against the real repositories on in-memory H2 (same base as {@code
 * SeedUpdateDecisionTest}) because the required assertions are persistence-shaped: the {@code
 * parkRemoval} idempotency lookup must go through the real derived query with a {@code null} {@code
 * field_name} to pin Spring Data's {@code IS NULL} conversion (carry-finding from Task 2's review —
 * a mocked repository cannot prove that). Only {@link BuildProperties} is mocked, mirroring {@code
 * DataMigrationRunnerTest}'s handling of the same dependency.
 */
class SeedUpdateReviewServiceTest extends AbstractJpaTest {

  /** The version this (simulated) boot runs under — distinct from the recorded previous version. */
  private static final String CURRENT_VERSION = "2.13.0";

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private DataMigrationHistoryRepository historyRepository;
  @Autowired private EntityManager entityManager;

  private BuildProperties buildProperties;

  @BeforeEach
  void resetReviewState() {
    // Deterministic starting point: no recorded migrations (fresh-install shape) and no parked
    // decisions. baseSetUp()'s dataInitializer.init() commits migration-history rows in separate
    // transactions, so deleteAllInBatch() (immediate DELETE) is required — a queued
    // entity-manager delete would be discarded by the clear() below.
    decisionRepository.deleteAllInBatch();
    historyRepository.deleteAllInBatch();
    entityManager.clear();
    buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn(CURRENT_VERSION);
  }

  private SeedUpdateReviewService service() {
    return new SeedUpdateReviewService(
        decisionRepository,
        historyRepository,
        Optional.of(buildProperties),
        new SeedReviewGate(decisionRepository));
  }

  private SeedUpdateReviewService serviceWithoutBuildInfo() {
    return new SeedUpdateReviewService(
        decisionRepository,
        historyRepository,
        Optional.empty(),
        new SeedReviewGate(decisionRepository));
  }

  /** Simulates one row recorded by a previous boot's data-migration run. */
  private void recordPreviousBoot(String appVersion) {
    DataMigrationHistory history = new DataMigrationHistory();
    history.setMigrationId("previous-boot-migration");
    history.setAppVersion(appVersion);
    history.setExecutedAt(Instant.now());
    history.setDurationMs(0L);
    history.setStatus(DataMigrationHistory.STATUS_SUCCESS);
    historyRepository.save(history);
  }

  @Test
  void freshInstallNeverParks() {
    // Fresh install shape: data_migration_history has no rows and no decisions are pending.
    SeedUpdateReviewService service = service();

    // No previous version exists, so a current version must NOT count as a version change —
    // a fresh DB matches the new seed exactly and detection (and therefore parking) must not run.
    assertFalse(service.shouldRunDetection());
    assertEquals(0, service.pendingCount());
    assertFalse(service.gate().isUp());
    assertEquals(0, decisionRepository.count());
  }

  @Test
  void freshInstallWithoutBuildInfoNeverDetects() {
    // Same fresh-install shape, but the boot could not resolve its own version ("unknown"):
    // absence of a previous version still must not look like a version change.
    SeedUpdateReviewService service = serviceWithoutBuildInfo();

    assertFalse(service.shouldRunDetection());
    assertFalse(service.gate().isUp());
  }

  @Test
  void sameVersionBootDoesNotReDetect() {
    recordPreviousBoot(CURRENT_VERSION);

    assertFalse(service().shouldRunDetection());
  }

  @Test
  void versionChangeTriggersDetection() {
    recordPreviousBoot("2.12.0");

    assertTrue(service().shouldRunDetection());
  }

  @Test
  void pendingDecisionsKeepDetectionAliveAcrossRestarts() {
    recordPreviousBoot(CURRENT_VERSION);
    service()
        .parkFieldConflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "catchphrase",
            "The Excellence of Execution",
            "The Best There Is",
            "fp-pending-1");

    // Same version as the last recorded boot, yet undecided leftovers keep detection alive.
    assertTrue(service().shouldRunDetection());
  }

  @Test
  void pendingDecisionsTriggerDetectionEvenWithoutHistory() {
    service().parkRemoval("npc", "npc:samael", "Samael", "fp-pending-2");

    assertTrue(service().shouldRunDetection());
  }

  @Test
  void parkFieldConflictIsIdempotent() {
    SeedUpdateReviewService service = service();

    service.parkFieldConflict(
        "wrestler",
        "wrestler:bret-hart",
        "Bret Hart",
        "catchphrase",
        "The Excellence of Execution",
        "The Best There Is",
        "fp-conflict-1");
    service.parkFieldConflict(
        "wrestler",
        "wrestler:bret-hart",
        "Bret Hart",
        "catchphrase",
        "The Excellence of Execution",
        "The Best There Is, The Best There Was",
        "fp-conflict-2");

    List<SeedUpdateDecision> rows = decisionRepository.findAll();
    assertEquals(1, rows.size());
    SeedUpdateDecision row = rows.get(0);
    // The incoming-seed snapshot is refreshed to the latest values, so a later Apply writes the
    // current release's values.
    assertEquals("The Best There Is, The Best There Was", row.getNewValue());
    assertEquals("fp-conflict-2", row.getNewFingerprint());
    // The database-side snapshot stays as originally captured.
    assertEquals("The Excellence of Execution", row.getCurrentValue());
    assertEquals("wrestler", row.getDomainKey());
    assertEquals("wrestler:bret-hart", row.getEntityKey());
    assertEquals("Bret Hart", row.getEntityDisplayName());
    assertEquals("catchphrase", row.getFieldName());
    assertEquals(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT, row.getDecisionType());
    assertEquals(SeedUpdateDecision.STATUS_PENDING, row.getStatus());
  }

  @Test
  void parkRemovalIsIdempotent() {
    SeedUpdateReviewService service = service();

    service.parkRemoval("npc", "npc:samael", "Samael", "fp-removal-1");
    service.parkRemoval("npc", "npc:samael", "Samael", "fp-removal-2");

    assertEquals(1, decisionRepository.count());
    // Carry-finding from Task 2's review: this lookup goes through the derived query with a NULL
    // field_name, pinning Spring Data's NULL -> IS NULL conversion against the real database.
    Optional<SeedUpdateDecision> row =
        decisionRepository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            "npc", "npc:samael", null, SeedUpdateDecision.STATUS_PENDING);
    assertTrue(row.isPresent());
    assertEquals("fp-removal-2", row.get().getNewFingerprint());
    assertEquals(SeedUpdateDecision.DECISION_TYPE_REMOVAL, row.get().getDecisionType());
    assertNull(row.get().getFieldName());
    assertNull(row.get().getCurrentValue());
    assertNull(row.get().getNewValue());
    assertEquals(SeedUpdateDecision.STATUS_PENDING, row.get().getStatus());
  }

  @Test
  void pendingCountReflectsRepository() {
    SeedUpdateReviewService service = service();
    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    service.parkFieldConflict("npc", "npc:samael", "Samael", "motive", "mine", "new", "fp-2");
    service.parkRemoval("deck", "deck:legend", "Legend Deck", "fp-3");
    SeedUpdateDecision applied = decisionRepository.findAll().get(2);
    applied.setStatus(SeedUpdateDecision.STATUS_APPLIED_REMOVE);
    decisionRepository.save(applied);

    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertEquals(2, service.pendingCount());
  }

  @Test
  void pendingDecisionsRaiseGateAfterPark() {
    SeedUpdateReviewService service = service();
    assertFalse(service.gate().isUp());

    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");

    assertTrue(service.gate().isUp());
    assertEquals(1, service.gate().pendingCount());
  }

  @Test
  void gateRefreshLowersWhenPendingResolved() {
    SeedUpdateReviewService service = service();
    service.parkRemoval("npc", "npc:samael", "Samael", "fp-1");
    assertTrue(service.gate().isUp());

    // An administrator applies the decision (Task 9's flow): the pending row becomes terminal.
    SeedUpdateDecision row = decisionRepository.findAll().get(0);
    row.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM);
    decisionRepository.save(row);

    service.gate().refresh();
    assertFalse(service.gate().isUp());
    assertEquals(0, service.gate().pendingCount());
  }

  @Test
  void parkingDoesNotRequireVersionChangePrecondition() {
    // Creating a row must not depend on shouldRunDetection() — the detector calls park* whenever
    // it finds a collision, including on boots where no version history exists yet.
    SeedUpdateReviewService service = service();
    assertEquals(0, historyRepository.count());

    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    assertEquals(1, decisionRepository.count());
    assertEquals(CURRENT_VERSION, decisionRepository.findAll().get(0).getAppVersion());

    // Without build info the detection version falls back to DataMigrationRunner's "unknown".
    serviceWithoutBuildInfo().parkRemoval("npc", "npc:samael", "Samael", "fp-2");
    Optional<SeedUpdateDecision> removal =
        decisionRepository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            "npc", "npc:samael", null, SeedUpdateDecision.STATUS_PENDING);
    assertTrue(removal.isPresent());
    assertEquals("unknown", removal.get().getAppVersion());
  }
}
