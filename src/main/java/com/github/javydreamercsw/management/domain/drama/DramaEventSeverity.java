package com.github.javydreamercsw.management.domain.drama;

/**
 * Severity levels for drama events in the ATW RPG system. Determines the impact magnitude and
 * consequences of the event.
 */
public enum DramaEventSeverity {
  /** Positive events that benefit wrestlers (fan gains, good publicity, etc.) */
  POSITIVE("Positive", "Beneficial events that improve wrestler standing", "✨"),

  /** Minor events with minimal impact (small news, minor incidents) */
  NEUTRAL("Neutral", "Minor events with limited consequences", "📰"),

  /** Negative events that harm wrestlers (fan losses, bad publicity, minor injuries) */
  NEGATIVE("Negative", "Harmful events that damage wrestler standing", "⚠️"),

  /**
   * Major events with significant consequences (serious injuries, major scandals, career changes)
   */
  MAJOR("Major", "Significant events with lasting consequences", "🚨");

  private final String displayName;
  private final String description;
  private final String emoji;

  DramaEventSeverity(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public String getEmoji() {
    return emoji;
  }

  /** Get the typical fan impact range for this severity level. */
  public FanImpactRange getFanImpactRange() {
    return switch (this) {
      case POSITIVE -> new FanImpactRange(1000L, 5000L);
      case NEUTRAL -> new FanImpactRange(-500L, 500L);
      case NEGATIVE -> new FanImpactRange(-3000L, -500L);
      case MAJOR -> new FanImpactRange(-10000L, 10000L); // Can be very positive or very negative
    };
  }

  /** Get the typical heat impact range for this severity level. */
  public HeatImpactRange getHeatImpactRange() {
    return switch (this) {
      case POSITIVE -> new HeatImpactRange(-2, 1); // Might reduce heat slightly
      case NEUTRAL -> new HeatImpactRange(-1, 2); // Minor heat changes
      case NEGATIVE -> new HeatImpactRange(1, 5); // Creates heat
      case MAJOR -> new HeatImpactRange(3, 10); // Major heat generation
    };
  }

  /** Check if this severity level can cause injuries. */
  public boolean canCauseInjury() {
    return this == NEGATIVE || this == MAJOR;
  }

  /** Check if this severity level can create new rivalries. */
  public boolean canCreateRivalry() {
    return this == NEGATIVE || this == MAJOR;
  }

  /** Check if this severity level can end rivalries. */
  public boolean canEndRivalry() {
    return this == POSITIVE || this == MAJOR;
  }

  /** Get display string with emoji. */
  public String getDisplayWithEmoji() {
    return emoji + " " + displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  /** Represents a range of fan impact values. */
  public record FanImpactRange(Long min, Long max) {
    public Long getRandomValue(java.util.Random random) {
      if (min.equals(max)) return min;
      return min + (long) (random.nextDouble() * (max - min));
    }
  }

  /** Represents a range of heat impact values. */
  public record HeatImpactRange(Integer min, Integer max) {
    public Integer getRandomValue(java.util.Random random) {
      if (min.equals(max)) return min;
      return min + random.nextInt(max - min + 1);
    }
  }
}
