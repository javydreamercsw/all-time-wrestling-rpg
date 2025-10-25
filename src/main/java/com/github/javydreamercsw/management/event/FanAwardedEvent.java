package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

@Getter
public class FanAwardedEvent extends ApplicationEvent {

  private final Wrestler wrestler;
  private final Long fanChange;

  public FanAwardedEvent(
      @NonNull Object source, @NonNull Wrestler wrestler, @NonNull Long fanChange) {
    super(source);
    this.wrestler = wrestler;
    this.fanChange = fanChange;
  }
}
