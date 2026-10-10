/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ShowTemplateDTO {
  private String name;
  private String description;
  private String showTypeName;
  private String externalId;
  private String commentaryTeamName;
  private Integer expectedMatches;
  private Integer expectedPromos;
  private Integer durationDays;
  private String recurrenceType;
  private String dayOfWeek;
  private Integer dayOfMonth;
  private Integer weekOfMonth;
  private String month;
  private String genderConstraint;

  /**
   * Expansion codes that must ALL be enabled for shows of this template to be created (ATW-xtf0).
   * Mirrors {@code CampaignChapterDTO.requiredExpansions}. Empty list = no restriction.
   */
  private List<String> requiredExpansions = new ArrayList<>();

  /**
   * Segment assignment rows seeded with the template (ATW-cpuu) — e.g. an event-only type like the
   * Abu Dhabi Rumble as a PLE's main event. Each row targets a segment type and/or rule by NAME
   * (names resolve through SegmentTypeSync @Order(40)/SegmentRuleSync @Order(30), which run before
   * ShowTemplateSync @Order(60)); unknown names are skipped with a warning. Tournament-spec fields
   * are deliberately omitted — spec rows are booker-authored configuration, not seed data.
   */
  private List<AssignmentDTO> assignments = new ArrayList<>();

  @Data
  public static class AssignmentDTO {
    /** Segment type name (e.g. "One on One"), or null for a rule-only row. */
    private String segmentTypeName;

    /** Segment rule name (e.g. "Rumble Rules"), or null. */
    private String segmentRuleName;

    /**
     * Catalog tournament code ({@code Tournament.code}, e.g. {@code deadly_combat}) to attach —
     * resolves via TournamentSync @Order(55), which runs before this sync. Mutually exclusive with
     * the spec fields below.
     */
    private String tournamentCode;

    /**
     * Tournament spec fields (ATW-etws): when {@code specName} is set, the booking path creates ONE
     * persistent tournament from the spec on first use. {@code specFormatId} is required for a
     * valid spec; {@code specEntrantCount} falls back to the format max when null.
     */
    private String specName;

    private String specFormatId;
    private Integer specEntrantCount;

    /** Rule forced onto the bracket final (e.g. "Barbwire Exploding Deathmatch"), or null. */
    private String specFinalRuleName;

    /** Allowed-rules pool for the other rounds; resolved by name, unknown names skipped. */
    private List<String> allowedRuleNames = new ArrayList<>();

    /** AUTO_ATTACH (deterministic merge) or ENCOURAGED (AI preference). Default ENCOURAGED. */
    private String mode;
  }
}
