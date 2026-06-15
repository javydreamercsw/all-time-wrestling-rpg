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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.universe.UniverseJoinRequest;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.inbox.InboxService.TargetInfo;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Creates an in-app inbox notification for every ADMIN member of the universe when a new join
 * request is submitted, so admins don't have to poll the Requests UI.
 */
@Component
@Slf4j
public class JoinRequestInboxListener implements ApplicationListener<JoinRequestSubmittedEvent> {

  private final InboxService inboxService;
  private final UniverseMembershipService membershipService;
  private final InboxEventType joinRequestEventType;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public JoinRequestInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull final UniverseMembershipService membershipService,
      @NonNull @Qualifier("JOIN_REQUEST_SUBMITTED") final InboxEventType joinRequestEventType,
      @NonNull final ApplicationEventPublisher eventPublisher,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.membershipService = membershipService;
    this.joinRequestEventType = joinRequestEventType;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final JoinRequestSubmittedEvent event) {
    UniverseJoinRequest request = event.getRequest();
    String universeName = request.getUniverse().getName();
    String requesterName = request.getRequesterName();

    String message =
        "'%s' has requested to join universe '%s'.".formatted(requesterName, universeName);

    // Notifications require auth (getMembersForUniverse, createInboxItem); the caller may be an
    // anonymous registrant who has not yet established a SecurityContext, so elevate to system.
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          List<TargetInfo> targets =
              membershipService.getMembersForUniverse(request.getUniverse()).stream()
                  .map(m -> m.getAccount())
                  .filter(a -> a.hasRole(RoleName.ADMIN))
                  .map(Account::getId)
                  .filter(id -> id != null)
                  .map(id -> new TargetInfo(id.toString(), InboxItemTarget.TargetType.ACCOUNT))
                  .toList();

          if (targets.isEmpty()) {
            log.debug(
                "No ADMIN members found for universe {} to notify of join request {}",
                universeName,
                request.getId());
            return;
          }

          try {
            com.github.javydreamercsw.management.domain.inbox.InboxItem inboxItem =
                inboxService.createInboxItem(joinRequestEventType, message, targets);
            inboxItem.setActionType("NAVIGATE");
            inboxItem.setActionPayload("{\"route\":\"universe-list\"}");
            inboxService.save(inboxItem);
            InboxUpdateEvent updateEvent = new InboxUpdateEvent(this);
            eventPublisher.publishEvent(updateEvent);
            inboxUpdateBroadcaster.broadcast(updateEvent);
            log.debug(
                "Notified {} admin(s) of join request {} for universe {}",
                targets.size(),
                request.getId(),
                universeName);
          } catch (Exception ex) {
            // Notifications are best-effort — don't fail the join request if inbox write fails
            log.warn(
                "Failed to create inbox notification for join request {}: {}",
                request.getId(),
                ex.getMessage());
          }
        });
  }
}
