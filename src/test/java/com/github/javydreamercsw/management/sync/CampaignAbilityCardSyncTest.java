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
class CampaignAbilityCardSyncTest {

  @Mock private CampaignAbilityCardService campaignAbilityCardService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

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
    CampaignAbilityCardSync sync =
        new CampaignAbilityCardSync(
            "true",
            campaignAbilityCardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
    CampaignAbilityCardSync verifySync =
        new CampaignAbilityCardSync(
            "verify",
            campaignAbilityCardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
    CampaignAbilityCardSync verifySync =
        new CampaignAbilityCardSync(
            "verify",
            campaignAbilityCardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() {
    // Clean: stamped over its own (then-current) fields; the seed's description has since changed.
    CampaignAbilityCard clean = card("Poke Eye");
    clean.setDescription("stale description");
    clean.setOneTimeUse(false);
    clean.setTiming(AbilityTiming.DEFENSE);
    clean.setEffectScript("gainInitiative()");
    clean.setSeedFingerprint(cardFingerprint(clean));

    CampaignAbilityCardSync fastSync =
        new CampaignAbilityCardSync(
            "verify",
            campaignAbilityCardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of(
                    "campaign_ability_card",
                    Map.of("Poke Eye|HEEL|1", clean.getSeedFingerprint()))));
    when(campaignAbilityCardService.count()).thenReturn(1L);
    when(campaignAbilityCardService.findAll()).thenReturn(List.of(clean));
    // Top-up runs before the gate and inserts the other 13 seed cards through the service.
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

    fastSync.sync();

    // The stale description proves reconcile never ran (it would have adopted the seed's
    // "Gain Initiative.").
    assertEquals(
        "stale description",
        clean.getDescription(),
        "fast path must skip reconcile; the stale seed-owned field survives");
  }
}
