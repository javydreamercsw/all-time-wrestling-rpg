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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HeatChangeInboxListener implements ApplicationListener<HeatChangeEvent> {

  private final InboxService inboxService;
  private final InboxEventType rivalryHeatChange;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public HeatChangeInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("rivalryHeatChange") final InboxEventType rivalryHeatChange,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.rivalryHeatChange = rivalryHeatChange;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final HeatChangeEvent event) {
    String message =
        "Rivalry between %s and %s %s %d heat. New total: %d. Reason: %s"
            .formatted(
                event.getWrestlers().get(0).getName(),
                event.getWrestlers().get(1).getName(),
                (event.getNewHeat() - event.getOldHeat()) > 0 ? "gained" : "lost",
                Math.abs(event.getNewHeat() - event.getOldHeat()),
                event.getNewHeat(),
                event.getReason());

    // Assuming the rivalry ID is the relevant reference for the inbox item
    com.github.javydreamercsw.management.domain.inbox.InboxItem inboxItem =
        inboxService.createInboxItem(
            rivalryHeatChange,
            "Rivalry Heat " + (event.getNewHeat() - event.getOldHeat() > 0 ? "Gained" : "Lost"),
            message,
            InboxItem.Urgency.INFO,
            event.getRivalryId().toString(),
            InboxItemTarget.TargetType.RIVALRY);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload("{\"route\":\"rivalry-list\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
