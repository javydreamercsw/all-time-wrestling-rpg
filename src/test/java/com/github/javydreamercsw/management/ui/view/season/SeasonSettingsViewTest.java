/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.season;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowSchedulerService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class SeasonSettingsViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private TierBoundaryService tierBoundaryService;
  @Mock private ShowSchedulerService showSchedulerService;
  @Mock private SeasonService seasonService;
  @Mock private UniverseContextService universeContextService;
  @Mock private NotificationService notificationService;

  private SeasonSettingsView view;

  @BeforeEach
  void setup() {
    view =
        new SeasonSettingsView(
            wrestlerService,
            tierBoundaryService,
            showSchedulerService,
            seasonService,
            universeContextService,
            notificationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("SeasonSettingsView should construct without throwing")
  void viewConstructs() {
    assertNotNull(view, "SeasonSettingsView should not be null");
  }

  @Test
  @DisplayName("View should contain exactly 4 action buttons")
  void actionButtonsExist() {
    List<Button> buttons = _find(view, Button.class);
    assertEquals(
        4,
        buttons.size(),
        "Expected 4 buttons: reset boundaries, recalibrate fans, reset fans, generate schedule");
  }

  @Test
  @DisplayName("reset-boundaries-button should exist")
  void resetBoundariesButtonHasId() {
    Button btn = _get(view, Button.class, spec -> spec.withId("reset-boundaries-button"));
    assertNotNull(btn, "reset-boundaries-button should be present");
  }

  @Test
  @DisplayName("full-reset-button should exist")
  void fullResetButtonHasId() {
    Button btn = _get(view, Button.class, spec -> spec.withId("full-reset-button"));
    assertNotNull(btn, "full-reset-button should be present");
  }

  @Test
  @DisplayName("generate-schedule-button should exist")
  void generateScheduleButtonHasId() {
    Button btn = _get(view, Button.class, spec -> spec.withId("generate-schedule-button"));
    assertNotNull(btn, "generate-schedule-button should be present");
  }
}
