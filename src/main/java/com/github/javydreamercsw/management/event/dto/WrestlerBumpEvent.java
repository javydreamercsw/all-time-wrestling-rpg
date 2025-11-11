package com.github.javydreamercsw.management.event.dto;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

@Getter
public class WrestlerBumpEvent extends ApplicationEvent {

  private final Wrestler wrestler;

  public WrestlerBumpEvent(@NonNull Object source, @NonNull Wrestler wrestler) {
    super(source);
    this.wrestler = wrestler;
  }
}
