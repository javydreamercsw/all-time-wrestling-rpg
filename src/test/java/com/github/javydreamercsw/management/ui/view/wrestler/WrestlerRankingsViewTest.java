package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.event.FanAwardedEvent;
import com.github.javydreamercsw.base.event.FanChangeBroadcaster;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WrestlerRankingsViewTest {

  private WrestlerService wrestlerService;
  private TitleService titleService;

  @BeforeEach
  public void setup() {
    wrestlerService = mock(WrestlerService.class);
    titleService = mock(TitleService.class);
    MockVaadin.setup();
  }

  @AfterEach
  public void tearDown() {
    MockVaadin.tearDown();
  }

  @Test
  void testFanChangeNotification() {
    // Given
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    wrestler.setFans(100L);
    wrestler.setTier(WrestlerTier.MIDCARDER);

    List<Wrestler> wrestlers = new ArrayList<>();
    wrestlers.add(wrestler);

    when(wrestlerService.findAll()).thenReturn(wrestlers);
    when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler));

    // When
    FanAwardedEvent event = new FanAwardedEvent(this, wrestler, 100L);
    UI.getCurrent().add(new WrestlerRankingsView(wrestlerService, titleService));
    FanChangeBroadcaster.broadcast(event);

    // Then
    Failsafe.with(
            RetryPolicy.builder().withDelay(Duration.ofMillis(100)).withMaxRetries(10).build())
        .run(() -> expectNotifications("Test Wrestler gained 100 fans!"));
  }
}
