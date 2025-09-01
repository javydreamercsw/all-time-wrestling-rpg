package com.github.javydreamercsw.management.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Transfer Object for Match information from Notion. Used for synchronizing match data between
 * Notion and the local database.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MatchDTO {

  /** External ID from Notion (page ID) */
  private String externalId;

  /** Name/title of the match */
  private String name;

  /** Description of the match */
  private String description;

  /** List of participant wrestler names */
  private List<String> participants;

  /** Name of the winning wrestler */
  private String winner;

  /** Name of the match type */
  private String matchType;

  /** Name of the show where the match took place */
  private String show;

  /** Duration of the match in minutes */
  private Integer duration;

  /** Rating of the match (1-5) */
  private Integer rating;

  /** Match stipulation/rules */
  private String stipulation;

  /** Match narration text */
  private String narration;

  /** Date/time when the match occurred */
  private Instant matchDate;

  /** Whether this was a title match */
  private Boolean isTitleMatch;

  /** Whether this match was NPC-generated */
  private Boolean isNpcGenerated;

  /** Creation date from Notion */
  private Instant createdTime;

  /** Last edited date from Notion */
  private Instant lastEditedTime;

  /** Created by user from Notion */
  private String createdBy;

  /** Last edited by user from Notion */
  private String lastEditedBy;

  /**
   * Validates that the DTO has the minimum required fields for creating a match.
   *
   * @return true if the DTO is valid for match creation
   */
  public boolean isValid() {
    return name != null
        && !name.trim().isEmpty()
        && participants != null
        && !participants.isEmpty()
        && matchType != null
        && !matchType.trim().isEmpty()
        && show != null
        && !show.trim().isEmpty();
  }

  /**
   * Gets the number of participants in the match.
   *
   * @return Number of participants, or 0 if participants list is null
   */
  public int getParticipantCount() {
    return participants != null ? participants.size() : 0;
  }

  /**
   * Checks if this is a singles match (2 participants).
   *
   * @return true if this is a singles match
   */
  public boolean isSinglesMatch() {
    return getParticipantCount() == 2;
  }

  /**
   * Checks if this is a multi-person match (3+ participants).
   *
   * @return true if this is a multi-person match
   */
  public boolean isMultiPersonMatch() {
    return getParticipantCount() > 2;
  }

  /**
   * Gets a formatted string of all participants.
   *
   * @return Comma-separated list of participant names
   */
  public String getParticipantsAsString() {
    if (participants == null || participants.isEmpty()) {
      return "No participants";
    }
    return String.join(", ", participants);
  }

  /**
   * Gets a summary string for logging purposes.
   *
   * @return Summary string with key match information
   */
  public String getSummary() {
    return String.format(
        "Match[name='%s', participants=%d, winner='%s', show='%s']",
        name, getParticipantCount(), winner, show);
  }
}
