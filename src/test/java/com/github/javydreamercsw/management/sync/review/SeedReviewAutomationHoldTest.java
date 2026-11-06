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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.migration.DataMigrationHistoryRepository;
import com.github.javydreamercsw.management.service.GameClock;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryDecayService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Automation-hold contract tests for the seed-update review gate (Task 8): while the gate is up,
 * the game-date advance and the scheduled gameplay writers must return early, and the {@code
 * seed.review.force-resolve=keep-all} escape hatch must force-decide pending rows as keep-mine and
 * lower the gate.
 *
 * <p>The gate itself is real ({@link SeedReviewGate} over the real decision repository) so "gate
 * up" means an actual PENDING row exists; only each held component's own collaborators are mocked.
 * The force-resolve assertions run against the real repositories on in-memory H2 (same base as
 * {@code SeedUpdateReviewServiceTest}) because "only PENDING rows are touched" is a
 * persistence-shaped contract.
 */
class SeedReviewAutomationHoldTest extends AbstractJpaTest {

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private DataMigrationHistoryRepository historyRepository;
  @Autowired private EntityManager entityManager;

  private SeedReviewGate gate;
  private SeedUpdateReviewService reviewService;
  private SeedReviewConfig config;

  @BeforeEach
  void resetReviewState() {
    decisionRepository.deleteAllInBatch();
    historyRepository.deleteAllInBatch();
    entityManager.clear();
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.13.0");
    gate = new SeedReviewGate(decisionRepository);
    reviewService =
        new SeedUpdateReviewService(
            decisionRepository, historyRepository, Optional.of(buildProperties), gate);
    config = new SeedReviewConfig(reviewService);
  }

  private void parkFieldConflict() {
    reviewService.parkFieldConflict(
        "wrestler",
        "wrestler:bret-hart",
        "Bret Hart",
        "catchphrase",
        "The Excellence of Execution",
        "The Best There Is",
        "fp-hold-1");
  }

  private void parkRemoval() {
    reviewService.parkRemoval("npc", "npc:samael", "Samael", "fp-hold-2");
  }

  // ── Game-date advance hold ────────────────────────────────────────────────

  @Test
  void gameClockDoesNotAdvanceUnderGate() {
    parkFieldConflict();
    assertTrue(gate.isUp());

    GameSettingRepository gameSettingRepository = mock(GameSettingRepository.class);
    ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    UniverseContextService universeContextService = mock(UniverseContextService.class);
    GameSettingService gameSettingService =
        new GameSettingService(gameSettingRepository, eventPublisher, universeContextService, gate);

    gameSettingService.saveCurrentGameDate(LocalDate.now().plusDays(1));

    // The advance must be refused before any persistence or event happens.
    verifyNoInteractions(gameSettingRepository);
    verifyNoInteractions(eventPublisher);
    assertEquals(
        1,
        decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING),
        "The parked decision must be untouched by the refused advance");
    assertTrue(gate.isUp());
  }

  // ── Scheduled gameplay-writer hold ────────────────────────────────────────

  /** Representative gameplay writer: the nightly rivalry maintenance (decay + auto-close). */
  @Test
  void scheduledWriterNoOpsUnderGate() {
    parkFieldConflict();
    assertTrue(gate.isUp());

    RivalryRepository rivalryRepository = mock(RivalryRepository.class);
    GameSettingService gameSettingService = mock(GameSettingService.class);
    GameClock gameClock = mock(GameClock.class);
    RivalryDecayService decayService =
        new RivalryDecayService(rivalryRepository, gameSettingService, gameClock, gate);

    decayService.runDailyMaintenance();

    // The early return must happen before any gameplay read or write.
    verifyNoInteractions(rivalryRepository);
    assertEquals(
        1,
        decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING),
        "The parked decision must be untouched by the held writer");
  }

  @Test
  void scheduledWriterRunsWhenGateDown() {
    assertFalse(gate.isUp());

    RivalryRepository rivalryRepository = mock(RivalryRepository.class);
    GameSettingService gameSettingService = mock(GameSettingService.class);
    GameClock gameClock = mock(GameClock.class);
    when(gameSettingService.isRivalryHeatDecayEnabled()).thenReturn(true);
    when(gameSettingService.getRivalryHeatDecayPerInterval()).thenReturn(1);
    when(gameSettingService.getRivalryHeatDecayIntervalDays()).thenReturn(7);
    when(gameClock.gameNow()).thenReturn(Instant.now());
    when(rivalryRepository.findByIsActiveTrue()).thenReturn(List.of());
    RivalryDecayService decayService =
        new RivalryDecayService(rivalryRepository, gameSettingService, gameClock, gate);

    decayService.runDailyMaintenance();

    // Negative control: the guard must not block gameplay when no decisions are pending.
    verify(rivalryRepository).findByIsActiveTrue();
  }

  // ── Force-resolve escape hatch ────────────────────────────────────────────

  @Test
  void forceResolveKeepAllAppliesAndLowersGate() {
    parkFieldConflict();
    parkRemoval();
    assertTrue(gate.isUp());
    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));

    ReflectionTestUtils.setField(config, "forceResolve", "keep-all");
    config.forceResolveIfConfigured();

    assertEquals(0, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertEquals(2, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP));
    assertFalse(gate.isUp());
    assertEquals(0, gate.pendingCount());
  }

  @Test
  void forceResolveIgnoredWithoutProperty() {
    parkFieldConflict();
    assertTrue(gate.isUp());

    // The property is never set — forceResolveIfConfigured() must be a no-op.
    config.forceResolveIfConfigured();

    assertEquals(1, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertTrue(gate.isUp());
  }

  @Test
  void forceResolveUnknownValueIsIgnored() {
    parkFieldConflict();

    ReflectionTestUtils.setField(config, "forceResolve", "true");
    config.forceResolveIfConfigured();

    assertEquals(1, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING));
    assertTrue(gate.isUp());
  }

  @Test
  void forceResolveOnlyTouchesPendingRows() {
    parkFieldConflict(); // stays PENDING → force-resolved
    parkRemoval(); // flipped to a terminal status BEFORE the hatch runs → must not be re-touched
    SeedUpdateDecision alreadyApplied =
        decisionRepository
            .findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
                "npc", "npc:samael", null, SeedUpdateDecision.STATUS_PENDING)
            .orElseThrow();
    alreadyApplied.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM);
    decisionRepository.save(alreadyApplied);
    String fingerprintBefore = alreadyApplied.getNewFingerprint();
    // Flush the status UPDATE before clearing, otherwise the terminal status never reaches the
    // database (the clear() discards the queued write) and the hatch would see it as PENDING.
    entityManager.flush();
    entityManager.clear();

    ReflectionTestUtils.setField(config, "forceResolve", "keep-all");
    config.forceResolveIfConfigured();

    // The previously applied row keeps its terminal status and its first-detection snapshot.
    SeedUpdateDecision untouched =
        decisionRepository.findById(alreadyApplied.getId()).orElseThrow();
    assertEquals(SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM, untouched.getStatus());
    assertEquals(fingerprintBefore, untouched.getNewFingerprint());

    // The PENDING row was force-decided as keep-mine.
    Optional<SeedUpdateDecision> forced =
        decisionRepository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            "wrestler",
            "wrestler:bret-hart",
            "catchphrase",
            SeedUpdateDecision.STATUS_APPLIED_KEEP);
    assertTrue(forced.isPresent());
    assertEquals(SeedUpdateDecision.STATUS_APPLIED_KEEP, forced.get().getStatus());
    assertEquals(1, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP));
    assertEquals(
        1, decisionRepository.countByStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM));
  }
}
