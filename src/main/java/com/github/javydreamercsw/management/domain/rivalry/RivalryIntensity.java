package com.github.javydreamercsw.management.domain.rivalry;

/** Represents the intensity levels of a rivalry based on heat in the ATW RPG system. */
public enum RivalryIntensity {
  /** 0-9 heat - Early stages, building tension */
  SIMMERING("Simmering", 0, 9, "Early stages, building tension", "😐"),

  /** 10-19 heat - Must wrestle at next show */
  HEATED("Heated", 10, 19, "Must wrestle at next show", "🔥"),

  /** 20-29 heat - Can attempt resolution with dice roll */
  INTENSE("Intense", 20, 29, "Can attempt resolution with dice roll", "💥"),

  /** 30+ heat - Requires stipulation match */
  EXPLOSIVE("Explosive", 30, Integer.MAX_VALUE, "Requires stipulation match", "🌋");

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

  /** Check if this intensity requires a stipulation match. */
  public boolean requiresStipulationMatch() {
    return this == EXPLOSIVE;
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
