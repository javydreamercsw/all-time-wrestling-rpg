package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FeudResolvedEvent extends ApplicationEvent {

  private final MultiWrestlerFeud feud;

  public FeudResolvedEvent(Object source, MultiWrestlerFeud feud) {
    super(source);
    this.feud = feud;
  }
}
