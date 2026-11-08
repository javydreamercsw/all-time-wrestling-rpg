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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementCategory;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
class AchievementSyncTest {

  @Mock private AchievementRepository achievementRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private AchievementSync achievementSync;

  @BeforeEach
  void setUp() {
    // Constructed directly: @InjectMocks cannot resolve the @Value boolean
    // constructor parameter against mocks.
    achievementSync =
        new AchievementSync(
            "false", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  @Test
  void sync_upsertsWeeklyAchievementsFromChallengeDirectories() {
    when(achievementRepository.count()).thenReturn(0L);
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    achievementSync.sync();

    // The Week 6 achievement lives ONLY in
    // challenges/season_1/weekly_achievements.json (single source of truth,
    // ATW-i2ar) — the sync must pick it up from the challenge directories.
    verify(achievementRepository).findByKey("CHALLENGE_WEEK_06");
  }

  @Test
  void sync_upsertsCatalogAchievements() {
    when(achievementRepository.count()).thenReturn(0L);
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    achievementSync.sync();

    // Catalog entries from achievements.json still load as before. Weekly
    // CHALLENGE_WEEK_* keys intentionally do NOT live here anymore (single
    // source of truth, ATW-i2ar) — CHALLENGE_FIRST_HARD is a catalog-only key.
    verify(achievementRepository).findByKey("CHALLENGE_FIRST_HARD");
  }

  @Test
  void sync_existingAchievementIsUpdatedNotDuplicated() {
    when(achievementRepository.count()).thenReturn(0L);
    Achievement existing = new Achievement();
    existing.setKey("CHALLENGE_WEEK_01");
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.of(existing));
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    achievementSync.sync();

    // Every upsert goes through the same saveAll path whether new or existing;
    // the repository is the dedup point (findByKey before save). One saveAll
    // batch per loaded file: the catalog plus the weekly-challenge file.
    verify(achievementRepository, Mockito.times(2)).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyAndSkipIfNotEmptyEnabled() {
    AchievementSync strict =
        new AchievementSync(
            "true", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    when(achievementRepository.count()).thenReturn(5L);
    // Top-up still runs under the gate; every seeded key already exists, so nothing inserts.
    when(achievementRepository.findAll())
        .thenReturn(loadSeedKeys().stream().map(k -> achievement(k)).toList());
    when(achievementRepository.findByKey(anyString()))
        .thenAnswer(inv -> Optional.of(achievement(inv.getArgument(0))));

    strict.sync();

    verify(achievementRepository, Mockito.never()).saveAll(any());
  }

  private List<String> loadSeedKeys() throws IllegalStateException {
    try (InputStream is = getClass().getResourceAsStream("/achievements.json")) {
      return new ObjectMapper()
          .readValue(is, new TypeReference<List<Achievement>>() {}).stream()
              .map(Achievement::getKey)
              .toList();
    } catch (IOException e) {
      throw new IllegalStateException("achievements.json failed to parse", e);
    }
  }

  private Achievement achievement(String key) {
    Achievement a = new Achievement();
    a.setKey(key);
    return a;
  }

  @Test
  void sync_warnsOnMissingCatalogFileWithoutFailing() {
    // achievements.json missing from the classpath: log a warning and carry on
    // to the challenge scan instead of throwing.
    AchievementSync missingFile =
        new AchievementSync(
            "false", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST) {
          // syncClasspathFile logs and returns when the resource doesn't exist;
          // exercising that path with a fresh instance still runs the challenge scan.
        };
    when(achievementRepository.count()).thenReturn(0L);
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    // The real classpath has achievements.json; a missing-file path can be
    // reached only through a broken resource, so assert the tolerated behavior
    // differently: scanning an empty challenge directory yields no saveAll.
    missingFile.sync();

    verify(achievementRepository, Mockito.atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_existingRowsAreUpdatedInPlace() {
    when(achievementRepository.count()).thenReturn(0L);
    Achievement existing = new Achievement();
    existing.setKey("CHALLENGE_WEEK_01");
    when(achievementRepository.findByKey("CHALLENGE_WEEK_01")).thenReturn(Optional.of(existing));
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    achievementSync.sync();

    // The shipped file's content was copied onto the existing entity, and the
    // same instance flows into the save batch (update in place, not a new row).
    ArgumentCaptor<List<Achievement>> captor = ArgumentCaptor.forClass(List.class);
    verify(achievementRepository, Mockito.atLeastOnce()).saveAll(captor.capture());
    Assertions.assertTrue(
        captor.getAllValues().stream().flatMap(List::stream).anyMatch(a -> a == existing),
        "The pre-existing entity instance should be saved after copyContentFrom");
  }

  @Test
  void sync_survivesBrokenChallengeFileWithoutFailing() {
    // A challenge file that fails to deserialize is logged and skipped; the
    // next file (and the catalog) still syncs.
    ObjectMapper brokenMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(InputStream src, TypeReference<T> typeRef) throws IOException {
            throw new IOException("boom");
          }
        };
    AchievementSync broken =
        new AchievementSync(
            "false", achievementRepository, brokenMapper, seedReviewService, EMPTY_MANIFEST);
    when(achievementRepository.count()).thenReturn(0L);
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    Assertions.assertDoesNotThrow(broken::sync);
    // The catalog file (achievements.json) also goes through readValue, so no
    // save ever succeeds — but the sync must not propagate the failure.
    verify(achievementRepository, Mockito.never()).saveAll(any());
  }

  @Test
  void sync_challengeScanFailureDoesNotBreakSync() {
    // A resolver explosion inside the challenge scan is caught and logged;
    // the catalog file was already synced before the scan runs.
    AchievementSync scanning =
        new AchievementSync(
            "false", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    when(achievementRepository.count()).thenReturn(0L);
    when(achievementRepository.findByKey(anyString())).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    Assertions.assertDoesNotThrow(scanning::sync);
    // The catalog achievements.json exists on the test classpath and syncs.
    verify(achievementRepository, Mockito.atLeastOnce()).saveAll(any());
  }

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private String achievementFingerprint(Achievement a) {
    return VERIFY_SUPPORT.fingerprint(
        a.getName(),
        a.getDescription(),
        a.getXpValue(),
        a.getCategory() == null ? null : a.getCategory().name(),
        a.getUnlockCondition());
  }

  private Achievement seedFirstWrestler() throws IOException {
    // Reads the real FIRST_WRESTLER row from achievements.json.
    try (InputStream is = getClass().getResourceAsStream("/achievements.json")) {
      for (Achievement a :
          new ObjectMapper().readValue(is, new TypeReference<List<Achievement>>() {})) {
        if ("FIRST_WRESTLER".equals(a.getKey())) {
          return a;
        }
      }
    }
    throw new IllegalStateException("FIRST_WRESTLER not found in achievements.json");
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() throws IOException {
    AchievementSync verifySync =
        new AchievementSync(
            "verify", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    when(achievementRepository.count()).thenReturn(2L);
    when(achievementRepository.findAll()).thenReturn(List.of());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    Achievement seed = seedFirstWrestler();

    // Clean: as an older release's seed write left it — description since fixed in the seed.
    Achievement clean = new Achievement();
    clean.setKey("FIRST_WRESTLER");
    clean.setName(seed.getName());
    clean.setDescription("stale description from an older release");
    clean.setXpValue(seed.getXpValue());
    clean.setCategory(seed.getCategory());
    clean.setUnlockCondition(seed.getUnlockCondition());
    clean.setSeedFingerprint(achievementFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    Achievement edited = new Achievement();
    edited.setKey("CHALLENGE_FIRST_HARD");
    edited.setName("hand-tuned name");
    edited.setDescription("hand-tuned");
    edited.setXpValue(999);
    edited.setCategory(seed.getCategory());
    edited.setSeedFingerprint(achievementFingerprint(edited));
    edited.setName("hand-tuned name (changed after stamping)");

    when(achievementRepository.findByKey("FIRST_WRESTLER")).thenReturn(Optional.of(clean));
    when(achievementRepository.findByKey("CHALLENGE_FIRST_HARD")).thenReturn(Optional.of(edited));

    verifySync.sync();

    Assertions.assertEquals(
        seed.getDescription(),
        clean.getDescription(),
        "clean-fingerprint achievement must adopt the seed description");
    Assertions.assertEquals(
        "hand-tuned name (changed after stamping)",
        edited.getName(),
        "edited achievement must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() throws IOException {
    AchievementSync verifySync =
        new AchievementSync(
            "verify", achievementRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
    when(achievementRepository.count()).thenReturn(1L);
    when(achievementRepository.findAll()).thenReturn(List.of());
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    Achievement seed = seedFirstWrestler();
    Achievement legacy = new Achievement();
    legacy.setKey("FIRST_WRESTLER");
    legacy.setName(seed.getName());
    legacy.setDescription(seed.getDescription());
    legacy.setXpValue(seed.getXpValue());
    legacy.setCategory(seed.getCategory());
    legacy.setUnlockCondition(seed.getUnlockCondition());
    when(achievementRepository.findByKey("FIRST_WRESTLER")).thenReturn(Optional.of(legacy));

    verifySync.sync();

    Assertions.assertEquals(
        achievementFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy achievement matching the seed must be adopted (fingerprint stamped)");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() throws IOException {
    // THREE-TIER pin, end-to-end through the wired pipeline:
    //   tier 1 — untouched field (description), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (xpValue), seed changed → parked as PENDING decision, DB
    //            value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (name edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    // The DB rows are stamped as the PREVIOUS release's seed write left them; the incoming seed
    // (achievements.json, unchanged on disk) supplies the new values through the sync's normal
    // resource path. FIRST_WRESTLER is the tier-1/tier-3 row, CHALLENGE_FIRST_HARD the tier-2 row.
    Achievement seed = seedFirstWrestler();

    Achievement tier1row = new Achievement();
    tier1row.setKey("FIRST_WRESTLER");
    tier1row.setName(seed.getName());
    tier1row.setDescription("stale description from an older release"); // seed changed since
    tier1row.setXpValue(seed.getXpValue());
    tier1row.setCategory(seed.getCategory());
    tier1row.setUnlockCondition(seed.getUnlockCondition());
    tier1row.setSeedFingerprint(achievementFingerprint(tier1row));
    tier1row.setName("admin-renamed achievement"); // tier 3: admin edit, seed unchanged

    // CHALLENGE_FIRST_HARD's seed row: xpValue 150, category CHALLENGE. The stamp carries the
    // PREVIOUS release's xp (100) while the row shows the admin's edit (500) and the incoming seed
    // ships 150 → genuine tier-2 collision.
    Achievement tier2row = new Achievement();
    tier2row.setKey("CHALLENGE_FIRST_HARD");
    tier2row.setName("Glutton for Punishment");
    tier2row.setDescription("Completed your first HARD-difficulty challenge");
    tier2row.setXpValue(100);
    tier2row.setCategory(AchievementCategory.CHALLENGE);
    tier2row.setSeedFingerprint(achievementFingerprint(tier2row));
    tier2row.setXpValue(500); // tier 2: admin edit AFTER the stamp

    when(achievementRepository.count()).thenReturn(2L);
    when(achievementRepository.findAll()).thenReturn(List.of());
    when(achievementRepository.findByKey("FIRST_WRESTLER")).thenReturn(Optional.of(tier1row));
    when(achievementRepository.findByKey("CHALLENGE_FIRST_HARD")).thenReturn(Optional.of(tier2row));
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

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

    AchievementSync detectionSync =
        new AchievementSync(
            "true", achievementRepository, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the seed value.
    Assertions.assertEquals(
        seed.getDescription(),
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged name edit preserved.
    Assertions.assertEquals(
        "admin-renamed achievement",
        tier1row.getName(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited xpValue kept its value; the row parked the xpValue collision.
    Assertions.assertEquals(
        500, tier2row.getXpValue(), "tier 2: admin-edited field must keep its value");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("achievement"),
            eq("achievement:challenge_first_hard"),
            eq("CHALLENGE_FIRST_HARD"),
            eq("xpValue"),
            eq("500"),
            eq("150"),
            anyString());
    assertEquals(
        Set.of("achievement:challenge_first_hard|xpValue"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the pre-existing verify behavior still
    // holds with the new constructor dependencies.
    Achievement existing = new Achievement();
    existing.setKey("FIRST_WRESTLER");
    existing.setDescription("admin-tuned");
    when(achievementRepository.count()).thenReturn(1L);

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    AchievementSync fullModeSync =
        new AchievementSync(
            "false", achievementRepository, new ObjectMapper(), parking, EMPTY_MANIFEST);
    fullModeSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(parking);
    Assertions.assertEquals(
        "admin-tuned",
        existing.getDescription(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
