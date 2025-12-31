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
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FactionHeatChangeInboxListener implements ApplicationListener<FactionHeatChangeEvent> {

  private final InboxService inboxService;
  private final InboxEventType factionHeatChange;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public FactionHeatChangeInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("factionHeatChange") InboxEventType factionHeatChange,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.factionHeatChange = factionHeatChange;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull FactionHeatChangeEvent event) {
    log.info("Received FactionHeatChangeEvent for rivalry ID: {}", event.getFactionRivalryId());

    String message =
        String.format(
            "Faction rivalry between %s and %s %s %d heat. New total: %d. Reason: %s",
            event.getFaction1Name(),
            event.getFaction2Name(),
            (event.getNewHeat() - event.getOldHeat()) > 0 ? "gained" : "lost",
            Math.abs(event.getNewHeat() - event.getOldHeat()),
            event.getNewHeat(),
            event.getReason());

    inboxService.createInboxItem(
        factionHeatChange, message, event.getFactionRivalryId().toString());
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
