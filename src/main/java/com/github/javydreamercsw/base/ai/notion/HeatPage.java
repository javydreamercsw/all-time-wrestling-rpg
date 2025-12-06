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

@Data
@EqualsAndHashCode(callSuper = false)
public class HeatPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Heat-specific properties only (common properties inherited from base)
    private Property Wrestler1; // First wrestler in the feud
    private Property Wrestler2; // Second wrestler in the feud
    private Property HeatLevel; // Intensity of the feud (1-10, Hot/Cold, etc.)
    private Property HeatType; // Type of heat (Face vs Heel, Personal, Professional, etc.)
    private Property Reason; // Storyline reason for the heat
    private Property StartDate; // When the feud began
    private Property EndDate; // When the feud ended (if resolved)
    private Property Status; // Current status (Active, Resolved, On Hold, etc.)
    private Property Winner; // Who came out on top (if resolved)
    private Property RelatedMatches; // Matches that were part of this storyline
  }
}
