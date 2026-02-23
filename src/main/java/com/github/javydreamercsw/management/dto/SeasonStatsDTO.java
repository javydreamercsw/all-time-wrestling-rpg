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

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for Season statistics summary for a player. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonStatsDTO {
  private String seasonName;
  private Integer wins;
  private Integer losses;
  private Integer draws;
  private Long startingFans;
  private Long endingFans;
  @Builder.Default private List<String> accolades = new ArrayList<>();

  /**
   * Calculates the fan growth during the season.
   *
   * @return the difference between ending and starting fans.
   */
  public Long getFanGrowth() {
    if (startingFans == null || endingFans == null) {
      return 0L;
    }
    return endingFans - startingFans;
  }
}
