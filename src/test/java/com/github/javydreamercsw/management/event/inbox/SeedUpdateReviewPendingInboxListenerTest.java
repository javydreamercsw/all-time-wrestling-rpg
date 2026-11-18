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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.event.SeedUpdateReviewPendingEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the batched seed-review inbox notification (seed-update review feature): one event
 * → one ACTION_REQUIRED admin item with the batched count in the message.
 */
@ExtendWith(MockitoExtension.class)
class SeedUpdateReviewPendingInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private InboxEventType seedUpdateReviewPendingEventType;
  @Mock private AccountRepository accountRepository;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private SeedUpdateReviewPendingInboxListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new SeedUpdateReviewPendingInboxListener(
            inboxService,
            seedUpdateReviewPendingEventType,
            accountRepository,
            inboxUpdateBroadcaster);
  }

  private Account admin(Long id) {
    Account admin = new Account();
    admin.setId(id);
    return admin;
  }

  @Test
  void onApplicationEvent_notifiesAdminsWithBatchedCount() {
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN))
        .thenReturn(List.of(admin(1L), admin(2L)));
    when(inboxService.createInboxItem(any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());

    listener.onApplicationEvent(new SeedUpdateReviewPendingEvent(this, 3, "2.13.0"));

    ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItem.Urgency> urgency = ArgumentCaptor.forClass(InboxItem.Urgency.class);
    verify(inboxService)
        .createInboxItem(any(), subject.capture(), message.capture(), urgency.capture(), any());
    assertEquals("Seed Update Review", subject.getValue());
    assertEquals("3 seed data decisions pending from v2.13.0 update.", message.getValue());
    assertEquals(InboxItem.Urgency.ACTION_REQUIRED, urgency.getValue());
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  void onApplicationEvent_skipsWhenNoAdmins() {
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(Collections.emptyList());

    listener.onApplicationEvent(new SeedUpdateReviewPendingEvent(this, 1, "2.13.0"));

    verifyNoInteractions(inboxService);
    verifyNoInteractions(inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_skipsAccountsWithNullId() {
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(List.of(admin(null)));

    listener.onApplicationEvent(new SeedUpdateReviewPendingEvent(this, 1, "2.13.0"));

    verify(inboxService, never()).createInboxItem(any(), any(), any(), any(), any());
    verifyNoInteractions(inboxUpdateBroadcaster);
  }
}
