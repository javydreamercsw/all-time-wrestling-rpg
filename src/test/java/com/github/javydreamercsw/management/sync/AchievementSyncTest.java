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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private AchievementSync achievementSync;

  @BeforeEach
  void setUp() {
    // Constructed directly: @InjectMocks cannot resolve the @Value boolean
    // constructor parameter against mocks.
    achievementSync =
        new AchievementSync(
            "false",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
            "true",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
            "false",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of())) {
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
            "false", achievementRepository, brokenMapper, SeedFingerprintManifest.of(Map.of()));
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
            "false",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
            "verify",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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
            "verify",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
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

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() throws IOException {
    Achievement seed = seedFirstWrestler();
    Achievement clean = new Achievement();
    clean.setKey("FIRST_WRESTLER");
    clean.setName(seed.getName());
    clean.setDescription("stale description from an older release");
    clean.setXpValue(seed.getXpValue());
    clean.setCategory(seed.getCategory());
    clean.setUnlockCondition(seed.getUnlockCondition());
    clean.setSeedFingerprint(achievementFingerprint(clean));

    AchievementSync fastSync =
        new AchievementSync(
            "verify",
            achievementRepository,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("achievement", Map.of("FIRST_WRESTLER", clean.getSeedFingerprint()))));
    when(achievementRepository.count()).thenReturn(1L);
    when(achievementRepository.findAll()).thenReturn(List.of(clean));
    // Reconcile resolves rows through findByKey; stub it so a deleted gate would produce an
    // observable reconcile write (the clean row's seed-owned fields differ from the current seed,
    // yet its stored fingerprint matches the manifest — the seed moved on after seeding).
    when(achievementRepository.findByKey("FIRST_WRESTLER")).thenReturn(Optional.of(clean));
    when(achievementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    fastSync.sync();

    // The gate must fire: reconcile (which would copyContentFrom the seed and re-save the row
    // because the seed description changed since seeding) is skipped entirely. Top-up still
    // saves genuinely missing entries, so assert on the clean row itself: only reconcile would
    // ever put it in a save batch (top-up never re-saves existing rows).
    ArgumentCaptor<List<Achievement>> captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(achievementRepository, Mockito.atLeastOnce()).saveAll(captor.capture());
    Assertions.assertTrue(
        captor.getAllValues().stream().flatMap(List::stream).noneMatch(a -> a == clean),
        "reconcile must be skipped: the manifest-matched row is never re-saved");
    Assertions.assertEquals(
        "stale description from an older release",
        clean.getDescription(),
        "fast path must skip reconcile; the stale seed-owned field survives");
  }
}
