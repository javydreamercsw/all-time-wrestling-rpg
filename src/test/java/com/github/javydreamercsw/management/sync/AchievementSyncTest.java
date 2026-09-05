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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    achievementSync = new AchievementSync(false, achievementRepository, new ObjectMapper());
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
    verify(achievementRepository, org.mockito.Mockito.times(2)).saveAll(any());
  }
}
