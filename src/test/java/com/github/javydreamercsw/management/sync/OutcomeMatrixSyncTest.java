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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
        new PathMatchingResourcePatternResolver(),
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

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              parkedCoordinates.add("#" + inv.getArgument(2));
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
            new PathMatchingResourcePatternResolver(),
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1 (matrix header): the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Storyline developments that drive ongoing rivalries and generate dramatic twists",
        tier1matrix.getDescription(),
        "tier 1: untouched matrix description with a changed seed value must update silently");
    // Sync-side tier-1 re-stamp pin (ATW-1rmi item 6): the silent write also re-stamps the row
    // to the incoming seed's content fingerprint.
    assertEquals(
        VERIFY_SUPPORT.fingerprint(
            "Storyline developments that drive ongoing rivalries and generate dramatic twists",
            "FEUD_ANGLE"),
        tier1matrix.getSeedFingerprint(),
        "tier 1: the silent update must re-stamp the row");
    // Tier 2 (entry row): the admin's template kept its value; the row parked the collision.
    assertEquals(
        "admin's template", tier2entry.getTemplateText(), "tier 2: admin edit must keep its value");
    Mockito.verify(parking)
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
        Set.of("entry:feud-angle#1|templateText", "#Feud Angle (roll 1)"),
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

    verify(outcomeMatrixService, Mockito.atLeastOnce())
        .updateMatrix(ArgumentMatchers.argThat(m -> m.getSeedFingerprint() != null));
    verify(outcomeMatrixService, Mockito.atLeastOnce())
        .updateEntry(ArgumentMatchers.argThat(e -> e.getSeedFingerprint() != null));
  }

  @Test
  void versionChange_silentUpdatesCoverCategoryAndEntryArms() {
    // Tier-1 arms the three-tier pin never reached, driven against the real feud_angle.json:
    // the "Feud Angle" MATRIX header is stamped clean at a stale category (MATCH_FLOW vs the
    // seed's FEUD_ANGLE) so the category SILENT_UPDATE arm fires; the roll-3 entry is stamped
    // clean (hash of its current values) at drifted mechanical values so the delta, injuryCaused
    // and null-redirect arms all fire in one pass; the roll-2 entry equals the seed exactly, so
    // it takes the stamp-only save path through a manifest hash.
    String roll2Text =
        "{WRESTLER_1} leaves {WRESTLER_2} alone in the ring to be systematically destroyed by a"
            + " rival stable, walking up the ramp with a smirk.";
    String roll3Text =
        "{WRESTLER_1} grabs a microphone and reveals they only teamed with {WRESTLER_2} to keep"
            + " them away from the championship picture.";
    OutcomeMatrix matrix =
        matrix(
            "Feud Angle",
            "Storyline developments that drive ongoing rivalries and generate dramatic twists",
            OutcomeMatrixCategory.MATCH_FLOW);
    matrix.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "Storyline developments that drive ongoing rivalries and generate dramatic twists",
            "MATCH_FLOW"));

    OutcomeMatrixEntry stampOnly = entry(matrix, 2, roll2Text);
    stampOnly.setHeatDelta(5);
    stampOnly.setFanDelta(-300L);
    stampOnly.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(roll2Text, "5", "-300", null, null, "false", null));

    OutcomeMatrixEntry drift = entry(matrix, 3, roll3Text);
    drift.setHeatDelta(9);
    drift.setFanDelta(9L);
    drift.setTvGradeDelta(9);
    drift.setGrudgeGradeDelta(9);
    drift.setInjuryCaused(true);
    drift.setRedirectToMatrix(matrix);
    drift.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(roll3Text, "9", "9", "9", "9", "true", "Feud Angle"));

    when(outcomeMatrixService.count()).thenReturn(1L);
    when(outcomeMatrixService.getByName("Feud Angle")).thenReturn(Optional.of(matrix));
    when(entryRepository.findAllWithMatrixAndRedirect()).thenReturn(List.of(stampOnly, drift));
    when(outcomeMatrixService.updateMatrix(any())).thenReturn(matrix);

    SeedProvenanceManifest manifest =
        SeedProvenanceManifest.of(
            Map.of("outcome_matrix", Map.of("Feud Angle (roll 2)", "manifest-hash-roll2")));
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    OutcomeMatrixSync detectionSync =
        new OutcomeMatrixSync(
            "true",
            outcomeMatrixService,
            matrixRepository,
            entryRepository,
            new PathMatchingResourcePatternResolver(),
            new ObjectMapper(),
            parking,
            manifest);
    detectionSync.detectVersionChangeCollisions();

    assertEquals(
        OutcomeMatrixCategory.FEUD_ANGLE,
        matrix.getCategory(),
        "category SILENT_UPDATE arm wrote the seed value");
    Mockito.verify(outcomeMatrixService, Mockito.atLeastOnce()).updateMatrix(matrix);
    // Stamp-only row: everything equals the seed → no silent write, only the manifest re-stamp.
    assertEquals(
        "manifest-hash-roll2",
        stampOnly.getSeedFingerprint(),
        "a clean seed-unchanged row must adopt the manifest hash (stamp-only save)");
    // Drifted entry: every mechanical arm wrote the seed value; the null redirect arm cleared.
    assertEquals(4, drift.getHeatDelta(), "heatDelta arm wrote the seed value");
    assertEquals(-250L, drift.getFanDelta(), "fanDelta arm wrote the seed value");
    assertNull(drift.getTvGradeDelta(), "tvGradeDelta arm wrote the seed's null");
    assertNull(drift.getGrudgeGradeDelta(), "grudgeGradeDelta arm wrote the seed's null");
    assertFalse(drift.isInjuryCaused(), "injuryCaused arm wrote the seed value");
    assertNull(drift.getRedirectToMatrix(), "null-redirect silent arm cleared the reference");
    assertEquals(
        VERIFY_SUPPORT.fingerprint(roll3Text, "4", "-250", null, null, "false", null),
        drift.getSeedFingerprint(),
        "the tier-1 entry re-stamps with the incoming content hash");
    verify(entryRepository)
        .saveAll(
            Mockito.argThat(
                l ->
                    StreamSupport.stream(l.spliterator(), false).anyMatch(e -> e == drift)
                        && StreamSupport.stream(l.spliterator(), false)
                            .anyMatch(e -> e == stampOnly)));
    Mockito.verify(parking, Mockito.never())
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
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

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doNothing()
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    OutcomeMatrixSync detectionSync =
        new OutcomeMatrixSync(
            "true",
            outcomeMatrixService,
            matrixRepository,
            entryRepository,
            new PathMatchingResourcePatternResolver(),
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
