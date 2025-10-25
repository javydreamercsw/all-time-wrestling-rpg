package com.github.javydreamercsw.management.domain.injury;

import java.util.Random;

/**
 * Represents the severity levels of injuries in the ATW RPG system. Different severities have
 * different health penalties and healing costs.
 */
public enum InjurySeverity {
  /** Minor injury - small health penalty, easy to heal */
  MINOR("Minor", 1, 2, 5000L, "🟡"),

  /** Moderate injury - medium health penalty, standard healing cost */
  MODERATE("Moderate", 2, 3, 10000L, "🟠"),

  /** Severe injury - high health penalty, expensive to heal */
  SEVERE("Severe", 3, 5, 15000L, "🔴"),

  /** Critical injury - very high health penalty, very expensive to heal */
  CRITICAL("Critical", 4, 7, 25000L, "💀");

  private final String displayName;
  private final int minHealthPenalty;
  private final int maxHealthPenalty;
  private final Long baseHealingCost;
  private final String emoji;

  InjurySeverity(
      String displayName,
      int minHealthPenalty,
      int maxHealthPenalty,
      Long baseHealingCost,
      String emoji) {
    this.displayName = displayName;
    this.minHealthPenalty = minHealthPenalty;
    this.maxHealthPenalty = maxHealthPenalty;
    this.baseHealingCost = baseHealingCost;
    this.emoji = emoji;
  }

  /** Get a random health penalty within the severity range. */
  public int getRandomHealthPenalty(Random random) {
    if (minHealthPenalty == maxHealthPenalty) {
      return minHealthPenalty;
    }
    return minHealthPenalty + random.nextInt(maxHealthPenalty - minHealthPenalty + 1);
  }

  /** Get the healing success chance for this severity. Higher severity = lower success chance. */
  public int getHealingSuccessThreshold() {
    return switch (this) {
      case MINOR -> 3; // 4-6 on d6 = 50% success
      case MODERATE -> 4; // 4-6 on d6 = 50% success
      case SEVERE -> 5; // 5-6 on d6 = 33% success
      case CRITICAL -> 6; // 6 on d6 = 17% success
    };
  }

  /** Check if a healing roll is successful. */
  public boolean isHealingSuccessful(int rollResult) {
    return rollResult >= getHealingSuccessThreshold();
  }

  /** Get the healing success percentage. */
  public int getHealingSuccessPercentage() {
    int threshold = getHealingSuccessThreshold();
    return ((6 - threshold + 1) * 100) / 6;
  }

  // Getters
  public String getDisplayName() {
    return displayName;
  }

  public int getMinHealthPenalty() {
    return minHealthPenalty;
  }

  public int getMaxHealthPenalty() {
    return maxHealthPenalty;
  }

  public Long getBaseHealingCost() {
    return baseHealingCost;
  }

  public String getEmoji() {
    return emoji;
  }

  /** Get display string with emoji. */
  public String getDisplayWithEmoji() {
    return emoji + " " + displayName;
  }

  /** Get health penalty range display. */
  public String getHealthPenaltyRangeDisplay() {
    if (minHealthPenalty == maxHealthPenalty) {
      return String.valueOf(minHealthPenalty);
    }
    return minHealthPenalty + "-" + maxHealthPenalty;
  }

  /** Get healing info display. */
  public String getHealingInfoDisplay() {
    return String.format("%,d fans (%d%% success)", baseHealingCost, getHealingSuccessPercentage());
  }
}
