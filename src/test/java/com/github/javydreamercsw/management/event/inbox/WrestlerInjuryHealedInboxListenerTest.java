package com.github.javydreamercsw.management.event.inbox;

import static org.mockito.Mockito.verify;

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
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(inboxService).createInboxItem(messageCaptor.capture(), referenceIdCaptor.capture());

    String expectedMessage = "Test Wrestler's injury (Test Injury) has been healed!";
    String expectedReferenceId = "1";

    assert (messageCaptor.getValue().equals(expectedMessage));
    assert (referenceIdCaptor.getValue().equals(expectedReferenceId));
  }
}
