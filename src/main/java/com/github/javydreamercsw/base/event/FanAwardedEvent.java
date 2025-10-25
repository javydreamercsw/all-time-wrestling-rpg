package com.github.javydreamercsw.base.event;

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
