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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RingsideAiServiceTest {

  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideActionDataService ringsideActionDataService;
  @InjectMocks private RingsideAiService ringsideAiService;

  private Segment segment;
  private Npc heelManager;
  private Npc faceManager;
  private Wrestler beneficiary;
  private RingsideAction heelAction;
  private RingsideAction faceAction;

  @BeforeEach
  void setUp() {
    segment = new Segment();
    segment.setRefereeAwarenessLevel(0);

    heelManager = new Npc();
    heelManager.setAlignment(AlignmentType.HEEL);
    heelManager.setName("Bobby Heenan");

    faceManager = new Npc();
    faceManager.setAlignment(AlignmentType.FACE);
    faceManager.setName("Miss Elizabeth");

    beneficiary = new Wrestler();
    beneficiary.setName("Ric Flair");

    heelAction = new RingsideAction();
    heelAction.setName("Referee Distraction");
    heelAction.setRisk(20);
    heelAction.setImpact(10);
    heelAction.setAlignment(AlignmentType.HEEL);

    faceAction = new RingsideAction();
    faceAction.setName("Crowd Support");
    faceAction.setRisk(0);
    faceAction.setImpact(5);
    faceAction.setAlignment(AlignmentType.FACE);
  }

  @Test
  void heelManagerShouldEventuallyAct() {
    when(ringsideActionDataService.findAllActions()).thenReturn(List.of(heelAction));
    when(ringsideActionService.performAction(any(), any(), any(), any()))
        .thenReturn(
            new RingsideActionService.RingsideActionResult(
                heelAction, true, 10, false, false, "Success"));

    // We'll run it a few times since it's probabilistic
    boolean acted = false;
    for (int i = 0; i < 100; i++) {
      Optional<RingsideActionService.RingsideActionResult> result =
          ringsideAiService.evaluateRingsideAction(segment, heelManager, beneficiary);
      if (result.isPresent()) {
        acted = true;
        break;
      }
    }
    assertTrue(acted, "Heel manager should perform action at least once in 100 attempts");
    verify(ringsideActionService, atLeastOnce()).performAction(any(), any(), any(), eq(heelAction));
  }

  @Test
  void faceManagerShouldAvoidHeelActions() {
    // Only heel actions available
    when(ringsideActionDataService.findAllActions()).thenReturn(List.of(heelAction));

    // Try many times
    for (int i = 0; i < 100; i++) {
      Optional<RingsideActionService.RingsideActionResult> result =
          ringsideAiService.evaluateRingsideAction(segment, faceManager, beneficiary);
      // It should NOT perform the heel action.
      // Note: pickAction has a fallback to all actions if none suitable,
      // but let's check current implementation behavior.
      result.ifPresent(r -> assertNotEquals(AlignmentType.HEEL, r.action().getAlignment()));
    }
  }

  @Test
  void faceManagerShouldPickFaceActions() {
    when(ringsideActionDataService.findAllActions()).thenReturn(List.of(heelAction, faceAction));
    when(ringsideActionService.performAction(any(), any(), any(), eq(faceAction)))
        .thenReturn(
            new RingsideActionService.RingsideActionResult(
                faceAction, true, 0, false, false, "Success"));

    boolean acted = false;
    for (int i = 0; i < 100; i++) {
      Optional<RingsideActionService.RingsideActionResult> result =
          ringsideAiService.evaluateRingsideAction(segment, faceManager, beneficiary);
      if (result.isPresent()) {
        acted = true;
        assertEquals(faceAction, result.get().action());
        assertEquals(AlignmentType.FACE, result.get().action().getAlignment());
      }
    }
    assertTrue(acted, "Face manager should eventually perform face action");
  }
}
