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
package com.github.javydreamercsw.management.event.inbox;

import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerInjuryHealedInboxListenerTest {

  @Mock private InboxService inboxService;

  @InjectMocks private WrestlerInjuryHealedInboxListener listener;

  private Wrestler wrestler;
  private Injury injury;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    injury = new Injury();
    injury.setId(1L);
    injury.setName("Test Injury");
  }

  @Test
  void handleWrestlerInjuryHealedEvent_createsInboxItem() {
    // Given
    WrestlerInjuryHealedEvent event = new WrestlerInjuryHealedEvent(this, wrestler, injury);

    // When
    listener.handleWrestlerInjuryHealedEvent(event);

    // Then
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(inboxService)
        .createInboxItem(
            eventTypeCaptor.capture(), messageCaptor.capture(), referenceIdCaptor.capture());

    String expectedMessage = "Test Wrestler's injury (Test Injury) has been healed!";
    String expectedReferenceId = "1";

    assert (eventTypeCaptor.getValue().equals(InboxEventType.WRESTLER_INJURY_HEALED));
    assert (messageCaptor.getValue().equals(expectedMessage));
    assert (referenceIdCaptor.getValue().equals(expectedReferenceId));
  }
}
