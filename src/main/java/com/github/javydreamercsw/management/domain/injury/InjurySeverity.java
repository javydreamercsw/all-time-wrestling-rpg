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
import lombok.Getter;

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

  @Getter private final String displayName;
  @Getter private final int minHealthPenalty;
  @Getter private final int maxHealthPenalty;
  @Getter private final int minStaminaPenalty;
  @Getter private final int maxStaminaPenalty;
  @Getter private final int minHandSizePenalty;
  @Getter private final int maxHandSizePenalty;
  @Getter private final Long baseHealingCost;
  @Getter private final String emoji;

  InjurySeverity(
      final String displayName,
      final int minHealthPenalty,
      final int maxHealthPenalty,
      final int minStaminaPenalty,
      final int maxStaminaPenalty,
      final int minHandSizePenalty,
      final int maxHandSizePenalty,
      final Long baseHealingCost,
      final String emoji) {
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
  public int getRandomHealthPenalty(final Random random) {
    if (minHealthPenalty == maxHealthPenalty) {
      return minHealthPenalty;
    }
    return minHealthPenalty + random.nextInt(maxHealthPenalty - minHealthPenalty + 1);
  }

  /** Get a random stamina penalty within the severity range. */
  public int getRandomStaminaPenalty(final Random random) {
    if (minStaminaPenalty == maxStaminaPenalty) {
      return minStaminaPenalty;
    }
    return minStaminaPenalty + random.nextInt(maxStaminaPenalty - minStaminaPenalty + 1);
  }

  /** Get a random hand size penalty within the severity range. */
  public int getRandomHandSizePenalty(final Random random) {
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
  public boolean isHealingSuccessful(final int rollResult) {
    return rollResult >= getHealingSuccessThreshold();
  }

  /** Get the healing success percentage. */
  public int getHealingSuccessPercentage() {
    int threshold = getHealingSuccessThreshold();
    return ((6 - threshold + 1) * 100) / 6;
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
    return "%,d fans (%d%% success)".formatted(baseHealingCost, getHealingSuccessPercentage());
  }
}
