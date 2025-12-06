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
package com.github.javydreamercsw.management.dto;

import java.util.List;
import lombok.Data;

/**
 * Data Transfer Object for Faction entity. Used for JSON serialization/deserialization in sync and
 * export operations.
 */
@Data
public class FactionDTO {
  private String name;
  private String description;
  private String leader; // Leader wrestler name (will be resolved to Wrestler entity)
  private List<String>
      members; // List of member wrestler names (will be resolved to Wrestler entities)
  private List<String> teams; // List of team names associated with this faction

  private Boolean isActive;
  private String formedDate; // ISO date format (YYYY-MM-DD)
  private String disbandedDate; // ISO date format (YYYY-MM-DD)
  private String externalId; // External system ID (e.g., Notion page ID)
}
