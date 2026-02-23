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

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterferenceService {

  private final SegmentRepository segmentRepository;
  private final NpcService npcService;
  private final FactionService factionService;

  public static final int EJECTION_THRESHOLD = 80;
  public static final int DQ_THRESHOLD = 100;

  /** Result of an interference attempt. */
  public record InterferenceResult(
      InterferenceType type,
      boolean success,
      int detectionIncrease,
      boolean ejected,
      boolean disqualified,
      String message) {}

  /**
   * Processes an interference attempt.
   *
   * @param segment The segment where interference occurs.
   * @param interferer The wrestler or NPC interfering.
   * @param beneficiary The wrestler benefitting from the interference.
   * @param type The type of interference.
   * @return The result of the attempt.
   */
  @Transactional
  public InterferenceResult attemptInterference(
      @NonNull Segment segment,
      @NonNull Object interferer,
      @NonNull Wrestler beneficiary,
      @NonNull InterferenceType type) {

    Npc referee = segment.getReferee();
    int baseAwareness = npcService.getAwareness(referee);
    int currentMeter = segment.getRefereeAwarenessLevel();

    // Calculate risk
    int detectionIncrease = type.getBaseRisk();

    // Adjust risk based on referee base awareness (higher awareness = more detection)
    detectionIncrease = (int) (detectionIncrease * (baseAwareness / 50.0));

    // Faction Affinity Bonus: Reduce risk if interferer and beneficiary are in the same faction
    if (interferer instanceof Wrestler w) {
      int affinity = factionService.getAffinityBetween(w, beneficiary);
      if (affinity > 0) {
        // Up to 30% reduction in risk for max affinity
        double reduction = (affinity / 100.0) * 0.3;
        detectionIncrease = (int) (detectionIncrease * (1.0 - reduction));
      }
    }

    // Success chance: Inverse of current meter and base awareness
    // Base success is high at low meter, but drops as ref pays more attention
    double successChance = 1.0 - (currentMeter / 100.0);
    boolean success = Math.random() < successChance;

    // Update segment state
    int newMeter = Math.min(100, currentMeter + detectionIncrease);
    segment.setRefereeAwarenessLevel(newMeter);
    segmentRepository.save(segment);

    boolean ejected = newMeter >= EJECTION_THRESHOLD;
    boolean disqualified = newMeter >= DQ_THRESHOLD && !isNoDqMatch(segment);

    String message = buildResultMessage(type, success, ejected, disqualified);

    return new InterferenceResult(type, success, detectionIncrease, ejected, disqualified, message);
  }

  private boolean isNoDqMatch(@NonNull Segment segment) {
    return segment.getSegmentRules().stream()
        .anyMatch(
            com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule::getNoDq);
  }

  private String buildResultMessage(
      InterferenceType type, boolean success, boolean ejected, boolean disqualified) {
    if (disqualified) {
      return "The referee saw the illegal " + type.getDisplayName() + "! DISQUALIFICATION!";
    }
    if (ejected) {
      return "The "
          + type.getDisplayName()
          + " was spotted! The referee is EJECTING the interferer from ringside!";
    }
    if (success) {
      return "The " + type.getDisplayName() + " was successful and unnoticed by the referee!";
    } else {
      return "The " + type.getDisplayName() + " failed, and the referee is growing suspicious.";
    }
  }
}
