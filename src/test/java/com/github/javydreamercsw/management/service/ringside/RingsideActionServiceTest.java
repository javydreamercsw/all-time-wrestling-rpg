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
package com.github.javydreamercsw.management.service.ringside;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
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
class RingsideActionServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private NpcService npcService;
  @Mock private FactionService factionService;

  @InjectMocks private RingsideActionService ringsideActionService;

  private Segment segment;
  private Npc referee;
  private Wrestler interferer;
  private Wrestler beneficiary;

  private RingsideActionType legalType;
  private RingsideActionType illegalType;

  private RingsideAction coachAction;
  private RingsideAction weaponAction;

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

    legalType = new RingsideActionType();
    legalType.setName("Legal Support");
    legalType.setIncreasesAwareness(false);
    legalType.setCanCauseDq(false);
    legalType.setBaseRiskMultiplier(0.0);

    illegalType = new RingsideActionType();
    illegalType.setName("Illegal Interference");
    illegalType.setIncreasesAwareness(true);
    illegalType.setCanCauseDq(true);
    illegalType.setBaseRiskMultiplier(1.0);

    coachAction = new RingsideAction();
    coachAction.setName("Coaching");
    coachAction.setType(legalType);
    coachAction.setRisk(0);
    coachAction.setImpact(5);

    weaponAction = new RingsideAction();
    weaponAction.setName("Weapon Slide");
    weaponAction.setType(illegalType);
    weaponAction.setRisk(40);
    weaponAction.setImpact(25);
  }

  @Test
  void legalActionShouldNotIncreaseAwareness() {
    when(npcService.getAwareness(referee)).thenReturn(50);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, interferer, beneficiary, coachAction);

    assertEquals(0, segment.getRefereeAwarenessLevel());
    assertEquals(0, result.detectionIncrease());
    verify(segmentRepository).save(segment);
  }

  @Test
  void illegalActionShouldIncreaseAwareness() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(factionService.getAffinityBetween(interferer, beneficiary)).thenReturn(0);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, interferer, beneficiary, weaponAction);

    assertTrue(segment.getRefereeAwarenessLevel() > 0);
    assertEquals(weaponAction.getRisk(), result.detectionIncrease());
  }

  @Test
  void shouldApplyFactionAffinityReduction() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    // Max affinity should give 30% reduction
    when(factionService.getAffinityBetween(interferer, beneficiary)).thenReturn(100);

    int baseRisk = weaponAction.getRisk();
    int expectedRisk = (int) (baseRisk * 0.7); // 30% reduction

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, interferer, beneficiary, weaponAction);

    assertEquals(expectedRisk, result.detectionIncrease());
  }

  @Test
  void shouldTriggerEjectionAndDQ() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    segment.setRefereeAwarenessLevel(90);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, interferer, beneficiary, weaponAction);

    assertTrue(result.ejected());
    assertTrue(result.disqualified());
    assertTrue(result.message().contains("DISQUALIFICATION"));
  }
}
