package com.github.javydreamercsw.management.domain.feud;

/**
 * Represents the role of a wrestler in a multi-wrestler feud. Roles help define storyline dynamics
 * and match booking priorities.
 */
public enum FeudRole {
  /** Main antagonist - primary villain in the feud */
  ANTAGONIST("Antagonist", "Primary villain driving the conflict", "😈"),

  /** Protagonist - heroic figure opposing the antagonist */
  PROTAGONIST("Protagonist", "Heroic figure fighting against the antagonist", "😇"),

  /** Secondary antagonist - supporting villain */
  SECONDARY_ANTAGONIST(
      "Secondary Antagonist", "Supporting villain allied with main antagonist", "👿"),

  /** Secondary protagonist - supporting hero */
  SECONDARY_PROTAGONIST(
      "Secondary Protagonist", "Supporting hero allied with main protagonist", "🦸"),

  /** Neutral participant - no clear allegiance */
  NEUTRAL("Neutral", "Participant with no clear allegiance in the feud", "😐"),

  /** Wild card - unpredictable participant */
  WILD_CARD("Wild Card", "Unpredictable participant who may switch sides", "🃏"),

  /** Authority figure - referee, manager, or official involved */
  AUTHORITY(
      "Authority Figure", "Official, manager, or authority figure involved in the feud", "👔");

  private final String displayName;
  private final String description;
  private final String emoji;

  FeudRole(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  /** Check if this role is antagonistic. */
  public boolean isAntagonistic() {
    return this == ANTAGONIST || this == SECONDARY_ANTAGONIST;
  }

  /** Check if this role is protagonistic. */
  public boolean isProtagonistic() {
    return this == PROTAGONIST || this == SECONDARY_PROTAGONIST;
  }

  /** Check if this role is a main character (primary antagonist or protagonist). */
  public boolean isMainCharacter() {
    return this == ANTAGONIST || this == PROTAGONIST;
  }

  /** Check if this role is a supporting character. */
  public boolean isSupportingCharacter() {
    return this == SECONDARY_ANTAGONIST || this == SECONDARY_PROTAGONIST;
  }

  /** Check if this role is neutral or unpredictable. */
  public boolean isUnpredictable() {
    return this == NEUTRAL || this == WILD_CARD;
  }

  /** Get the opposing role (Antagonist <-> Protagonist, etc.). */
  public FeudRole getOpposingRole() {
    return switch (this) {
      case ANTAGONIST -> PROTAGONIST;
      case PROTAGONIST -> ANTAGONIST;
      case SECONDARY_ANTAGONIST -> SECONDARY_PROTAGONIST;
      case SECONDARY_PROTAGONIST -> SECONDARY_ANTAGONIST;
      case NEUTRAL, WILD_CARD, AUTHORITY -> this; // No clear opposite
    };
  }

  /** Check if two roles are naturally opposed. */
  public boolean isOpposedTo(FeudRole other) {
    return (isAntagonistic() && other.isProtagonistic())
        || (isProtagonistic() && other.isAntagonistic());
  }

  /** Get heat multiplier for interactions based on role dynamics. */
  public double getHeatMultiplier(FeudRole other) {
    if (isOpposedTo(other)) {
      if (isMainCharacter() && other.isMainCharacter()) {
        return 2.0; // Main character conflicts generate the most heat
      } else if (isMainCharacter() || other.isMainCharacter()) {
        return 1.5; // Main vs supporting character
      } else {
        return 1.2; // Supporting character conflicts
      }
    } else if (this == other && (isAntagonistic() || isProtagonistic())) {
      return 0.7; // Same-side conflicts generate less heat
    } else if (this == WILD_CARD || other == WILD_CARD) {
      return 1.3; // Wild cards create unpredictable heat
    }
    return 1.0; // Normal heat generation
  }

  /** Get booking priority for matches (higher = more important). */
  public int getBookingPriority() {
    return switch (this) {
      case ANTAGONIST, PROTAGONIST -> 10; // Highest priority
      case SECONDARY_ANTAGONIST, SECONDARY_PROTAGONIST -> 7;
      case WILD_CARD -> 5;
      case NEUTRAL -> 3;
      case AUTHORITY -> 1; // Lowest priority for matches
    };
  }

  // Getters
  public String getDisplayName() {
    return displayName;
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

  @Override
  public String toString() {
    return displayName;
  }
}
