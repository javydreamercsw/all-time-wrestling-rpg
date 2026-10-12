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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.RivalryPurgedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Unit tests for the rivalry-purge summary inbox notification (ATW-8nc8). */
class RivalryPurgedInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType rivalryPurged;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private RivalryPurgedInboxListener listener;

  @BeforeEach
  public void setUp() {
    inboxService = mock(InboxService.class);
    rivalryPurged = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    listener =
        new RivalryPurgedInboxListener(
            inboxService, rivalryPurged, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  void testOnApplicationEvent_createsSingleSummaryItem() {
    listener.onApplicationEvent(new RivalryPurgedEvent(this, 5, 7, Instant.EPOCH));

    verify(inboxService)
        .createInboxItem(
            eq(rivalryPurged),
            anyString(),
            contains("5 ended rivalries"),
            any(InboxItem.Urgency.class),
            contains("rivalry-purge-"),
            eq(InboxItemTarget.TargetType.OTHER));
    verify(inboxService).save(any(InboxItem.class));
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }
}
