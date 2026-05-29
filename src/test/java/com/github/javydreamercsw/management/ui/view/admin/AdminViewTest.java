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
package com.github.javydreamercsw.management.ui.view.admin;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.image.ImageCleanupService;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.tabs.Tabs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AdminViewTest extends AbstractViewTest {

  @Mock private RankingService rankingService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private ImageCleanupService imageCleanupService;
  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseContextService universeContextService;
  @Mock private NotificationService notificationService;

  private AdminView view;

  @BeforeEach
  void setup() {
    view =
        new AdminView(
            rankingService,
            wrestlerStateRepository,
            imageCleanupService,
            wrestlerService,
            universeContextService,
            notificationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Admin Tools toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName("Should render tabs")
  void shouldRenderTabs() {
    Tabs tabs = _get(view, Tabs.class);
    assertTrue(tabs.isVisible());
  }
}
