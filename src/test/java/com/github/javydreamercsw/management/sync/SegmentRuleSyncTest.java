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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentRuleSyncTest {

  @Mock private SegmentRuleService segmentRuleService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private static String ruleFingerprint(SegmentRule sr) {
    return VERIFY_SUPPORT.fingerprint(
        sr.getDescription(),
        sr.getRequiresHighHeat(),
        sr.getNoDq(),
        sr.isAllowsRefereeStopage(),
        sr.getBumpAddition() == null ? null : sr.getBumpAddition().name(),
        sr.getExpansionCode(),
        sr.getGuideHash());
  }

  private SegmentRule rule(String name, String description) {
    SegmentRule sr = new SegmentRule();
    sr.setName(name);
    sr.setDescription(description);
    sr.setRequiresHighHeat(false);
    sr.setNoDq(false);
    sr.setAllowsRefereeStopage(true);
    sr.setBumpAddition(BumpAddition.NONE);
    sr.setExpansionCode("BASE_GAME");
    return sr;
  }

  @Test
  void sync_fullModeReconcilesAndStampsFingerprint() {
    SegmentRuleSync sync =
        new SegmentRuleSync(
            "false", segmentRuleService, new ObjectMapper(), SeedFingerprintManifest.of(Map.of()));
    SegmentRule existing = rule("Normal", "stale description");
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(existing));
    List<SegmentRule> saved = new ArrayList<>();
    when(segmentRuleService.createOrUpdateRule(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean()))
        .thenAnswer(
            inv -> {
              SegmentRule sr = rule(inv.getArgument(0), inv.getArgument(1));
              saved.add(sr);
              return sr;
            });

    sync.sync();

    // FULL behavior preserved: the seed row goes through the service unconditionally, and the
    // saved row carries a seed fingerprint afterwards.
    verify(segmentRuleService)
        .createOrUpdateRule(
            eq("Normal"),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean());
    assertNotNull(
        saved.getFirst().getSeedFingerprint(), "FULL mode must stamp fingerprints on synced rules");
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    SegmentRuleSync verifySync =
        new SegmentRuleSync(
            "verify", segmentRuleService, new ObjectMapper(), SeedFingerprintManifest.of(Map.of()));
    // Clean: stamped over its own (then-current) fields; the seed's description has since changed.
    SegmentRule clean = rule("Normal", "stale description");
    clean.setSeedFingerprint(ruleFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned (fields no longer hash to the stamp).
    SegmentRule edited = rule("Elimination", "user-tuned description");
    edited.setSeedFingerprint(ruleFingerprint(rule("Elimination", "seed description")));
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(clean, edited));
    when(segmentRuleService.createOrUpdateRule(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean()))
        .thenAnswer(
            inv -> {
              SegmentRule sr = rule(inv.getArgument(0), inv.getArgument(1));
              return sr;
            });

    verifySync.sync();

    verify(segmentRuleService)
        .createOrUpdateRule(
            eq("Normal"),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean());
    verify(segmentRuleService, never())
        .createOrUpdateRule(
            eq("Elimination"),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean());
    assertEquals(
        "user-tuned description", edited.getDescription(), "edited rule must be preserved");
  }

  @Test
  void verifyMode_ignoresUserCreatedRule() {
    SegmentRuleSync verifySync =
        new SegmentRuleSync(
            "verify", segmentRuleService, new ObjectMapper(), SeedFingerprintManifest.of(Map.of()));
    // User-created rule: no seed entry shares its name, so it must never be written or stamped.
    SegmentRule custom = rule("Legacy Rule", "A custom rule the user created long ago");
    custom.setExpansionCode("CUSTOM");
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(custom));

    verifySync.sync();

    verify(segmentRuleService, never())
        .createOrUpdateRule(
            eq("Legacy Rule"),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean());
    assertEquals(null, custom.getSeedFingerprint(), "user-created rule must remain unstamped");
  }

  @Test
  void verifyMode_stampsPersistThroughTheService() {
    // Regression: the stamp was previously applied to the detached instance returned by
    // createOrUpdateRule and never saved — verify twice to prove the stamp reaches the repo.
    SegmentRuleSync verifySync =
        new SegmentRuleSync(
            "verify", segmentRuleService, new ObjectMapper(), SeedFingerprintManifest.of(Map.of()));
    SegmentRule clean = rule("Normal", "stale description");
    clean.setSeedFingerprint(ruleFingerprint(clean));
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(clean));

    List<SegmentRule> persisted = new ArrayList<>();
    when(segmentRuleService.createOrUpdateRule(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean()))
        .thenAnswer(
            inv -> {
              SegmentRule sr = rule(inv.getArgument(0), inv.getArgument(1));
              return sr;
            });
    when(segmentRuleService.saveRule(any()))
        .thenAnswer(
            inv -> {
              persisted.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    verifySync.sync();

    assertEquals(1, persisted.size(), "the stamped rule must be persisted through the service");
    assertNotNull(
        persisted.getFirst().getSeedFingerprint(), "the persisted row must carry the fingerprint");
  }

  @Test
  void fullMode_stampsPersistThroughTheService() {
    SegmentRuleSync sync =
        new SegmentRuleSync(
            "false", segmentRuleService, new ObjectMapper(), SeedFingerprintManifest.of(Map.of()));
    SegmentRule existing = rule("Normal", "stale description");
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(existing));
    List<SegmentRule> persisted = new ArrayList<>();
    when(segmentRuleService.createOrUpdateRule(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean()))
        .thenAnswer(
            inv -> {
              SegmentRule sr = rule(inv.getArgument(0), inv.getArgument(1));
              return sr;
            });
    when(segmentRuleService.saveRule(any()))
        .thenAnswer(
            inv -> {
              persisted.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    sync.sync();

    assertEquals(20, persisted.size(), "every reconciled rule must be re-persisted with its stamp");
    for (SegmentRule sr : persisted) {
      assertNotNull(sr.getSeedFingerprint(), "persisted rule must carry a fingerprint");
    }
  }

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() {
    // Clean: stamped over its own (then-current) fields; the seed's description has since changed.
    SegmentRule clean = rule("Normal", "stale description");
    clean.setSeedFingerprint(ruleFingerprint(clean));

    SegmentRuleSync fastSync =
        new SegmentRuleSync(
            "verify",
            segmentRuleService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("segment_rule", Map.of("Normal", clean.getSeedFingerprint()))));
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(clean));

    fastSync.sync();

    verify(segmentRuleService, never())
        .createOrUpdateRule(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            anyString(),
            any(),
            anyBoolean());
    verify(segmentRuleService, never()).saveRule(any());
    assertEquals(
        "stale description",
        clean.getDescription(),
        "fast path must skip reconcile; the stale seed-owned field survives");
  }
}
