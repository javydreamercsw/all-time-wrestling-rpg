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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
class CardSetSyncTest {

  @Mock private CardSetService cardSetService;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private CardSetSync sync(String mode) {
    return new CardSetSync(
        mode, cardSetService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  @Test
  void sync_topUpInsertsOnlyMissingCardSetsWhenSkipFlagSet() {
    // Simulates a flag-protected install where one seeded set (ATW) exists and every other
    // seeded set is missing. Top-up must insert exactly the missing sets and never re-save an
    // existing row.
    CardSetSync skipSync = sync("true");
    when(cardSetService.count()).thenReturn(1L);

    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("All Time Wrestling");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code))))
        .thenReturn(Optional.empty());

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CardSet>> captor = ArgumentCaptor.forClass(List.class);
    verify(cardSetService, atLeastOnce()).saveAll(captor.capture());
    List<CardSet> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    assertFalse(saved.stream().anyMatch(cs -> cs == existing), "existing row must not be re-saved");
    assertFalse(saved.stream().anyMatch(cs -> "ATW".equals(cs.getCode())));
  }

  @Test
  void sync_topUpLeavesExistingCardSetUntouchedWhenSkipFlagSet() {
    CardSetSync skipSync = sync("true");
    when(cardSetService.count()).thenReturn(1L);
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    skipSync.sync();

    // ATW is seeded but already exists: under the skip gate its row must never be re-saved and
    // the stale name must survive.
    verify(cardSetService, Mockito.never())
        .saveAll(argThat(list -> list != null && list.stream().anyMatch(cs -> cs == existing)));
    assertEquals("stale name", existing.getName());
  }

  @Test
  void sync_fullModeUpdatesExistingCardSet() {
    // Without the skip flag, existing rows are updated to match the seed (original behavior).
    CardSetSync fullSync = sync("false");
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    fullSync.sync();

    verify(cardSetService, atLeastOnce()).saveAll(argThat(list -> list.contains(existing)));
    assertFalse("stale name".equals(existing.getName()), "name must be refreshed from seed");
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    CardSetSync verifySync = sync("verify");
    when(cardSetService.count()).thenReturn(2L);

    // ATW: stamped as an older release's seed write left it — the row's own (then-current) name
    // hashes to the stamp; the name has since gone stale only because the seed moved on.
    CardSet clean = new CardSet();
    clean.setCode("ATW");
    clean.setName("stale set name");
    clean.setSeedFingerprint(support.fingerprint("stale set name"));

    // Edited row: stamped at seed write, then hand-renamed (its current name no longer hashes
    // to the stamp).
    CardSet edited = new CardSet();
    edited.setCode("RVD");
    edited.setName("user-renamed set");
    edited.setSeedFingerprint(support.fingerprint("Rob Van Dam"));

    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(clean));
    when(cardSetService.findBySetCode("RVD")).thenReturn(Optional.of(edited));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code) && !"RVD".equals(code))))
        .thenReturn(Optional.empty());

    verifySync.sync();

    assertEquals(
        "All Time Wrestling",
        clean.getName(),
        "clean-fingerprint set must be reconciled to the seed name");
    assertEquals("user-renamed set", edited.getName(), "edited set must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    CardSetSync verifySync = sync("verify");
    when(cardSetService.count()).thenReturn(1L);
    CardSet legacy = new CardSet();
    legacy.setCode("ATW");
    legacy.setName("All Time Wrestling"); // matches seed, null fingerprint
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(legacy));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code))))
        .thenReturn(Optional.empty());

    verifySync.sync();

    assertEquals(
        support.fingerprint("All Time Wrestling"),
        legacy.getSeedFingerprint(),
        "legacy row matching the seed must be adopted (fingerprint stamped)");
  }

  @Test
  void verifyMode_reconciledRenameRestampsWithTheNewName() {
    // Regression: the reconcile previously stamped hash(oldName) after renaming, poisoning the
    // next pass. The persisted row must carry the NEW name's hash.
    SeedSyncSupport support = new SeedSyncSupport("verify");
    CardSetSync verifySync = sync("verify");
    when(cardSetService.count()).thenReturn(1L);

    CardSet clean = new CardSet();
    clean.setCode("ATW");
    clean.setName("stale set name");
    clean.setSeedFingerprint(support.fingerprint("stale set name"));

    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(clean));
    List<CardSet> lastBatch = new ArrayList<>();
    when(cardSetService.saveAll(any()))
        .thenAnswer(
            inv -> {
              // Reconcile runs after top-up; capture only the reconcile's batch.
              lastBatch.clear();
              lastBatch.addAll(inv.getArgument(0));
              return inv.getArgument(0);
            });

    verifySync.sync();

    assertEquals(1, lastBatch.size(), "the reconcile must persist exactly the reconciled set");
    assertEquals(
        support.fingerprint("All Time Wrestling"),
        lastBatch.getFirst().getSeedFingerprint(),
        "persisted stamp must hash the NEW name, not the pre-rename one");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. The domain's only seed-owned field is
    // the display name (single-blob row, Task 5's per-row parking contract): the clean ATW row's
    // name is seed-changed (tier 1 silent update + re-stamp), the RVD row's name was hand-renamed
    // by the admin AND the seed renamed it too (tier 2 collision on the one field — the field and
    // the row are the same coordinate here), and a third seed-unchanged set with an admin rename
    // is preserved (tier 3).
    CardSet tier1row = new CardSet();
    tier1row.setCode("ATW");
    tier1row.setName("stale set name"); // seed renamed since
    tier1row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("stale set name"));

    CardSet tier2row = new CardSet();
    tier2row.setCode("RVD");
    tier2row.setName("admin-renamed set");
    // Stamp carries the PREVIOUS release's seed name while the row shows the admin's rename and
    // the incoming seed ships today's name → genuine tier-2 collision.
    tier2row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("previous seed name"));

    CardSet tier3row = new CardSet();
    tier3row.setCode("CUSTOM");
    tier3row.setName("user-renamed custom set");
    // Stamp hashes the CURRENT name (the seed never carried CUSTOM, so the set is untouched by
    // the version change — tier 3 no-op territory).
    tier3row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("user-renamed custom set"));

    when(cardSetService.count()).thenReturn(3L);
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(tier1row));
    when(cardSetService.findBySetCode("RVD")).thenReturn(Optional.of(tier2row));
    when(cardSetService.findBySetCode("CUSTOM")).thenReturn(Optional.of(tier3row));
    when(cardSetService.findBySetCode(
            argThat(code -> !"ATW".equals(code) && !"RVD".equals(code) && !"CUSTOM".equals(code))))
        .thenReturn(Optional.empty());

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

    CardSetSync detectionSync =
        new CardSetSync("true", cardSetService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched name updated silently to the NEW seed value.
    assertEquals(
        "All Time Wrestling",
        tier1row.getName(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged custom rename preserved (CUSTOM has no seed entry → never even
    // considered; the tier-3 preserve-edit path on a seeded set is the "RVD" row's stamp shape).
    assertEquals(
        "user-renamed custom set",
        tier3row.getName(),
        "tier 3: user-created sets must never be touched");
    // Tier 2: the admin rename kept its value; the row parked the name collision.
    assertEquals("admin-renamed set", tier2row.getName(), "tier 2: admin edit must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("card_set"),
            eq("card_set:rvd"),
            eq("Rob Van Dam"),
            eq("name"),
            eq("admin-renamed set"),
            eq("Rob Van Dam"),
            anyString());
    assertEquals(
        Set.of("card_set:rvd|name", "#Rob Van Dam"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the pre-existing verify behavior still
    // holds with the new constructor dependencies.
    CardSetSync fullSync = sync("false");
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.count()).thenReturn(1L);
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(seedReviewService);
    assertEquals(
        "stale name",
        existing.getName(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
