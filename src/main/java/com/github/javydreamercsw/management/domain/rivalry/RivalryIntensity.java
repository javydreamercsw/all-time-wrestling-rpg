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
package com.github.javydreamercsw.management.domain.rivalry;

/** Represents the intensity levels of a rivalry based on heat in the ATW RPG system. */
public enum RivalryIntensity {
  /** 0-9 heat - Early stages, building tension */
  SIMMERING("Simmering", 0, 9, "Early stages, building tension", "😐"),

  /** 10-19 heat - Must wrestle at next show */
  HEATED("Heated", 10, 19, "Must wrestle at next show", "🔥"),

  /** 20-29 heat - Can attempt resolution with dice roll */
  INTENSE("Intense", 20, 29, "Can attempt resolution with dice roll", "💥"),

  /** 30+ heat - Requires rule segment */
  EXPLOSIVE("Explosive", 30, Integer.MAX_VALUE, "Requires rule segment", "🌋");

  private final String displayName;
  private final int minHeat;
  private final int maxHeat;
  private final String description;
  private final String emoji;

  RivalryIntensity(String displayName, int minHeat, int maxHeat, String description, String emoji) {
    this.displayName = displayName;
    this.minHeat = minHeat;
    this.maxHeat = maxHeat;
    this.description = description;
    this.emoji = emoji;
  }

  /** Get the intensity level for a given heat value. */
  public static RivalryIntensity fromHeat(int heat) {
    for (RivalryIntensity intensity : values()) {
      if (heat >= intensity.minHeat && heat <= intensity.maxHeat) {
        return intensity;
      }
    }
    return EXPLOSIVE; // Fallback for very high heat
  }

  /** Check if this intensity requires wrestlers to fight at next show. */
  public boolean requiresNextShowMatch() {
    return this.ordinal() >= HEATED.ordinal();
  }

  /** Check if this intensity allows resolution attempts. */
  public boolean allowsResolutionAttempt() {
    return this.ordinal() >= INTENSE.ordinal();
  }

  /** Check if this intensity requires a rule segment. */
  public boolean requiresStipulationMatch() {
    return this == EXPLOSIVE;
  }

  /** Get heat multiplier based on rivalry intensity. */
  public double getHeatMultiplier() {
    return switch (this) {
      case SIMMERING -> 1.0;
      case HEATED -> 1.2;
      case INTENSE -> 1.5;
      case EXPLOSIVE -> 2.0;
    };
  }

  // Getters
  public String getDisplayName() {
    return displayName;
  }

  public int getMinHeat() {
    return minHeat;
  }

  public int getMaxHeat() {
    return maxHeat;
  }

  public String getDescription() {
    return description;
  }

  public String getEmoji() {
    return emoji;
  }

  /** Get display string with emoji. */
  public String getDisplayWithEmoji() {
    return emoji + " " + displayName;
  }

  /** Get heat range display. */
  public String getHeatRangeDisplay() {
    if (maxHeat == Integer.MAX_VALUE) {
      return minHeat + "+ heat";
    }
    return minHeat + "-" + maxHeat + " heat";
  }
}
