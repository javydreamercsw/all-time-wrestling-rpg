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
package com.github.javydreamercsw.management.service.interference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterferenceServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private NpcService npcService;
  @Mock private FactionService factionService;

  @InjectMocks private InterferenceService interferenceService;

  private Segment segment;
  private Npc referee;
  private Wrestler interferer;
  private Wrestler beneficiary;

  @BeforeEach
  void setUp() {
    referee = new Npc();
    referee.setName("Earl Hebner");

    segment = new Segment();
    segment.setReferee(referee);
    segment.setRefereeAwarenessLevel(0);

    interferer = new Wrestler();
    interferer.setName("Hulk Hogan");

    beneficiary = new Wrestler();
    beneficiary.setName("Kevin Nash");
  }

  @Test
  void shouldIncreaseAwarenessOnAttempt() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(factionService.getAffinityBetween(interferer, beneficiary)).thenReturn(0);

    InterferenceService.InterferenceResult result =
        interferenceService.attemptInterference(
            segment, interferer, beneficiary, InterferenceType.REF_DISTRACTION);

    assertTrue(segment.getRefereeAwarenessLevel() > 0);
    assertEquals(InterferenceType.REF_DISTRACTION.getBaseRisk(), result.detectionIncrease());
    verify(segmentRepository).save(segment);
  }

  @Test
  void shouldApplyFactionAffinityReduction() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    // Max affinity should give 30% reduction
    when(factionService.getAffinityBetween(interferer, beneficiary)).thenReturn(100);

    int baseRisk = InterferenceType.WEAPON_SLIDE.getBaseRisk();
    int expectedRisk = (int) (baseRisk * 0.7); // 30% reduction

    InterferenceService.InterferenceResult result =
        interferenceService.attemptInterference(
            segment, interferer, beneficiary, InterferenceType.WEAPON_SLIDE);

    assertEquals(expectedRisk, result.detectionIncrease());
  }

  @Test
  void shouldTriggerEjectionAndDQ() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    segment.setRefereeAwarenessLevel(90);

    InterferenceService.InterferenceResult result =
        interferenceService.attemptInterference(
            segment, interferer, beneficiary, InterferenceType.CHEAP_SHOT);

    assertTrue(result.ejected());
    assertTrue(result.disqualified());
    assertTrue(result.message().contains("DISQUALIFICATION"));
  }
}
