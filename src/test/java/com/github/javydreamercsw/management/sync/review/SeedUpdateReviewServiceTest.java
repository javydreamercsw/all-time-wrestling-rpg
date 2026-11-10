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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.event.SeedUpdateReviewPendingEvent;
import com.github.javydreamercsw.management.migration.DataMigrationHistory;
import com.github.javydreamercsw.management.migration.DataMigrationHistoryRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;

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
  @Autowired private GameSettingRepository gameSettingRepository;
  @Autowired private EntityManager entityManager;

  private BuildProperties buildProperties;
  private ApplicationEventPublisher eventPublisher;

  /** Gate of the most recently created service, for cross-path state assertions. */
  private SeedReviewGate gate;

  @BeforeEach
  void resetReviewState() {
    // Deterministic starting point: no recorded migrations (fresh-install shape) and no parked
    // decisions. baseSetUp()'s dataInitializer.init() commits migration-history rows in separate
    // transactions, so deleteAllInBatch() (immediate DELETE) is required — a queued
    // entity-manager delete would be discarded by the clear() below.
    decisionRepository.deleteAllInBatch();
    historyRepository.deleteAllInBatch();
    // Same immediate-delete discipline as above: the orchestrator's boot-time markDetected() now
    // writes the stamp outside the test transaction, and a queued repository.delete() here would
    // be discarded by the clear() below (the stamp row would survive and look like a previous
    // boot's version). flush() pushes the DELETE through before the context is cleared.
    gameSettingRepository
        .findGlobal(SeedUpdateReviewService.DETECTED_VERSION_KEY)
        .ifPresent(
            s -> {
              gameSettingRepository.delete(s);
              gameSettingRepository.flush();
            });
    entityManager.clear();
    gate = null;
    buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn(CURRENT_VERSION);
    eventPublisher = mock(ApplicationEventPublisher.class);
  }

  private SeedUpdateReviewService service() {
    gate = new SeedReviewGate(decisionRepository);
    return new SeedUpdateReviewService(
        decisionRepository,
        Optional.of(buildProperties),
        gate,
        eventPublisher,
        gameSettingRepository);
  }

  private SeedUpdateReviewService serviceWithoutBuildInfo() {
    gate = new SeedReviewGate(decisionRepository);
    return new SeedUpdateReviewService(
        decisionRepository, Optional.empty(), gate, eventPublisher, gameSettingRepository);
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
    // The stamp, not the migration history, drives the comparison: a previous boot of the same
    // version recorded "2.13.0" as seen, so a same-version restart must not re-detect — even
    // though a DataMigration row exists (the stale ATW-1pvd trigger would have compared against
    // migration rows that never change when a release ships no new migration).
    recordPreviousBoot(CURRENT_VERSION);
    service().markDetected();

    assertFalse(service().shouldRunDetection());
  }

  @Test
  void versionChangeTriggersDetection() {
    service().markDetected(); // previous boot stamped 2.13.0
    when(buildProperties.getVersion()).thenReturn("2.14.0");

    assertTrue(service().shouldRunDetection());
  }

  @Test
  void versionChangeFiresWithoutNewMigration() {
    // beads ATW-1pvd: a release that ships NO new DataMigration records no data_migration_history
    // row, so the old trigger (last migration row's app_version) went stale and re-ran detection
    // on every boot. The per-boot stamp must fire the version-change branch exactly once per
    // actual version change — with zero new migration rows.
    service().markDetected(); // boot 1: 2.13.0 stamped; migration history stays empty
    when(buildProperties.getVersion()).thenReturn("2.14.0");
    assertTrue(
        service().shouldRunDetection(),
        "version change with no new migration row must still trigger detection");
    service().markDetected(); // boot 2 (2.14.0): stamp updates

    // Same version again (restart): must NOT re-detect despite the migration history never
    // having been updated.
    assertFalse(
        service().shouldRunDetection(),
        "same-version boot after the stamp updated must not re-detect");
    assertEquals(0, historyRepository.count(), "no migration row was ever recorded");
  }

  @Test
  void markDetectedIsIdempotentPerVersion() {
    service().markDetected();
    service().markDetected();

    assertFalse(service().shouldRunDetection());
    assertEquals(
        1,
        gameSettingRepository.findGlobal(SeedUpdateReviewService.DETECTED_VERSION_KEY).stream()
            .count(),
        "stamp upserts in place — no duplicate game_setting rows");
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
    // Flip the removal row to a terminal status via its known coordinate, not an index into
    // findAll() (which would rely on IDENTITY insert order).
    SeedUpdateDecision removal =
        decisionRepository
            .findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
                "deck", "deck:legend", null, SeedUpdateDecision.STATUS_PENDING)
            .orElseThrow();
    removal.setStatus(SeedUpdateDecision.STATUS_APPLIED_REMOVE);
    decisionRepository.save(removal);

    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertEquals(2, service.pendingCount());
  }

  @Test
  void gateStartsUpOnBootWithPendingDecisions() {
    // Simulates a restart with undecided decisions left over from a previous boot: the rows are
    // already in the database when the gate bean is created. The gate must be correct from the
    // first request after any boot — it seeds its own snapshot on construction; nothing else is
    // guaranteed to call refresh() before the interceptors read it.
    service()
        .parkFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateReviewService freshBoot = service();

    assertTrue(freshBoot.gate().isUp());
    assertEquals(1, freshBoot.gate().pendingCount());
  }

  @Test
  void duplicateParkRefreshAlsoRaisesGate() {
    // Gate created while no PENDING row exists: snapshot 0, gate down.
    SeedUpdateReviewService service = service();
    assertFalse(gate.isUp());

    // A PENDING row appears in the database without the service's insert path (e.g. left by a
    // previous boot). The gate's snapshot is now stale: still down.
    SeedUpdateDecision external = new SeedUpdateDecision();
    external.setDomainKey("wrestler");
    external.setEntityKey("wrestler:bret-hart");
    external.setEntityDisplayName("Bret Hart");
    external.setFieldName("catchphrase");
    external.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    external.setCurrentValue("mine");
    external.setNewValue("new");
    external.setNewFingerprint("fp-external");
    external.setStatus(SeedUpdateDecision.STATUS_PENDING);
    external.setAppVersion(CURRENT_VERSION);
    decisionRepository.save(external);
    assertFalse(gate.isUp());

    // Re-parking the same coordinate goes through the refresh path (row exists, no insert) —
    // and that path must raise the gate too: an undecided decision exists.
    service.parkFieldConflict(
        "wrestler",
        "wrestler:bret-hart",
        "Bret Hart",
        "catchphrase",
        "The Excellence of Execution",
        "The Best There Is, The Best There Was",
        "fp-2");

    assertTrue(gate.isUp());
    assertEquals(1, gate.pendingCount());
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
    SeedUpdateDecision row =
        decisionRepository
            .findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
                "npc", "npc:samael", null, SeedUpdateDecision.STATUS_PENDING)
            .orElseThrow();
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

  // ── Batched pending notification (Task 11) ─────────────────────────────────

  @Test
  void notifyPendingPublishesOnceAfterParking() {
    SeedUpdateReviewService service = service();
    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    service.parkRemoval("npc", "npc:samael", "Samael", "fp-2");

    service.notifyPending();

    // One batched event (never per row) carrying the pending count and the boot's version.
    ArgumentCaptor<SeedUpdateReviewPendingEvent> event =
        ArgumentCaptor.forClass(SeedUpdateReviewPendingEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertEquals(2, event.getValue().getPendingCount());
    assertEquals(CURRENT_VERSION, event.getValue().getAppVersion());
  }

  @Test
  void notifyPendingWithoutParkingPublishesNothing() {
    service().notifyPending();

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void notifyPendingIsInertAfterFirstPublish() {
    SeedUpdateReviewService service = service();
    service.parkRemoval("npc", "npc:samael", "Samael", "fp-1");

    service.notifyPending();
    service.notifyPending();

    // The flag is consumed on the first publish — a repeated invocation must not re-notify.
    verify(eventPublisher, Mockito.times(1)).publishEvent(any(SeedUpdateReviewPendingEvent.class));
  }

  @Test
  void reParkRefreshDoesNotArmNotification() {
    SeedUpdateReviewService service = service();

    // First boot: a fresh park arms the flag, and notifyPending() consumes it.
    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    service.notifyPending();
    verify(eventPublisher).publishEvent(any(SeedUpdateReviewPendingEvent.class));

    // A later same-version boot re-parks the (still unresolved) coordinate: that path only
    // refreshes the incoming snapshot and must NOT re-arm the notification — otherwise every
    // restart would duplicate the inbox item while the decision stays pending.
    service.parkFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "newer", "fp-2");
    assertEquals(1, decisionRepository.count(), "Re-park must stay on the same decision row");

    service.notifyPending();
    verify(eventPublisher, Mockito.times(1)).publishEvent(any(SeedUpdateReviewPendingEvent.class));
  }
}
