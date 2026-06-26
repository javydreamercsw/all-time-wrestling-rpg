/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.drama;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.GameDateChangedEvent;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DramaEventSchedulerTest {

  @Mock private DramaEventService dramaEventService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseRepository universeRepository;

  private DramaEventScheduler scheduler;

  private Universe universe1;

  @BeforeEach
  void setUp() {
    scheduler = new DramaEventScheduler(dramaEventService, wrestlerRepository, universeRepository);
    // thresholdDays is @Value-injected; Spring isn't present in unit tests so set it explicitly
    ReflectionTestUtils.setField(scheduler, "thresholdDays", 7);
    universe1 = new Universe();
    universe1.setId(1L);
    when(universeRepository.findAll()).thenReturn(List.of(universe1));
  }

  // ==================== onGameDateChanged tests ====================

  @Test
  void onGameDateChanged_withSevenOrMoreDaysPassed_triggersGenerateRandomDramaEvents() {
    // Advance by exactly 7 days — always triggers drama events
    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    LocalDate newDate = oldDate.plusDays(7);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, newDate);

    // No wrestlers → nothing to generate, but the code path is exercised
    when(wrestlerRepository.findAllIds()).thenReturn(Collections.emptyList());

    scheduler.onGameDateChanged(event);

    // generateRandomDramaEvents() was invoked, which calls findAllIds()
    verify(wrestlerRepository).findAllIds();
  }

  @Test
  void onGameDateChanged_withMoreThanSevenDaysPassed_triggersGenerateRandomDramaEvents() {
    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    LocalDate newDate = oldDate.plusDays(30);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, newDate);

    when(wrestlerRepository.findAllIds()).thenReturn(Collections.emptyList());

    scheduler.onGameDateChanged(event);

    verify(wrestlerRepository).findAllIds();
  }

  @Test
  void onGameDateChanged_withZeroDaysPassed_doesNothing() {
    LocalDate date = LocalDate.of(2025, 1, 1);
    GameDateChangedEvent event = new GameDateChangedEvent(this, date, date);

    scheduler.onGameDateChanged(event);

    verifyNoInteractions(wrestlerRepository);
    verifyNoInteractions(dramaEventService);
  }

  @Test
  void onGameDateChanged_withNegativeDaysPassed_doesNothing() {
    // newDate is before oldDate → daysPassed < 0
    LocalDate oldDate = LocalDate.of(2025, 1, 10);
    LocalDate newDate = LocalDate.of(2025, 1, 1);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, newDate);

    scheduler.onGameDateChanged(event);

    verifyNoInteractions(wrestlerRepository);
    verifyNoInteractions(dramaEventService);
  }

  @Test
  void onGameDateChanged_withSmallTimejump_triggersWhenRandomFavors() {
    // Replace the internal Random with a mock that always returns a value that satisfies the check
    // daysPassed = 3, probability = 3/7 ≈ 0.428 → nextDouble() must return something < 0.428
    // We inject a mock Random that returns 0.0 (always triggers).
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.0); // 0.0 < (3/7) → triggers
    // nextInt() is needed by generateRandomDramaEvents() if wrestlers exist
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    LocalDate newDate = oldDate.plusDays(3);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, newDate);

    when(wrestlerRepository.findAllIds()).thenReturn(Collections.emptyList());

    scheduler.onGameDateChanged(event);

    // The probabilistic branch was taken; findAllIds() proves generateRandomDramaEvents ran
    verify(wrestlerRepository).findAllIds();
  }

  @Test
  void onGameDateChanged_customThreshold_triggersAtConfiguredDays() {
    // With threshold=3, advancing 3+ days should always trigger
    ReflectionTestUtils.setField(scheduler, "thresholdDays", 3);
    when(wrestlerRepository.findAllIds()).thenReturn(Collections.emptyList());

    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, oldDate.plusDays(3));

    scheduler.onGameDateChanged(event);

    verify(wrestlerRepository).findAllIds();
  }

  @Test
  void onGameDateChanged_withSmallTimejump_doesNotTriggerWhenRandomDisfavors() {
    // nextDouble() returns 1.0 → 1.0 >= (3/7) → does NOT trigger drama events
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(1.0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    LocalDate newDate = oldDate.plusDays(3);
    GameDateChangedEvent event = new GameDateChangedEvent(this, oldDate, newDate);

    scheduler.onGameDateChanged(event);

    verifyNoInteractions(wrestlerRepository);
    verifyNoInteractions(dramaEventService);
  }

  // ==================== generateRandomDramaEvents tests ====================

  @Test
  void generateRandomDramaEvents_withNoWrestlers_generatesNoEvents() {
    when(wrestlerRepository.findAllIds()).thenReturn(Collections.emptyList());

    scheduler.generateRandomDramaEvents();

    verify(wrestlerRepository).findAllIds();
    verifyNoInteractions(dramaEventService);
  }

  @Test
  void generateRandomDramaEvents_withWrestlersAndZeroEventCount_generatesNoEvents() {
    // Make getRandomEventCount() return 0 by forcing nextDouble() < 0.75
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.5); // < 0.75 → 0 events
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(1L, 2L));

    scheduler.generateRandomDramaEvents();

    verify(wrestlerRepository).findAllIds();
    verifyNoInteractions(dramaEventService);
  }

  @Test
  void generateRandomDramaEvents_withWrestlersAndOneEvent_callsDramaEventService() {
    // Force getRandomEventCount() to return 1:
    //   first nextDouble() call → 0.85 (>= 0.75, < 0.90 → 1 event)
    //   nextInt() → selects wrestler at index 0
    //   getActiveInjuryCount returns 0 (below limit of 3)
    //   generateRandomDramaEvent returns a present Optional
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.85);
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(10L));
    when(dramaEventService.getActiveInjuryCount(10L, 1L)).thenReturn(0);

    DramaEvent dummyEvent = new DramaEvent();
    dummyEvent.setTitle("Test Event");
    dummyEvent.setSeverity(DramaEventSeverity.NEUTRAL);
    when(dramaEventService.generateRandomDramaEvent(10L, 1L)).thenReturn(Optional.of(dummyEvent));

    scheduler.generateRandomDramaEvents();

    verify(dramaEventService).generateRandomDramaEvent(10L, 1L);
  }

  @Test
  void generateRandomDramaEvents_skipsWrestlerWithTooManyInjuries() {
    // Force 1 event to be generated, but wrestler already has 3 active injuries
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.85); // → 1 event
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(99L));
    when(dramaEventService.getActiveInjuryCount(99L, 1L)).thenReturn(3);

    scheduler.generateRandomDramaEvents();

    // Should NOT call generateRandomDramaEvent because injury count is at the limit
    verify(dramaEventService, never()).generateRandomDramaEvent(anyLong(), anyLong());
  }

  @Test
  void generateRandomDramaEvents_handlesEmptyOptionalFromService() {
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.85); // → 1 event
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(5L));
    when(dramaEventService.getActiveInjuryCount(5L, 1L)).thenReturn(0);
    when(dramaEventService.generateRandomDramaEvent(5L, 1L)).thenReturn(Optional.empty());

    // Should not throw; an empty Optional is just logged as a warning
    scheduler.generateRandomDramaEvents();

    verify(dramaEventService).generateRandomDramaEvent(5L, 1L);
  }

  @Test
  void generateRandomDramaEvents_withTwoEvents_generatesTwoEvents() {
    // Force getRandomEventCount() to return 2:
    //   nextDouble() → 0.91 (>= 0.90, < 0.95 → 2 events)
    //   nextInt() always picks index 0
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.91);
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(1L, 2L));
    when(dramaEventService.getActiveInjuryCount(anyLong(), anyLong())).thenReturn(0);

    DramaEvent dummyEvent = new DramaEvent();
    dummyEvent.setTitle("Event");
    dummyEvent.setSeverity(DramaEventSeverity.NEUTRAL);
    when(dramaEventService.generateRandomDramaEvent(anyLong(), anyLong()))
        .thenReturn(Optional.of(dummyEvent));

    scheduler.generateRandomDramaEvents();

    verify(dramaEventService, times(2)).generateRandomDramaEvent(anyLong(), anyLong());
  }

  @Test
  void generateRandomDramaEvents_withThreeEvents_generatesThreeEvents() {
    // Force getRandomEventCount() to return 3:
    //   nextDouble() → 0.96 (>= 0.95 → 3 events)
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.96);
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(7L));
    when(dramaEventService.getActiveInjuryCount(anyLong(), anyLong())).thenReturn(0);

    DramaEvent dummyEvent = new DramaEvent();
    dummyEvent.setTitle("Event");
    dummyEvent.setSeverity(DramaEventSeverity.NEUTRAL);
    when(dramaEventService.generateRandomDramaEvent(anyLong(), anyLong()))
        .thenReturn(Optional.of(dummyEvent));

    scheduler.generateRandomDramaEvents();

    verify(dramaEventService, times(3)).generateRandomDramaEvent(anyLong(), anyLong());
  }

  @Test
  void generateRandomDramaEvents_serviceThrowsException_doesNotPropagateException() {
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.85); // → 1 event
    when(mockRandom.nextInt(anyInt())).thenReturn(0);
    ReflectionTestUtils.setField(scheduler, "random", mockRandom);

    when(wrestlerRepository.findAllIds()).thenReturn(List.of(3L));
    when(dramaEventService.getActiveInjuryCount(3L, 1L)).thenReturn(0);
    when(dramaEventService.generateRandomDramaEvent(3L, 1L))
        .thenThrow(new RuntimeException("Simulated service failure"));

    // The scheduler catches all exceptions — this must not throw
    scheduler.generateRandomDramaEvents();
  }
}
