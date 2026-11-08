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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleReignRepairService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChampionshipSyncTest {

  @Mock private TitleService titleService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private TitleReignRepairService titleReignRepairService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  /** Empty manifest — detection falls back to content-computed fingerprints in these tests. */
  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private void stubUniverse() {
    Universe universe = new Universe();
    universe.setId(1L);
    when(universeRepository.findAll()).thenReturn(List.of(universe));
  }

  private String titleFingerprint(Title title) {
    return VERIFY_SUPPORT.fingerprint(
        title.getDescription(),
        title.getTier() == null ? null : title.getTier().name(),
        title.getChampionshipType() == null ? null : title.getChampionshipType().name(),
        title.getEffectScript(),
        title.getGender() == null ? null : title.getGender().name(),
        title.getImageUrl(),
        title.getExpansionCode(),
        title.getIncludeInRankings());
  }

  @Test
  void sync_topUpCreatesOnlyMissingTitlesWhenSkipFlagSet() {
    ChampionshipSync sync =
        new ChampionshipSync(
            "true",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    when(titleService.count()).thenReturn(1L);
    stubUniverse();

    Title existing = new Title();
    existing.setName("ATW World");
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(existing));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    sync.sync();

    verify(titleService, never()).saveAll(List.of(existing));
    verify(titleService, never()).awardTitleTo(any(), any());
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    ChampionshipSync verifySync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    when(titleService.count()).thenReturn(2L);
    stubUniverse();

    // Clean: as an older release's seed write left it — effect script since fixed in the seed.
    Title clean = new Title();
    clean.setName("ATW World");
    clean.setEffectScript("stale effect script");
    clean.setExpansionCode("BASE_GAME");
    clean.setIncludeInRankings(true);
    clean.setSeedFingerprint(titleFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    Title edited = new Title();
    edited.setName("ATW Women");
    edited.setEffectScript("user effect script");
    edited.setExpansionCode("BASE_GAME");
    edited.setIncludeInRankings(true);
    edited.setSeedFingerprint(titleFingerprint(edited));
    edited.setEffectScript("user effect script (changed after stamping)");

    when(titleService.findByName("ATW World")).thenReturn(Optional.of(clean));
    when(titleService.findByName("ATW Women")).thenReturn(Optional.of(edited));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    verifySync.sync();

    verify(titleService, Mockito.atLeastOnce()).saveAll(any());
    assertEquals(
        "user effect script (changed after stamping)",
        edited.getEffectScript(),
        "edited title must be preserved");
    // Finding 6: description/tier are seed-owned (spec) — the clean row adopts the seed's
    // description even though the FULL update path never writes it.
    assertEquals(
        "The top title in All Time Wrestling.",
        clean.getDescription(),
        "clean row must adopt the seed description");
    assertEquals(
        "gainInitiative()",
        clean.getEffectScript(),
        "clean-fingerprint title must adopt the seed effect script");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    ChampionshipSync verifySync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    when(titleService.count()).thenReturn(1L);
    stubUniverse();

    // Legacy: no stamp; fields equal what the seed write produces for ATW World (SINGLE/MALE,
    // gainInitiative() effect, BASE_GAME, null imageUrl, includeInRankings defaulting true).
    Title legacy = new Title();
    legacy.setName("ATW World");
    legacy.setDescription("The top title in All Time Wrestling.");
    legacy.setTier(WrestlerTier.MAIN_EVENTER);
    legacy.setChampionshipType(ChampionshipType.SINGLE);
    legacy.setEffectScript("gainInitiative()");
    legacy.setGender(Gender.MALE);
    legacy.setExpansionCode("BASE_GAME");
    legacy.setIncludeInRankings(true);
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(legacy));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    verifySync.sync();

    verify(titleService, Mockito.atLeastOnce()).saveAll(any());
    assertEquals(
        titleFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy title must be adopted (fingerprint stamped)");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 1 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline:
    //   tier 1 — untouched field (description), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (effectScript), seed changed → parked as PENDING decision,
    //            DB value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (imageUrl edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    // The DB rows are stamped as the PREVIOUS release's seed write left them; the incoming seed
    // (championships.json, unchanged on disk) supplies the new values through the sync's normal
    // resource path. ATW World is the tier-1/tier-3 row, ATW Extreme the tier-2 row.
    Title tier1row = new Title();
    tier1row.setName("ATW World");
    tier1row.setDescription("old description from a previous release"); // seed changed since
    tier1row.setTier(WrestlerTier.MAIN_EVENTER);
    tier1row.setChampionshipType(ChampionshipType.SINGLE);
    tier1row.setEffectScript("gainInitiative()");
    tier1row.setGender(Gender.MALE);
    tier1row.setExpansionCode("BASE_GAME");
    tier1row.setIncludeInRankings(true);
    tier1row.setSeedFingerprint(titleFingerprint(tier1row));
    tier1row.setImageUrl("http://example.com/admin-uploaded.png"); // tier 3: admin edit, seed null

    Title tier2row = new Title();
    tier2row.setName("ATW Extreme");
    tier2row.setDescription(
        "The most extreme title in wrestling. No rules, no limits."); // matches the real seed
    tier2row.setTier(WrestlerTier.ROOKIE);
    tier2row.setChampionshipType(ChampionshipType.SINGLE);
    tier2row.setEffectScript("addMatchRule('No DQ'); setupWeaponCard()"); // the real seed's value
    tier2row.setGender(Gender.MALE);
    tier2row.setExpansionCode("CUSTOM"); // the real seed's expansion for ATW Extreme
    tier2row.setIncludeInRankings(true);
    // Tier 2 needs the seed to have CHANGED the field too: the stamp carries the PREVIOUS
    // release's effect script (the real, unchanged championships.json ships today's value), the
    // row shows the admin's edit, and the incoming seed declares today's value →
    // stored(previous) != current(admin) and stored(previous) != new(today) → genuine collision.
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            tier2row.getDescription(),
            tier2row.getTier() == null ? null : tier2row.getTier().name(),
            tier2row.getChampionshipType() == null ? null : tier2row.getChampionshipType().name(),
            "previous release's effect script",
            tier2row.getGender() == null ? null : tier2row.getGender().name(),
            tier2row.getImageUrl(),
            tier2row.getExpansionCode(),
            tier2row.getIncludeInRankings()));
    tier2row.setEffectScript("admin's custom effect"); // tier 2: admin edit AFTER the stamp

    when(titleService.count()).thenReturn(2L);
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(tier1row));
    when(titleService.findByName("ATW Extreme")).thenReturn(Optional.of(tier2row));

    Set<String> parkedCoordinates = new HashSet<>();
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(seedReviewService)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    ChampionshipSync detectionSync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the seed value.
    assertEquals(
        "The top title in All Time Wrestling.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged imageUrl edit preserved.
    assertEquals(
        "http://example.com/admin-uploaded.png",
        tier1row.getImageUrl(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited effectScript kept its value; the row parked exactly that field.
    assertEquals(
        "admin's custom effect",
        tier2row.getEffectScript(),
        "tier 2: admin-edited field must keep its value — only a PENDING decision is created");
    Mockito.verify(seedReviewService)
        .parkFieldConflict(
            eq("championship"),
            eq("championship:atw-extreme"),
            eq("ATW Extreme"),
            eq("effectScript"),
            eq("admin's custom effect"),
            eq("addMatchRule('No DQ'); setupWeaponCard()"),
            anyString());
    // Only ATW Extreme's effectScript parked; the tier-1/tier-3 fields never park.
    assertEquals(
        Set.of("championship:atw-extreme|effectScript"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void stampOnlyRowPersistsItsReStamp() {
    // Finding-2 pin (fix round 1): a row whose fields are byte-identical to the incoming seed but
    // whose stored stamp predates the manifest's fingerprint (e.g. an older release's hash formula)
    // needs NO field writes and parks nothing — but it MUST still be re-stamped with the incoming
    // fingerprint AND that stamp-only change must reach saveAll. The buggy code set the stamp and
    // then compared against the just-set value, so the row was never added to the save list and the
    // re-stamp was silently discarded on every boot.
    // The manifest supplies the incoming fingerprint (a content-computed fingerprint could never
    // differ from a clean row's stored stamp — hence a populated manifest here, EMPTY_MANIFEST in
    // the other tests).
    Title cleanButStale = new Title();
    cleanButStale.setName("ATW World");
    cleanButStale.setDescription("The top title in All Time Wrestling.");
    cleanButStale.setTier(WrestlerTier.MAIN_EVENTER);
    cleanButStale.setChampionshipType(ChampionshipType.SINGLE);
    cleanButStale.setEffectScript("gainInitiative()");
    cleanButStale.setGender(Gender.MALE);
    cleanButStale.setExpansionCode("BASE_GAME");
    cleanButStale.setIncludeInRankings(true);
    // Clean row: the stored stamp hashes the CURRENT field values (step-1 provenance reconstruction
    // must succeed), yet differs from the manifest's incoming fingerprint below.
    cleanButStale.setSeedFingerprint(titleFingerprint(cleanButStale));

    when(titleService.findByName("ATW World")).thenReturn(Optional.of(cleanButStale));

    ArgumentCaptor<List<Title>> saved = ArgumentCaptor.forClass(List.class);
    ChampionshipSync detectionSync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            SeedProvenanceManifest.of(
                Map.of("championship", Map.of("ATW World", "manifest-hash-v2"))));
    detectionSync.detectVersionChangeCollisions();

    verify(titleService).saveAll(saved.capture());
    assertTrue(
        saved.getValue().contains(cleanButStale),
        "the stamp-only row must be in the save list — the re-stamp must not be discarded");
    assertEquals(
        "manifest-hash-v2",
        cleanButStale.getSeedFingerprint(),
        "a provenance-exact row with no collisions adopts the incoming manifest fingerprint");
    Mockito.verifyNoInteractions(seedReviewService);
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot: detection is gated off (the orchestrator never calls
    // detectVersionChangeCollisions when shouldRunDetection() is false), and the FULL-mode
    // detection pass itself is inert — the pre-existing FULL sync behavior is untouched.
    Title existing = new Title();
    existing.setName("ATW World");
    existing.setDescription("admin-tuned description");
    when(titleService.count()).thenReturn(1L);

    ChampionshipSync fullSync =
        new ChampionshipSync(
            "false",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(seedReviewService);
    assertEquals(
        "admin-tuned description",
        existing.getDescription(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
