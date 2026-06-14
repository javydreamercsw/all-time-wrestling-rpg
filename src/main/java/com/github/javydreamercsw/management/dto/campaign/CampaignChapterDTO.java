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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignChapterDTO {
  private String id;
  private String title;
  private String shortDescription;
  private String introText;
  private String aiSystemPrompt;
  private Difficulty difficulty;
  private boolean tournament;
  private boolean tagTeam;
  private List<ChapterPointDTO> entryPoints;
  private List<ChapterPointDTO> exitPoints;
  private ChapterRules rules;
  private ChapterExclusions exclusions;

  @Builder.Default private CampaignChapterMode mode = CampaignChapterMode.AI_ONLY;

  @Builder.Default private List<StaticEncounterDTO> staticEncounters = new ArrayList<>();

  /**
   * Expansion codes that must ALL be enabled for this chapter to appear in {@code
   * findAvailableChapters}. Empty list = no restriction (available in base game).
   */
  @Builder.Default private List<String> requiredExpansions = new ArrayList<>();

  /**
   * When true, the successor-availability check in the chapter simulation validator emits no
   * warning if this chapter has no static successor. Use for intentional content boundaries (e.g.
   * end of a campaign expansion) where the AI storyline handoff is the designed outcome.
   */
  @Builder.Default private boolean expansionBoundary = false;

  /**
   * Title setup applied when this chapter is entered via {@code advanceToChapter}. Each entry maps
   * a title name (e.g. {@code "ATW World"}) to the wrestler name who should hold it at chapter
   * start. Applied according to {@code overrideExistingChampion}:
   *
   * <ul>
   *   <li>{@code false} (default) — only assign if the title is currently vacant.
   *   <li>{@code true} — always replace the current champion.
   * </ul>
   *
   * Supports {{CHAMP}} as the wrestler name to leave the current champion unchanged (no-op).
   */
  @Builder.Default private Map<String, String> initialChampions = new LinkedHashMap<>();

  /**
   * When {@code true}, {@code initialChampions} entries always replace the current champion even if
   * the title is already held. Default {@code false} = only set when title is vacant.
   */
  @Builder.Default private boolean overrideExistingChampion = false;

  /**
   * Wrestler names permitted to play this chapter. The tutorial wrestler picker filters to these
   * names when they are set. {@code CampaignChapterService.findAvailableChapters()} also filters
   * out this chapter if the active wrestler is not in the list. Empty list = no restriction.
   */
  @Builder.Default private List<String> allowedWrestlerNames = new ArrayList<>();

  /**
   * Opponent names (or placeholders) to exclude from every match in this chapter unless a choice
   * overrides it. Applied in addition to any per-choice {@code excludedOpponents}. Supports {@code
   * {{CHAMP}}} so the current champion is always excluded without hardcoding a name.
   */
  @Builder.Default private List<String> defaultExcludedOpponents = new ArrayList<>();

  /**
   * Default opponent gender filter applied to every match in this chapter unless a choice sets its
   * own {@code opponentGenderFilter}. Null = no restriction.
   */
  private com.github.javydreamercsw.base.domain.wrestler.Gender defaultOpponentGenderFilter;

  public boolean hasStaticEncounters() {
    return mode != CampaignChapterMode.AI_ONLY
        && staticEncounters != null
        && !staticEncounters.isEmpty();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChapterRules {
    private int qualifyingMatches;
    private int minWinsToQualify;
    private int totalFinalsMatches; // e.g., 2 for semi-finals and finals
    private int victoryPointsWin;
    private int victoryPointsLoss;
    private int titleWinVP;
    private int titleDefenseVP;
    private Integer finaleTriggerVP;
    private String finalMatchType;
    private List<String> finalMatchRules;
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
