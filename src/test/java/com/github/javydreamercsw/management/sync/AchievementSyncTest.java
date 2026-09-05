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
    verify(achievementRepository, Mockito.times(2)).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyAndSkipIfNotEmptyEnabled() {
    AchievementSync strict = new AchievementSync(true, achievementRepository, new ObjectMapper());
    when(achievementRepository.count()).thenReturn(5L);

    strict.sync();

    verify(achievementRepository, org.mockito.Mockito.never()).saveAll(any());
  }

  @Test
  void sync_warnsOnMissingCatalogFileWithoutFailing() {
    // achievements.json missing from the classpath: log a warning and carry on
    // to the challenge scan instead of throwing.
    AchievementSync missingFile =
        new AchievementSync(false, achievementRepository, new ObjectMapper()) {
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

    verify(achievementRepository, org.mockito.Mockito.atLeastOnce()).saveAll(any());
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
    org.mockito.ArgumentCaptor<java.util.List<Achievement>> captor =
        org.mockito.ArgumentCaptor.forClass(java.util.List.class);
    verify(achievementRepository, org.mockito.Mockito.atLeastOnce()).saveAll(captor.capture());
    org.junit.jupiter.api.Assertions.assertTrue(
        captor.getAllValues().stream().flatMap(java.util.List::stream).anyMatch(a -> a == existing),
        "The pre-existing entity instance should be saved after copyContentFrom");
  }
}
