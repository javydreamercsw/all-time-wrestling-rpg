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
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.event.UpdateAvailableEvent;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateAvailableInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private InboxEventType updateAvailableEventType;
  @Mock private AccountRepository accountRepository;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private UpdateAvailableInboxListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new UpdateAvailableInboxListener(
            inboxService, updateAvailableEventType, accountRepository, inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_notifiesAdmins() {
    Account admin = new Account();
    admin.setId(1L);
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(List.of(admin));
    when(inboxService.createInboxItem(any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verify(inboxService).createInboxItem(any(), any(), any(), any(), any());
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  void onApplicationEvent_skipsWhenNoAdmins() {
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(Collections.emptyList());

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verifyNoInteractions(inboxService);
    verifyNoInteractions(inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_skipsAccountsWithNullId() {
    Account adminWithNullId = new Account();
    // id is null — should be filtered out
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN))
        .thenReturn(List.of(adminWithNullId));

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verifyNoInteractions(inboxService);
    verifyNoInteractions(inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_skipsWhenSameVersionUnreadItemExists() {
    Account admin = new Account();
    admin.setId(1L);
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(List.of(admin));
    // v2.6.0 notification already sitting unread in the inbox (e.g. created at last startup)
    when(inboxService.hasUnreadInboxItem(updateAvailableEventType, "Update Available: v2.6.0"))
        .thenReturn(true);

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verify(inboxService, never()).createInboxItem(any(), any(), any(), any(), any());
    verifyNoInteractions(inboxUpdateBroadcaster);
  }

  @Test
  void onApplicationEvent_createsItemWhenNoUnreadDuplicate() {
    Account admin = new Account();
    admin.setId(1L);
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(List.of(admin));
    when(inboxService.hasUnreadInboxItem(updateAvailableEventType, "Update Available: v2.6.0"))
        .thenReturn(false);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verify(inboxService).createInboxItem(any(), any(), any(), any(), any());
  }

  @Test
  void onApplicationEvent_supersedesOlderVersionUnreadItems() {
    Account admin = new Account();
    admin.setId(1L);
    when(accountRepository.findAllByRoles_Name(RoleName.ADMIN)).thenReturn(List.of(admin));
    // v2.6.0 is up; an older unread v2.5.2 item still lingers → must be marked read
    when(inboxService.hasUnreadInboxItem(updateAvailableEventType, "Update Available: v2.6.0"))
        .thenReturn(false);
    when(inboxService.markUnreadInboxItemsAsRead(updateAvailableEventType)).thenReturn(1);
    when(inboxService.createInboxItem(any(), any(), any(), any(), any()))
        .thenReturn(new InboxItem());

    listener.onApplicationEvent(
        new UpdateAvailableEvent(this, "2.6.0", "https://example.com/releases/v2.6.0"));

    verify(inboxService).markUnreadInboxItemsAsRead(updateAvailableEventType);
  }
}
