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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.event.ApplicationUpToDateEvent;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationUpToDateInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private InboxEventType updateAvailableEventType;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private ApplicationUpToDateInboxListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new ApplicationUpToDateInboxListener(
            inboxService, updateAvailableEventType, inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_marksUnreadUpdateItemsReadAndBroadcasts() {
    when(inboxService.markUnreadInboxItemsAsRead(updateAvailableEventType)).thenReturn(2);

    listener.onApplicationEvent(new ApplicationUpToDateEvent(this));

    verify(inboxService).markUnreadInboxItemsAsRead(updateAvailableEventType);
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  void onApplicationEvent_noUnreadItemsIsNoOpWithoutBroadcast() {
    when(inboxService.markUnreadInboxItemsAsRead(updateAvailableEventType)).thenReturn(0);

    listener.onApplicationEvent(new ApplicationUpToDateEvent(this));

    verify(inboxService).markUnreadInboxItemsAsRead(updateAvailableEventType);
    verifyNoInteractions(inboxUpdateBroadcaster);
  }

  @Test
  void constructor_nullEventTypeRejected() {
    Assertions.assertThatThrownBy(
            () -> new ApplicationUpToDateInboxListener(inboxService, null, inboxUpdateBroadcaster))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void onApplicationEvent_nullEventRejected() {
    Assertions.assertThatThrownBy(() -> listener.onApplicationEvent(null))
        .isInstanceOf(NullPointerException.class);
  }
}
