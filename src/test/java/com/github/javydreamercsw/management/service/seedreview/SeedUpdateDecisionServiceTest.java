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
package com.github.javydreamercsw.management.service.seedreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Contract tests for the decision apply service (seed-update review feature, Task 9).
 *
 * <p>Runs against the real repository on in-memory H2 (same base as {@code
 * SeedUpdateReviewServiceTest}) because the assertions are persistence-shaped: apply must move
 * exactly the pending rows to their terminal statuses and leave everything else untouched. The
 * applier SPI is exercised through a {@link RecordingApplier} stub that records dispatched calls
 * instead of touching real entities (real per-domain appliers arrive in the follow-up PR).
 */
class SeedUpdateDecisionServiceTest extends AbstractJpaTest {

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;

  /** Gate of the most recently created service, for cross-path state assertions. */
  private SeedReviewGate gate;

  @BeforeEach
  void resetDecisionState() {
    // Deterministic starting point: no parked decisions from the boot's data initializer.
    // deleteAllInBatch() issues an immediate DELETE, so nothing survives the clear() below.
    decisionRepository.deleteAllInBatch();
    entityManager.clear();
    gate = null;
  }

  private SeedUpdateDecisionService service(SeedDomainApplier... appliers) {
    gate = new SeedReviewGate(decisionRepository);
    return new SeedUpdateDecisionService(decisionRepository, List.of(appliers), gate);
  }

  /** Forces the test transaction's writes to the database and re-reads entities fresh. */
  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private SeedUpdateDecision pendingFieldConflict(
      String domainKey,
      String entityKey,
      String displayName,
      String fieldName,
      String currentValue,
      String newValue,
      String newFingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(fieldName);
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    decision.setCurrentValue(currentValue);
    decision.setNewValue(newValue);
    decision.setNewFingerprint(newFingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decisionRepository.save(decision);
  }

  private SeedUpdateDecision pendingRemoval(
      String domainKey, String entityKey, String displayName, String newFingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(null); // NULL for removal decisions
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    decision.setCurrentValue(null); // NULL for removals
    decision.setNewValue(null); // NULL for removals
    decision.setNewFingerprint(newFingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decisionRepository.save(decision);
  }

  @Test
  void applyWritesOnlyDecidedFields() {
    RecordingApplier wrestlerApplier = new RecordingApplier("wrestler");
    RecordingApplier npcApplier = new RecordingApplier("npc");
    SeedUpdateDecision bret =
        pendingFieldConflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "catchphrase",
            "The Excellence of Execution",
            "The Best There Is",
            "fp-1");
    SeedUpdateDecision shawn =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "Heartbreak Kid",
            "Sexy Boy",
            "fp-2");
    // Not pending anymore: an already-applied row must never reach any applier.
    SeedUpdateDecision applied =
        pendingFieldConflict(
            "wrestler",
            "wrestler:undertaker",
            "The Undertaker",
            "nickname",
            "The Deadman",
            "The Phenom",
            "fp-3");
    applied.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP);
    decisionRepository.save(applied);

    SeedUpdateDecisionService service = service(wrestlerApplier, npcApplier);
    service.apply(
        Map.of(
            bret.getId(), SeedFieldDecision.KEEP_MINE, shawn.getId(), SeedFieldDecision.USE_NEW));

    // The stub applier received EXACTLY the pending rows with their mapped choices, in id order.
    assertEquals(
        List.of("applyKeep:wrestler:bret-hart", "applyNew:wrestler:shawn-michaels"),
        wrestlerApplier.calls);
    assertEquals(List.of(bret.getId(), shawn.getId()), wrestlerApplier.receivedDecisionIds);
    // No other domain's applier was invoked, and the non-pending row was never dispatched.
    assertTrue(npcApplier.calls.isEmpty());
    assertFalse(wrestlerApplier.receivedDecisionIds.contains(applied.getId()));
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(bret.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(shawn.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(applied.getId()).orElseThrow().getStatus());
  }

