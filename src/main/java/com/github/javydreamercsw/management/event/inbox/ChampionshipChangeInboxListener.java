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
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChampionshipChangeInboxListener
    implements ApplicationListener<ChampionshipChangeEvent> {

  private final InboxService inboxService;
  private final InboxEventType championshipChange;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public ChampionshipChangeInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("championshipChange") InboxEventType championshipChange,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.championshipChange = championshipChange;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull ChampionshipChangeEvent event) {
    log.info("Received ChampionshipChangeEvent for title ID: {}", event.getTitleId());

    String newChampions =
        event.getNewChampions().stream().map(w -> w.getName()).collect(Collectors.joining(", "));
    String oldChampions =
        event.getOldChampions().stream().map(w -> w.getName()).collect(Collectors.joining(", "));

    String message;
    if (event.getOldChampions().isEmpty()) {
      message =
          String.format("New champions for title ID %d: %s", event.getTitleId(), newChampions);
    } else if (event.getNewChampions().isEmpty()) {
      message =
          String.format(
              "Title ID %d is now vacant. Former champions: %s", event.getTitleId(), oldChampions);
    } else {
      message =
          String.format(
              "Championship change for title ID %d. New champions: %s (formerly %s)",
              event.getTitleId(), newChampions, oldChampions);
    }

    inboxService.createInboxItem(
        championshipChange,
        message,
        event.getTitleId().toString(),
        InboxItemTarget.TargetType.TITLE);
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
