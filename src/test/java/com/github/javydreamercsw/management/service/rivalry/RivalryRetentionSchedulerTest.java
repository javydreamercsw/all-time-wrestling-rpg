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
package com.github.javydreamercsw.management.service.rivalry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.GameSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the nightly rivalry retention purge scheduler (ATW-8nc8). */
@ExtendWith(MockitoExtension.class)
class RivalryRetentionSchedulerTest {

  @Mock private RivalryService rivalryService;
  @Mock private GameSettingService gameSettingService;

  @InjectMocks private RivalryRetentionScheduler scheduler;

  @Test
  void disabledSettingSkipsPurge() {
    when(gameSettingService.getRivalryRetentionDays()).thenReturn(0);

    scheduler.runRetentionPurge();

    verify(rivalryService, never()).purgeEndedRivalries(anyInt());
  }

  @Test
  void enabledSettingPurgesThroughServiceProxy() {
    when(gameSettingService.getRivalryRetentionDays()).thenReturn(180);
    when(rivalryService.purgeEndedRivalries(180))
        .thenReturn(new RivalryService.RivalryRetentionResult(3, 5));

    scheduler.runRetentionPurge();

    verify(rivalryService).purgeEndedRivalries(180);
  }

  @Test
  void exceptionIsLoggedNotThrown() {
    when(gameSettingService.getRivalryRetentionDays()).thenReturn(30);
    when(rivalryService.purgeEndedRivalries(30)).thenThrow(new RuntimeException("boom"));

    assertDoesNotThrow(() -> scheduler.runRetentionPurge());
  }
}
