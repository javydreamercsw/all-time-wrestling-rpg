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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignAbilityCardSyncTest {

  @Mock private CampaignAbilityCardService campaignAbilityCardService;

  @Mock
  private com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService
      seedReviewService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private static final com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest
      EMPTY_MANIFEST =
          com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest.empty();

  private CampaignAbilityCardSync sync(String mode) {
    return new CampaignAbilityCardSync(
        mode, campaignAbilityCardService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  private CampaignAbilityCard card(String name) {
    CampaignAbilityCard c = new CampaignAbilityCard();
    c.setName(name);
    c.setAlignmentType(AlignmentType.HEEL);
    c.setLevel(1);
    return c;
  }

  private String cardFingerprint(CampaignAbilityCard c) {
    return VERIFY_SUPPORT.fingerprint(
        c.getDescription(),
        c.isOneTimeUse(),
        c.getTiming() == null ? null : c.getTiming().name(),
        c.getEffectScript(),
        c.getSecondaryEffectScript(),
        c.isSecondaryOneTimeUse(),
        c.getSecondaryTiming() == null ? null : c.getSecondaryTiming().name());
  }

  @Test
  void sync_topUpInsertsOnlyMissingCardsWhenSkipFlagSet() {
    CampaignAbilityCardSync sync = sync("true");
    when(campaignAbilityCardService.count()).thenReturn(1L);
    CampaignAbilityCard existing = card("Poke Eye");
    when(campaignAbilityCardService.findAll()).thenReturn(List.of(existing));
    when(campaignAbilityCardService.createOrUpdateCard(
            anyString(),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any()))
        .thenAnswer(
            inv -> {
              CampaignAbilityCard c = card(inv.getArgument(0));
              c.setDescription(inv.getArgument(1));
              c.setEffectScript(inv.getArgument(6));
              return c;
            });

    sync.sync();

    // Poke Eye exists; the other 13 seed entries must insert. The pre-existing row is never
    // passed back through the service.
    verify(campaignAbilityCardService, atLeast(13))
        .createOrUpdateCard(
            ArgumentMatchers.argThat(name -> !"Poke Eye".equals(name)),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any());
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    CampaignAbilityCardSync verifySync = sync("verify");
    when(campaignAbilityCardService.count()).thenReturn(2L);

    // Clean: as an older release's seed write left it — description since fixed in the seed.
    CampaignAbilityCard clean = card("Poke Eye");
    clean.setDescription("stale description");
    clean.setOneTimeUse(false);
    clean.setTiming(AbilityTiming.DEFENSE);
    clean.setEffectScript("gainInitiative()");
    clean.setSeedFingerprint(cardFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    CampaignAbilityCard edited = card("Low Blow");
    edited.setDescription("hand-tuned");
    edited.setOneTimeUse(false);
    edited.setTiming(AbilityTiming.DEFENSE);
    edited.setEffectScript("gainInitiative()");
    edited.setSeedFingerprint(cardFingerprint(edited));
    edited.setDescription("hand-tuned (changed after stamping)");

    when(campaignAbilityCardService.findAll()).thenReturn(List.of(clean, edited));
    when(campaignAbilityCardService.createOrUpdateCard(
            anyString(),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any()))
        .thenAnswer(
            inv -> {
              CampaignAbilityCard c = card(inv.getArgument(0));
              c.setDescription(inv.getArgument(1));
              c.setEffectScript(inv.getArgument(6));
              return c;
            });

    verifySync.sync();

    verify(campaignAbilityCardService)
        .createOrUpdateCard(
            ArgumentMatchers.eq("Poke Eye"),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any());
    verify(campaignAbilityCardService, never())
        .createOrUpdateCard(
            ArgumentMatchers.eq("Low Blow"),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any());
    assertEquals(
        "hand-tuned (changed after stamping)",
        edited.getDescription(),
        "edited card must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    CampaignAbilityCardSync verifySync = sync("verify");
    when(campaignAbilityCardService.count()).thenReturn(1L);

    // Legacy: no stamp; fields match the seed's Poke Eye entry.
    CampaignAbilityCard legacy = card("Poke Eye");
    legacy.setDescription("Gain Initiative.");
    legacy.setOneTimeUse(false);
    legacy.setTiming(AbilityTiming.DEFENSE);
    legacy.setEffectScript("gainInitiative()");
    when(campaignAbilityCardService.findAll()).thenReturn(List.of(legacy));
    when(campaignAbilityCardService.createOrUpdateCard(
            anyString(),
            any(),
            any(),
            anyInt(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any()))
        .thenAnswer(
            inv -> {
              CampaignAbilityCard c = card(inv.getArgument(0));
              c.setDescription(inv.getArgument(1));
              c.setEffectScript(inv.getArgument(6));
              return c;
            });

    verifySync.sync();

    assertEquals(
        cardFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy card matching the seed must be adopted (fingerprint stamped)");
    verify(campaignAbilityCardService).save(legacy);
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. The domain's seed-owned fields are
    // the seven content columns (the name|alignment|level lookup key is excluded); rows are
    // stamped as the previous release's seed write left them, the incoming seed
    // (campaign_ability_cards.json) supplies the new values through the sync's normal resource
    // path. Poke Eye is the tier-1 row (description changed in the stamp, seed current), Low Blow
    // the tier-2 row (admin-tuned effectScript vs the stamp, seed same as the stamp's shape).
    CampaignAbilityCard tier1row = card("Poke Eye");
    tier1row.setDescription("stale description from an older release"); // old seed's description
    tier1row.setOneTimeUse(false); // matches the seed — only `description` may differ
    tier1row.setEffectScript("gainInitiative()");
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "stale description from an older release",
            false,
            "DEFENSE",
            "gainInitiative()",
            null,
            false,
            null));

    // Tier-2 row: the stamp holds the PREVIOUS release's seed write (old description + old
    // effectScript) while the row shows the admin's effectScript edit; the incoming seed ships the
    // current description/effectScript → the effectScript field is the genuine tier-2 collision.
    CampaignAbilityCard tier2row = card("Low Blow");
    tier2row.setDescription("Deal 2 damage and gain the initiative."); // matches the seed
    tier2row.setOneTimeUse(false); // matches the seed
    tier2row.setEffectScript("adminTunedScript()");
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "Deal 2 damage and gain the initiative.",
            false,
            "DEFENSE",
            "damage(2); gainInitiative()",
            null,
            false,
            null));

    when(campaignAbilityCardService.count()).thenReturn(2L);
    when(campaignAbilityCardService.findByNameAndAlignmentAndLevel(
            "Poke Eye", AlignmentType.HEEL, 1))
        .thenReturn(java.util.Optional.of(tier1row));
    when(campaignAbilityCardService.findByNameAndAlignmentAndLevel(
            "Low Blow", AlignmentType.HEEL, 1))
        .thenReturn(java.util.Optional.of(tier2row));

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService parking =
        org.mockito.Mockito.mock(
            com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    CampaignAbilityCardSync detectionSync =
        new CampaignAbilityCardSync(
            "true", campaignAbilityCardService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Gain Initiative.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's effectScript kept its value; the row parked the collision.
    assertEquals(
        "adminTunedScript()", tier2row.getEffectScript(), "tier 2: admin edit must keep its value");
    assertEquals(1, parkedCoordinates.size(), "exactly one tier-2 collision parked");
    assertTrue(
        parkedCoordinates.contains(
            CampaignAbilityCardSync.entityKeyFor("Low Blow|HEEL|1") + "|effectScript"),
        "the tier-2 park must be keyed by the composite entity key and the effectScript field");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (no service reads beyond the gate), and the
    // pre-existing verify behavior still holds with the new constructor dependencies.
    CampaignAbilityCardSync fullSync = sync("false");
    when(campaignAbilityCardService.count()).thenReturn(14L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    org.mockito.Mockito.verifyNoInteractions(seedReviewService);
    verify(campaignAbilityCardService, org.mockito.Mockito.never()).save(any());
  }
}
