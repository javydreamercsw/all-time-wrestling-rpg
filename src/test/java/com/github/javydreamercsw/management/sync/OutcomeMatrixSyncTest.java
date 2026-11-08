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
package com.github.javydreamercsw.management.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the outcome_matrices seed sync, including the version-change pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutcomeMatrixSyncTest {

  @Mock private OutcomeMatrixService outcomeMatrixService;
  @Mock private OutcomeMatrixRepository matrixRepository;
  @Mock private OutcomeMatrixEntryRepository entryRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private OutcomeMatrixSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(outcomeMatrixService.count()).thenReturn(0L);
  }

  private OutcomeMatrixSync sync(String mode) {
    return new OutcomeMatrixSync(
        mode,
        outcomeMatrixService,
        matrixRepository,
        entryRepository,
        new org.springframework.core.io.support.PathMatchingResourcePatternResolver(),
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static OutcomeMatrix matrix(String name, String description, OutcomeMatrixCategory cat) {
    OutcomeMatrix m = new OutcomeMatrix();
    ReflectionTestUtils.setField(m, "id", 1L);
    m.setName(name);
    m.setDescription(description);
    m.setCategory(cat);
    return m;
  }

  private static OutcomeMatrixEntry entry(OutcomeMatrix m, int roll, String text) {
    OutcomeMatrixEntry e = new OutcomeMatrixEntry();
    e.setMatrix(m);
    e.setDiceRoll(roll);
    e.setTemplateText(text);
    return e;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin across the domain's two row families, against the real seed files (the
    // detection pass reads outcome_matrices/*.json). The "Feud Angle" MATRIX header is the
    // tier-1 row (clean: stamp hashes the current vector; the seed's description changed since
    // → silent update + re-stamp); its roll-1 ENTRY is the tier-2 row — an ADMIN-REWRITTEN
    // templateText whose seed value ALSO changed since the stamp. Every other entry field
    // equals the incoming seed (heatDelta 7, fanDelta -500, grudgeGradeDelta 2, injuryCaused
    // true, no redirect) → fail-safe parks exactly templateText; the tier-3 legs are those
    // NO_OP fields plus the matrix category.
    OutcomeMatrix tier1matrix =
        matrix("Feud Angle", "stale description", OutcomeMatrixCategory.FEUD_ANGLE);
    tier1matrix.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("stale description", "FEUD_ANGLE"));

    OutcomeMatrixEntry tier2entry = entry(tier1matrix, 1, "admin's template");
    tier2entry.setHeatDelta(7);
    tier2entry.setFanDelta(-500L);
    tier2entry.setGrudgeGradeDelta(2);
    tier2entry.setInjuryCaused(true);
    // The stamp carries the PREVIOUS release's seed templateText while the row shows the admin's
    // edit and the incoming seed ships a third value → genuine tier-2 collision (fail-safe).
    tier2entry.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("previous seed template", "7", "-500", null, "2", "true", null));

    when(outcomeMatrixService.count()).thenReturn(1L);
    when(outcomeMatrixService.getByName("Feud Angle")).thenReturn(Optional.of(tier1matrix));
    when(entryRepository.findAllWithMatrixAndRedirect()).thenReturn(List.of(tier2entry));

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    OutcomeMatrixSync detectionSync =
        new OutcomeMatrixSync(
            "true",
            outcomeMatrixService,
            matrixRepository,
            entryRepository,
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver(),
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1 (matrix header): the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Storyline developments that drive ongoing rivalries and generate dramatic twists",
        tier1matrix.getDescription(),
        "tier 1: untouched matrix description with a changed seed value must update silently");
    // Tier 2 (entry row): the admin's template kept its value; the row parked the collision.
    assertEquals(
        "admin's template", tier2entry.getTemplateText(), "tier 2: admin edit must keep its value");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            eq("outcome_matrix"),
            eq("entry:feud-angle#1"),
            eq("Feud Angle (roll 1)"),
            eq("templateText"),
            eq("admin's template"),
            eq(
                "{WRESTLER_1} hits {WRESTLER_2} with a steel chair during a tag team match,"
                    + " shattering their alliance in front of a stunned arena."),
            anyString());
    assertEquals(
        java.util.Set.of("entry:feud-angle#1|templateText"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    OutcomeMatrixSync fullSync = sync("false");
    when(outcomeMatrixService.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(outcomeMatrixService, never()).getByName(anyString());
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps both row families: the service's create/addEntry leaves stamps unset,
    // and the sync stamps them through the service.
    OutcomeMatrix seeded = matrix("Feud Angle", null, OutcomeMatrixCategory.FEUD_ANGLE);
    OutcomeMatrixEntry seededEntry = entry(seeded, 1, "template");
    when(outcomeMatrixService.count()).thenReturn(0L);
    when(outcomeMatrixService.getByName("Feud Angle")).thenReturn(Optional.of(seeded));
    when(outcomeMatrixService.createMatrix(any())).thenReturn(seeded);
    when(outcomeMatrixService.getEntries(seeded.getId())).thenReturn(List.of(seededEntry));
    when(outcomeMatrixService.addEntry(any(), any())).thenReturn(seededEntry);
    when(outcomeMatrixService.updateEntry(any())).thenReturn(seededEntry);
    when(outcomeMatrixService.updateMatrix(any())).thenReturn(seeded);

    sync.sync();

    verify(outcomeMatrixService, org.mockito.Mockito.atLeastOnce())
        .updateMatrix(org.mockito.ArgumentMatchers.argThat(m -> m.getSeedFingerprint() != null));
    verify(outcomeMatrixService, org.mockito.Mockito.atLeastOnce())
        .updateEntry(org.mockito.ArgumentMatchers.argThat(e -> e.getSeedFingerprint() != null));
  }

  @Test
  void parkRowKeepsItsStampForRecollision() {
    // The tier-2 leg's no-write pin for the entry family: a parked entry keeps its stale stamp so
    // it re-collides after resolution.
    OutcomeMatrix m = matrix("Feud Angle", null, OutcomeMatrixCategory.FEUD_ANGLE);
    OutcomeMatrixEntry parked = entry(m, 1, "admin's template");
    parked.setHeatDelta(7);
    parked.setFanDelta(-500L);
    parked.setGrudgeGradeDelta(2);
    parked.setInjuryCaused(true);
    parked.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("previous seed template", "7", "-500", null, "2", "true", null));

    when(outcomeMatrixService.count()).thenReturn(1L);
    when(outcomeMatrixService.getByName("Feud Angle")).thenReturn(Optional.of(m));
    when(entryRepository.findAllWithMatrixAndRedirect()).thenReturn(List.of(parked));

    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doNothing()
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    OutcomeMatrixSync detectionSync =
        new OutcomeMatrixSync(
            "true",
            outcomeMatrixService,
            matrixRepository,
            entryRepository,
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver(),
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    verify(entryRepository, never()).saveAll(any());
    assertEquals(
        VERIFY_SUPPORT.fingerprint("previous seed template", "7", "-500", null, "2", "true", null),
        parked.getSeedFingerprint(),
        "a fail-safe parked row keeps its stale stamp so it re-collides after resolution");
  }
}
