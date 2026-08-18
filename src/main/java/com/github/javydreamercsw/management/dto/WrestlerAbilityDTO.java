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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrestlerAbilityDTO {
  private String name;
  private String description;

  /** AbilityType enum value as a string: ALWAYS_ON, USES_LIMITED, or CONDITIONAL. */
  private String type;

  /** AbilityCategory enum value as a string: SIGNATURE, PASSIVE, or ACTION. */
  private String category;

  @JsonProperty("maxUses")
  private Integer maxUses;

  /** AbilityTiming enum value as a string: OFFENSE, DEFENSE, PINNED, or BACKSTAGE. */
  private String timing;

  @JsonProperty("default")
  private boolean isDefault = true;

  private String unlockCondition;
  private String swapCondition;
  private String costScript;
  private String effectScript;
}
