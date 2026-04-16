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
package com.github.javydreamercsw.management.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class HeatChangeInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType rivalryHeatChange;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private HeatChangeInboxListener listener;

  @BeforeEach
  void setUp() {
    inboxService = mock(InboxService.class);
    rivalryHeatChange = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    listener =
        new HeatChangeInboxListener(
            inboxService, rivalryHeatChange, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  void testOnApplicationEventGained() {
    Wrestler w1 = new Wrestler();
    w1.setName("Wrestler 1");
    Wrestler w2 = new Wrestler();
    w2.setName("Wrestler 2");

    HeatChangeEvent event = mock(HeatChangeEvent.class);
    when(event.getWrestlers()).thenReturn(List.of(w1, w2));
    when(event.getOldHeat()).thenReturn(10);
    when(event.getNewHeat()).thenReturn(20);
    when(event.getReason()).thenReturn("Good match");
    when(event.getRivalryId()).thenReturn(123L);

    listener.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            eq(rivalryHeatChange), anyString(), eq("123"), eq(InboxItemTarget.TargetType.RIVALRY));
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  void testOnApplicationEventLost() {
    Wrestler w1 = new Wrestler();
    w1.setName("Wrestler 1");
    Wrestler w2 = new Wrestler();
    w2.setName("Wrestler 2");

    HeatChangeEvent event = mock(HeatChangeEvent.class);
    when(event.getWrestlers()).thenReturn(List.of(w1, w2));
    when(event.getOldHeat()).thenReturn(20);
    when(event.getNewHeat()).thenReturn(10);
    when(event.getReason()).thenReturn("Bad promo");
    when(event.getRivalryId()).thenReturn(123L);

    listener.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            eq(rivalryHeatChange), anyString(), eq("123"), eq(InboxItemTarget.TargetType.RIVALRY));
  }
}
