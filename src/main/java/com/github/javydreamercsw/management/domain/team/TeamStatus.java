package com.github.javydreamercsw.management.domain.team;

/** Represents the status of a team in the ATW RPG system. */
public enum TeamStatus {
  /** Team is currently active and can compete in matches */
  ACTIVE("Active"),

  /** Team has been disbanded and is no longer active */
  DISBANDED("Disbanded"),

  /** Team is temporarily inactive (e.g., due to injuries, storyline) */
  INACTIVE("Inactive");

  private final String displayName;

  TeamStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
