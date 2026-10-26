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

import com.github.javydreamercsw.base.event.ApplicationUpToDateEvent;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Clears stale "update available" inbox items once the running build is up to date. Fired when
 * {@code UpdateCheckService} completes a successful check without finding a newer release — i.e.
 * right after an upgrade, so notifications for versions older than the running one disappear
 * instead of lingering unread forever (ATW-rap2).
 */
@Component
@Slf4j
public class ApplicationUpToDateInboxListener
    implements ApplicationListener<ApplicationUpToDateEvent> {

  private final InboxService inboxService;
  private final InboxEventType updateAvailableEventType;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public ApplicationUpToDateInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("updateAvailable") final InboxEventType updateAvailableEventType,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.updateAvailableEventType = updateAvailableEventType;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final ApplicationUpToDateEvent event) {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          int cleared = inboxService.markUnreadInboxItemsAsRead(updateAvailableEventType);
          if (cleared > 0) {
            log.info("Marked {} stale update-available inbox item(s) as read.", cleared);
            inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
          }
        });
  }
}
