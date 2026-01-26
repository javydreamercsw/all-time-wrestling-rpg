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
package com.github.javydreamercsw.management.domain.injury;

import java.util.Random;

/**
 * Represents the severity levels of injuries in the ATW RPG system. Different severities have
 * different health penalties and healing costs.
 */
public enum InjurySeverity {
  /** Minor injury - small health penalty, easy to heal */
  MINOR("Minor", 1, 2, 0, 0, 0, 0, 5000L, "🟡"),

  /** Moderate injury - medium health penalty, standard healing cost */
  MODERATE("Moderate", 2, 3, 0, 1, 0, 0, 10000L, "🟠"),

  /** Severe injury - high health penalty, expensive to heal */
  SEVERE("Severe", 3, 5, 1, 2, 0, 1, 15000L, "🔴"),

  /** Critical injury - very high health penalty, very expensive to heal */
  CRITICAL("Critical", 4, 7, 2, 3, 1, 1, 25000L, "💀");

  private final String displayName;
  private final int minHealthPenalty;
  private final int maxHealthPenalty;
  private final int minStaminaPenalty;
  private final int maxStaminaPenalty;
  private final int minHandSizePenalty;
  private final int maxHandSizePenalty;
  private final Long baseHealingCost;
  private final String emoji;

  InjurySeverity(
      String displayName,
      int minHealthPenalty,
      int maxHealthPenalty,
      int minStaminaPenalty,
      int maxStaminaPenalty,
      int minHandSizePenalty,
      int maxHandSizePenalty,
      Long baseHealingCost,
      String emoji) {
    this.displayName = displayName;
    this.minHealthPenalty = minHealthPenalty;
    this.maxHealthPenalty = maxHealthPenalty;
    this.minStaminaPenalty = minStaminaPenalty;
    this.maxStaminaPenalty = maxStaminaPenalty;
    this.minHandSizePenalty = minHandSizePenalty;
    this.maxHandSizePenalty = maxHandSizePenalty;
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

  /** Get a random stamina penalty within the severity range. */
  public int getRandomStaminaPenalty(Random random) {
    if (minStaminaPenalty == maxStaminaPenalty) {
      return minStaminaPenalty;
    }
    return minStaminaPenalty + random.nextInt(maxStaminaPenalty - minStaminaPenalty + 1);
  }

  /** Get a random hand size penalty within the severity range. */
  public int getRandomHandSizePenalty(Random random) {
    if (minHandSizePenalty == maxHandSizePenalty) {
      return minHandSizePenalty;
    }
    return minHandSizePenalty + random.nextInt(maxHandSizePenalty - minHandSizePenalty + 1);
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

  public int getMinStaminaPenalty() {
    return minStaminaPenalty;
  }

  public int getMaxStaminaPenalty() {
    return maxStaminaPenalty;
  }

  public int getMinHandSizePenalty() {
    return minHandSizePenalty;
  }

  public int getMaxHandSizePenalty() {
    return maxHandSizePenalty;
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
