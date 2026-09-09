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
package com.github.javydreamercsw.management.service.show.planning.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/** Compact representation of a FeudScriptBeat for use in show-planning AI prompts. */
@Data
public class FeudScriptBeatDTO {

  private Long beatId;
  private String scriptName;
  private String segmentType;
  private String segmentRule;
  private String winnerControl;
  private String plannedWinnerName;
  private boolean culmination;
  private String notes;

  /**
   * Feud participants only (rivalry pair or active feud members) — load-bearing for the planning
   * eviction pass, which must ignore externals.
   */
  private String participantNames;

  /** Feud participant wrestler IDs only, mirroring {@link #participantNames}. */
  private List<Long> participantIds;

  /**
   * Explicit team layout: team 1 = feud participants, team 2 = external opponent + extras. Null or
   * empty when the beat has no externals (derived one-wrestler-per-team shape is used instead).
   */
  private List<List<String>> teams;

  /** Explicit team layout by wrestler ID, mirroring {@link #teams}. */
  private List<List<Long>> teamIds;

  /** Human-readable external participant summary, e.g. "Randy Orton (Opponent)". */
  private String externalSummary;

  /** Rivalry Id of the arc (null for multi-wrestler feuds) — carried onto the segment. */
  private Long rivalryId;

  /** Participants as one team per wrestler, for locally-built beat segments. */
  public List<List<String>> getParticipantNameLists() {
    if (participantNames == null || participantNames.isBlank()) {
      return List.of();
    }
    return Arrays.stream(participantNames.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(List::of)
        .collect(Collectors.toList());
  }

  /** Wrestler IDs of the beat's participants, mirroring {@link #getParticipantNameLists()}. */
  public List<List<Long>> getParticipantIdLists() {
    if (participantIds == null) {
      return List.of();
    }
    return participantIds.stream().map(List::of).collect(Collectors.toList());
  }

  /** Active participant wrestler IDs, flattened. */
  public List<Long> getParticipantIds() {
    if (participantIds == null) {
      return List.of();
    }
    return participantIds;
  }

  /**
   * Team names for locally-built beat segments: the explicit layout when externals are present,
   * otherwise the derived one-wrestler-per-team shape (feud-only beat).
   */
  public List<List<String>> getTeamNameLists() {
    if (teams != null && !teams.isEmpty()) {
      return teams;
    }
    return getParticipantNameLists();
  }

  /** Team IDs for locally-built beat segments, mirroring {@link #getTeamNameLists()}. */
  public List<List<Long>> getTeamIdLists() {
    if (teamIds != null && !teamIds.isEmpty()) {
      return teamIds;
    }
    return getParticipantIdLists();
  }

  /** Returns a one-line AI instruction for the prompt builder. */
  public String toAiInstruction() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(segmentType);
    if (segmentRule != null && !segmentRule.isBlank()) {
      sb.append(" - ").append(segmentRule);
    }
    sb.append("]");
    if (participantNames != null && !participantNames.isBlank()) {
      sb.append(" ").append(participantNames);
    }
    if (culmination) {
      sb.append(" (Culmination/Blowoff)");
    }
    sb.append(" — Winner: ");
    switch (winnerControl) {
      case "BOOKER_PICKS" ->
          sb.append(
              plannedWinnerName != null && !plannedWinnerName.isBlank()
                  ? plannedWinnerName
                  : "Booker's choice");
      case "AI_PICKS" -> sb.append("AI choice");
      default -> sb.append("System roll");
    }
    if (notes != null && !notes.isBlank()) {
      sb.append(" — \"").append(notes).append("\"");
    }
    if (externalSummary != null && !externalSummary.isBlank()) {
      sb.append(" — External: ").append(externalSummary);
    }
    return sb.toString();
  }
}
