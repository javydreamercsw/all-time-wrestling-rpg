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
import org.springframework.stereotype.Component;

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
  @CacheEvict(value = CacheConfig.SCRIPTED_ACHIEVEMENTS_CACHE, allEntries = true)
  public void sync() {
    if (skipIfNotEmpty && achievementRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("achievements.json");
    if (resource.exists()) {
      log.debug("Loading achievements from file: {}", resource.getPath());
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
        log.debug("Achievement loading completed - {} achievements processed", fromFile.size());
      } catch (IOException e) {
        log.error("Error loading achievements from file", e);
      }
    } else {
      log.warn("Achievements file not found: {}", resource.getPath());
    }
  }
}
