package com.github.javydreamercsw.base.event;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WrestlerInjuryHealedEvent extends ApplicationEvent {

  private final Wrestler wrestler;
  private final Injury injury;

  public WrestlerInjuryHealedEvent(Object source, Wrestler wrestler, Injury injury) {
    super(source);
    this.wrestler = wrestler;
    this.injury = injury;
  }
}
