package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.match.Match;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningContext {
  private List<Match> lastMonthMatches;
  private List<Rivalry> currentRivalries;
  private List<Match> lastMonthPromos;
  private ShowTemplate showTemplate;
}
