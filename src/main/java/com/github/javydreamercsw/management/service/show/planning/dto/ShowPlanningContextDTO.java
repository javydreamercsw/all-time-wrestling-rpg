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
package com.github.javydreamercsw.management.service.show.planning.dto;

import com.github.javydreamercsw.management.service.show.planning.ShowTemplate;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningContextDTO {
  private List<ShowPlanningSegmentDTO> recentSegments;
  private List<ShowPlanningRivalryDTO> currentRivalries;
  private List<ShowPlanningSegmentDTO> recentPromos;
  private ShowTemplate showTemplate;
  private List<ShowPlanningWrestlerHeatDTO> wrestlerHeats;
  private List<ShowPlanningChampionshipDTO> championships;
  private ShowPlanningPleDTO nextPle;
  private List<com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO> fullRoster;
  private List<com.github.javydreamercsw.management.dto.FactionDTO> factions;
  private Instant showDate;
}
