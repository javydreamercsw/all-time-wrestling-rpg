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
package com.github.javydreamercsw.management.service.feud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeudResolutionServiceTest {

  @Mock private MultiWrestlerFeudRepository feudRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private Random random;

  @InjectMocks private FeudResolutionService feudResolutionService;

  private MultiWrestlerFeud feud;

  @BeforeEach
  void setUp() {
    feud = new MultiWrestlerFeud();
    feud.setName("Test Feud");
    feud.setHeat(25); // >= 20 so canAttemptResolution() returns true
    feud.setIsActive(true);
  }

  /** Add n active participants to the feud via the participants list directly. */
  private void addParticipants(final int count) {
    List<FeudParticipant> participants = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      FeudParticipant p = new FeudParticipant();
      p.setFeud(feud);
      p.setRole(FeudRole.NEUTRAL);
      p.setIsActive(true);
      participants.add(p);
    }
    feud.setParticipants(participants);
  }

  // ==================== not eligible for resolution ====================

  @Test
  void attemptFeudResolution_notEligible_returnsEarly() {
    feud.setHeat(5); // heat < 20 → canAttemptResolution() == false

    feudResolutionService.attemptFeudResolution(feud);

    verify(feudRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  // ==================== no participants ====================

  @Test
  void attemptFeudResolution_noParticipants_returnsEarlyWithoutRolling() {
    feud.setParticipants(new ArrayList<>());

    feudResolutionService.attemptFeudResolution(feud);

    verify(random, never()).nextInt(any(int.class));
    verify(feudRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  // ==================== roll above threshold → resolved ====================

  @Test
  void attemptFeudResolution_rollAboveThreshold_endsFeudAndPublishesEvent() {
    // 1 participant → threshold = 10; d20 returns 20 → roll=20 > 10
    addParticipants(1);
    when(random.nextInt(20)).thenReturn(19); // nextInt(20)+1 = 20

    feudResolutionService.attemptFeudResolution(feud);

    verify(feudRepository).save(feud);
    ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertNotNull(eventCaptor.getValue());
    assertFalse(feud.getIsActive());
    assertNotNull(feud.getEndedDate());
  }

  @Test
  void attemptFeudResolution_rollAboveThreshold_publishesCorrectEventType() {
    addParticipants(2);
    // 2 participants → threshold = 20; each d20 returns 15 → total = 30 > 20
    when(random.nextInt(20)).thenReturn(14); // 14+1=15, called twice

    feudResolutionService.attemptFeudResolution(feud);

    ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assert captor.getValue() instanceof FeudResolvedEvent;
    FeudResolvedEvent event = (FeudResolvedEvent) captor.getValue();
    assert event.getFeud() == feud;
  }

  // ==================== roll at or below threshold → not resolved ====================

  @Test
  void attemptFeudResolution_rollBelowThreshold_doesNotEndFeudOrPublishEvent() {
    // 1 participant → threshold = 10; d20 returns 5 → roll=5 <= 10
    addParticipants(1);
    when(random.nextInt(20)).thenReturn(4); // 4+1=5

    feudResolutionService.attemptFeudResolution(feud);

    verify(feudRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
    assert feud.getIsActive();
  }

  @Test
  void attemptFeudResolution_rollEqualToThreshold_doesNotResolve() {
    // 1 participant → threshold = 10; d20 returns 10 (not strictly > threshold)
    addParticipants(1);
    when(random.nextInt(20)).thenReturn(9); // 9+1=10 → 10 is NOT > 10

    feudResolutionService.attemptFeudResolution(feud);

    verify(feudRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
