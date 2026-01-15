/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.event.DramaEventCreatedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DramaEventInboxListener implements ApplicationListener<DramaEventCreatedEvent> {

  private final InboxService inboxService;
  private final InboxEventType dramaEventCreated;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public DramaEventInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("dramaEventCreated") InboxEventType dramaEventCreated,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.dramaEventCreated = dramaEventCreated;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull DramaEventCreatedEvent event) {
    DramaEvent dramaEvent = event.getDramaEvent();
    log.info("Received DramaEventCreatedEvent: {}", dramaEvent.getTitle());

    List<String> targetIds = new ArrayList<>();
    if (dramaEvent.getPrimaryWrestler() != null) {
      targetIds.add(dramaEvent.getPrimaryWrestler().getId().toString());
    }
    if (dramaEvent.getSecondaryWrestler() != null) {
      targetIds.add(dramaEvent.getSecondaryWrestler().getId().toString());
    }

    inboxService.createInboxItem(
        dramaEventCreated,
        String.format("%s: %s", dramaEvent.getTitle(), dramaEvent.getDescription()),
        targetIds);

    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
