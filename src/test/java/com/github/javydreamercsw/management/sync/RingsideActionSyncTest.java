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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RingsideActionSyncTest {

  @Mock private RingsideActionDataService ringsideActionDataService;

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
    RingsideActionSync sync =
        new RingsideActionSync(
            "true",
            ringsideActionDataService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
    RingsideActionSync verifySync =
        new RingsideActionSync(
            "verify",
            ringsideActionDataService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
    RingsideActionSync verifySync =
        new RingsideActionSync(
            "verify",
            ringsideActionDataService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() {
    // Clean: stamped over its own (then-current) fields; the seed's description has since changed.
    RingsideAction clean = action("Encouraging Call", "Legal Support");
    clean.setDescription("stale description");
    clean.setImpact(1);
    clean.setRisk(1);
    clean.setSeedFingerprint(actionFingerprint(clean));

    RingsideActionSync fastSync =
        new RingsideActionSync(
            "verify",
            ringsideActionDataService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("ringside_action", Map.of("Encouraging Call", clean.getSeedFingerprint()))));
    when(ringsideActionDataService.countTypes()).thenReturn(3L);
    when(ringsideActionDataService.countActions()).thenReturn(1L);
    when(ringsideActionDataService.findAllActionsUnfiltered()).thenReturn(List.of(clean));
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

    fastSync.sync();

    // The gate must fire: the row's seed-owned fields deliberately differ from the current seed
    // (impact/risk moved on after seeding), so a deleted gate would make reconcile rewrite it
    // through createOrUpdateAction("Encouraging Call", ...) and persist via saveAction. Top-up
    // never calls the service for existing names, so that call is reconcile-only.
    verify(ringsideActionDataService, never())
        .createOrUpdateAction(
            eq("Encouraging Call"), anyString(), any(), anyInt(), anyInt(), any(), any());
    assertEquals(
        "stale description",
        clean.getDescription(),
        "fast path must skip reconcile; the stale seed-owned field survives");
  }

  private static String eqName(String name) {
    return ArgumentMatchers.eq(name);
  }
}
