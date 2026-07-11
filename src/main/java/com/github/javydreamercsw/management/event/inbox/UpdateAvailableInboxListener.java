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

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.event.UpdateAvailableEvent;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateAvailableInboxListener implements ApplicationListener<UpdateAvailableEvent> {

  private final InboxService inboxService;

  @Qualifier("UPDATE_AVAILABLE") private final InboxEventType updateAvailableEventType;

  private final AccountRepository accountRepository;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  @Override
  public void onApplicationEvent(@NonNull final UpdateAvailableEvent event) {
    String message =
        "A new version of All Time Wrestling RPG is available: v%s. Visit %s to download."
            .formatted(event.getNewVersion(), event.getReleaseUrl());

    List<InboxService.TargetInfo> adminTargets =
        GeneralSecurityUtils.runAsAdmin(
            () ->
                accountRepository.findAllByRoles_Name(RoleName.ADMIN).stream()
                    .filter(a -> a.getId() != null)
                    .map(
                        a ->
                            new InboxService.TargetInfo(
                                a.getId().toString(), InboxItemTarget.TargetType.ACCOUNT))
                    .toList());

    if (adminTargets.isEmpty()) {
      log.warn("No ADMIN accounts found to notify about update v{}", event.getNewVersion());
      return;
    }

    GeneralSecurityUtils.runAsAdmin(
        () ->
            inboxService.createInboxItem(
                updateAvailableEventType,
                "Update Available: v" + event.getNewVersion(),
                message,
                InboxItem.Urgency.INFO,
                adminTargets));

    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
    log.info(
        "Update available notification sent to {} admin(s) for v{}",
        adminTargets.size(),
        event.getNewVersion());
  }
}
