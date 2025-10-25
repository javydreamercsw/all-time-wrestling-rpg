package com.github.javydreamercsw.management.ui.view;

import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.mockito.Mockito.mock;

import com.github.javydreamercsw.base.event.FanAwardedEvent;
import com.github.javydreamercsw.base.event.FanChangeBroadcaster;
import com.github.javydreamercsw.base.ui.view.MainLayout;
import com.github.javydreamercsw.base.ui.view.MenuService;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainLayoutTest {

  @BeforeEach
  public void setup() {
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

    // When
    FanAwardedEvent event = new FanAwardedEvent(this, wrestler, 100L);
    UI.getCurrent().add(new MainLayout(mock(MenuService.class)));
    FanChangeBroadcaster.broadcast(event);

    // Then
    expectNotifications("Test Wrestler gained 100 fans!");
  }
}
