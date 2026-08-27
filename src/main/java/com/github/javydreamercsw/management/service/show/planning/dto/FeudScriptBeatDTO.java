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
