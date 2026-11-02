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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

  private final SeedSyncSupport syncSupport;
  private final AchievementRepository achievementRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public AchievementSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final AchievementRepository achievementRepository,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.achievementRepository = achievementRepository;
    this.objectMapper = objectMapper;
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

  /** Canonical seed-owned field values for an achievement row — the {@code copyContentFrom} set. */
  private String achievementFingerprint(Achievement a) {
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
    return java.util.Objects.equals(existing.getName(), seed.getName())
        && java.util.Objects.equals(existing.getDescription(), seed.getDescription())
        && java.util.Objects.equals(existing.getXpValue(), seed.getXpValue())
        && existing.getCategory() == seed.getCategory()
        && java.util.Objects.equals(existing.getUnlockCondition(), seed.getUnlockCondition());
  }
}
