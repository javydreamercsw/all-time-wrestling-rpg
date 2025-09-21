package com.github.javydreamercsw.management.service.show.planning.dto;

import com.github.javydreamercsw.management.service.show.planning.ShowTemplate;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningContextDTO {
  private List<ShowPlanningSegmentDTO> lastMonthSegments;
  private List<ShowPlanningRivalryDTO> currentRivalries;
  private List<ShowPlanningSegmentDTO> lastMonthPromos;
  private ShowTemplate showTemplate;
  private List<ShowPlanningWrestlerHeatDTO> wrestlerHeats;
  private List<ShowPlanningChampionshipDTO> championships;
  private ShowPlanningPleDTO nextPle;
}
