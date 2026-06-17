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
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.RivalryContinuesEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RivalryContinuesInboxListener implements ApplicationListener<RivalryContinuesEvent> {

  private final InboxService inboxService;
  private final InboxEventType rivalryContinues;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public RivalryContinuesInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("rivalryContinues") final InboxEventType rivalryContinues,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.rivalryContinues = rivalryContinues;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final RivalryContinuesEvent event) {
    log.debug(
        "Received RivalryContinuesEvent for rivalry: {}", event.getRivalry().getDisplayName());
    com.github.javydreamercsw.management.domain.inbox.InboxItem inboxItem =
        inboxService.createInboxItem(
            rivalryContinues,
            "Rivalry Continues: " + event.getRivalry().getDisplayName(),
            "Rivalry '%s' continues.".formatted(event.getRivalry().getDisplayName()),
            InboxItem.Urgency.INFO,
            event.getRivalry().getId().toString(),
            InboxItemTarget.TargetType.RIVALRY);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload("{\"route\":\"rivalry-list\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
