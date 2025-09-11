package com.github.javydreamercsw.management.service.show.planning.dto;

import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningRivalryDTO {
  private Long id;
  private String name;
  private List<String> participants;
}
