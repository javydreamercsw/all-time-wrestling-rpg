package com.github.javydreamercsw.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for loading MatchType data from JSON files. Used for deserializing match
 * type configuration from external files.
 */
@Data
@NoArgsConstructor
public class MatchTypeDTO {

  /** Name of the match type */
  @JsonProperty("name")
  private String name;

  /** Description of the match type */
  @JsonProperty("description")
  private String description;

  /** Number of players/wrestlers for this match type. Use -1 for unlimited/variable amounts. */
  @JsonProperty("playerAmount")
  private int playerAmount;

  /**
   * Check if this match type supports unlimited players.
   *
   * @return true if playerAmount is -1 (unlimited)
   */
  public boolean isUnlimited() {
    return playerAmount == -1;
  }

  /**
   * Get the minimum number of players for this match type.
   *
   * @return minimum players (2 for unlimited types, actual value otherwise)
   */
  public int getMinPlayers() {
    return isUnlimited() ? 2 : playerAmount;
  }

  /**
   * Get the maximum number of players for this match type.
   *
   * @return maximum players (Integer.MAX_VALUE for unlimited, actual value otherwise)
   */
  public int getMaxPlayers() {
    return isUnlimited() ? Integer.MAX_VALUE : playerAmount;
  }
}
