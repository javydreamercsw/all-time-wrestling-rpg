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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerBumpInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private Wrestler wrestler;

  private final InboxEventType wrestlerBump = new InboxEventType("WRESTLER_BUMP", "Wrestler Bump");

  private WrestlerBumpInboxListener listener;

  @BeforeEach
  void setUp() {
    listener = new WrestlerBumpInboxListener(inboxService, wrestlerBump);
  }

  @Test
  void onApplicationEvent_createsInboxItem() {
    // Given
    Long wrestlerId = 1L;
    String wrestlerName = "Test Wrestler";
    int bumps = 1;

    org.mockito.Mockito.when(wrestler.getId()).thenReturn(wrestlerId);
    org.mockito.Mockito.when(wrestler.getName()).thenReturn(wrestlerName);
    org.mockito.Mockito.when(wrestler.getBumps()).thenReturn(bumps);

    WrestlerBumpEvent event = new WrestlerBumpEvent(this, wrestler);

    // When
    listener.onApplicationEvent(event);

    // Then
    verify(inboxService)
        .createInboxItem(
            wrestlerBump,
            String.format("Wrestler %s received a bump. Total bumps: %d", wrestlerName, bumps),
            wrestlerId.toString());
  }
}
