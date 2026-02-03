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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
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
public class ChampionshipDefendedInboxListener
    implements ApplicationListener<ChampionshipDefendedEvent> {

  private final InboxService inboxService;
  private final InboxEventType championshipDefended;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public ChampionshipDefendedInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("championshipDefended") InboxEventType championshipDefended,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.championshipDefended = championshipDefended;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull ChampionshipDefendedEvent event) {
    log.debug("Received ChampionshipDefendedEvent for title ID: {}", event.getTitleId());

    String champions =
        event.getChampions().stream().map(Wrestler::getName).collect(Collectors.joining(", "));

    String challengers =
        event.getChallengers().stream().map(Wrestler::getName).collect(Collectors.joining(", "));

    String message =
        String.format(
            "Champion(s) %s successfully defended the %s title against %s!",
            champions, event.getTitleName(), challengers);

    inboxService.createInboxItem(
        championshipDefended,
        message,
        event.getChallengers().stream().map(w -> w.getId().toString()).toList());
    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
