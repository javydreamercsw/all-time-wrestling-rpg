package com.github.javydreamercsw.management.event.inbox;

import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerBumpHealedInboxListenerTest {

  @Mock private InboxService inboxService;
  @Mock private Wrestler wrestler;

  private WrestlerBumpHealedInboxListener listener;

  @BeforeEach
  void setUp() {
    listener = new WrestlerBumpHealedInboxListener(inboxService);
  }

  @Test
  void onApplicationEvent_createsInboxItem() {
    // Given
    Long wrestlerId = 1L;
    String wrestlerName = "Test Wrestler";
    int bumps = 0; // After healing, bumps should be 0 or less than before

    org.mockito.Mockito.when(wrestler.getId()).thenReturn(wrestlerId);
    org.mockito.Mockito.when(wrestler.getName()).thenReturn(wrestlerName);
    org.mockito.Mockito.when(wrestler.getBumps()).thenReturn(bumps);

    WrestlerBumpHealedEvent event = new WrestlerBumpHealedEvent(this, wrestler);

    // When
    listener.onApplicationEvent(event);

    // Then
    verify(inboxService)
        .createInboxItem(
            InboxEventType.WRESTLER_BUMP_HEALED,
            String.format("Wrestler %s healed a bump. Total bumps: %d", wrestlerName, bumps),
            wrestlerId.toString());
  }
}
