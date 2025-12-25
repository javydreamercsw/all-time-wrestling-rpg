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
package com.github.javydreamercsw.management.service.wrestler;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WrestlerServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private InjuryService injuryService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private WrestlerService wrestlerService;

  private Wrestler wrestler;

  @Mock private DiceBag diceBag;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
  }

  @Test
  void testAddBump_PublishesEvent() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);

    // When
    wrestlerService.addBump(1L);

    // Then
    ArgumentCaptor<WrestlerBumpEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }

  @Test
  void testHealChance_PublishesEvent() {
    // Given
    wrestler.setBumps(1);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    when(diceBag.roll()).thenReturn(4); // Ensure bump is healed

    // When
    wrestlerService.healChance(1L, diceBag);

    // Then
    ArgumentCaptor<WrestlerBumpHealedEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpHealedEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }
}
