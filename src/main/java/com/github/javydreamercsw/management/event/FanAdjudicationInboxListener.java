package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class FanAdjudicationInboxListener implements ApplicationListener<FanAwardedEvent> {

  private final InboxService inboxService;

  public FanAdjudicationInboxListener(@NonNull InboxService inboxService) {
    this.inboxService = inboxService;
  }

  @Override
  public void onApplicationEvent(@NonNull FanAwardedEvent event) {
    String message =
        String.format(
            "Wrestler %s %s %d fans. New total: %d",
            event.getWrestler().getName(),
            event.getFanChange() > 0 ? "gained" : "lost",
            Math.abs(event.getFanChange()),
            event.getWrestler().getFans());

    inboxService.createInboxItem(message, event.getWrestler().getId().toString());
  }
}
