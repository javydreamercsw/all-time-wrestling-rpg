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

import com.github.javydreamercsw.management.domain.title.DefenseFrequencyType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningChampionshipDTO {
  private String championshipName;
  private String championName;
  private String contenderName;

  /** The defended Title's id — lets the local planner attach real title rows to the card. */
  private Long titleId;

  /**
   * Top ranked contenders per RankingService (tier filters, champion removal, failed-challenge
   * cooldowns, fans ordering). The local planner picks the first BOOKABLE name from this list when
   * no designated contender is set — rankings, not roster order.
   */
  private List<String> rankedContenderNames = new ArrayList<>();

  private DefenseFrequencyType defenseFrequencyType;
  private Long daysSinceLastDefense;

  /** True when the title is past its defense cadence threshold. */
  public boolean isOverdue() {
    if (defenseFrequencyType == null || daysSinceLastDefense == null) {
      return false;
    }
    return defenseFrequencyType.isOverdue(daysSinceLastDefense);
  }
}
