package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HeatChangeInboxListener implements ApplicationListener<HeatChangeEvent> {

  private final InboxService inboxService;

  public HeatChangeInboxListener(@NonNull InboxService inboxService) {
    this.inboxService = inboxService;
  }

  @Override
  public void onApplicationEvent(@NonNull HeatChangeEvent event) {
    String message =
        String.format(
            "Rivalry between %s and %s %s %d heat. New total: %d. Reason: %s",
            event.getWrestlers().get(0).getName(),
            event.getWrestlers().get(1).getName(),
            (event.getNewHeat() - event.getOldHeat()) > 0 ? "gained" : "lost",
            Math.abs(event.getNewHeat() - event.getOldHeat()),
            event.getNewHeat(),
            event.getReason());

    // Assuming the rivalry ID is the relevant reference for the inbox item
    inboxService.createInboxItem("Rivalry Heat Change", message, event.getRivalryId().toString());
  }
}
