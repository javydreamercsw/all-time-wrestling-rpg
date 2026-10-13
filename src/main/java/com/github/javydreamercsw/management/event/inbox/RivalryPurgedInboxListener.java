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

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.RivalryPurgedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Writes ONE summary inbox notification after the nightly rivalry retention purge removes ended
 * rivalries (ATW-8nc8). Runs synchronously in the scheduler's admin context, satisfying
 * InboxService's role checks.
 */
@Component
@Slf4j
public class RivalryPurgedInboxListener implements ApplicationListener<RivalryPurgedEvent> {

  private final InboxService inboxService;
  private final InboxEventType rivalryPurged;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public RivalryPurgedInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("rivalryPurged") final InboxEventType rivalryPurged,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.rivalryPurged = rivalryPurged;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final RivalryPurgedEvent event) {
    log.debug("Received RivalryPurgedEvent: {} rivalries purged", event.getRivalriesPurged());
    String message =
        ("%d ended rivalries older than the retention window were removed (%d segment"
                + " references cleared).")
            .formatted(event.getRivalriesPurged(), event.getSegmentsUnlinked());
    InboxItem inboxItem =
        inboxService.createInboxItem(
            rivalryPurged,
            "Rivalries purged",
            message,
            InboxItem.Urgency.INFO,
            "rivalry-purge-" + event.getCutoff().toEpochMilli(),
            InboxItemTarget.TargetType.OTHER);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload("{\"route\":\"rivalry-list\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
