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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a team of wrestlers for segment resolution purposes. A team can have one or more
 * members, making this suitable for singles matches (1v1), tag team matches (2v2), handicap matches
 * (1v2), and complex multi-team scenarios.
 */
@Getter
public class SegmentTeam {
  private final List<Wrestler> members;
  private final String teamName;
  private int totalWeight;
  private double averageTierBonus;
  private int totalHealthPenalty;

  /** Create a team with a single wrestler (for singles matches). */
  public SegmentTeam(@NonNull Wrestler wrestler) {
    this.members = List.of(wrestler);
    this.teamName = wrestler.getName();
  }

  /** Create a team with multiple wrestlers. */
  public SegmentTeam(@NonNull List<Wrestler> wrestlers, String teamName) {
    if (wrestlers.isEmpty()) {
      throw new IllegalArgumentException("Team must have at least one member");
    }
    this.members = new ArrayList<>(wrestlers);
    this.teamName = teamName != null ? teamName : generateTeamName();
  }

  /** Create a team with multiple wrestlers (auto-generated team name). */
  public SegmentTeam(@NonNull List<Wrestler> wrestlers) {
    this(wrestlers, null);
  }

  /** Generate a team name from wrestler names. */
  private String generateTeamName() {
    if (members.size() == 1) {
      return members.get(0).getName();
    } else if (members.size() == 2) {
      return members.get(0).getName() + " & " + members.get(1).getName();
    } else {
      return "the team of " + getMemberNames();
    }
  }

  /** Calculate team statistics for segment resolution. */
  public void calculateTeamStats(
      @NonNull NPCSegmentResolutionService.TeamStatsCalculator calculator) {
    this.totalWeight = calculator.calculateTeamWeight(this);
    this.averageTierBonus = calculator.calculateAverageTierBonus(this);
    this.totalHealthPenalty = calculator.calculateTeamHealthPenalty(this);
  }

  /** Check if this is a singles team (1 member). */
  public boolean isSingles() {
    return members.size() == 1;
  }

  /** Check if this is a tag team (2 members). */
  public boolean isTagTeam() {
    return members.size() == 2;
  }

  /** Check if this is a multi-person team (3+ members). */
  public boolean isMultiPerson() {
    return members.size() > 2;
  }

  /** Get the number of team members. */
  public int getSize() {
    return members.size();
  }

  /** Get a specific team member by index. */
  public Wrestler getMember(int index) {
    return members.get(index);
  }

  /** Get the primary wrestler (first member) for display purposes. */
  public Wrestler getPrimaryWrestler() {
    return members.get(0);
  }

  /** Check if the team contains a specific wrestler. */
  public boolean containsWrestler(@NonNull Wrestler wrestler) {
    return members.contains(wrestler);
  }

  /** Get all wrestler names as a formatted string. */
  public String getMemberNames() {
    if (members.size() == 1) {
      return members.get(0).getName();
    } else if (members.size() == 2) {
      return members.get(0).getName() + " & " + members.get(1).getName();
    } else {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < members.size(); i++) {
        if (i > 0) {
          sb.append(i == members.size() - 1 ? " & " : ", ");
        }
        sb.append(members.get(i).getName());
      }
      return sb.toString();
    }
  }

  @Override
  public String toString() {
    return teamName + " (" + members.size() + " member" + (members.size() == 1 ? "" : "s") + ")";
  }
}
