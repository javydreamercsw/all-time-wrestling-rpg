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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership.UniverseMemberRole;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinRequestInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private UniverseMembershipService membershipService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private final InboxEventType eventType =
      new InboxEventType("JOIN_REQUEST_SUBMITTED", "Universe Join Request");

  private JoinRequestInboxListener listener;

  private Universe universe;
  private Account adminAccount;
  private Account memberAccount;

  @BeforeEach
  void setUp() {
    // Construct manually — @InjectMocks can't resolve @Qualifier for the event type
    listener =
        new JoinRequestInboxListener(
            inboxService, membershipService, eventType, eventPublisher, inboxUpdateBroadcaster);

    universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    adminAccount = new Account("admin", "hash", null);
    adminAccount.setId(10L);
    // Give admin the ADMIN role
    com.github.javydreamercsw.base.domain.account.Role adminRole =
        new com.github.javydreamercsw.base.domain.account.Role();
    adminRole.setName(RoleName.ADMIN);
    adminAccount.setRoles(new java.util.HashSet<>(List.of(adminRole)));

    memberAccount = new Account("player", "hash", null);
    memberAccount.setId(11L);
  }

  private InboxItem stubInboxItem() {
    InboxItem item = new InboxItem();
    item.setEventType(eventType);
    when(inboxService.createInboxItem(any(), any(), anyList())).thenReturn(item);
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    return item;
  }

  @Test
  void onApplicationEvent_notifiesAdminMembers() {
    UniverseMembership adminMembership = new UniverseMembership();
    adminMembership.setAccount(adminAccount);
    adminMembership.setRole(UniverseMemberRole.OWNER);

    UniverseMembership memberMembership = new UniverseMembership();
    memberMembership.setAccount(memberAccount);
    memberMembership.setRole(UniverseMemberRole.MEMBER);

    when(membershipService.getMembersForUniverse(universe))
        .thenReturn(List.of(adminMembership, memberMembership));
    stubInboxItem();

    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(1L);
    request.setUniverse(universe);
    request.setRequesterName("New Player");

    listener.onApplicationEvent(new JoinRequestSubmittedEvent(this, request));

    // Should create inbox item with only the ADMIN account as target
    verify(inboxService)
        .createInboxItem(
            any(InboxEventType.class),
            argThat(msg -> msg.contains("New Player") && msg.contains("Test Universe")),
            argThat(targets -> targets.size() == 1 && targets.get(0).targetId().equals("10")));
    verify(eventPublisher).publishEvent(any());
    verify(inboxUpdateBroadcaster).broadcast(any());
  }

  @Test
  void onApplicationEvent_setsNavigateActionTypeAndPayload() {
    UniverseMembership adminMembership = new UniverseMembership();
    adminMembership.setAccount(adminAccount);
    adminMembership.setRole(UniverseMemberRole.OWNER);

    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of(adminMembership));
    InboxItem item = stubInboxItem();

    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(1L);
    request.setUniverse(universe);
    request.setRequesterName("New Player");

    listener.onApplicationEvent(new JoinRequestSubmittedEvent(this, request));

    assertThat(item.getActionType()).isEqualTo("NAVIGATE");
    assertThat(item.getActionPayload()).contains("universe-list");
    verify(inboxService).save(item);
  }

  @Test
  void onApplicationEvent_noAdminMembers_skipsInboxWrite() {
    UniverseMembership memberOnly = new UniverseMembership();
    memberOnly.setAccount(memberAccount); // PLAYER, not ADMIN
    memberOnly.setRole(UniverseMemberRole.MEMBER);

    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of(memberOnly));

    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(1L);
    request.setUniverse(universe);
    request.setRequesterName("New Player");

    listener.onApplicationEvent(new JoinRequestSubmittedEvent(this, request));

    verify(inboxService, never()).createInboxItem(any(), any(), anyList());
  }

  @Test
  void onApplicationEvent_inboxWriteFailure_doesNotPropagate() {
    UniverseMembership adminMembership = new UniverseMembership();
    adminMembership.setAccount(adminAccount);
    adminMembership.setRole(UniverseMemberRole.OWNER);

    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of(adminMembership));
    when(inboxService.createInboxItem(any(), any(), anyList()))
        .thenThrow(new RuntimeException("DB down"));
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UniverseJoinRequest request = new UniverseJoinRequest();
    request.setId(1L);
    request.setUniverse(universe);
    request.setRequesterName("New Player");

    // Should not throw — best-effort notification
    listener.onApplicationEvent(new JoinRequestSubmittedEvent(this, request));
  }
}
