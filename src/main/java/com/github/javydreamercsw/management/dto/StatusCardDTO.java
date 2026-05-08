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
package com.github.javydreamercsw.management.dto;

import lombok.Data;

@Data
public class StatusCardDTO {
  private String key;
  private String level1Name;
  private String level2Name;
  private String description;
  private boolean positive;
  private String level1Effect;
  private String level2Effect;
  private String flipUpCondition;
  private String flipDownCondition;
  private String discardCondition;
}
