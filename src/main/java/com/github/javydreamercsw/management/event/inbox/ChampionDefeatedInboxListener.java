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
import com.github.javydreamercsw.management.event.ChampionDefeatedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Writes an inbox notification when a wrestler defeats a reigning champion in a non-title match
 * (champion-slayer rule, ATW-9ub4). Synchronous during adjudication — single insert, never throws.
 */
@Component
@Slf4j
public class ChampionDefeatedInboxListener implements ApplicationListener<ChampionDefeatedEvent> {

  private final InboxService inboxService;
  private final InboxEventType championDefeated;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public ChampionDefeatedInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("championDefeated") final InboxEventType championDefeated,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.championDefeated = championDefeated;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final ChampionDefeatedEvent event) {
    try {
      log.debug(
          "Received ChampionDefeatedEvent: {} defeated champion {} for {}",
          event.getWinner().getName(),
          event.getChampion().getName(),
          event.getTitle().getName());
      InboxItem inboxItem =
          inboxService.createInboxItem(
              championDefeated,
              "Champion Defeated: " + event.getTitle().getName(),
              "%s beat champion %s in a non-title match — %s is now available as a challenger"
                  + " for '%s'."
                      .formatted(
                          event.getWinner().getName(),
                          event.getChampion().getName(),
                          event.getWinner().getName(),
                          event.getTitle().getName()),
              InboxItem.Urgency.INFO,
              event.getTitle().getId().toString(),
              InboxItemTarget.TargetType.TITLE);
      inboxItem.setActionType("NAVIGATE");
      inboxItem.setActionPayload("{\"route\":\"title-list\"}");
      inboxService.save(inboxItem);
      eventPublisher.publishEvent(new InboxUpdateEvent(this));
      inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
    } catch (Exception e) {
      // Outcome notifications must never fail adjudication.
      log.error("Failed to write champion-defeated inbox item", e);
    }
  }
}
