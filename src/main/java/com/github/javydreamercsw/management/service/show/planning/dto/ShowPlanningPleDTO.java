package com.github.javydreamercsw.management.service.show.planning.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class ShowPlanningPleDTO {
  private String pleName;
  private Instant pleDate;
  private String summary;
}
