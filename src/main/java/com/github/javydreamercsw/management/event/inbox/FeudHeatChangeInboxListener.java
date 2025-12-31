/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.event.FeudHeatChangeEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeudHeatChangeInboxListener implements ApplicationListener<FeudHeatChangeEvent> {

  private final InboxService inboxService;
  private final InboxEventType feudHeatChange;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public FeudHeatChangeInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("feudHeatChange") InboxEventType feudHeatChange,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.feudHeatChange = feudHeatChange;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull FeudHeatChangeEvent event) {
    log.info("Received FeudHeatChangeEvent for feud ID: {}", event.getFeudId());

    String wrestlers =
        event.getWrestlers().stream().map(w -> w.getName()).collect(Collectors.joining(", "));

    String message =
        String.format(
            "Feud '%s' involving %s %s %d heat. New total: %d. Reason: %s",
            event.getFeudName(),
            wrestlers,
            (event.getNewHeat() - event.getOldHeat()) > 0 ? "gained" : "lost",
            Math.abs(event.getNewHeat() - event.getOldHeat()),
            event.getNewHeat(),
            event.getReason());

    inboxService.createInboxItem(feudHeatChange, message, event.getFeudId().toString());
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
