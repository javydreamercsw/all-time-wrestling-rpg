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

import lombok.Data;

/**
 * Data Transfer Object for Season entity. Used for JSON serialization/deserialization in sync and
 * export operations.
 */
@Data
public class SeasonDTO {
  private String name;
  private String description;
  private String startDate; // ISO date format (YYYY-MM-DD)
  private String endDate; // ISO date format (YYYY-MM-DD)
  private Boolean isActive;
  private Integer showsPerPpv;
  private String notionId; // External system ID (e.g., Notion page ID)
}
