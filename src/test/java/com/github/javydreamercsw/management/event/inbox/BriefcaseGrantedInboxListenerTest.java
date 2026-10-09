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
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.BriefcaseGrantedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BriefcaseGrantedInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType briefcaseGranted;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private BriefcaseGrantedInboxListener listener;

  @BeforeEach
  public void setUp() {
    inboxService = mock(InboxService.class);
    briefcaseGranted = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    listener =
        new BriefcaseGrantedInboxListener(
            inboxService, briefcaseGranted, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  @DisplayName("Grant creates an inbox item targeting the winner's wrestler with a career link")
  void testOnApplicationEvent() {
    Wrestler winner = new Wrestler();
    winner.setId(8L);
    winner.setName("Mukundi Shumba");
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setId(10L);
    opportunity.setName("Time Vault briefcase");
    opportunity.setWrestler(winner);
    BriefcaseGrantedEvent event = new BriefcaseGrantedEvent(this, opportunity);

    listener.onApplicationEvent(event);

    ArgumentCaptor<InboxItem> item = ArgumentCaptor.forClass(InboxItem.class);
    verify(inboxService).save(item.capture());
    verify(inboxService)
        .createInboxItem(
            eq(briefcaseGranted),
            anyString(),
            anyString(),
            eq(InboxItem.Urgency.INFO),
            eq("8"),
            eq(InboxItemTarget.TargetType.WRESTLER));
    Assertions.assertEquals("NAVIGATE", item.getValue().getActionType());
    Assertions.assertEquals(
        "{\"route\":\"wrestler-career/8\"}", item.getValue().getActionPayload());
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }
}
