package com.github.javydreamercsw.management.domain.wrestler;

/**
 * Represents the different championship tiers in the ATW RPG system. Each title has specific fan
 * requirements and challenge costs.
 */
public enum TitleTier {
  /** Extreme Title - Entry level championship for rising stars */
  EXTREME(
      "Extreme Title", 25000L, 15000L, "First step up — great for newer or struggling wrestlers."),

  /** Tag Team Titles - Midcard level requiring team coordination */
  TAG_TEAM(
      "Tag Team Titles", 40000L, 15000L, "Midcard level, needs some credibility. (Per wrestler)"),

  /** Intertemporal Title - Upper midcard championship */
  INTERTEMPORAL("Intertemporal Title", 60000L, 15000L, "Recognizes a solid run or overness."),

  /** World Title - The top championship in the promotion */
  WORLD(
      "World Title",
      100000L,
      15000L,
      "Main event — top of the card, only the most over should get a shot.");

  private final String titleName;
  private final Long requiredFans;
  private final Long challengeCost;
  private final String description;

  TitleTier(String titleName, Long requiredFans, Long challengeCost, String description) {
    this.titleName = titleName;
    this.requiredFans = requiredFans;
    this.challengeCost = challengeCost;
    this.description = description;
  }

  /**
   * Get the #1 Contender match entry fee for this title. All wrestlers must pay this to participate
   * in #1 contender matches.
   *
   * @return The entry fee in fans
   */
  public Long getContenderEntryFee() {
    return challengeCost;
  }

  /**
   * Check if a wrestler meets the fan requirement for this title.
   *
   * @param wrestlerFans The wrestler's current fan count
   * @return true if the wrestler can challenge for this title
   */
  public boolean isEligible(Long wrestlerFans) {
    return wrestlerFans != null && wrestlerFans >= requiredFans;
  }

  /**
   * Get all titles that a wrestler is eligible for based on their fan count.
   *
   * @param wrestlerFans The wrestler's current fan count
   * @return Array of eligible title tiers
   */
  public static TitleTier[] getEligibleTitles(Long wrestlerFans) {
    if (wrestlerFans == null || wrestlerFans < 0) {
      return new TitleTier[0];
    }

    return java.util.Arrays.stream(values())
        .filter(title -> title.isEligible(wrestlerFans))
        .toArray(TitleTier[]::new);
  }

  /**
   * Get the highest title tier a wrestler is eligible for.
   *
   * @param wrestlerFans The wrestler's current fan count
   * @return The highest eligible title tier, or null if none
   */
  public static TitleTier getHighestEligibleTitle(Long wrestlerFans) {
    TitleTier[] eligible = getEligibleTitles(wrestlerFans);
    if (eligible.length == 0) {
      return null;
    }

    // Return the last (highest) title since enum is ordered by prestige
    return eligible[eligible.length - 1];
  }

  // Getters
  public String getTitleName() {
    return titleName;
  }

  public Long getRequiredFans() {
    return requiredFans;
  }

  public Long getChallengeCost() {
    return challengeCost;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Get a formatted display string with requirements.
   *
   * @return Formatted string like "World Title (100,000 fans required)"
   */
  public String getDisplayWithRequirement() {
    return String.format("%s (%,d fans required)", titleName, requiredFans);
  }

  /**
   * Get the ordinal position for prestige ranking (0 = lowest, 3 = highest).
   *
   * @return The prestige ranking
   */
  public int getPrestigeRank() {
    return this.ordinal();
  }
}
