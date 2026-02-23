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

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RingsideActionService {

  private final SegmentRepository segmentRepository;
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final FactionService factionService;
  private final TeamService teamService;
  private final AlignmentService alignmentService;

  public RingsideActionService(
      SegmentRepository segmentRepository,
      WrestlerRepository wrestlerRepository,
      NpcService npcService,
      FactionService factionService,
      TeamService teamService,
      AlignmentService alignmentService) {
    this.segmentRepository = segmentRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.factionService = factionService;
    this.teamService = teamService;
    this.alignmentService = alignmentService;
  }

  public static final int EJECTION_THRESHOLD = 80;
  public static final int DQ_THRESHOLD = 100;

  /** Result of a ringside action attempt. */
  public record RingsideActionResult(
      RingsideAction action,
      boolean success,
      int detectionIncrease,
      boolean ejected,
      boolean disqualified,
      String message) {}

  @Transactional(readOnly = true)
  public boolean hasSupportAtRingside(Segment segment, Wrestler wrestler) {
    if (wrestler == null || segment == null) return false;

    // Refresh/Reattach to ensure session is open for lazy loading
    Segment attachedSegment = segmentRepository.findById(segment.getId()).orElse(segment);
    Wrestler attachedWrestler = wrestlerRepository.findById(wrestler.getId()).orElse(wrestler);

    // Check for direct manager
    if (attachedWrestler.getManager() != null) return true;

    // Check for faction manager or other members
    if (attachedWrestler.getFaction() != null) {
      if (attachedWrestler.getFaction().getManager() != null) return true;

      boolean otherFactionMembersExist =
          attachedWrestler.getFaction().getMembers().stream()
              .anyMatch(m -> !attachedSegment.getWrestlers().contains(m));
      if (otherFactionMembersExist) return true;
    }

    // Check for team members not in the match
    boolean otherTeamMembersExist =
        teamService.getActiveTeamsByWrestler(attachedWrestler).stream()
            .anyMatch(
                t -> !attachedSegment.getWrestlers().contains(t.getPartner(attachedWrestler)));
    if (otherTeamMembersExist) return true;

    return false;
  }

  @Transactional(readOnly = true)
  public Object getBestSupporter(Segment segment, Wrestler wrestler) {
    if (wrestler == null || segment == null) return null;

    Segment attachedSegment = segmentRepository.findById(segment.getId()).orElse(segment);
    Wrestler attachedWrestler = wrestlerRepository.findById(wrestler.getId()).orElse(wrestler);

    // 1. Direct Manager
    if (attachedWrestler.getManager() != null) return attachedWrestler.getManager();

    // 2. Faction Manager
    if (attachedWrestler.getFaction() != null
        && attachedWrestler.getFaction().getManager() != null) {
      return attachedWrestler.getFaction().getManager();
    }

    // 3. Faction Members (not in match)
    if (attachedWrestler.getFaction() != null) {
      Wrestler otherMember =
          attachedWrestler.getFaction().getMembers().stream()
              .filter(m -> !attachedSegment.getWrestlers().contains(m))
              .findFirst()
              .orElse(null);
      if (otherMember != null) return otherMember;
    }

    // 4. Team Partner (not in match)
    return teamService.getActiveTeamsByWrestler(attachedWrestler).stream()
        .map(t -> t.getPartner(attachedWrestler))
        .filter(m -> !attachedSegment.getWrestlers().contains(m))
        .findFirst()
        .orElse(null);
  }

  /**
   * Processes a ringside action attempt.
   *
   * @param segment The segment where the action occurs.
   * @param interferer The wrestler or NPC performing the action.
   * @param beneficiary The wrestler benefitting from the action.
   * @param action The ringside action.
   * @return The result of the attempt.
   */
  @Transactional
  public RingsideActionResult performAction(
      Segment segment, Object interferer, Wrestler beneficiary, @NonNull RingsideAction action) {

    Npc referee = segment.getReferee();
    int baseAwareness = npcService.getAwareness(referee);
    int currentMeter = segment.getRefereeAwarenessLevel();

    int detectionIncrease = 0;
    if (action.getType().isIncreasesAwareness()) {
      // Calculate risk
      detectionIncrease = action.getRisk();

      // Adjust risk based on referee base awareness (higher awareness = more detection)
      detectionIncrease = (int) (detectionIncrease * (baseAwareness / 50.0));

      // Apply base risk multiplier from the type
      detectionIncrease = (int) (detectionIncrease * action.getType().getBaseRiskMultiplier());

      // Faction Affinity Bonus: Reduce risk if interferer and beneficiary are in the same faction
      if (interferer instanceof Wrestler w) {
        int affinity = factionService.getAffinityBetween(w, beneficiary);
        if (affinity > 0) {
          // Up to 30% reduction in risk for max affinity
          double reduction = (affinity / 100.0) * 0.3;
          detectionIncrease = (int) (detectionIncrease * (1.0 - reduction));
        }
      }
    }

    // Success chance: Inverse of current meter and base awareness
    // Legal actions are always successful in terms of execution, but narration still applies
    double successChance = 1.0;
    if (action.getType().isIncreasesAwareness()) {
      successChance = 1.0 - (currentMeter / 100.0);
    }

    boolean success = Math.random() < successChance;

    // Update segment state
    int newMeter = Math.min(100, currentMeter + detectionIncrease);
    segment.setRefereeAwarenessLevel(newMeter);
    segmentRepository.save(segment);

    // Apply alignment shifts if beneficiary is in a campaign
    if (success
        && beneficiary.getAlignment() != null
        && beneficiary.getAlignment().getCampaign() != null) {
      if (action.getAlignment()
          == com.github.javydreamercsw.management.domain.campaign.AlignmentType.FACE) {
        alignmentService.shiftAlignment(beneficiary.getAlignment().getCampaign(), 1);
      } else if (action.getAlignment()
          == com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL) {
        alignmentService.shiftAlignment(beneficiary.getAlignment().getCampaign(), -1);
      }
    }

    boolean ejected = action.getType().isIncreasesAwareness() && newMeter >= EJECTION_THRESHOLD;
    boolean disqualified =
        action.getType().isCanCauseDq() && newMeter >= DQ_THRESHOLD && !isNoDqMatch(segment);

    String message = buildResultMessage(action, success, ejected, disqualified);

    return new RingsideActionResult(
        action, success, detectionIncrease, ejected, disqualified, message);
  }

  private boolean isNoDqMatch(Segment segment) {
    if (segment == null || segment.getSegmentRules() == null) {
      return false;
    }
    return segment.getSegmentRules().stream()
        .anyMatch(
            com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule::getNoDq);
  }

  private String buildResultMessage(
      RingsideAction action, boolean success, boolean ejected, boolean disqualified) {
    if (disqualified) {
      return "The referee saw the illegal " + action.getName() + "! DISQUALIFICATION!";
    }
    if (ejected) {
      return "The "
          + action.getName()
          + " was spotted! The referee is EJECTING the person from ringside!";
    }
    if (success) {
      return "The " + action.getName() + " was successful!";
    } else {
      return "The " + action.getName() + " failed to help as much as intended.";
    }
  }
}
