package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Data;

@Data
public class ShowPlanningChampionship {
  private Title title;
  private Wrestler champion;
  private Wrestler contender;
}
