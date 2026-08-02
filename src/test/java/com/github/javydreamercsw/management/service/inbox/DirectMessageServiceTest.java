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
package com.github.javydreamercsw.management.service.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItem.Urgency;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget.TargetType;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DirectMessageServiceTest {

  @Mock private InboxRepository inboxRepository;
  @Mock private InboxEventTypeRegistry registry;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  @Mock private SecurityUtils securityUtils;

  @InjectMocks private DirectMessageService directMessageService;

  private Account senderAccount;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    senderAccount = mock(Account.class);
    when(senderAccount.getId()).thenReturn(1L);
    userDetails = new CustomUserDetails(senderAccount, null);

    InboxEventType directMessageType = new InboxEventType("DIRECT_MESSAGE", "Direct Message");
    when(registry.getEventTypes()).thenReturn(List.of(directMessageType));
  }

  @Test
  @DisplayName("send() saves item with correct senderAccountId, event type, and recipient target")
  void send_savesItemWithCorrectFields() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    InboxItem saved = new InboxItem();
    saved.setId(99L);
    when(inboxRepository.save(any())).thenReturn(saved);

    InboxItem result = directMessageService.send(42L, "Hello", "Body text");

    ArgumentCaptor<InboxItem> captor = ArgumentCaptor.forClass(InboxItem.class);
    verify(inboxRepository).save(captor.capture());
    InboxItem captured = captor.getValue();

    assertThat(captured.getSenderAccountId()).isEqualTo(1L);
    assertThat(captured.getEventType().getName()).isEqualTo("DIRECT_MESSAGE");
    assertThat(captured.getSubject()).isEqualTo("Hello");
    assertThat(captured.getDescription()).isEqualTo("Body text");
    assertThat(captured.getUrgency()).isEqualTo(Urgency.INFO);
    assertThat(captured.getActionType()).isEqualTo("REPLY");
    assertThat(captured.getTargets())
        .anyMatch(t -> t.getTargetId().equals("42") && t.getTargetType() == TargetType.ACCOUNT);
    assertThat(result).isEqualTo(saved);
  }

  @Test
  @DisplayName("send() broadcasts after saving the item")
  void send_broadcastsAfterSave() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(inboxRepository.save(any())).thenReturn(new InboxItem());

    directMessageService.send(42L, "Subject", "Body");

    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }

  @Test
  @DisplayName("send() throws IllegalStateException when no authenticated user")
  void send_throwsWhenNotAuthenticated() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> directMessageService.send(42L, "Subject", "Body"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No authenticated user");
  }
}
