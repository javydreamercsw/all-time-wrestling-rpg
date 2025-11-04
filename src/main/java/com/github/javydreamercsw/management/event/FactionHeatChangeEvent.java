package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FactionHeatChangeEvent extends ApplicationEvent {

  private final Long factionRivalryId;
  private final int oldHeat;
  private final int newHeat;
  private final String reason;
  private final List<Wrestler> wrestlers;

  public FactionHeatChangeEvent(
      Object source, FactionRivalry rivalry, int oldHeat, String reason, List<Wrestler> wrestlers) {
    super(source);
    this.factionRivalryId = rivalry.getId();
    this.oldHeat = oldHeat;
    this.newHeat = rivalry.getHeat();
    this.reason = reason;
    this.wrestlers = wrestlers;
  }
}
