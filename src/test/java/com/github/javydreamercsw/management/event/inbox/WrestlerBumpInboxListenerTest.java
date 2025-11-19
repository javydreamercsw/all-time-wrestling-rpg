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

  private WrestlerBumpInboxListener listener;

  @BeforeEach
  void setUp() {
    listener = new WrestlerBumpInboxListener(inboxService);
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
            InboxEventType.WRESTLER_BUMP,
            String.format("Wrestler %s received a bump. Total bumps: %d", wrestlerName, bumps),
            wrestlerId.toString());
  }
}
