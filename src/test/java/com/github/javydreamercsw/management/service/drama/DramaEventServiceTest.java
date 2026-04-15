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
package com.github.javydreamercsw.management.service.drama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.PermissionService;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class DramaEventServiceTest {

  private DramaEventRepository dramaEventRepository;
  private WrestlerRepository wrestlerRepository;
  private WrestlerService wrestlerService;
  private RivalryService rivalryService;
  private InjuryService injuryService;
  private PermissionService permissionService;
  private Clock clock;
  private Random random;
  private ApplicationEventPublisher eventPublisher;
  private DramaEventService dramaEventService;

  @BeforeEach
  void setUp() {
    dramaEventRepository = mock(DramaEventRepository.class);
    wrestlerRepository = mock(WrestlerRepository.class);
    wrestlerService = mock(WrestlerService.class);
    rivalryService = mock(RivalryService.class);
    injuryService = mock(InjuryService.class);
    permissionService = mock(PermissionService.class);
    clock = Clock.fixed(Instant.parse("2026-04-04T10:00:00Z"), ZoneId.systemDefault());
    random = new Random(42); // Fixed seed for reproducibility
    eventPublisher = mock(ApplicationEventPublisher.class);

    dramaEventService =
        new DramaEventService(
            dramaEventRepository,
            wrestlerRepository,
            wrestlerService,
            rivalryService,
            injuryService,
            permissionService,
            clock,
            random,
            eventPublisher);
  }

  @Test
  void testCreateDramaEvent() {
    Wrestler primary = new Wrestler();
    primary.setId(1L);
    primary.setName("Primary");

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(primary));
    when(dramaEventRepository.save(any(DramaEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var eventOpt =
        dramaEventService.createDramaEvent(
            1L,
            null,
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Title",
            "Description");

    assertThat(eventOpt).isPresent();
    verify(dramaEventRepository).save(any(DramaEvent.class));
    verify(eventPublisher).publishEvent(any());
  }

  @Test
  void testGenerateRandomDramaEventForAllTypes() {
    Wrestler primary = new Wrestler();
    primary.setId(1L);
    primary.setName("Primary");
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(primary));
    when(wrestlerRepository.findAllIds()).thenReturn(List.of(1L, 2L));
    Wrestler secondary = new Wrestler();
    secondary.setId(2L);
    secondary.setName("Secondary");
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(secondary));

    when(dramaEventRepository.save(any(DramaEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Test all event types to cover the switch statement in generateEventTemplate
    for (DramaEventType type : DramaEventType.values()) {
      // Run multiple times to try and hit different severities and multi-wrestler paths
      for (int i = 0; i < 20; i++) {
        dramaEventService.generateRandomDramaEvent(1L);
      }
    }

    verify(dramaEventRepository, atLeastOnce()).save(any(DramaEvent.class));
  }

  @Test
  void testProcessEvent() {
    Wrestler primary = mock(Wrestler.class);
    when(primary.getId()).thenReturn(1L);
    when(primary.getName()).thenReturn("Primary");
    when(primary.getFans()).thenReturn(100L);
    when(primary.addBump()).thenReturn(true); // Trigger injury logic

    Wrestler secondary = mock(Wrestler.class);
    when(secondary.getId()).thenReturn(2L);
    when(secondary.getName()).thenReturn("Secondary");

    DramaEvent event = new DramaEvent();
    event.setId(1L);
    event.setPrimaryWrestler(primary);
    event.setSecondaryWrestler(secondary);
    event.setFanImpact(10L);
    event.setHeatImpact(5);
    event.setInjuryCaused(true);
    event.setRivalryCreated(true);
    event.setRivalryEnded(true);
    event.setEventType(DramaEventType.BACKSTAGE_INCIDENT);
    event.setTitle("Test Event");

    dramaEventService.processEvent(event);

    verify(primary).setFans(110L);
    verify(rivalryService).addHeatBetweenWrestlers(1L, 2L, 5, "Drama Event: Test Event");
    verify(injuryService).createInjuryFromBumps(1L);
    verify(rivalryService).createRivalry(any(), any(), any());
    verify(dramaEventRepository, atLeastOnce()).save(event);
  }

  @Test
  void testGetters() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(dramaEventRepository.findByWrestler(any())).thenReturn(Collections.emptyList());
    when(dramaEventRepository.findByWrestler(any(), any()))
        .thenReturn(org.springframework.data.domain.Page.empty());
    when(dramaEventRepository.findBetweenWrestlers(any(), any()))
        .thenReturn(Collections.emptyList());

    assertThat(dramaEventService.getEventsForWrestler(1L)).isEmpty();
    assertThat(
            dramaEventService.getEventsForWrestler(
                1L, org.springframework.data.domain.Pageable.unpaged()))
        .isEmpty();
    assertThat(dramaEventService.getEventsBetweenWrestlers(1L, 2L)).isEmpty();
  }
}
