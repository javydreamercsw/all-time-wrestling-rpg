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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Faction entity. Used for JSON serialization/deserialization in sync and
 * export operations.
 */
@Data
@NoArgsConstructor
public class FactionDTO {
  private String name;
  private String description;
  private String leader; // Leader wrestler name
  private String leaderExternalId;
  private String managerName;
  private String managerExternalId;
  private String alignment;
  private List<String> members = new ArrayList<>(); // Member wrestler names
  private List<String> memberExternalIds = new ArrayList<>();
  private List<String> teams = new ArrayList<>(); // Team names
  private List<String> teamExternalIds = new ArrayList<>();

  private Boolean isActive;
  private String formedDate; // ISO date format (YYYY-MM-DD)
  private String disbandedDate; // ISO date format (YYYY-MM-DD)
  private String externalId; // External system ID (e.g., Notion page ID)
  private int affinity;
}
