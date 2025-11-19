package com.github.javydreamercsw.management.service.show.planning.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningSegmentDTO {
  private Long id;
  private String name;
  private String showName;
  private Instant showDate;
  private Instant segmentDate;
  private List<String> participants;
  private List<String> winners;
  private String summary;
}
