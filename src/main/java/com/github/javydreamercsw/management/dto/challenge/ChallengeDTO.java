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

  /** Week number for weekly challenges; null for non-weekly content. */
  private Integer weekNumber;

  private String title;

  /** Narrative/hype text describing the scenario. */
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

  /** Wrestler names required by the challenge setup. */
  @Builder.Default private List<String> requiredWrestlerNames = new ArrayList<>();

  private Difficulty difficulty;

  /** One-line objective the player must achieve. */
  private String objective;

  /** Step-by-step setup instructions. */
  private String setupInstructions;

  /** Match type name, e.g. "Barbwire Exploding Death Match". Null for standard matches. */
  private String matchType;

  /** Rule restrictions that the player must not violate (as plain text). */
  @Builder.Default private List<String> conditions = new ArrayList<>();

  /** Special modifiers or house-rule overrides that apply during this challenge (as plain text). */
  @Builder.Default private List<String> modifiers = new ArrayList<>();

  /** Extra notes, e.g. page references to the rulebook. Null if none. */
  private String notes;

  /** When false this challenge is hidden from all views. */
  @Builder.Default private boolean active = true;
}
