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

import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A pre-authored narrative encounter used when a chapter runs in static or fallback mode. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticEncounterDTO {
  private String id;
  private String title;
  private String narrativeText;
  private List<StaticChoiceDTO> choices;

  /**
   * Expansion code required for this encounter to be shown. In sequential mode, encounters whose
   * expansion is not enabled are skipped. Null = available in base game.
   */
  private String requiredExpansion;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StaticChoiceDTO {
    private String id;
    private String text;
    private String label;
    private int vpReward;
    private int alignmentShift;
    private int momentumBonus;
    private boolean unlockPromo;
    private boolean unlockAttack;

    /** Arbitrary key/value pairs merged into featureData via FeatureDataService. */
    private Map<String, Object> featureFlags;

    private CampaignPhase nextPhase;
    private List<String> statusCardKeys;
    private String outcomeText;

    /**
     * When nextPhase is MATCH: the segment type name (e.g. "One on One", "Tag Team"). Null defaults
     * to One on One.
     */
    private String matchType;

    /**
     * When nextPhase is MATCH: segment rule names to apply (e.g. ["No DQ"]). Null or empty defaults
     * to Normal rules.
     */
    private List<String> segmentRules;

    /**
     * When nextPhase is MATCH: a specific opponent wrestler name. Null means the opponent is
     * selected randomly from the available roster, excluding the player's own wrestler and any
     * chapter-excluded wrestlers.
     */
    private String forcedOpponentName;

    /** For non-match choices: jump directly to this encounter card ID. */
    private String nextEncounterId;

    /** After a MATCH win: jump to this encounter card ID. */
    private String onWinNextEncounterId;

    /** After a MATCH loss: jump to this encounter card ID. */
    private String onLossNextEncounterId;

    /**
     * Expansion code required for this choice to be offered. Filtered out of the choices list if
     * the expansion is not enabled — lets authors write "if you have X go to card Y, else end here"
     * by pairing an expansion-gated choice with an ungated fallback. Null = always shown.
     */
    private String requiredExpansion;
  }
}
