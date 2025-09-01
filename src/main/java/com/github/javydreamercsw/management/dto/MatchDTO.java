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

  private String externalId;
  private String name;
  private String showName;
  private List<String> participantNames;
  private List<String> winnerNames;
  private String matchTypeName;
  private Instant matchDate;
  private Instant createdTime;
  private Instant lastEditedTime;
}
