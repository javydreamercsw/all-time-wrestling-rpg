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
package com.github.javydreamercsw.management.service.wrestler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.WrestlerRetiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RetirementServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CampaignRepository campaignRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private RetirementService retirementService;

  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Old Timer");
    wrestler.setPhysicalCondition(100);
    wrestler.setActive(true);
  }

  @Test
  void testNoRetirementAtFullHealth() {
    retirementService.checkRetirement(wrestler);
    assertTrue(wrestler.getActive());
    verify(wrestlerRepository, never()).save(any());
  }

  @Test
  void testForcedRetirement() {
    wrestler.setPhysicalCondition(5);
    // We can't easily mock the internal Random without injecting it,
    // but we can test the retireWrestler method directly
    retirementService.retireWrestler(wrestler, "Injured");

    assertFalse(wrestler.getActive());
    verify(wrestlerRepository, times(1)).save(wrestler);
    verify(eventPublisher, times(1)).publishEvent(any(WrestlerRetiredEvent.class));
  }

  @Test
  void testRetirementCheckSkipsInactive() {
    wrestler.setActive(false);
    wrestler.setPhysicalCondition(0);
    retirementService.checkRetirement(wrestler);
    verify(wrestlerRepository, never()).save(any());
  }
}
