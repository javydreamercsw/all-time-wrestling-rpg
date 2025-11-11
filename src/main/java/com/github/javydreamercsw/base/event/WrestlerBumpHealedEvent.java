package com.github.javydreamercsw.base.event;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WrestlerBumpHealedEvent extends ApplicationEvent {

  private final Wrestler wrestler;

  public WrestlerBumpHealedEvent(Object source, Wrestler wrestler) {
    super(source);
    this.wrestler = wrestler;
  }
}
