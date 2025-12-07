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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class InboxListenersIntegrationTest {

  @Autowired private ApplicationEventPublisher eventPublisher;

  @MockitoBean private InboxService inboxService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Rivalry rivalry;

  @BeforeEach
  void setUp() {
    // Reset mock before each test
    Mockito.reset(inboxService);

    wrestler1 = Wrestler.builder().id(1L).name("Wrestler A").fans(1000L).build();
    wrestler2 = Wrestler.builder().id(2L).name("Wrestler B").fans(500L).build();

    rivalry = new Rivalry();
    rivalry.setId(10L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(50);
  }

  @Test
  void testFanAwardedEventCreatesInboxItem() {
    Long fanChange = 200L;
    FanAwardedEvent event = new FanAwardedEvent(this, wrestler1, fanChange);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(), messageCaptor.capture(), referenceIdCaptor.capture());

    assertEquals(InboxEventType.FAN_ADJUDICATION, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Wrestler %s gained %d fans. New total: %d",
            wrestler1.getName(), fanChange, wrestler1.getFans());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
  }

  @Test
  void testHeatChangeEventCreatesInboxItem() {
    int oldHeat = 50;
    int heatChange = 10; // The actual change in heat
    int newHeat = oldHeat + heatChange; // The expected new total heat
    String reason = "From segment: Singles Match";

    // Update the rivalry object's heat before creating the event
    rivalry.setHeat(newHeat); // Set the rivalry's heat to the new total

    HeatChangeEvent event =
        new HeatChangeEvent(this, rivalry, oldHeat, reason, List.of(wrestler1, wrestler2));
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(), messageCaptor.capture(), referenceIdCaptor.capture());

    assertEquals(InboxEventType.RIVALRY_HEAT_CHANGE, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Rivalry between %s and %s gained %d heat. New total: %d. Reason: %s",
            wrestler1.getName(),
            wrestler2.getName(),
            heatChange,
            newHeat,
            reason); // Use heatChange here
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(rivalry.getId());
    assertEquals(rivalry.getId().toString(), referenceIdCaptor.getValue());
  }
}
