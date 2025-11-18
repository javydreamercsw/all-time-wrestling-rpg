package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WrestlerBumpInboxListener implements ApplicationListener<WrestlerBumpEvent> {

  private final InboxService inboxService;

  public WrestlerBumpInboxListener(InboxService inboxService) {
    this.inboxService = inboxService;
  }

  @Override
  public void onApplicationEvent(@NonNull WrestlerBumpEvent event) {
    log.info("Received WrestlerBumpEvent for wrestler: {}", event.getWrestler().getName());
    inboxService.createInboxItem(
        InboxEventType.WRESTLER_BUMP,
        String.format(
            "Wrestler %s received a bump. Total bumps: %d",
            event.getWrestler().getName(), event.getWrestler().getBumps()),
        event.getWrestler().getId().toString());
  }
}
