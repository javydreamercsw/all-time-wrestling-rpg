package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WrestlerInjuryHealedEventListener {

  @EventListener
  public void handleWrestlerInjuryHealedEvent(WrestlerInjuryHealedEvent event) {
    WrestlerInjuryHealedBroadcaster.broadcast(event);
  }
}
