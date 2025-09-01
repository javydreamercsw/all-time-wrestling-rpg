package com.github.javydreamercsw.management.domain.storyline;

/** Represents the status of a storyline branch in the ATW RPG system. */
public enum StorylineBranchStatus {
  /** Branch is waiting for conditions to be met */
  WAITING_FOR_CONDITIONS("Waiting for Conditions", "Branch conditions are not yet met", "⏳"),

  /** Branch conditions are met and ready to activate */
  READY_TO_ACTIVATE("Ready to Activate", "All conditions met, ready for activation", "✅"),

  /** Branch has been activated and effects are being executed */
  ACTIVATED("Activated", "Branch has been triggered and is executing effects", "🚀"),

  /** Branch has been completed successfully */
  COMPLETED("Completed", "Branch has finished executing all effects", "🏁"),

  /** Branch has been cancelled or failed */
  CANCELLED("Cancelled", "Branch was cancelled before completion", "❌"),

  /** Branch has expired due to time constraints */
  EXPIRED("Expired", "Branch expired before conditions could be met", "⏰");

  private final String displayName;
  private final String description;
  private final String emoji;

  StorylineBranchStatus(String displayName, String description, String emoji) {
    this.displayName = displayName;
    this.description = description;
    this.emoji = emoji;
  }

  /** Check if this status indicates the branch is still active. */
  public boolean isActive() {
    return this == WAITING_FOR_CONDITIONS || this == READY_TO_ACTIVATE || this == ACTIVATED;
  }

  /** Check if this status indicates the branch is finished. */
  public boolean isFinished() {
    return this == COMPLETED || this == CANCELLED || this == EXPIRED;
  }

  /** Check if this status indicates the branch can be activated. */
  public boolean canActivate() {
    return this == READY_TO_ACTIVATE;
  }

  /** Check if this status indicates the branch is currently executing. */
  public boolean isExecuting() {
    return this == ACTIVATED;
  }

  /** Check if this status indicates success. */
  public boolean isSuccessful() {
    return this == COMPLETED;
  }

  /** Check if this status indicates failure. */
  public boolean isFailed() {
    return this == CANCELLED || this == EXPIRED;
  }

  /** Get the next logical status transition. */
  public StorylineBranchStatus getNextStatus() {
    return switch (this) {
      case WAITING_FOR_CONDITIONS -> READY_TO_ACTIVATE;
      case READY_TO_ACTIVATE -> ACTIVATED;
      case ACTIVATED -> COMPLETED;
      case COMPLETED, CANCELLED, EXPIRED -> this; // Terminal states
    };
  }

  /** Get the priority level for processing (higher = more urgent). */
  public int getPriorityLevel() {
    return switch (this) {
      case READY_TO_ACTIVATE -> 10; // Highest priority - ready to go
      case ACTIVATED -> 8; // High priority - currently executing
      case WAITING_FOR_CONDITIONS -> 5; // Medium priority - waiting
      case COMPLETED -> 2; // Low priority - finished successfully
      case CANCELLED, EXPIRED -> 1; // Lowest priority - failed states
    };
  }

  /** Check if this status should be included in active storyline reports. */
  public boolean includeInActiveReports() {
    return isActive();
  }

  /** Check if this status should be included in completed storyline reports. */
  public boolean includeInCompletedReports() {
    return isFinished();
  }

  /** Get color code for UI display. */
  public String getColorCode() {
    return switch (this) {
      case WAITING_FOR_CONDITIONS -> "#FFA500"; // Orange
      case READY_TO_ACTIVATE -> "#00FF00"; // Green
      case ACTIVATED -> "#0080FF"; // Blue
      case COMPLETED -> "#008000"; // Dark Green
      case CANCELLED -> "#FF0000"; // Red
      case EXPIRED -> "#800080"; // Purple
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
