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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RingsideActionSyncTest {

  @Mock private RingsideActionDataService ringsideActionDataService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private RingsideActionSync sync(String mode) {
    return new RingsideActionSync(
        mode, ringsideActionDataService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private RingsideAction action(String name, String typeName) {
    RingsideAction a = new RingsideAction();
    a.setName(name);
    RingsideActionType type = new RingsideActionType();
    type.setName(typeName);
    a.setType(type);
    a.setExpansionCode("BASE_GAME");
    a.setAlignment(AlignmentType.NEUTRAL);
    return a;
  }

  private String actionFingerprint(RingsideAction a) {
    return VERIFY_SUPPORT.fingerprint(
        a.getType() == null ? null : a.getType().getName(),
        a.getDescription(),
        a.getImpact(),
        a.getRisk(),
        a.getAlignment() == null ? null : a.getAlignment().name(),
        a.getExpansionCode());
  }

  @Test
  void sync_topUpInsertsOnlyMissingActionsWhenSkipFlagSet() {
    RingsideActionSync sync = sync("true");
    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(1L);
    RingsideAction existing = action("Encouraging Call", "Legal Support");
    when(ringsideActionDataService.findAllActionsUnfiltered()).thenReturn(List.of(existing));
    when(ringsideActionDataService.createOrUpdateAction(
            anyString(), anyString(), any(), anyInt(), anyInt(), any(), any()))
        .thenAnswer(
            inv -> {
              RingsideAction a = action(inv.getArgument(0), inv.getArgument(1));
              a.setDescription(inv.getArgument(2));
              a.setImpact(inv.getArgument(3));
              a.setRisk(inv.getArgument(4));
              return a;
            });

    sync.sync();

    // Top-up: pre-existing rows are never re-saved via the service; only missing ones insert.
    verify(ringsideActionDataService, never())
        .createOrUpdateAction(
            ArgumentMatchers.eq("Encouraging Call"),
            anyString(),
            any(),
            anyInt(),
            anyInt(),
            any(),
            any());
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    RingsideActionSync verifySync = sync("verify");
    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(2L);

    // Clean: as an older release's seed write left it — description since fixed in the seed.
    RingsideAction clean = action("Encouraging Call", "Legal Support");
    clean.setDescription("stale description");
    clean.setImpact(1);
    clean.setRisk(1);
    clean.setSeedFingerprint(actionFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    RingsideAction edited = action("Coaching Advice", "Legal Support");
    edited.setDescription("hand-tuned");
    edited.setImpact(2);
    edited.setRisk(2);
    edited.setSeedFingerprint(actionFingerprint(edited));
    edited.setDescription("hand-tuned (changed after stamping)");

    when(ringsideActionDataService.findAllActionsUnfiltered()).thenReturn(List.of(clean, edited));
    when(ringsideActionDataService.createOrUpdateAction(
            anyString(), anyString(), any(), anyInt(), anyInt(), any(), any()))
        .thenAnswer(
            inv -> {
              RingsideAction a = action(inv.getArgument(0), inv.getArgument(1));
              a.setDescription(inv.getArgument(2));
              a.setImpact(inv.getArgument(3));
              a.setRisk(inv.getArgument(4));
              return a;
            });

    verifySync.sync();

    verify(ringsideActionDataService)
        .createOrUpdateAction(
            eqName("Encouraging Call"), anyString(), any(), anyInt(), anyInt(), any(), any());
    verify(ringsideActionDataService, never())
        .createOrUpdateAction(
            eqName("Coaching Advice"), anyString(), any(), anyInt(), anyInt(), any(), any());
    assertEquals(
        "hand-tuned (changed after stamping)",
        edited.getDescription(),
        "edited action must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    RingsideActionSync verifySync = sync("verify");
    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(1L);

    // Legacy: no stamp; fields match the seed's Encouraging Call entry.
    RingsideAction legacy = action("Encouraging Call", "Legal Support");
    legacy.setDescription(
        "A motivational shout from ringside. Provides a small win probability boost with no risk.");
    legacy.setImpact(5);
    legacy.setRisk(0);
    legacy.setAlignment(AlignmentType.FACE);
    when(ringsideActionDataService.findAllActionsUnfiltered()).thenReturn(List.of(legacy));
    when(ringsideActionDataService.createOrUpdateAction(
            anyString(), anyString(), any(), anyInt(), anyInt(), any(), any()))
        .thenAnswer(
            inv -> {
              RingsideAction a = action(inv.getArgument(0), inv.getArgument(1));
              a.setDescription(inv.getArgument(2));
              a.setImpact(inv.getArgument(3));
              a.setRisk(inv.getArgument(4));
              return a;
            });

    verifySync.sync();

    assertEquals(
        actionFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy action matching the seed must be adopted (fingerprint stamped)");
    verify(ringsideActionDataService).saveAction(legacy);
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline:
    //   tier 1 — untouched field (description), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (impact), seed changed → parked as PENDING decision, DB
    //            value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (risk edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    // The DB rows are stamped as the PREVIOUS release's seed write left them; the incoming seed
    // (ringside_actions.json, unchanged on disk) supplies the new values through the sync's normal
    // resource path. "Encouraging Call" is the tier-1/tier-3 row, "Coaching Advice" the tier-2 row.
    RingsideAction tier1row = action("Encouraging Call", "Legal Support");
    tier1row.setDescription("stale description"); // seed changed since
    tier1row.setImpact(5);
    tier1row.setRisk(0);
    tier1row.setSeedFingerprint(actionFingerprint(tier1row));
    tier1row.setRisk(3); // tier 3: admin edit AFTER the stamp, seed says 0

    // Coaching Advice's seed row: impact 8. The stamp carries the PREVIOUS release's impact (5)
    // while the row shows the admin's edit (12) and the incoming seed ships 8 → genuine tier-2
    // collision.
    RingsideAction tier2row = action("Coaching Advice", "Legal Support");
    tier2row.setDescription("Shout technical advice to the wrestler. Legal and helps strategy.");
    tier2row.setImpact(5);
    tier2row.setRisk(0);
    tier2row.setSeedFingerprint(actionFingerprint(tier2row));
    tier2row.setImpact(12); // tier 2: admin edit AFTER the stamp

    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(2L);
    when(ringsideActionDataService.findAllActionsUnfiltered())
        .thenReturn(List.of(tier1row, tier2row));
    when(ringsideActionDataService.findAllTypes()).thenReturn(List.of());

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    RingsideActionSync detectionSync =
        new RingsideActionSync(
            "true", ringsideActionDataService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the seed value.
    assertEquals(
        "A motivational shout from ringside. Provides a small win probability boost with no risk.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged risk edit preserved.
    assertEquals(
        3, tier1row.getRisk(), "tier 3: admin edit on an unchanged seed field must be preserved");
    // Tier 2: the admin-edited impact kept its value; the row parked the impact collision.
    assertEquals(12, tier2row.getImpact(), "tier 2: admin-edited field must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("ringside_action"),
            eq("ringside_action:coaching-advice"),
            eq("Coaching Advice"),
            eq("impact"),
            eq("12"),
            eq("8"),
            anyString());
    assertEquals(
        Set.of("ringside_action:coaching-advice|impact"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (FULL applies the seed everywhere).
    RingsideAction existing = action("Encouraging Call", "Legal Support");
    existing.setDescription("admin-tuned");
    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(1L);
    when(ringsideActionDataService.findAllActionsUnfiltered()).thenReturn(List.of(existing));

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    RingsideActionSync fullModeSync = sync("false");
    // Rebuild the sync against the parking mock: the helper binds the class mock, so a direct
    // constructor call is clearer for the FULL pin.
    RingsideActionSync fullSync =
        new RingsideActionSync(
            "false", ringsideActionDataService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(parking);
    Mockito.verifyNoInteractions(seedReviewService);
    assertEquals(
        "admin-tuned",
        existing.getDescription(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }

  private static String eqName(String name) {
    return ArgumentMatchers.eq(name);
  }
}
