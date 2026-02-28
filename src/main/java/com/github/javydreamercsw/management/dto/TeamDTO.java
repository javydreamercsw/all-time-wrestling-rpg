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
package com.github.javydreamercsw.management.dto;

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TeamDTO {
  private Long id;
  private String name;
  private String description;
  private String externalId;
  private Long wrestler1Id;
  private String wrestler1Name;
  private String wrestler1ExternalId;
  private Long wrestler2Id;
  private String wrestler2Name;
  private String wrestler2ExternalId;
  private String formedDate;
  private String disbandedDate;
  private TeamStatus status;
  private Long factionId;
  private String factionName;
  private Long managerId;
  private String managerName;
  private String managerExternalId;
  private String themeSong;
  private String artist;
  private String teamFinisher;
  private boolean active;
  private boolean disbanded;

  public static TeamDTO fromEntity(Team team) {
    TeamDTO dto = new TeamDTO();
    dto.setId(team.getId());
    dto.setName(team.getName());
    dto.setDescription(team.getDescription());
    dto.setExternalId(team.getExternalId());
    if (team.getWrestler1() != null) {
      dto.setWrestler1Id(team.getWrestler1().getId());
      dto.setWrestler1Name(team.getWrestler1().getName());
      dto.setWrestler1ExternalId(team.getWrestler1().getExternalId());
    }
    if (team.getWrestler2() != null) {
      dto.setWrestler2Id(team.getWrestler2().getId());
      dto.setWrestler2Name(team.getWrestler2().getName());
      dto.setWrestler2ExternalId(team.getWrestler2().getExternalId());
    }
    if (team.getFormedDate() != null) {
      dto.setFormedDate(team.getFormedDate().toString());
    }
    if (team.getDisbandedDate() != null) {
      dto.setDisbandedDate(team.getDisbandedDate().toString());
    }
    dto.setStatus(team.getStatus());
    if (team.getFaction() != null) {
      dto.setFactionId(team.getFaction().getId());
      dto.setFactionName(team.getFaction().getName());
    }
    if (team.getManager() != null) {
      dto.setManagerId(team.getManager().getId());
      dto.setManagerName(team.getManager().getName());
      dto.setManagerExternalId(team.getManager().getExternalId());
    }
    dto.setThemeSong(team.getThemeSong());
    dto.setArtist(team.getArtist());
    dto.setTeamFinisher(team.getTeamFinisher());
    dto.setActive(team.isActive());
    dto.setDisbanded(team.isDisbanded());
    return dto;
  }

  public List<String> getMemberNames() {
    List<String> names = new ArrayList<>();
    if (wrestler1Name != null) {
      names.add(wrestler1Name);
    }
    if (wrestler2Name != null) {
      names.add(wrestler2Name);
    }
    return names;
  }

  public String getDisplayName() {
    String baseName = name != null ? name : String.join(" & ", getMemberNames());
    if (status == TeamStatus.DISBANDED) {
      return baseName + " (Disbanded)";
    }
    return baseName;
  }

  public boolean isInactive() {
    return status == TeamStatus.INACTIVE;
  }

  public String getStatusDisplayName() {
    if (status == null) {
      return "Unknown";
    }
    return status.getDisplayName();
  }
}
