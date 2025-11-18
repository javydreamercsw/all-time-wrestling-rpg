package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WrestlerBumpHealedInboxListener
    implements ApplicationListener<WrestlerBumpHealedEvent> {

  private final InboxService inboxService;

  public WrestlerBumpHealedInboxListener(InboxService inboxService) {
    this.inboxService = inboxService;
  }

  @Override
  public void onApplicationEvent(@NonNull WrestlerBumpHealedEvent event) {
    log.info("Received WrestlerBumpHealedEvent for wrestler: {}", event.getWrestler().getName());
    inboxService.createInboxItem(
        InboxEventType.WRESTLER_BUMP_HEALED,
        String.format(
            "Wrestler %s healed a bump. Total bumps: %d",
            event.getWrestler().getName(), event.getWrestler().getBumps()),
        event.getWrestler().getId().toString());
  }
}
