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
package com.github.javydreamercsw.management.dto.challenge;

import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDTO {

  private String id;

  private Integer weekNumber;

  private String title;

  private String flavorText;

  /** Human-readable product label, e.g. "ATW Extreme Edition". Display only. */
  private String productLine;

  /**
   * The expansion this challenge was published under. Use {@code "CUSTOM"} for user-created
   * challenges; any other code indicates official ATW content. Mirrors the convention used by
   * {@code SegmentRule} and {@code Npc}.
   */
  @Builder.Default private String expansionCode = "CUSTOM";

  /**
   * Expansion codes that must ALL be enabled for this challenge to appear in the filtered view.
   * Empty list = available with Base Game only.
   */
  @Builder.Default private List<String> requiredExpansions = new ArrayList<>();

  @Builder.Default private List<String> requiredWrestlerNames = new ArrayList<>();

  private Difficulty difficulty;

  private String objective;

  private String setupInstructions;

  /**
   * Name of the ATW segment rule that governs this challenge (e.g. {@code "Barbwire Exploding
   * Deathmatch"}, {@code "Normal"}). Must match the {@code SegmentRule.name} exactly so the detail
   * dialog can resolve an in-app match-info link. Null = standard match with no special rule page.
   */
  private String matchType;

  @Builder.Default private List<String> conditions = new ArrayList<>();

  @Builder.Default private List<String> modifiers = new ArrayList<>();

  private String notes;

  /** Optional image URL displayed in the challenge detail dialog. */
  private String imageUrl;

  @Builder.Default private boolean active = true;
}
