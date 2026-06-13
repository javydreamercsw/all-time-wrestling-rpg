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
  }
}
