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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.ChampionDefeatedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ChampionDefeatedInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType championDefeated;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private ChampionDefeatedInboxListener listener;

  @BeforeEach
  public void setUp() {
    inboxService = mock(InboxService.class);
    championDefeated = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    listener =
        new ChampionDefeatedInboxListener(
            inboxService, championDefeated, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  @DisplayName("Champion defeated creates an inbox item targeting the title with a title link")
  void testOnApplicationEvent() {
    Wrestler champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Andre the Giant");
    Wrestler winner = new Wrestler();
    winner.setId(2L);
    winner.setName("Bobby Lashley");
    Title title = new Title();
    title.setId(7L);
    title.setName("ATW World");
    ChampionDefeatedEvent event = new ChampionDefeatedEvent(this, title, champion, winner);

    listener.onApplicationEvent(event);

    ArgumentCaptor<InboxItem> item = ArgumentCaptor.forClass(InboxItem.class);
    verify(inboxService).save(item.capture());
    verify(inboxService)
        .createInboxItem(
            eq(championDefeated),
            eq("Champion Defeated: ATW World"),
            anyString(),
            eq(InboxItem.Urgency.INFO),
            eq("7"),
            eq(InboxItemTarget.TargetType.TITLE));
    Assertions.assertEquals("NAVIGATE", item.getValue().getActionType());
    Assertions.assertEquals("{\"route\":\"title-list\"}", item.getValue().getActionPayload());
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName(
      "A persist failure never propagates — outcome notifications cannot fail adjudication")
  void testOnApplicationEvent_swallowsException() {
    Wrestler champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Andre the Giant");
    Wrestler winner = new Wrestler();
    winner.setId(2L);
    winner.setName("Bobby Lashley");
    Title title = new Title();
    title.setId(7L);
    title.setName("ATW World");

    doThrow(new IllegalStateException("boom"))
        .when(inboxService)
        .createInboxItem(any(), any(), any(), any(), any(), any());

    Assertions.assertDoesNotThrow(
        () ->
            listener.onApplicationEvent(new ChampionDefeatedEvent(this, title, champion, winner)));
    verify(inboxService, org.mockito.Mockito.never()).save(any());
  }
}
