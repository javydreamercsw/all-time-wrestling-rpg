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
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgradeRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the campaign_upgrades.json seed sync, including the version-change pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignUpgradeSyncTest {

  @Mock private CampaignUpgradeService campaignUpgradeService;
  @Mock private CampaignUpgradeRepository upgradeRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private CampaignUpgradeSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(upgradeRepository.count()).thenReturn(0L);
  }

  private CampaignUpgradeSync sync(String mode) {
    return new CampaignUpgradeSync(
        mode,
        campaignUpgradeService,
        upgradeRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static CampaignUpgrade upgrade(String name, String description, String type) {
    return CampaignUpgrade.builder().name(name).description(description).type(type).build();
  }

  @Test
  void sync_delegatesToLoadUpgrades() {
    sync.sync();

    verify(campaignUpgradeService).loadUpgrades();
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline, against the real seed file (the
    // detection pass reads campaign_upgrades.json). "Unbreakable" is the tier-1 row (clean:
    // stamp hashes the row's current vector; the seed's description changed since → silent
    // update + re-stamp). "Lethal Strike" is the tier-2 row — an ADMIN-CHANGED description
    // whose seed value ALSO changed since the stamp (previous release shipped "previous seed
    // description", the admin swapped to "admin's description", the incoming release ships
    // "All Strike attack [[card]]s deal +1 extra damage."): the clean and single-edit
    // reconstructions both fail → fail-safe, exactly the description collision parks while
    // type/subType (equal to the seed) stay NO_OP. The tier-3 leg is the tier-2 row's
    // type/subType and the tier-1 row's type (unchanged → NO_OP, never parked).
    CampaignUpgrade tier1row = upgrade("Unbreakable", "stale description", "HEALTH");
    tier1row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("stale description", "HEALTH", null));

    CampaignUpgrade tier2row = upgrade("Lethal Strike", "admin's description", "DAMAGE");
    tier2row.setSubType("Strike");
    // The stamp carries the PREVIOUS release's seed description while the row shows the admin's
    // edit and the incoming seed ships a third value → genuine tier-2 collision.
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("previous seed description", "DAMAGE", "Strike"));

    when(upgradeRepository.count()).thenReturn(2L);
    when(upgradeRepository.findAll()).thenReturn(List.of(tier1row, tier2row));

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

    CampaignUpgradeSync detectionSync =
        new CampaignUpgradeSync(
            "true",
            campaignUpgradeService,
            upgradeRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Increases your wrestler's maximum [[health]] by 2.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's description kept its value; the row parked the collision.
    assertEquals(
        "admin's description", tier2row.getDescription(), "tier 2: admin edit must keep its value");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            eq("campaign_upgrade"),
            eq("campaign_upgrade:lethal-strike"),
            eq("Lethal Strike"),
            eq("description"),
            eq("admin's description"),
            eq("All Strike attack [[card]]s deal +1 extra damage."),
            anyString());
    assertEquals(
        java.util.Set.of("campaign_upgrade:lethal-strike|description"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the PROTECT-mode gate returns early when
    // the table is empty.
    CampaignUpgradeSync fullSync = sync("false");
    when(upgradeRepository.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(upgradeRepository, never()).findAll();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load delegates to the service (deleteAll + saveAll), then stamps every freshly
    // seeded row through the repository.
    CampaignUpgrade seeded = upgrade("Iron Man", "seed description", "STAMINA");
    when(upgradeRepository.count()).thenReturn(0L);
    when(upgradeRepository.findAll()).thenReturn(List.of(seeded));

    sync.sync();

    verify(upgradeRepository, org.mockito.Mockito.atLeastOnce())
        .saveAll(
            ArgumentMatchers.argThat(
                rows -> {
                  if (rows == null) {
                    return false;
                  }
                  for (var u : rows) {
                    if (u.getSeedFingerprint() == null) {
                      return false;
                    }
                  }
                  return true;
                }));
  }

  @Test
  void protectModeSkipsTheLoadButDetectionStillClassifies() {
    // PROTECT mode: the seed load is skipped when rows exist — but the detection pass still runs
    // its classification (here with a clean stamped row → NO_OP re-stamp, no park).
    CampaignUpgrade clean = upgrade("Iron Man", "seed description", "STAMINA");
    clean.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("seed description", "STAMINA", null));
    when(upgradeRepository.count()).thenReturn(1L);
    when(upgradeRepository.findAll()).thenReturn(List.of(clean));

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

    CampaignUpgradeSync detectionSync =
        new CampaignUpgradeSync(
            "true",
            campaignUpgradeService,
            upgradeRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    verify(campaignUpgradeService, never()).loadUpgrades();
    assertEquals(java.util.Set.of(), parkedCoordinates, "a clean row collides with nothing");
  }
}
