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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RingsideAiService {

  private final RingsideActionService ringsideActionService;
  private final RingsideActionDataService ringsideActionDataService;
  private final Random random = new Random();

  /**
   * Determines if an NPC (manager or teammate) should perform a ringside action.
   *
   * @param segment The current segment.
   * @param interferer The NPC or Wrestler potentially helping.
   * @param beneficiary The wrestler who would benefit.
   * @return Optional result if an action was attempted.
   */
  public Optional<RingsideActionService.RingsideActionResult> evaluateRingsideAction(
      Segment segment, Object interferer, Wrestler beneficiary) {

    AlignmentType alignment = getAlignment(interferer);
    int awareness = segment.getRefereeAwarenessLevel();

    // AI logic based on alignment
    double actionChance = 0.0;

    switch (alignment) {
      case HEEL -> {
        // Heels act often, especially illegal ones
        actionChance = 0.30;
        if (awareness < 50) actionChance += 0.15;
      }
      case NEUTRAL -> {
        actionChance = 0.15;
      }
      case FACE -> {
        // Faces act less often, preferring legal support
        actionChance = 0.10;
      }
    }

    if (random.nextDouble() < actionChance) {
      // Pick an action based on alignment and awareness
      RingsideAction action = pickAction(alignment, awareness);
      if (action != null) {
        return Optional.ofNullable(
            ringsideActionService.performAction(segment, interferer, beneficiary, action));
      }
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

  private RingsideAction pickAction(AlignmentType alignment, int awareness) {
    List<RingsideAction> allActions = ringsideActionDataService.findAllActions();
    if (allActions.isEmpty()) return null;

    // Filter by alignment: Heels can do anything, Faces prefer non-Heel actions
    List<RingsideAction> suitableActions =
        allActions.stream()
            .filter(
                a -> {
                  if (alignment == AlignmentType.HEEL) return true;
                  return a.getAlignment() != AlignmentType.HEEL;
                })
            .toList();

    if (suitableActions.isEmpty()) suitableActions = allActions;

    // If awareness is high, avoid risky/illegal actions
    if (awareness > 70) {
      List<RingsideAction> lowRisk =
          suitableActions.stream().filter(a -> a.getRisk() <= 20).toList();
      if (!lowRisk.isEmpty()) suitableActions = lowRisk;
    }

    return suitableActions.get(random.nextInt(suitableActions.size()));
  }
}
