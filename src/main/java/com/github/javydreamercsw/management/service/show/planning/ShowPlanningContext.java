package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningContext {
  private List<Segment> lastMonthSegments;
  private List<Rivalry> currentRivalries;
  private List<Segment> lastMonthPromos;
  private ShowTemplate showTemplate;
  private List<ShowPlanningChampionship> championships;
  private ShowPlanningPle nextPle;
  private List<com.github.javydreamercsw.management.domain.wrestler.Wrestler> fullRoster;
  private List<com.github.javydreamercsw.management.domain.faction.Faction> factions;
}
