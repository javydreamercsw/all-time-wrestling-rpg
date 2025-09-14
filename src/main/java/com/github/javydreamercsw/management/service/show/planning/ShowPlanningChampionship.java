package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningChampionship {
  private Title title;
  private List<Wrestler> champions = new ArrayList<>();
  private List<Wrestler> contenders = new ArrayList<>();
}
