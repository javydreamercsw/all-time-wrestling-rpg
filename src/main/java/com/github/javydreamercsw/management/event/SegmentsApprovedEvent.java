package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.show.Show;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class SegmentsApprovedEvent extends ApplicationEvent {
  @Getter private final Show show;

  public SegmentsApprovedEvent(Object source, Show show) {
    super(source);
    this.show = show;
  }
}
