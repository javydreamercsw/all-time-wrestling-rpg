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
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WrestlerBumpInboxListener implements ApplicationListener<WrestlerBumpEvent> {

  private final InboxService inboxService;
  private final InboxEventType wrestlerBump;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public WrestlerBumpInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("wrestlerBump") final InboxEventType wrestlerBump,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.wrestlerBump = wrestlerBump;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final WrestlerBumpEvent event) {
    log.debug("Received WrestlerBumpEvent for wrestler: {}", event.getWrestlerState().getName());
    com.github.javydreamercsw.management.domain.inbox.InboxItem inboxItem =
        inboxService.createInboxItem(
            wrestlerBump,
            "Wrestler Bump: " + event.getWrestlerState().getName(),
            "Wrestler %s received a bump. Total bumps: %d"
                .formatted(event.getWrestlerState().getName(), event.getWrestlerState().getBumps()),
            InboxItem.Urgency.WARNING,
            event.getWrestlerState().getWrestler().getId().toString(),
            InboxItemTarget.TargetType.WRESTLER);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload(
        "{\"route\":\"wrestler-profile/" + event.getWrestlerState().getWrestler().getId() + "\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
