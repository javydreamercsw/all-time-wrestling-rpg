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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.npc.NpcService;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterferenceAiService {

  private final InterferenceService interferenceService;
  private final NpcService npcService;
  private final Random random = new Random();

  /**
   * Determines if an NPC (manager or teammate) should interfere in a match.
   *
   * @param segment The current segment.
   * @param interferer The NPC or Wrestler potentially interfering.
   * @param beneficiary The wrestler who would benefit.
   * @return Optional result if interference was attempted.
   */
  public Optional<InterferenceService.InterferenceResult> evaluateInterference(
      Segment segment, Object interferer, Wrestler beneficiary) {

    AlignmentType alignment = getAlignment(interferer);
    int awareness = segment.getRefereeAwarenessLevel();

    // AI logic based on alignment
    double interferenceChance = 0.0;

    switch (alignment) {
      case HEEL -> {
        // Heels interfere often, especially if awareness is low
        interferenceChance = 0.25;
        if (awareness < 50) interferenceChance += 0.15;
      }
      case NEUTRAL -> {
        interferenceChance = 0.10;
      }
      case FACE -> {
        // Faces rarely interfere, usually only if desperate
        interferenceChance = 0.05;
      }
    }

    if (random.nextDouble() < interferenceChance) {
      // Pick an action based on risk vs awareness
      InterferenceType type = pickAction(alignment, awareness);
      return Optional.ofNullable(
          interferenceService.attemptInterference(segment, interferer, beneficiary, type));
    }

    return Optional.empty();
  }

  private AlignmentType getAlignment(Object interferer) {
    if (interferer instanceof Wrestler w && w.getAlignment() != null) {
      return w.getAlignment().getAlignmentType();
    }
    if (interferer instanceof Npc n) {
      return n.getAlignment();
    }
    return AlignmentType.NEUTRAL;
  }

  private InterferenceType pickAction(AlignmentType alignment, int awareness) {
    if (awareness > 70) {
      return InterferenceType.REF_DISTRACTION; // Low risk
    }
    if (alignment == AlignmentType.HEEL && awareness < 40) {
      return InterferenceType.CHEAP_SHOT; // High risk, high reward
    }
    return InterferenceType.TRIP_ANKLE_PULL; // Medium
  }
}
