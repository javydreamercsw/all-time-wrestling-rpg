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

import com.github.javydreamercsw.management.domain.title.Title;
import java.time.LocalDate;
import java.util.ArrayList;
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
   * Explicit team layout: team 1 = feud participants, team 2 = external opponent + extras (or a
   * fully custom layout when {@link #customTeams} is set). Null or empty when the beat has no
   * externals and no custom layout (derived one-wrestler-per-team shape is used instead).
   */
  private List<List<String>> teams;

  /** Explicit team layout by wrestler ID, mirroring {@link #teams}. */
  private List<List<Long>> teamIds;

  /** Human-readable external participant summary, e.g. "Randy Orton (Opponent)". */
  private String externalSummary;

  /** Rivalry Id of the arc (null for multi-wrestler feuds) — carried onto the segment. */
  private Long rivalryId;

  /** True when the beat stores an explicit custom team layout (FEUD_MEMBER rows). */
  private boolean customTeams;

  /** True when the beat is contested for the titles in {@link #titles}. */
  private boolean titleSegment;

  /** Titles at stake when this beat is a title match. */
  private List<Title> titles = new ArrayList<>();

  /** #1 contender designation outcome: winner of the beat's segment becomes #1 contender. */
  private Long contenderTitleId;

  private String contenderTitleName;

  /** Optional target show the beat is reserved for. */
  private Long targetShowId;

  private String targetShowName;

  private LocalDate targetShowDate;

  /**
   * Participants as one team per wrestler. Splits the "A vs B" display string on its actual
   * separator (a plain comma split would merge the whole string into one bogus participant).
   */
  public List<List<String>> getParticipantNameLists() {
    if (participantNames == null || participantNames.isBlank()) {
      return List.of();
    }
    return Arrays.stream(participantNames.split(" vs "))
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
   * Team names for locally-built beat segments: the explicit layout when the beat has externals or
   * a custom team layout, otherwise the derived one-wrestler-per-team shape (feud-only beat).
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
    if (titleSegment && titles != null && !titles.isEmpty()) {
      sb.append(
          " — Title"
              + (titles.size() > 1 ? "s" : "")
              + " on the line: "
              + titles.stream().map(Title::getName).collect(Collectors.joining(", ")));
    }
    if (contenderTitleName != null && !contenderTitleName.isBlank()) {
      sb.append(" — Winner becomes #1 contender for ").append(contenderTitleName);
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
    if (targetShowName != null && !targetShowName.isBlank()) {
      sb.append(" — Target show: ").append(targetShowName);
    }
    return sb.toString();
  }
}
