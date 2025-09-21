package com.github.javydreamercsw.management.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Transfer Object for Segment information from Notion. Used for synchronizing segment data
 * between Notion and the local database.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SegmentDTO {

  private String externalId;
  private String name;
  private String showName;
  private String showExternalId;
  private List<String> participantNames;
  private List<String> winnerNames;
  private String segmentTypeName;
  private Instant segmentDate;
  private Instant createdTime;
  private Instant lastEditedTime;
  private String narration;
}
