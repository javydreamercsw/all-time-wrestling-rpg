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
package com.github.javydreamercsw.base.domain.wrestler;

import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents the different tiers/ranks a wrestler can achieve based on their fan count. Each tier
 * determines title eligibility and special abilities in the ATW RPG system.
 */
@Getter
public enum WrestlerTier {
  /** 0 – 24,999 fans - Just starting out. Local pops only. */
  ROOKIE("Rookie", 0L, 24999L, "Just starting out. Local pops only.", "🐍", 0L, 0L, 0L),

  /** 25,000 – 39,999 fans - Making noise in the lower card. Known for chaos. */
  RISER(
      "Riser",
      25000L,
      39999L,
      "Making noise in the lower card. Known for chaos.",
      "🔥",
      25000L,
      25000L,
      15000L),

  /** 40,000 – 59,999 fans - Credible threat. Building rivalries and alliances. */
  CONTENDER(
      "Contender",
      40000L,
      59999L,
      "Credible threat. Building rivalries and alliances.",
      "💪",
      40000L,
      40000L,
      15000L),

  /** 60,000 – 99,999 fans - A serious player in the midcard scene. */
  MIDCARDER(
      "Midcarder",
      60000L,
      99999L,
      "A serious player in the midcard scene.",
      "🧠",
      60000L,
      60000L,
      15000L),

  /** 100,000+ fans - Top of the mountain. Can headline PPVs. */
  MAIN_EVENTER(
      "Main Eventer",
      100000L,
      149999L,
      "Top of the mountain. Can headline PPVs.",
      "👑",
      100000L,
      100000L,
      15000L),

  /** 150,000+ fans - Transcends eras. May cut promos as a bonus once per show. */
  ICON(
      "Icon",
      150000L,
      Long.MAX_VALUE,
      "Transcends eras. May cut promos as a bonus once per show.",
      "🌟",
      150000L,
      150000L,
      15000L);

  // Getters
  private final String displayName;
  private final Long minFans;
  private final Long maxFans;
  private final String description;
  private final String emoji;
  private final Long requiredFans;
  private final Long challengeCost;
  private final Long contenderEntryFee;

  WrestlerTier(
      final String displayName,
      final Long minFans,
      final Long maxFans,
      final String description,
      final String emoji,
      final Long requiredFans,
      final Long challengeCost,
      final Long contenderEntryFee) {
    this.displayName = displayName;
    this.minFans = minFans;
    this.maxFans = maxFans;
    this.description = description;
    this.emoji = emoji;
    this.requiredFans = requiredFans;
    this.challengeCost = challengeCost;
    this.contenderEntryFee = contenderEntryFee;
  }

  /**
   * Determines the appropriate tier based on fan count.
   *
   * @param fans The wrestler's current fan count
   * @return The appropriate WrestlerTier
   */
  public static WrestlerTier fromFanCount(final Long fans) {
    if (fans == null || fans < 0) {
      return ROOKIE;
    }

    for (WrestlerTier tier : values()) {
      if (fans >= tier.minFans && fans <= tier.maxFans) {
        return tier;
      }
    }

    // Fallback to highest tier if fans exceed all ranges
    return ICON;
  }

  /**
   * Finds a WrestlerTier enum from its display name.
   *
   * @param displayName The display name to find.
   * @return The matching WrestlerTier.
   * @throws IllegalArgumentException if no matching tier is found.
   */
  public static WrestlerTier fromDisplayName(@NonNull final String displayName) {
    return Arrays.stream(values())
        .filter(tier -> tier.getDisplayName().equalsIgnoreCase(displayName))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unknown tier display name: " + displayName));
  }

  /**
   * Check if a wrestler meets the fan requirement for this title tier.
   *
   * @param wrestlerFans The wrestler's current fan count
   * @return true if the wrestler can challenge for this title tier
   */
  public boolean isEligible(final Long wrestlerFans) {
    return wrestlerFans != null && wrestlerFans >= requiredFans;
  }

  /**
   * Get a formatted display string with emoji and name.
   *
   * @return Formatted string like "🔥 Riser"
   */
  public String getDisplayWithEmoji() {
    return emoji + " " + displayName;
  }

  /**
   * Get the fan range as a formatted string.
   *
   * @return Formatted string like "25,000 - 39,999"
   */
  public String getFanRangeDisplay() {
    if (maxFans == Long.MAX_VALUE) {
      return "%,d+".formatted(minFans);
    }
    return "%,d - %,d".formatted(minFans, maxFans);
  }

  /**
   * Get the next tier up.
   *
   * @return The next tier up, or null if this is the highest tier.
   */
  public WrestlerTier getNextTier() {
    if (this.ordinal() < WrestlerTier.values().length - 1) {
      return WrestlerTier.values()[this.ordinal() + 1];
    }
    return null;
  }
}
