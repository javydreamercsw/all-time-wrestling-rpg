package com.github.javydreamercsw.management.domain.storyline;

/**
 * Represents the type of storyline branch in the ATW RPG system. Different types have different
 * triggers and effects.
 */
public enum StorylineBranchType {
  /** Triggered by segment outcomes - winner/loser specific */
  MATCH_OUTCOME("Match Outcome", "Branch triggered by specific segment results", "🏆"),

  /** Triggered by rivalry escalation */
  RIVALRY_ESCALATION(
      "Rivalry Escalation", "Branch triggered by rivalry heat reaching thresholds", "🔥"),

  /** Triggered by faction dynamics */
  FACTION_DYNAMICS(
      "Faction Dynamics", "Branch triggered by faction formation, betrayal, or dissolution", "👥"),

  /** Triggered by title changes */
  TITLE_CHANGE("Title Change", "Branch triggered by championship wins or losses", "🏅"),

  /** Triggered by injury events */
  INJURY_RESPONSE("Injury Response", "Branch triggered by wrestler injuries", "🏥"),

  /** Triggered by drama events */
  DRAMA_RESPONSE("Drama Response", "Branch triggered by backstage drama or incidents", "🎭"),

  /** Triggered by fan reactions */
  FAN_REACTION("Fan Reaction", "Branch triggered by significant fan count changes", "👏"),

  /** Triggered by seasonal events */
  SEASONAL_EVENT(
      "Seasonal Event", "Branch triggered by season progression or special events", "📅"),

  /** Triggered by external factors */
  EXTERNAL_TRIGGER("External Trigger", "Branch triggered by external storyline factors", "🌟"),

  /** Triggered by time-based conditions */
  TIME_BASED("Time-Based", "Branch triggered by time passing or specific dates", "⏰");

  private final String displayName;
  private final String description;
  private final String emoji;

  StorylineBranchType(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  /** Check if this branch type is segment-related. */
  public boolean isMatchRelated() {
    return this == MATCH_OUTCOME || this == TITLE_CHANGE;
  }

  /** Check if this branch type is rivalry-related. */
  public boolean isRivalryRelated() {
    return this == RIVALRY_ESCALATION || this == FACTION_DYNAMICS;
  }

  /** Check if this branch type is event-driven. */
  public boolean isEventDriven() {
    return this == DRAMA_RESPONSE || this == INJURY_RESPONSE || this == SEASONAL_EVENT;
  }

  /** Check if this branch type is time-sensitive. */
  public boolean isTimeSensitive() {
    return this == TIME_BASED || this == SEASONAL_EVENT;
  }

  /** Get the default priority for this branch type. */
  public int getDefaultPriority() {
    return switch (this) {
      case TITLE_CHANGE -> 10; // Highest priority
      case MATCH_OUTCOME, RIVALRY_ESCALATION -> 8;
      case FACTION_DYNAMICS, INJURY_RESPONSE -> 6;
      case DRAMA_RESPONSE, FAN_REACTION -> 4;
      case SEASONAL_EVENT, EXTERNAL_TRIGGER -> 3;
      case TIME_BASED -> 2; // Lowest priority
    };
  }

  /** Get the typical activation window in days. */
  public int getTypicalActivationWindowDays() {
    return switch (this) {
      case MATCH_OUTCOME, TITLE_CHANGE -> 1; // Immediate
      case RIVALRY_ESCALATION, INJURY_RESPONSE -> 3; // Within a few days
      case FACTION_DYNAMICS, DRAMA_RESPONSE -> 7; // Within a week
      case FAN_REACTION, EXTERNAL_TRIGGER -> 14; // Within two weeks
      case SEASONAL_EVENT -> 30; // Within a month
      case TIME_BASED -> 365; // Can be very long-term
    };
  }

  /** Check if this branch type can have multiple active instances. */
  public boolean allowsMultipleInstances() {
    return switch (this) {
      case MATCH_OUTCOME, RIVALRY_ESCALATION, DRAMA_RESPONSE, INJURY_RESPONSE, FAN_REACTION ->
          true; // Can have many
      case TITLE_CHANGE, FACTION_DYNAMICS, SEASONAL_EVENT, EXTERNAL_TRIGGER, TIME_BASED ->
          false; // Usually singular
    };
  }

  /** Get suggested condition types for this branch type. */
  public String[] getSuggestedConditionTypes() {
    return switch (this) {
      case MATCH_OUTCOME ->
          new String[] {"WRESTLER_WINS", "WRESTLER_LOSES", "MATCH_TYPE", "STIPULATION"};
      case RIVALRY_ESCALATION ->
          new String[] {"HEAT_THRESHOLD", "RIVALRY_ACTIVE", "WRESTLERS_INVOLVED"};
      case FACTION_DYNAMICS -> new String[] {"FACTION_MEMBER", "FACTION_ACTIVE", "MEMBER_COUNT"};
      case TITLE_CHANGE -> new String[] {"TITLE_HOLDER", "TITLE_VACANT", "CHALLENGER"};
      case INJURY_RESPONSE -> new String[] {"WRESTLER_INJURED", "INJURY_SEVERITY", "RECOVERY_TIME"};
      case DRAMA_RESPONSE -> new String[] {"DRAMA_TYPE", "DRAMA_SEVERITY", "WRESTLERS_INVOLVED"};
      case FAN_REACTION -> new String[] {"FAN_THRESHOLD", "FAN_CHANGE", "WRESTLER_TIER"};
      case SEASONAL_EVENT -> new String[] {"SEASON_ACTIVE", "SHOW_COUNT", "DATE_RANGE"};
      case EXTERNAL_TRIGGER -> new String[] {"CUSTOM_CONDITION", "EXTERNAL_EVENT"};
      case TIME_BASED -> new String[] {"DATE_REACHED", "DAYS_PASSED", "SHOW_COUNT"};
    };
  }

  /** Get suggested effect types for this branch type. */
  public String[] getSuggestedEffectTypes() {
    return switch (this) {
      case MATCH_OUTCOME ->
          new String[] {"CREATE_RIVALRY", "ADD_HEAT", "CHANGE_ALIGNMENT", "AWARD_FANS"};
      case RIVALRY_ESCALATION -> new String[] {"FORCE_MATCH", "ADD_STIPULATION", "ESCALATE_HEAT"};
      case FACTION_DYNAMICS ->
          new String[] {"CREATE_FACTION", "ADD_MEMBER", "REMOVE_MEMBER", "DISBAND_FACTION"};
      case TITLE_CHANGE -> new String[] {"TITLE_SHOT", "CONTENDER_TOURNAMENT", "TITLE_FEUD"};
      case INJURY_RESPONSE ->
          new String[] {"REPLACEMENT_WRESTLER", "SYMPATHY_FANS", "REVENGE_ANGLE"};
      case DRAMA_RESPONSE -> new String[] {"SUSPENSION", "FINE", "MANDATORY_MATCH", "HEAT_PENALTY"};
      case FAN_REACTION -> new String[] {"TIER_PROMOTION", "TITLE_OPPORTUNITY", "SPECIAL_MATCH"};
      case SEASONAL_EVENT -> new String[] {"TOURNAMENT", "SPECIAL_SHOW", "SEASON_FINALE"};
      case EXTERNAL_TRIGGER -> new String[] {"CUSTOM_EFFECT", "STORYLINE_TWIST"};
      case TIME_BASED ->
          new String[] {"AUTOMATIC_PROGRESSION", "DEADLINE_EFFECT", "ANNIVERSARY_EVENT"};
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
