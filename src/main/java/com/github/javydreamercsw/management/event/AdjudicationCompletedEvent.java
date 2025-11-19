package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.show.Show;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AdjudicationCompletedEvent extends ApplicationEvent {
  private final Show show;

  public AdjudicationCompletedEvent(Object source, Show show) {
    super(source);
    this.show = show;
  }
}
