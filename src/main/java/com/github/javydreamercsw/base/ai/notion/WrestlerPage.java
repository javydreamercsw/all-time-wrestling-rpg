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
public class WrestlerPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Wrestler-specific properties only (common properties inherited from base)
    private Property player;
    private Property titleBonus;
    private Property totalBonus;
    private Property titles;
    private Property heat1;
    private Property fans;
    private Property bumps;
    private Property faction;
    private Property heatBonus;
    private Property matches;
    private Property fanWeight;
    private Property heat;
    private Property gender;
    private Property alignment;
    private Property deckSize;
    private Property startingHealth;
    private Property lowHealth;
    private Property startingStamina;
    private Property lowStamina;
    private Property drive;
    private Property resilience;
    private Property charisma;
    private Property brawl;
    private Property heritageTag;
    private Property tier;
    private Property manager;
    private Property injuries;
    private Property teams;
  }
}
