package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HeatChangeEvent extends ApplicationEvent {

  private final Long rivalryId;
  private final int oldHeat;
  private final int newHeat;
  private final String reason;

  public HeatChangeEvent(Object source, Rivalry rivalry, int oldHeat, String reason) {
    super(source);
    this.rivalryId = rivalry.getId();
    this.oldHeat = oldHeat;
    this.newHeat = rivalry.getHeat();
    this.reason = reason;
  }
}
