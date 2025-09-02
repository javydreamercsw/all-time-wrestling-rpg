package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a team of wrestlers for match resolution purposes. A team can have one or more
 * members, making this suitable for singles matches (1v1), tag team matches (2v2), handicap matches
 * (1v2), and complex multi-team scenarios.
 */
@Getter
public class MatchTeam {
  private final List<Wrestler> members;
  private final String teamName;
  private int totalWeight;
  private double averageTierBonus;
  private int totalHealthPenalty;

  /** Create a team with a single wrestler (for singles matches). */
  public MatchTeam(@NonNull Wrestler wrestler) {
    this.members = List.of(wrestler);
    this.teamName = wrestler.getName();
  }

  /** Create a team with multiple wrestlers. */
  public MatchTeam(@NonNull List<Wrestler> wrestlers, String teamName) {
    if (wrestlers.isEmpty()) {
      throw new IllegalArgumentException("Team must have at least one member");
    }
    this.members = new ArrayList<>(wrestlers);
    this.teamName = teamName != null ? teamName : generateTeamName(wrestlers);
  }

  /** Create a team with multiple wrestlers (auto-generated team name). */
  public MatchTeam(@NonNull List<Wrestler> wrestlers) {
    this(wrestlers, null);
  }

  /** Generate a team name from wrestler names. */
  private String generateTeamName(List<Wrestler> wrestlers) {
    if (wrestlers.size() == 1) {
      return wrestlers.get(0).getName();
    } else if (wrestlers.size() == 2) {
      return wrestlers.get(0).getName() + " & " + wrestlers.get(1).getName();
    } else {
      return wrestlers.get(0).getName() + " & " + (wrestlers.size() - 1) + " others";
    }
  }

  /** Calculate team statistics for match resolution. */
  public void calculateTeamStats(NPCMatchResolutionService.TeamStatsCalculator calculator) {
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
  public boolean containsWrestler(Wrestler wrestler) {
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
