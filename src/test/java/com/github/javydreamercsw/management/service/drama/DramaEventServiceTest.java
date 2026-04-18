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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DramaEventServiceTest {

  @Mock private DramaEventRepository dramaEventRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private RivalryService rivalryService;
  @Mock private InjuryService injuryService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private Random random;

  private DramaEventService dramaEventService;
  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    dramaEventService =
        new DramaEventService(
            dramaEventRepository,
            wrestlerRepository,
            universeRepository,
            wrestlerStateRepository,
            wrestlerService,
            rivalryService,
            injuryService,
            securityUtils,
            clock,
            random,
            eventPublisher);
  }

  @Test
  void testCreateDramaEvent() {
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);

    Universe u = new Universe();
    u.setId(1L);

    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(w1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(w2));
    when(universeRepository.findById(1L)).thenReturn(Optional.of(u));
    when(securityUtils.canCreate()).thenReturn(true);
    when(dramaEventRepository.save(any(DramaEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<DramaEvent> result =
        dramaEventService.createDramaEvent(
            1L,
            2L,
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Title",
            "Desc",
            1L);

    assertTrue(result.isPresent());
    assertEquals("Title", result.get().getTitle());
    assertEquals(w1, result.get().getPrimaryWrestler());
    assertEquals(w2, result.get().getSecondaryWrestler());
  }

  @Test
  void testProcessBackstageIncident() {
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);

    Universe u = new Universe();
    u.setId(1L);

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(w1);
    event.setSecondaryWrestler(w2);
    event.setUniverse(u);
    event.setEventType(DramaEventType.BACKSTAGE_INCIDENT);
    event.setSeverity(DramaEventSeverity.NEGATIVE);

    dramaEventService.processEvent(event);

    verify(rivalryService).addHeatBetweenWrestlers(eq(1L), eq(2L), anyInt(), anyString());
    assertTrue(event.getIsProcessed());
    assertNotNull(event.getProcessedDate());
  }

  @Test
  void testProcessInjuryEvent() {
    Wrestler w1 = new Wrestler();
    w1.setId(1L);

    Universe u = new Universe();
    u.setId(1L);

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(w1);
    event.setUniverse(u);
    event.setEventType(DramaEventType.INJURY_INCIDENT);
    event.setSeverity(DramaEventSeverity.MAJOR);

    dramaEventService.processEvent(event);

    verify(injuryService).createInjuryFromBumps(eq(1L), eq(1L));
    assertTrue(event.getIsProcessed());
  }
}
