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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.BriefcaseCashedInEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BriefcaseCashedInInboxListenerTest {

  private InboxService inboxService;
  private InboxEventType briefcaseCashedIn;
  private ApplicationEventPublisher eventPublisher;
  private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private BriefcaseCashedInInboxListener listener;

  @BeforeEach
  public void setUp() {
    inboxService = mock(InboxService.class);
    briefcaseCashedIn = mock(InboxEventType.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    inboxUpdateBroadcaster = mock(InboxUpdateBroadcaster.class);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    listener =
        new BriefcaseCashedInInboxListener(
            inboxService, briefcaseCashedIn, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  @DisplayName("Cash-in names the challenged title in the inbox item")
  void testOnApplicationEvent() {
    Wrestler holder = new Wrestler();
    holder.setId(8L);
    holder.setName("Mukundi Shumba");
    Title title = new Title();
    title.setId(2L);
    title.setName("ATW World");
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setId(10L);
    opportunity.setName("Time Vault briefcase");
    opportunity.setWrestler(holder);
    opportunity.setCashedAgainstTitle(title);
    BriefcaseCashedInEvent event = new BriefcaseCashedInEvent(this, opportunity);

    listener.onApplicationEvent(event);

    ArgumentCaptor<InboxItem> item = ArgumentCaptor.forClass(InboxItem.class);
    verify(inboxService).save(item.capture());
    verify(inboxService)
        .createInboxItem(
            eq(briefcaseCashedIn),
            eq("Briefcase Cashed In: ATW World"),
            anyString(),
            eq(InboxItem.Urgency.INFO),
            eq("8"),
            eq(InboxItemTarget.TargetType.WRESTLER));
    Assertions.assertEquals("NAVIGATE", item.getValue().getActionType());
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName("Null cashed-against title falls back to a generic championship label")
  void testOnApplicationEventNullTitle() {
    Wrestler holder = new Wrestler();
    holder.setId(9L);
    holder.setName("Holder");
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setId(11L);
    opportunity.setName("Briefcase");
    opportunity.setWrestler(holder);
    BriefcaseCashedInEvent event = new BriefcaseCashedInEvent(this, opportunity);

    listener.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            eq(briefcaseCashedIn),
            eq("Briefcase Cashed In: the championship"),
            anyString(),
            any(InboxItem.Urgency.class),
            eq("9"),
            eq(InboxItemTarget.TargetType.WRESTLER));
  }
}
