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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FactionHeatChangeInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType factionHeatChange;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private FactionHeatChangeInboxListener listener;

  @BeforeEach
  void setUp() {
    inboxService = mock(InboxService.class);
    factionHeatChange = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    listener =
        new FactionHeatChangeInboxListener(
            inboxService, factionHeatChange, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  void testOnApplicationEvent() {
    FactionHeatChangeEvent event = mock(FactionHeatChangeEvent.class);
    when(event.getFaction1Name()).thenReturn("F1");
    when(event.getFaction2Name()).thenReturn("F2");
    when(event.getOldHeat()).thenReturn(10);
    when(event.getNewHeat()).thenReturn(20);
    when(event.getReason()).thenReturn("Reason");
    when(event.getFactionRivalryId()).thenReturn(123L);

    listener.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            eq(factionHeatChange), anyString(), eq("123"), eq(InboxItemTarget.TargetType.FACTION));
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }
}
