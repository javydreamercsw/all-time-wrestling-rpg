package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WrestlerInjuryHealedInboxListener {

  private final InboxService inboxService;

  @EventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleWrestlerInjuryHealedEvent(WrestlerInjuryHealedEvent event) {
    String message =
        String.format(
            "%s's injury (%s) has been healed!",
            event.getWrestler().getName(), event.getInjury().getName());
    inboxService.createInboxItem(message, event.getWrestler().getId().toString());
    log.info("Inbox item created for WrestlerInjuryHealedEvent: {}", message);
  }
}
