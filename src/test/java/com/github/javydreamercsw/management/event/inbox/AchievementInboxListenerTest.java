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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.AchievementUnlockedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AchievementInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private InboxEventType achievementUnlocked;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private AchievementInboxListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new AchievementInboxListener(
            inboxService, achievementUnlocked, eventPublisher, inboxUpdateBroadcaster);
  }

  @Test
  void testOnApplicationEvent() {
    Account account = new Account();
    account.setId(1L);
    account.setUsername("testuser");

    Achievement achievement = new Achievement();
    achievement.setName("Test Achievement");
    achievement.setXpValue(100);

    AchievementUnlockedEvent event = new AchievementUnlockedEvent(this, account, achievement);

    listener.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            eq(achievementUnlocked),
            contains("Test Achievement"),
            eq("1"),
            eq(InboxItemTarget.TargetType.ACCOUNT));
    verify(eventPublisher).publishEvent(any(InboxUpdateEvent.class));
    verify(inboxUpdateBroadcaster).broadcast(any(InboxUpdateEvent.class));
  }
}
