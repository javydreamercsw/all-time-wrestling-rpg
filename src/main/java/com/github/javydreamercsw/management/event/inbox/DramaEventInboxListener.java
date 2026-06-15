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
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
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
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("dramaEventCreated") final InboxEventType dramaEventCreated,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.dramaEventCreated = dramaEventCreated;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final DramaEventCreatedEvent event) {
    DramaEvent dramaEvent = event.getDramaEvent();
    log.info("Received DramaEventCreatedEvent: {}", dramaEvent.getTitle());

    List<InboxService.TargetInfo> targets = new ArrayList<>();
    if (dramaEvent.getPrimaryWrestler() != null) {
      targets.add(
          new InboxService.TargetInfo(
              dramaEvent.getPrimaryWrestler().getId().toString(),
              InboxItemTarget.TargetType.WRESTLER));
    }
    if (dramaEvent.getSecondaryWrestler() != null) {
      targets.add(
          new InboxService.TargetInfo(
              dramaEvent.getSecondaryWrestler().getId().toString(),
              InboxItemTarget.TargetType.WRESTLER));
    }

    com.github.javydreamercsw.management.domain.inbox.InboxItem inboxItem =
        inboxService.createInboxItem(
            dramaEventCreated,
            "%s: %s".formatted(dramaEvent.getTitle(), dramaEvent.getDescription()),
            targets);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload(
        "{\"route\":\"wrestler-profile/" + dramaEvent.getPrimaryWrestler().getId() + "\"}");
    inboxService.save(inboxItem);

    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
