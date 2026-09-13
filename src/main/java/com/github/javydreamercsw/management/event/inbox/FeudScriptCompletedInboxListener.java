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
import com.github.javydreamercsw.management.event.FeudScriptCompletedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Writes an inbox notification when a story arc (FeudScript) completes. */
@Component
@Slf4j
public class FeudScriptCompletedInboxListener
    implements ApplicationListener<FeudScriptCompletedEvent> {

  private final InboxService inboxService;
  private final InboxEventType feudScriptCompleted;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public FeudScriptCompletedInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("feudScriptCompleted") final InboxEventType feudScriptCompleted,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.feudScriptCompleted = feudScriptCompleted;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final FeudScriptCompletedEvent event) {
    log.debug("Received FeudScriptCompletedEvent for arc: {}", event.getScript().getName());
    InboxItem inboxItem =
        inboxService.createInboxItem(
            feudScriptCompleted,
            "Story Arc Completed: " + event.getScript().getName(),
            "Story arc '%s' has wrapped — every beat is resolved."
                .formatted(event.getScript().getName()),
            InboxItem.Urgency.INFO,
            event.getScript().getId().toString(),
            InboxItemTarget.TargetType.FEUD);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload("{\"route\":\"story-arcs\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
