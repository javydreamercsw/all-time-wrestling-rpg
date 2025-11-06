package com.github.javydreamercsw.management.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.event.FanAwardedEvent;
import com.github.javydreamercsw.base.event.FanChangeBroadcaster;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FanChangeEventListenerTest {

  @Autowired private ApplicationEventPublisher publisher;

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
