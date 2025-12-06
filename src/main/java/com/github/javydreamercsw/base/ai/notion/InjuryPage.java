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
package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an injury page from the Notion Injuries database. Contains injury-specific properties
 * for wrestler injury tracking.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InjuryPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Injury-specific properties based on REAL Notion structure

    /** Name of the injury type (e.g., "Head injury", "Back injury") */
    private Property InjuryName;

    /** Health effect penalty (numeric, e.g., -3, -1, -2) */
    private Property HealthEffect;

    /** Stamina effect penalty (numeric, e.g., 0, -3, -2) */
    private Property StaminaEffect;

    /** Card effect penalty (numeric, e.g., -2, 0, -1) */
    private Property CardEffect;

    /** Special game effects description (text, e.g., "No reversal ability") */
    private Property SpecialEffects;
  }
}
