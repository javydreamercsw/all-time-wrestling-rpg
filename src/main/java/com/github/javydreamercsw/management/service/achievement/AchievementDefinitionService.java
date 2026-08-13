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
package com.github.javydreamercsw.management.service.achievement;

import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.config.CacheConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Cached lookup of achievements that carry a scripted unlock condition, so the achievement-check
 * call sites (legacy score recalculation, challenge completion, match adjudication) don't hit the
 * database on every event. Evicted by {@code AchievementSync} whenever achievements.json is
 * re-synced.
 */
@Service
@RequiredArgsConstructor
public class AchievementDefinitionService {

  private final AchievementRepository achievementRepository;

  @Cacheable(value = CacheConfig.SCRIPTED_ACHIEVEMENTS_CACHE, key = "'all'")
  public List<Achievement> getScriptedAchievements() {
    return achievementRepository.findByUnlockConditionIsNotNull();
  }
}
