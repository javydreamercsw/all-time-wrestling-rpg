package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WrestlerInjuryHealedEventListener {

  private final WrestlerInjuryHealedBroadcaster broadcaster;

  @EventListener
  public void handleWrestlerInjuryHealedEvent(WrestlerInjuryHealedEvent event) {
    broadcaster.broadcast(event);
  }
}
