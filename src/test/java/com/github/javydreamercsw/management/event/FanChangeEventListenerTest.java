/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.management.config.ManagementTestConfig;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(roles = ADMIN_ROLE)
@Import({TestSecurityConfig.class, ManagementTestConfig.class})
class FanChangeEventListenerTest {

  @Autowired private ApplicationEventPublisher publisher;
  @MockitoBean private FanAdjudicationInboxListener listener;

  @Test
  void testOnApplicationEvent() {
    // Create a mock listener
    Consumer<FanAwardedEvent> listener = mock(Consumer.class);

    // Register the listener
    FanChangeBroadcaster.register(listener);

    // Create a mock event
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    FanAwardedEvent event = new FanAwardedEvent(this, wrestler, 100L);

    // Publish the event
    publisher.publishEvent(event);

    // Verify that the listener received the event
    verify(listener, timeout(1000)).accept(event);
  }
}
