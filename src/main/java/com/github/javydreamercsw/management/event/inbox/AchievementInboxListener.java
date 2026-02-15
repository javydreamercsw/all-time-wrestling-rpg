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

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.AchievementUnlockedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AchievementInboxListener implements ApplicationListener<AchievementUnlockedEvent> {

  private final InboxService inboxService;
  private final InboxEventType achievementUnlocked;
  private final ApplicationEventPublisher eventPublisher;
  private final InboxUpdateBroadcaster inboxUpdateBroadcaster;

  public AchievementInboxListener(
      @NonNull InboxService inboxService,
      @NonNull @Qualifier("ACHIEVEMENT_UNLOCKED") InboxEventType achievementUnlocked,
      @NonNull ApplicationEventPublisher eventPublisher,
      @NonNull InboxUpdateBroadcaster inboxUpdateBroadcaster) {
    this.inboxService = inboxService;
    this.achievementUnlocked = achievementUnlocked;
    this.eventPublisher = eventPublisher;
    this.inboxUpdateBroadcaster = inboxUpdateBroadcaster;
  }

  @Override
  public void onApplicationEvent(@NonNull AchievementUnlockedEvent event) {
    log.info(
        "Received AchievementUnlockedEvent for account ID: {} - Achievement: {}",
        event.getAccountId(),
        event.getAchievementName());

    String message =
        String.format(
            "New career achievement unlocked: %s! Awarded %d Prestige XP.",
            event.getAchievementName(), event.getXpValue());

    inboxService.createInboxItem(
        achievementUnlocked,
        message,
        event.getAccountId().toString(),
        InboxItemTarget.TargetType.ACCOUNT);

    eventPublisher.publishEvent(new InboxUpdateEvent(this));
    inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
  }
}