  @Test
  void applyIsAllOrNothing() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision decided =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecision missing =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "mine",
            "new",
            "fp-2");
    SeedUpdateDecisionService service = service(applier);

    // Every pending id must be present in the map before anything is dispatched or modified.
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service.apply(Map.of(decided.getId(), SeedFieldDecision.USE_NEW)));

    assertTrue(exception.getMessage().contains(String.valueOf(missing.getId())));
    assertTrue(applier.calls.isEmpty());
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(decided.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(missing.getId()).orElseThrow().getStatus());
    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
  }

  @Test
  void keepMineStampsNewManifestHash() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision row =
        pendingFieldConflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "catchphrase",
            "The Excellence of Execution",
            "The Best There Is",
            "fp-detection-42");
    SeedUpdateDecisionService service = service(applier);

    service.apply(Map.of(row.getId(), SeedFieldDecision.KEEP_MINE));

    // Service half of the keep-mine stamp contract: the applier receives the FULL decision row so
    // it can read the detection-time fingerprint and stamp it on the entity.
    assertEquals("fp-detection-42", applier.receivedDecisions.get(0).getNewFingerprint());
    flushAndClear();
    SeedUpdateDecision appliedRow = decisionRepository.findById(row.getId()).orElseThrow();
    assertEquals(SeedUpdateDecision.STATUS_APPLIED_KEEP, appliedRow.getStatus());
    // The row's newFingerprint survives apply unchanged — the applier (not this service) owns the
    // stamp write; the service must not clear or overwrite it.
    assertEquals("fp-detection-42", appliedRow.getNewFingerprint());
  }

  @Test
  void approveEverythingExcludesRemovals() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision conflict1 =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecision conflict2 =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "mine",
            "new",
            "fp-2");
    SeedUpdateDecision removal = pendingRemoval("wrestler", "wrestler:jobber", "Jobber", "fp-3");
    SeedUpdateDecisionService service = service(applier);

    Map<Long, SeedFieldDecision> decisions = service.approveEverythingExceptRemovals();

    // Field conflicts map to USE_NEW; removal rows are excluded — they need explicit per-entity
    // choices because removing data the seed no longer carries is never automatic.
    assertEquals(
        Map.of(
            conflict1.getId(),
            SeedFieldDecision.USE_NEW,
            conflict2.getId(),
            SeedFieldDecision.USE_NEW),
        decisions);
    assertFalse(decisions.containsKey(removal.getId()));
    // Convenience only: it BUILDS the map for the UI to pass back to apply() — the Apply button
    // stays the single write point, so nothing is dispatched and no row changes here.
    assertTrue(applier.calls.isEmpty());
    assertEquals(3, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
  }

  @Test
  void removeRefusedWhenDependentsExist() {
    RecordingApplier applier = new RecordingApplier("npc");
    applier.dependentsExist = true;
    SeedUpdateDecision removal = pendingRemoval("npc", "npc:samael", "Samael", "fp-1");
    SeedUpdateDecisionService service = service(applier);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service.apply(Map.of(removal.getId(), SeedFieldDecision.REMOVE)));

    assertTrue(exception.getMessage().contains("npc:samael"));
    // Refused before dispatch: applyRemove never runs and the decision stays PENDING.
    assertTrue(applier.calls.isEmpty());
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(removal.getId()).orElseThrow().getStatus());
  }

  @Test
  void applyUnknownDomainFails() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision ghost =
        pendingFieldConflict(
            "ghost-domain", "ghost:entity", "Ghost", "field", "mine", "new", "fp-1");
    SeedUpdateDecisionService service = service(applier);

    // Fail loud: an apply against a domain with no registered applier must never silently no-op.
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service.apply(Map.of(ghost.getId(), SeedFieldDecision.USE_NEW)));

    assertTrue(exception.getMessage().contains("ghost-domain"));
    assertTrue(applier.calls.isEmpty());
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(ghost.getId()).orElseThrow().getStatus());
  }

  @Test
  void applyLowersGateAfterSuccess() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    pendingFieldConflict(
        "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecisionService service = service(applier);
    // Rows pre-exist the gate bean, whose constructor refresh() sees them: gate up.
    assertTrue(gate.isUp());

    service.apply(
        Map.of(service.pendingGrouped().get(0).getId(), SeedFieldDecision.KEEP_AS_CUSTOM));

    // After ALL rows are applied the gate re-reads its snapshot — gameplay is unfrozen.
    assertFalse(gate.isUp());
    assertEquals(0, gate.pendingCount());
    assertEquals(0, service.pendingCount());
  }

  @Test
  void applyMapsEveryChoiceToItsTerminalStatus() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision keep =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecision useNew =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "mine",
            "new",
            "fp-2");
    SeedUpdateDecision custom =
        pendingFieldConflict(
            "wrestler", "wrestler:undertaker", "The Undertaker", "nickname", "mine", "new", "fp-3");
    SeedUpdateDecisionService service = service(applier);

    service.apply(
        Map.of(
            keep.getId(),
            SeedFieldDecision.KEEP_MINE,
            useNew.getId(),
            SeedFieldDecision.USE_NEW,
            custom.getId(),
            SeedFieldDecision.KEEP_AS_CUSTOM));

    assertEquals(
        List.of(
            "applyKeep:wrestler:bret-hart",
            "applyNew:wrestler:shawn-michaels",
            "applyKeepAsCustom:wrestler:undertaker"),
        applier.calls);
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP,
        decisionRepository.findById(keep.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_NEW,
        decisionRepository.findById(useNew.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM,
        decisionRepository.findById(custom.getId()).orElseThrow().getStatus());
  }

  @Test
  void removeAllowedWhenNoDependents() {
    RecordingApplier applier = new RecordingApplier("npc");
    SeedUpdateDecision removal = pendingRemoval("npc", "npc:jobber", "Jobber", "fp-1");
    SeedUpdateDecisionService service = service(applier);

    service.apply(Map.of(removal.getId(), SeedFieldDecision.REMOVE));

    assertEquals(List.of("applyRemove:npc:jobber"), applier.calls);
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_APPLIED_REMOVE,
        decisionRepository.findById(removal.getId()).orElseThrow().getStatus());
  }

  @Test
  void pendingGroupedReturnsPendingRowsInDetectionOrder() {
    RecordingApplier applier = new RecordingApplier("wrestler");
    SeedUpdateDecision first =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecision second = pendingRemoval("wrestler", "wrestler:jobber", "Jobber", "fp-2");
    SeedUpdateDecision third =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "mine",
            "new",
            "fp-3");
    // Resolve the middle row out-of-band: only the two survivors stay in the queue.
    second.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP);
    decisionRepository.save(second);
    SeedUpdateDecisionService service = service(applier);

    List<SeedUpdateDecision> pending = service.pendingGrouped();

    // Contract is the ordered pending list (id order = detection order); the UI does its own
    // domain/entity grouping on top of it.
    assertEquals(
        List.of(first.getId(), third.getId()),
        pending.stream().map(SeedUpdateDecision::getId).toList());
    assertEquals(2, service.pendingCount());
  }

  /** Stub applier that records dispatched calls instead of touching real entities. */
  private static final class RecordingApplier implements SeedDomainApplier {

    private final String domain;
    private final List<String> calls = new ArrayList<>();
    private final List<Long> receivedDecisionIds = new ArrayList<>();
    private final List<SeedUpdateDecision> receivedDecisions = new ArrayList<>();
    private boolean dependentsExist;

    private RecordingApplier(String domain) {
      this.domain = domain;
    }

    @Override
    public String domainKey() {
      return domain;
    }

    @Override
    public void applyKeep(SeedUpdateDecision decision) {
      record("applyKeep", decision);
    }

    @Override
    public void applyNew(SeedUpdateDecision decision) {
      record("applyNew", decision);
    }

    @Override
    public void applyKeepAsCustom(SeedUpdateDecision decision) {
      record("applyKeepAsCustom", decision);
    }

    @Override
    public void applyRemove(SeedUpdateDecision decision) {
      record("applyRemove", decision);
    }

    @Override
    public boolean hasDependents(String entityKey) {
      return dependentsExist;
    }

    private void record(String method, SeedUpdateDecision decision) {
      calls.add(method + ":" + decision.getEntityKey());
      receivedDecisionIds.add(decision.getId());
      receivedDecisions.add(decision);
    }
  }

  // ── Atomicity (PR 4 carry ATW-1bvw item a): a real applier throwing mid-batch must persist
  // zero decisions — the rollback is observable through the real repository. ──

  /** Real applier that flips one row's status, then blows up on the second decision. */
  private static final class ExplodingApplier implements SeedDomainApplier {

    private final String domain;
    private int dispatched;

    private ExplodingApplier(String domain) {
      this.domain = domain;
    }

    @Override
    public String domainKey() {
      return domain;
    }

    @Override
    public void applyKeep(SeedUpdateDecision decision) {
      dispatched++;
      if (dispatched >= 2) {
        throw new IllegalStateException("applier exploded mid-batch");
      }
    }

    @Override
    public void applyNew(SeedUpdateDecision decision) {
      throw new UnsupportedOperationException("not exercised by this test");
    }

    @Override
    public void applyKeepAsCustom(SeedUpdateDecision decision) {
      throw new UnsupportedOperationException("not exercised by this test");
    }

    @Override
    public void applyRemove(SeedUpdateDecision decision) {
      throw new UnsupportedOperationException("not exercised by this test");
    }

    @Override
    public boolean hasDependents(String entityKey) {
      return false;
    }
  }

  @Test
  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void applyRollsBackEverythingWhenApplierThrowsMidBatch() {
    // Real (Spring-proxied) applier service, real repository, REAL transaction boundary: the
    // base class's test transaction is suspended (NOT_SUPPORTED) so the PROXIED service's
    // @Transactional opens its own transaction — the first KEEP_MINE dispatch succeeds, the
    // second explodes, and the rollback must leave ZERO decisions terminal in the database.
    ExplodingApplier applier = new ExplodingApplier("wrestler");
    SeedUpdateDecisionService proxied =
        new SeedUpdateDecisionService(
            decisionRepository,
            List.of(applier),
            new com.github.javydreamercsw.management.sync.review.SeedReviewGate(
                decisionRepository));
    org.springframework.transaction.support.TransactionTemplate template =
        new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    SeedUpdateDecision first =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    SeedUpdateDecision second =
        pendingFieldConflict(
            "wrestler",
            "wrestler:shawn-michaels",
            "Shawn Michaels",
            "theme",
            "mine",
            "new",
            "fp-2");

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                template.executeWithoutResult(
                    status ->
                        proxied.apply(
                            Map.of(
                                first.getId(), SeedFieldDecision.KEEP_MINE,
                                second.getId(), SeedFieldDecision.KEEP_MINE))));

    assertTrue(exception.getMessage().contains("exploded mid-batch"));
    // Rollback observable: re-read from the database — both rows must still be PENDING, exactly
    // as before the failed batch (the mid-batch APPLIED_KEEP write rolled back with the rest).
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(first.getId()).orElseThrow().getStatus());
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(second.getId()).orElseThrow().getStatus());
    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
  }

  @Test
  void applyRefusesBeforeDispatchWhenDomainHasNoApplier() {
    // Hoisted validation (ATW-1bvw item b): with two pending rows — one resolvable, one in an
    // unregistered domain — the apply must fail in the VALIDATION phase, before ANY dispatch.
    // The resolvable row's applier must never be called (pre-hoist behavior dispatched rows in
    // id order and failed mid-batch on the unresolvable one).
    RecordingApplier wrestlerApplier = new RecordingApplier("wrestler");
    SeedUpdateDecision resolvable =
        pendingFieldConflict(
            "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase", "mine", "new", "fp-1");
    pendingFieldConflict("ghost-domain", "ghost:entity", "Ghost", "field", "mine", "new", "fp-2");
    SeedUpdateDecisionService service = service(wrestlerApplier);

    assertThrows(
        IllegalStateException.class,
        () ->
            service.apply(
                Map.of(
                    resolvable.getId(),
                    SeedFieldDecision.USE_NEW,
                    service.pendingGrouped().stream()
                        .filter(d -> "ghost-domain".equals(d.getDomainKey()))
                        .findFirst()
                        .orElseThrow()
                        .getId(),
                    SeedFieldDecision.USE_NEW)));

    assertTrue(
        wrestlerApplier.calls.isEmpty(),
        "validation must run before ANY dispatch — a resolvable row in a batch with an"
            + " unresolvable one must not be applied");
    flushAndClear();
    assertEquals(
        SeedUpdateDecision.STATUS_PENDING,
        decisionRepository.findById(resolvable.getId()).orElseThrow().getStatus());
  }
}
