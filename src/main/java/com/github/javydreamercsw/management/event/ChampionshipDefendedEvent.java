package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChampionshipDefendedEvent extends ApplicationEvent {

  private final Long titleId;
  private final List<Wrestler> champions;

  public ChampionshipDefendedEvent(Object source, Title title, List<Wrestler> champions) {
    super(source);
    this.titleId = title.getId();
    this.champions = champions;
  }
}
