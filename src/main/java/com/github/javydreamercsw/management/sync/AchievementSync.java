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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(230)
public class AchievementSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "achievement";

  private final SeedSyncSupport syncSupport;
  private final AchievementRepository achievementRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public AchievementSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final AchievementRepository achievementRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.achievementRepository = achievementRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @CacheEvict(value = CacheConfig.SCRIPTED_ACHIEVEMENTS_CACHE, allEntries = true)
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && achievementRepository.count() > 0) {
      topUpMissingAchievements();
      if (syncSupport.isVerify()) {
        reconcileVerifiedAchievements();
      }
      return;
    }
    upsertClasspathFile("achievements.json", true);
    // Weekly-challenge achievements live in their challenge directories (one
    // entry per week's file) rather than being hand-copied into
    // achievements.json — single source of truth, and the live-update path
    // (ChallengeUpdateService via the content manifest) publishes the same
    // file to installed clients.
    syncChallengeAchievements();
  }

  private void syncChallengeAchievements() {
    upsertChallengeAchievements(true);
  }

  private void upsertChallengeAchievements(final boolean updateExisting) {
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] files = resolver.getResources("classpath*:challenges/**/*achievements*.json");
      for (Resource file : files) {
        // getFilename() drops subdirectories; recover the full classpath path
        // (e.g. challenges/season_1/weekly_achievements.json) from the URL.
        String url = file.getURL().toString();
        int idx = url.indexOf("challenges/");
        if (idx >= 0) {
          upsertClasspathFile(url.substring(idx), updateExisting);
        } else {
          log.warn("Skipping unrecognized challenge achievement resource: {}", url);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning challenge achievement files", e);
    }
  }

  /**
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false (top-up), only entries missing from the database are inserted and existing rows are
   *     left untouched.
   */
  private void upsertClasspathFile(final String path, final boolean updateExisting) {
    ClassPathResource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      log.warn("Achievements file not found: {}", path);
      return;
    }
    log.debug("Loading achievements from file: {}", path);
    try (var is = resource.getInputStream()) {
      List<Achievement> fromFile = objectMapper.readValue(is, new TypeReference<>() {});
      List<Achievement> toSave = new ArrayList<>();
      for (Achievement a : fromFile) {
        Optional<Achievement> existingOpt = achievementRepository.findByKey(a.getKey());
        if (existingOpt.isPresent()) {
          if (!updateExisting) {
            // Top-up mode: never re-save an existing achievement.
            continue;
          }
          Achievement existing = existingOpt.get();
          existing.copyContentFrom(a);
          existing.setSeedFingerprint(achievementFingerprint(existing));
          toSave.add(existing);
        } else {
          a.setSeedFingerprint(achievementFingerprint(a));
          toSave.add(a);
        }
      }
      if (!toSave.isEmpty()) {
        achievementRepository.saveAll(toSave);
      }
      log.debug(
          "Achievement loading completed for {} - {} achievements processed",
          path,
          fromFile.size());
    } catch (IOException e) {
      log.error("Error loading achievements from file {}", path, e);
    }
  }

  /**
   * Skip-gate branch: achievements exist and the flag protects them. Still insert seed entries that
   * don't exist yet (both sources: catalog + challenge directories). Existing rows are never
   * re-saved.
   */
  private void topUpMissingAchievements() {
    int before = achievementRepository.findAll().size();
    upsertClasspathFile("achievements.json", false);
    upsertChallengeAchievements(false);
    int inserted = achievementRepository.findAll().size() - before;
    if (inserted > 0) {
      log.info("Achievement top-up: inserting {} missing seed achievement(s).", inserted);
    }
  }

  /**
   * Canonical seed-owned field values for an achievement row — the {@code copyContentFrom} set.
   * Public for the seed-provenance manifest generator, which re-hashes fresh seed rows through the
   * owning sync's own method.
   */
  public String achievementFingerprint(Achievement a) {
    return syncSupport.fingerprint(
        a.getName(),
        a.getDescription(),
        a.getXpValue(),
        a.getCategory() == null ? null : a.getCategory().name(),
        a.getUnlockCondition());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing achievements. The stored fingerprint is
   * the hash of the row's seed-owned fields as the last seed write left them; re-hashing the row's
   * current fields detects hand edits. A row that still hashes to its stamp is clean and adopts
   * seed changes; a row whose fields drifted is user-edited — warned and preserved. Achievements
   * with no seed entry are never considered.
   */
  private void reconcileVerifiedAchievements() {
    long start = System.currentTimeMillis();
    List<Achievement> seedRows = loadSeedAchievements();
    if (seedRows.isEmpty()) {
      return;
    }
    int reconciled = 0;
    int preserved = 0;
    List<Achievement> toSave = new ArrayList<>();
    for (Achievement seed : seedRows) {
      Achievement existing = achievementRepository.findByKey(seed.getKey()).orElse(null);
      if (existing == null) {
        continue; // user-created rows have no seed match and are never touched
      }
      String current = achievementFingerprint(existing);
      if (SeedSyncSupport.isBlankFingerprint(existing.getSeedFingerprint())) {
        // Legacy row: adopt only if its seed-owned fields already equal the seed.
        if (achievementMatchesSeed(existing, seed)) {
          existing.setSeedFingerprint(current);
          toSave.add(existing);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: achievement '{}' has hand-edited seed-owned fields (legacy row, "
                  + "no fingerprint); preserving user values.",
              existing.getKey());
          preserved++;
        }
      } else if (current.equals(existing.getSeedFingerprint())) {
        if (achievementMatchesSeed(existing, seed)) {
          continue; // already in sync
        }
        existing.copyContentFrom(seed);
        existing.setSeedFingerprint(achievementFingerprint(existing));
        toSave.add(existing);
        reconciled++;
      } else {
        log.warn(
            "Seed verify: achievement '{}' was modified after seeding (fingerprint mismatch); "
                + "preserving user values.",
            existing.getKey());
        preserved++;
      }
    }
    if (!toSave.isEmpty()) {
      achievementRepository.saveAll(toSave);
    }
    log.info(
        "{} ({} ms)",
        syncSupport.reconcileLog("achievement", reconciled, preserved),
        System.currentTimeMillis() - start);
  }

  /** Reads every seed achievement row (catalog + challenge directories) without writing. */
  private List<Achievement> loadSeedAchievements() {
    List<Achievement> all = new ArrayList<>();
    collectSeedFile(all, "achievements.json");
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] files = resolver.getResources("classpath*:challenges/**/*achievements*.json");
      for (Resource file : files) {
        String url = file.getURL().toString();
        int idx = url.indexOf("challenges/");
        if (idx >= 0) {
          collectSeedFile(all, url.substring(idx));
        }
      }
    } catch (IOException e) {
      log.error("Error scanning challenge achievement files for verify reconcile", e);
    }
    return all;
  }

  private void collectSeedFile(List<Achievement> into, final String path) {
    ClassPathResource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      into.addAll(objectMapper.readValue(is, new TypeReference<>() {}));
    } catch (IOException e) {
      log.error("Error reading achievements from file {}", path, e);
    }
  }

  private boolean achievementMatchesSeed(Achievement existing, Achievement seed) {
    return Objects.equals(existing.getName(), seed.getName())
        && Objects.equals(existing.getDescription(), seed.getDescription())
        && Objects.equals(existing.getXpValue(), seed.getXpValue())
        && existing.getCategory() == seed.getCategory()
        && Objects.equals(existing.getUnlockCondition(), seed.getUnlockCondition());
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing achievements
   * field-by-field through {@link SeedCollisionDetector} instead of reconciling directly — tier-1
   * fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields
   * stay untouched. Only meaningful in PROTECT/VERIFY mode with existing rows.
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    List<Achievement> seedRows = loadSeedAchievements();
    if (seedRows.isEmpty()) {
      return;
    }
    List<Achievement> toSave = new ArrayList<>();
    for (Achievement seed : seedRows) {
      Achievement existing = achievementRepository.findByKey(seed.getKey()).orElse(null);
      if (existing == null) {
        continue; // user-created rows have no seed match; missing rows are top-up's job
      }
      String newFingerprint =
          SeedReviewDetectionSupport.incomingFingerprint(
              provenanceManifest, DOMAIN_KEY, seed.getKey(), achievementFingerprint(seed));
      boolean rowHasStoredStamp =
          SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
      List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(existing, seed);
      SeedReviewDetectionSupport.DetectionResult result =
          SeedReviewDetectionSupport.detectRow(
              DOMAIN_KEY,
              entityKey(seed.getKey()),
              // The display name is the seed's stable identity for this domain — the achievement
              // key. The human-readable name field is itself seed-owned content (a collision
              // target), so it cannot serve as the resolution handle; the applier resolves
              // findByKey with this exact value.
              seed.getKey(),
              rowHasStoredStamp,
              existing.getSeedFingerprint(),
              observations,
              this::achievementValueListFingerprint,
              newFingerprint,
              seedReviewService);
      String stamp =
          SeedReviewDetectionSupport.stampForResolvedRow(result.provenanceExact(), newFingerprint);
      if (!result.silentUpdateFields().isEmpty()) {
        // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
        // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
        applySilentSeedFields(existing, seed, result.silentUpdateFields());
        toSave.add(existing);
      }
      // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
      // comparing after the set reads the NEW value and the save is silently skipped forever
      // (the row never converges).
      if (stamp != null && !stamp.equals(existing.getSeedFingerprint())) {
        existing.setSeedFingerprint(stamp);
        if (!toSave.contains(existing)) {
          toSave.add(existing);
        }
      }
    }
    if (!toSave.isEmpty()) {
      achievementRepository.saveAll(toSave);
    }
    log.info(
        "Seed review detection (achievement) complete ({} ms).",
        System.currentTimeMillis() - start);
  }

  /** The detection-time per-field observations for one achievement row against its seed row. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Achievement existing, Achievement seed) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "name", null, existing.getName(), seed.getName()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), seed.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "xpValue",
            null,
            existing.getXpValue() == null ? null : existing.getXpValue().toString(),
            seed.getXpValue() == null ? null : seed.getXpValue().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "category",
            null,
            existing.getCategory() == null ? null : existing.getCategory().name(),
            seed.getCategory() == null ? null : seed.getCategory().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "unlockCondition", null, existing.getUnlockCondition(), seed.getUnlockCondition()));
    return obs;
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #achievementFingerprint(Achievement)} exactly.
   */
  private String achievementValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0), values.get(1), values.get(2), values.get(3), values.get(4));
  }

  private static String entityKey(String key) {
    return entityKeyFor(key);
  }

  /**
   * The canonical entity key for an achievement key — shared with {@code AchievementSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String key) {
    return "achievement:" + key.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed row onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@code copyContentFrom}.
   */
  private void applySilentSeedFields(
      Achievement existing, Achievement seed, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "name" -> existing.setName(seed.getName());
        case "description" -> existing.setDescription(seed.getDescription());
        case "xpValue" -> existing.setXpValue(seed.getXpValue());
        case "category" -> existing.setCategory(seed.getCategory());
        case "unlockCondition" -> existing.setUnlockCondition(seed.getUnlockCondition());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update achievement field '" + field + "'");
      }
    }
  }
}
