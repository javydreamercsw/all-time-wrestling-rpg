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
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.title.BriefcaseCashInMode;
import com.github.javydreamercsw.management.domain.title.Title;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.Setter;

@Data
public class ProposedSegment {
  private String type; // "segment" or "promo"
  private String narration;
  private String summary;
  private String notes;
  private List<List<String>> teams;
  private List<List<Long>> teamIds;
  private Long rivalryId;
  private List<String> winners;
  private Boolean isTitleSegment = false;
  private Boolean isContenderMatch = false;

  /** Title id whose #1 contender is decided by this segment (resolved at approval time). */
  private Long contenderTitleId;

  @Setter private Set<Title> titles = new HashSet<>();
  private List<String> rules;
  private String refereeName;

  /** Source stamps for the planning grid's badge; approval strips both as consent rows. */
  public static final String SOURCE_TOURNAMENT = "Tournament";

  public static final String SOURCE_BRIEFCASE = "Briefcase";

  /**
   * Where this row came from, for the planning grid's Source badge: "Scripted beat" (deterministic
   * arc slot), {@link #SOURCE_TOURNAMENT} (show-attached tournament slot — paced round or payoff),
   * {@link #SOURCE_BRIEFCASE} (NPC briefcase cash-in proposal, ATW-2b9u), or null (the AI's own
   * proposal). Badge display only — approval treats every row the same.
   */
  private String source;

  /** Identity of the briefcase proposal this row consents to (ATW-2b9u); null for other rows. */
  private Long briefcaseOpportunityId;

  /** Proposed cash-in mode (ATW-2b9u): PLANNED / AMBUSH / RUN_IN. */
  private BriefcaseCashInMode briefcaseMode;
}
