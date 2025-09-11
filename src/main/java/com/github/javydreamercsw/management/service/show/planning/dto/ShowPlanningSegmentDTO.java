package com.github.javydreamercsw.management.service.show.planning.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class ShowPlanningSegmentDTO {
  private Long id;
  private String name;
  private ShowPlanningShowDTO show;
  private Instant segmentDate;
}
