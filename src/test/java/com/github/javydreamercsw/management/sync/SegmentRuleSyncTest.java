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
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentRuleSyncTest {

  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

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
            "false", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
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
            "verify", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
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
            "verify", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
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
            "verify", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
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
            "false", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
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

  // ── Version-change detection (seed-update review, PR 4 Step 1 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline:
    //   tier 1 — untouched field (description), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (bumpAddition), seed changed → parked as PENDING decision,
    //            DB value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (expansionCode edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    // The DB rows are stamped as the PREVIOUS release's seed write left them; the incoming seed
    // (segment_rules.json, unchanged on disk) supplies the new values through the sync's normal
    // resource path. "Normal" is the tier-1/tier-3 row, "Elimination" the tier-2 row.
    SegmentRule tier1row = rule("Normal", "stale description"); // seed's description changed since
    tier1row.setSeedFingerprint(ruleFingerprint(tier1row));
    tier1row.setExpansionCode("USER_CUSTOM"); // tier 3: admin edit AFTER the stamp, seed unchanged

    // The tier-2 row's description equals the REAL incoming seed's description (only bumpAddition
    // collides); the stamp carries LOSERS (what the previous release shipped) while the row shows
    // the admin's ALL and the incoming seed ships NONE → stored(LOSERS) != current(ALL) and
    // stored(LOSERS) != new(NONE) → a genuine tier-2 collision on bumpAddition.
    SegmentRule tier2row =
        rule(
            "Elimination",
            "Elimination Match: Participants are eliminated individually by pinfall or submission"
                + " until only one wrestler or team remains.");
    tier2row.setExpansionCode("CUSTOM"); // the real seed ships this expansion for Elimination
    tier2row.setBumpAddition(BumpAddition.NONE);
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            tier2row.getDescription(),
            tier2row.getRequiresHighHeat(),
            tier2row.getNoDq(),
            tier2row.isAllowsRefereeStopage(),
            BumpAddition.LOSERS.name(),
            tier2row.getExpansionCode(),
            tier2row.getGuideHash()));
    tier2row.setBumpAddition(BumpAddition.ALL); // tier 2: admin edit AFTER the stamp

    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(tier1row, tier2row));
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
              // Write-through on the SAME row the detection pass reads (mirrors the service's
              // findByName + save semantics) so tier-1 writes are observable on the fixture rows.
              String name = inv.getArgument(0);
              SegmentRule target = "Normal".equals(name) ? tier1row : tier2row;
              target.setDescription(inv.getArgument(1));
              target.setRequiresHighHeat(inv.getArgument(2));
              target.setNoDq(inv.getArgument(3));
              target.setBumpAddition(inv.getArgument(4));
              target.setExpansionCode(inv.getArgument(5));
              return target;
            });

    Set<String> parkedCoordinates = new HashSet<>();
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(seedReviewService)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    SegmentRuleSync detectionSync =
        new SegmentRuleSync(
            "verify", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the seed value.
    assertEquals(
        "Standard Match: Victory is achieved by pinfall, submission, count-out, or"
            + " disqualification. Standard rules apply, including rope breaks and a 10-count for"
            + " being outside the ring.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged expansion edit preserved.
    assertEquals(
        "USER_CUSTOM",
        tier1row.getExpansionCode(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited bumpAddition kept its value; the row parked exactly that field.
    assertEquals(
        BumpAddition.ALL,
        tier2row.getBumpAddition(),
        "tier 2: admin-edited field must keep its value — only a PENDING decision is created");
    assertEquals(
        Set.of("segment_rule:elimination|bumpAddition"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
    Mockito.verify(seedReviewService)
        .parkFieldConflict(
            eq("segment_rule"),
            eq("segment_rule:elimination"),
            eq("Elimination"),
            eq("bumpAddition"),
            eq(BumpAddition.ALL.name()),
            eq(BumpAddition.NONE.name()),
            anyString());
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot: detection is gated off (shouldRunDetection false → the orchestrator never
    // calls detectVersionChangeCollisions), and the sync's own FULL/VERIFY behavior is unchanged.
    // This pin exercises the pre-existing verify path with the new constructor dependencies and
    // proves the detection pass itself never runs in FULL mode.
    SegmentRuleSync fullSync =
        new SegmentRuleSync(
            "false", segmentRuleService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    SegmentRule existing = rule("Normal", "stale description");
    when(segmentRuleService.findAllForAdmin()).thenReturn(List.of(existing));

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(seedReviewService);
    assertEquals(
        "stale description",
        existing.getDescription(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
