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

  private final boolean skipIfNotEmpty;
  private final AchievementRepository achievementRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public AchievementSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final AchievementRepository achievementRepository,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.achievementRepository = achievementRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @CacheEvict(value = CacheConfig.SCRIPTED_ACHIEVEMENTS_CACHE, allEntries = true)
  public void sync() {
    if (skipIfNotEmpty && achievementRepository.count() > 0) {
      return;
    }
    syncClasspathFile("achievements.json");
    // Weekly-challenge achievements live in their challenge directories (one
    // entry per week's file) rather than being hand-copied into
    // achievements.json — single source of truth, and the live-update path
    // (ChallengeUpdateService via the content manifest) publishes the same
    // file to installed clients.
    syncChallengeAchievements();
  }

  private void syncChallengeAchievements() {
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] files = resolver.getResources("classpath*:challenges/**/*achievements*.json");
      for (Resource file : files) {
        // getFilename() drops subdirectories; recover the full classpath path
        // (e.g. challenges/season_1/weekly_achievements.json) from the URL.
        String url = file.getURL().toString();
        int idx = url.indexOf("challenges/");
        if (idx >= 0) {
          syncClasspathFile(url.substring(idx));
        } else {
          log.warn("Skipping unrecognized challenge achievement resource: {}", url);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning challenge achievement files", e);
    }
  }

  private void syncClasspathFile(final String path) {
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
          Achievement existing = existingOpt.get();
          existing.copyContentFrom(a);
          toSave.add(existing);
        } else {
          toSave.add(a);
        }
      }
      achievementRepository.saveAll(toSave);
      log.debug(
          "Achievement loading completed for {} - {} achievements processed",
          path,
          fromFile.size());
    } catch (IOException e) {
      log.error("Error loading achievements from file {}", path, e);
    }
  }
}
