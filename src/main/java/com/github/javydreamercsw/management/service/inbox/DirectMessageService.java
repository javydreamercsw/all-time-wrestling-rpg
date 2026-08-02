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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItem.Urgency;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget.TargetType;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DirectMessageService {

  private final InboxRepository inboxRepository;
  private final InboxEventTypeRegistry registry;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;
  private final SecurityUtils securityUtils;

  @PreAuthorize("isAuthenticated()")
  public InboxItem send(final Long recipientAccountId, final String subject, final String body) {
    Account sender =
        securityUtils
            .getAuthenticatedUser()
            .map(CustomUserDetails::getAccount)
            .orElseThrow(() -> new IllegalStateException("No authenticated user"));

    InboxItem item = new InboxItem();
    item.setSenderAccountId(sender.getId());
    item.setEventType(
        registry.getEventTypes().stream()
            .filter(t -> "DIRECT_MESSAGE".equals(t.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("DIRECT_MESSAGE event type not found")));
    item.setSubject(subject);
    item.setDescription(body);
    item.setUrgency(Urgency.INFO);
    item.setActionType("REPLY");
    item.setActionPayload("{\"senderId\":\"" + sender.getId() + "\"}");
    item.addTarget(String.valueOf(recipientAccountId), TargetType.ACCOUNT);

    InboxItem saved = inboxRepository.save(item);
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
    return saved;
  }
}
