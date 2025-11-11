package com.github.javydreamercsw.management.event;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WrestlerInjuryHealedEventListenerTest {

  @Mock private WrestlerInjuryHealedBroadcaster broadcaster;

  @InjectMocks private WrestlerInjuryHealedEventListener listener;

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
  void handleWrestlerInjuryHealedEvent_broadcastsEvent() {
    // Given
    WrestlerInjuryHealedEvent event = new WrestlerInjuryHealedEvent(this, wrestler, injury);

    // When
    listener.handleWrestlerInjuryHealedEvent(event);

    // Then
    verify(broadcaster, times(1)).broadcast(event);
  }
}
