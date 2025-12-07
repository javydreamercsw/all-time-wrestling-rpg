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

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import java.time.Instant;
import lombok.Data;

/** Data Transfer Object for Team entities. Used for UI forms and API responses. */
@Data
public class TeamDTO {
  private Long id;
  private String name;
  private String description;
  private Long wrestler1Id;
  private String wrestler1Name;
  private Long wrestler2Id;
  private String wrestler2Name;
  private Long factionId;
  private String factionName;
  private TeamStatus status;
  private Instant formedDate;
  private Instant disbandedDate;
  private String externalId;

  // Computed fields for display
  private String memberNames;
  private String displayName;
  private boolean active;

  /** Create DTO from Team entity. */
  public static TeamDTO fromEntity(Team team) {
    TeamDTO dto = new TeamDTO();
    dto.setId(team.getId());
    dto.setName(team.getName());
    dto.setDescription(team.getDescription());
    dto.setStatus(team.getStatus());
    dto.setFormedDate(team.getFormedDate());
    dto.setDisbandedDate(team.getDisbandedDate());
    dto.setExternalId(team.getExternalId());

    // Wrestler information
    if (team.getWrestler1() != null) {
      dto.setWrestler1Id(team.getWrestler1().getId());
      dto.setWrestler1Name(team.getWrestler1().getName());
    }
    if (team.getWrestler2() != null) {
      dto.setWrestler2Id(team.getWrestler2().getId());
      dto.setWrestler2Name(team.getWrestler2().getName());
    }

    // Faction information
    if (team.getFaction() != null) {
      dto.setFactionId(team.getFaction().getId());
      dto.setFactionName(team.getFaction().getName());
    }

    // Computed fields
    dto.setMemberNames(team.getMemberNames());
    dto.setDisplayName(team.getDisplayName());
    dto.setActive(team.isActive());

    return dto;
  }

  /** Get status display name. */
  public String getStatusDisplayName() {
    return status != null ? status.getDisplayName() : "Unknown";
  }

  /** Check if team is disbanded. */
  public boolean isDisbanded() {
    return status == TeamStatus.DISBANDED;
  }

  /** Check if team is inactive. */
  public boolean isInactive() {
    return status == TeamStatus.INACTIVE;
  }
}
