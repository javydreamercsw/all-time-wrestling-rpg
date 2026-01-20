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
package com.github.javydreamercsw.management.dto.campaign;

import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignChapterDTO {
  private int chapterNumber;
  private String title;
  private String shortDescription;
  private String introText;
  private String aiSystemPrompt;
  private ChapterRules rules;
  private ChapterExclusions exclusions;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChapterRules {
    private Difficulty defaultDifficulty;
    private int qualifyingMatches;
    private int minWinsToQualify;
    private int victoryPointsWin;
    private int victoryPointsLoss;
    private int titleWinVP;
    private int titleDefenseVP;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChapterExclusions {
    private boolean excludeChampions;
    private List<Long> npcIds;
  }
}
