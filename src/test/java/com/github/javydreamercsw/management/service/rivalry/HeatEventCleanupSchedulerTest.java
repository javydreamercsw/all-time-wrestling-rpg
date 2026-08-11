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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.rivalry.RivalryService.HeatEventCleanupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HeatEventCleanupSchedulerTest {

  @Mock private RivalryService rivalryService;

  private HeatEventCleanupScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new HeatEventCleanupScheduler(rivalryService);
    ReflectionTestUtils.setField(scheduler, "retentionDays", 30);
  }

  @Test
  void runCleanup_delegatesToRivalryServiceWithConfiguredRetention() {
    when(rivalryService.purgeClosedRivalryHeatEvents(30))
        .thenReturn(new HeatEventCleanupResult(2, 10));

    scheduler.runCleanup();

    verify(rivalryService).purgeClosedRivalryHeatEvents(30);
  }

  @Test
  void runCleanup_withCustomRetention_passesCorrectDays() {
    ReflectionTestUtils.setField(scheduler, "retentionDays", 60);
    when(rivalryService.purgeClosedRivalryHeatEvents(60))
        .thenReturn(new HeatEventCleanupResult(0, 0));

    scheduler.runCleanup();

    verify(rivalryService).purgeClosedRivalryHeatEvents(60);
  }

  @Test
  void runCleanup_serviceThrowsException_doesNotPropagate() {
    when(rivalryService.purgeClosedRivalryHeatEvents(30))
        .thenThrow(new RuntimeException("Simulated failure"));

    scheduler.runCleanup();
  }
}
