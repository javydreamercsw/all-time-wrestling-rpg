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
import com.github.javydreamercsw.management.event.BriefcaseCashedInEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BriefcaseCashedInInboxListener implements ApplicationListener<BriefcaseCashedInEvent> {

  private final InboxService inboxService;
  private final InboxEventType briefcaseCashedIn;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public BriefcaseCashedInInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("briefcaseCashedIn") final InboxEventType briefcaseCashedIn,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.briefcaseCashedIn = briefcaseCashedIn;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final BriefcaseCashedInEvent event) {
    log.debug("Received BriefcaseCashedInEvent: {}", event.getOpportunity().getName());
    String titleName =
        event.getOpportunity().getCashedAgainstTitle() != null
            ? event.getOpportunity().getCashedAgainstTitle().getName()
            : "the championship";
    InboxItem inboxItem =
        inboxService.createInboxItem(
            briefcaseCashedIn,
            "Briefcase Cashed In: " + titleName,
            "%s cashed in '%s' for a %s title match."
                .formatted(
                    event.getOpportunity().getWrestler().getName(),
                    event.getOpportunity().getName(),
                    titleName),
            InboxItem.Urgency.INFO,
            event.getOpportunity().getWrestler().getId().toString(),
            InboxItemTarget.TargetType.WRESTLER);
    inboxItem.setActionType("NAVIGATE");
    inboxItem.setActionPayload(
        "{\"route\":\"wrestler-career/" + event.getOpportunity().getWrestler().getId() + "\"}");
    inboxService.save(inboxItem);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
