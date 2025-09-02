package com.github.javydreamercsw.management.domain.wrestler;

/**
 * Represents the different tiers/ranks a wrestler can achieve based on their fan count. Each tier
 * determines title eligibility and special abilities in the ATW RPG system.
 */
public enum WrestlerTier {
  /** 0 – 24,999 fans - Just starting out. Local pops only. */
  ROOKIE("Rookie", 0L, 24999L, "Just starting out. Local pops only.", "🐍"),

  /** 25,000 – 39,999 fans - Making noise in the lower card. Known for chaos. */
  RISER("Riser", 25000L, 39999L, "Making noise in the lower card. Known for chaos.", "🔥"),

  /** 40,000 – 59,999 fans - Credible threat. Building rivalries and alliances. */
  CONTENDER(
      "Contender", 40000L, 59999L, "Credible threat. Building rivalries and alliances.", "💪"),

  /** 60,000 – 99,999 fans - A serious player in the midcard scene. */
  INTERTEMPORAL_TIER(
      "Intertemporal Tier", 60000L, 99999L, "A serious player in the midcard scene.", "🧠"),

  /** 100,000+ fans - Top of the mountain. Can headline PPVs. */
  MAIN_EVENTER("Main Eventer", 100000L, 149999L, "Top of the mountain. Can headline PPVs.", "👑"),

  /** 150,000+ fans - Transcends eras. May cut promos as a bonus once per show. */
  ICON(
      "Icon",
      150000L,
      Long.MAX_VALUE,
      "Transcends eras. May cut promos as a bonus once per show.",
      "🌟");

  private final String displayName;
  private final Long minFans;
  private final Long maxFans;
  private final String description;
  private final String emoji;

  WrestlerTier(String displayName, Long minFans, Long maxFans, String description, String emoji) {
    this.displayName = displayName;
    this.minFans = minFans;
    this.maxFans = maxFans;
    this.description = description;
    this.emoji = emoji;
  }

  /**
   * Determines the appropriate tier based on fan count.
   *
   * @param fans The wrestler's current fan count
   * @return The appropriate WrestlerTier
   */
  public static WrestlerTier fromFanCount(Long fans) {
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
   * Check if this tier allows challenging for a specific title tier.
   *
   * @param titleTier The title tier to check eligibility for
   * @return true if this wrestler tier can challenge for the title
   */
  public boolean canChallengeFor(TitleTier titleTier) {
    return this.minFans >= titleTier.getRequiredFans();
  }

  // Getters
  public String getDisplayName() {
    return displayName;
  }

  public Long getMinFans() {
    return minFans;
  }

  public Long getMaxFans() {
    return maxFans;
  }

  public String getDescription() {
    return description;
  }

  public String getEmoji() {
    return emoji;
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
      return String.format("%,d+", minFans);
    }
    return String.format("%,d - %,d", minFans, maxFans);
  }
}
