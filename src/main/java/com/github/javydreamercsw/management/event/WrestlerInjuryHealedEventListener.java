package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.base.event.WrestlerInjuryHealedBroadcaster;
import com.github.javydreamercsw.base.event.WrestlerInjuryHealedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WrestlerInjuryHealedEventListener {

  @EventListener
  public void handleWrestlerInjuryHealedEvent(WrestlerInjuryHealedEvent event) {
    WrestlerInjuryHealedBroadcaster.broadcast(event);
  }
}
