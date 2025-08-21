package com.github.javydreamercsw.management.domain.faction;

/**
 * Represents the alignment of a faction in the ATW RPG system. Alignments affect storyline
 * development and fan reactions.
 */
public enum FactionAlignment {
  /** Face faction - heroic, fan favorites */
  FACE("Face", "Heroic faction beloved by fans", "😇"),

  /** Heel faction - villainous, antagonistic */
  HEEL("Heel", "Villainous faction that fans love to hate", "😈"),

  /** Tweener faction - morally ambiguous */
  TWEENER("Tweener", "Morally ambiguous faction with mixed reactions", "😐"),

  /** Neutral faction - no clear alignment */
  NEUTRAL("Neutral", "Faction with no clear moral stance", "⚖️");

  private final String displayName;
  private final String description;
  private final String emoji;

  FactionAlignment(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  /** Check if this alignment is heroic. */
  public boolean isFace() {
    return this == FACE;
  }

  /** Check if this alignment is villainous. */
  public boolean isHeel() {
    return this == HEEL;
  }

  /** Check if this alignment is morally ambiguous. */
  public boolean isTweener() {
    return this == TWEENER;
  }

  /** Check if this alignment is neutral. */
  public boolean isNeutral() {
    return this == NEUTRAL;
  }

  /** Get the opposite alignment (Face <-> Heel, others stay the same). */
  public FactionAlignment getOpposite() {
    return switch (this) {
      case FACE -> HEEL;
      case HEEL -> FACE;
      case TWEENER, NEUTRAL -> this;
    };
  }

  /** Check if two alignments are naturally opposed. */
  public boolean isOpposedTo(FactionAlignment other) {
    return (this == FACE && other == HEEL) || (this == HEEL && other == FACE);
  }

  /** Get heat multiplier for rivalries based on alignment clash. */
  public double getHeatMultiplier(FactionAlignment other) {
    if (isOpposedTo(other)) {
      return 1.5; // Face vs Heel generates more heat
    } else if (this == other && (this == FACE || this == HEEL)) {
      return 0.8; // Same alignment generates less heat
    }
    return 1.0; // Normal heat generation
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
