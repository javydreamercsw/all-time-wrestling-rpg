package com.github.javydreamercsw.base.event;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WrestlerInjuryEvent extends ApplicationEvent {

  private final Wrestler wrestler;
  private final Injury injury;

  public WrestlerInjuryEvent(Object source, Wrestler wrestler, Injury injury) {
    super(source);
    this.wrestler = wrestler;
    this.injury = injury;
  }
}
