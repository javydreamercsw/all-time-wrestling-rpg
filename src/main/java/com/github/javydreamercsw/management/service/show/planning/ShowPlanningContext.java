package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningContext {
  private List<MatchResult> lastMonthMatches;
  private List<Rivalry> currentRivalries;
  private List<MatchResult> lastMonthPromos;
  private ShowTemplate showTemplate;
}
