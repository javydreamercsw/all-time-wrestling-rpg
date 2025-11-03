package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChampionshipChangeEvent extends ApplicationEvent {

  private final Long titleId;
  private final List<Wrestler> newChampions;
  private final List<Wrestler> oldChampions;

  public ChampionshipChangeEvent(
      Object source, Title title, List<Wrestler> newChampions, List<Wrestler> oldChampions) {
    super(source);
    this.titleId = title.getId();
    this.newChampions = newChampions;
    this.oldChampions = oldChampions;
  }
}
