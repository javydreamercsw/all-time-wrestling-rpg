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
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.SeedUpdateReviewPendingEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Turns one batched {@link SeedUpdateReviewPendingEvent} into a single administrator inbox item
 * (seed-update review feature). The event fires once per boot after the detection/parking phase —
 * the message carries the batched count, never one item per parked row.
 */
@Component
@Slf4j
public class SeedUpdateReviewPendingInboxListener
    implements ApplicationListener<SeedUpdateReviewPendingEvent> {

  private final InboxService inboxService;
  private final InboxEventType seedUpdateReviewPending;
  private final AccountRepository accountRepository;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public SeedUpdateReviewPendingInboxListener(
      @NonNull final InboxService inboxService,
      @NonNull @Qualifier("seedUpdateReviewPending") final InboxEventType seedUpdateReviewPending,
      @NonNull final AccountRepository accountRepository,
      @NonNull final InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.seedUpdateReviewPending = seedUpdateReviewPending;
    this.accountRepository = accountRepository;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull final SeedUpdateReviewPendingEvent event) {
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
      log.warn("No ADMIN accounts found to notify about pending seed decisions.");
      return;
    }

    String message =
        "%d seed data decisions pending from v%s update."
            .formatted(event.getPendingCount(), event.getAppVersion());

    GeneralSecurityUtils.runAsAdmin(
        () ->
            inboxService.createInboxItem(
                seedUpdateReviewPending,
                "Seed Update Review",
                message,
                InboxItem.Urgency.ACTION_REQUIRED,
                adminTargets));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
    log.info(
        "Seed-review inbox notification sent to {} admin(s): {}", adminTargets.size(), message);
  }
}
