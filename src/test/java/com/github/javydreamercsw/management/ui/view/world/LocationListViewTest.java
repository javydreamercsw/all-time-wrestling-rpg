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
package com.github.javydreamercsw.management.ui.view.world;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class LocationListViewTest extends AbstractViewTest {

  @Mock private LocationService locationService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageStorageService storageService;

  private LocationListView view;

  @BeforeEach
  void setup() {
    when(locationService.findAll()).thenReturn(Collections.emptyList());

    view = new LocationListView(locationService, securityUtils, storageService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the location grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("location-grid"));
    assertTrue(grid.isVisible());
  }
}
