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
import com.github.javydreamercsw.management.event.RivalryCompletedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RivalryCompletedInboxListener implements ApplicationListener<RivalryCompletedEvent> {

  private final InboxService inboxService;
  private final InboxEventType rivalryCompleted;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public RivalryCompletedInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("rivalryCompleted") InboxEventType rivalryCompleted,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.rivalryCompleted = rivalryCompleted;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull RivalryCompletedEvent event) {
    log.info("Received RivalryCompletedEvent for rivalry: {}", event.getRivalry().getDisplayName());
    inboxService.createInboxItem(
        rivalryCompleted,
        String.format("Rivalry '%s' has been completed.", event.getRivalry().getDisplayName()),
        event.getRivalry().getId().toString());
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
