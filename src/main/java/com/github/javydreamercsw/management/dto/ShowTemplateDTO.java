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

@Data
public class ShowTemplateDTO {
  private String name;
  private String description;
  private String showTypeName;
  private String notionUrl;
  private String externalId;
  private String commentaryTeamName;
  private Integer expectedMatches;
  private Integer expectedPromos;
  private Integer durationDays;
  private String recurrenceType;
  private String dayOfWeek;
  private Integer dayOfMonth;
  private Integer weekOfMonth;
  private String month;
}
