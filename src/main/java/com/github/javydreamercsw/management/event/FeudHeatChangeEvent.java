package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FeudHeatChangeEvent extends ApplicationEvent {

  private final Long feudId;
  private final int oldHeat;
  private final int newHeat;
  private final String reason;
  private final List<Wrestler> wrestlers;

  public FeudHeatChangeEvent(
      Object source, MultiWrestlerFeud feud, int oldHeat, String reason, List<Wrestler> wrestlers) {
    super(source);
    this.feudId = feud.getId();
    this.oldHeat = oldHeat;
    this.newHeat = feud.getHeat();
    this.reason = reason;
    this.wrestlers = wrestlers;
  }
}
