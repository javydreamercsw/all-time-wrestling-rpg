package com.github.javydreamercsw.management.service.show.planning.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class AiGeneratedSegmentDTO {
  private String segmentId;
  private String type; // e.g., "match", "promo", "interview", "angle"
  private String description;
  private String outcome;
  private java.util.List<String> participants;
}
