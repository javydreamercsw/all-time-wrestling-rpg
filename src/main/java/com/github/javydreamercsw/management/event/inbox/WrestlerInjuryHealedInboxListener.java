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
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WrestlerInjuryHealedInboxListener
    implements ApplicationListener<WrestlerInjuryHealedEvent> {

  private final InboxService inboxService;
  private final InboxEventType wrestlerInjuryHealed;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public WrestlerInjuryHealedInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("wrestlerInjuryHealed") InboxEventType wrestlerInjuryHealed,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.wrestlerInjuryHealed = wrestlerInjuryHealed;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull WrestlerInjuryHealedEvent event) {
    log.info("Received WrestlerInjuryHealedEvent for wrestler: {}", event.getWrestler().getName());
    inboxService.createInboxItem(
        wrestlerInjuryHealed,
        String.format(
            "Wrestler %s's %s injury has healed. New total: %d",
            event.getWrestler().getName(),
            event.getInjury().getDescription(),
            event.getWrestler().getInjuries().size()),
        event.getWrestler().getId().toString(),
        InboxItemTarget.TargetType.WRESTLER);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
