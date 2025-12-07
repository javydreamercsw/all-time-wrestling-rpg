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
package com.github.javydreamercsw.management.service.sync.entity;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FactionDTO {
  private String name;
  private String description;
  private String externalId; // Notion page ID
  private String leader;
  private Boolean isActive;
  private String formedDate;
  private String disbandedDate;
  private List<String> members;
  private List<String> teams;
}
