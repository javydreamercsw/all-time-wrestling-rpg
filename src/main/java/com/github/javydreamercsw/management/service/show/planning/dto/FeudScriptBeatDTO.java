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
  private String participantNames;
  private List<Long> participantIds;

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
    return sb.toString();
  }
}
