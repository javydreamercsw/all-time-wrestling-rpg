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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterferenceAiServiceTest {

  @Mock private InterferenceService interferenceService;
  @InjectMocks private InterferenceAiService interferenceAiService;

  private Segment segment;
  private Npc heelManager;
  private Wrestler beneficiary;

  @BeforeEach
  void setUp() {
    segment = new Segment();
    segment.setRefereeAwarenessLevel(0);

    heelManager = new Npc();
    heelManager.setAlignment(AlignmentType.HEEL);
    heelManager.setName("Bobby Heenan");

    beneficiary = new Wrestler();
    beneficiary.setName("Ric Flair");
  }

  @Test
  void heelManagerShouldEventuallyInterfere() {
    // We'll run it a few times since it's probabilistic
    boolean interfered = false;
    for (int i = 0; i < 100; i++) {
      Optional<InterferenceService.InterferenceResult> result =
          interferenceAiService.evaluateInterference(segment, heelManager, beneficiary);
      if (result.isPresent()) {
        interfered = true;
        break;
      }
    }
    assertTrue(interfered, "Heel manager should interfere at least once in 100 attempts");
    verify(interferenceService, atLeastOnce()).attemptInterference(any(), any(), any(), any());
  }
}
