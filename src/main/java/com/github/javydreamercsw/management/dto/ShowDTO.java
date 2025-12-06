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
 * Data Transfer Object for Show entity. Used for JSON serialization/deserialization in export
 * operations.
 */
@Data
public class ShowDTO {
  private String name;
  private String showType;
  private String description;
  private String showDate; // ISO date format (YYYY-MM-DD)
  private String seasonName; // Reference to season by name
  private String templateName; // Reference to show template by name
  private String externalId; // External system ID (e.g., Notion page ID)
}
