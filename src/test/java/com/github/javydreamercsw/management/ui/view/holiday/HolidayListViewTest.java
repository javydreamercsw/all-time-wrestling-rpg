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
package com.github.javydreamercsw.management.ui.view.holiday;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class HolidayListViewTest extends AbstractViewTest {

  @Mock private HolidayService holidayService;
  @Mock private SecurityUtils securityUtils;

  private HolidayListView view;

  @BeforeEach
  void setup() {
    when(holidayService.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    view = new HolidayListView(holidayService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the holiday grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("holiday-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the create holiday button")
  void shouldRenderCreateButton() {
    Button createButton = _get(view, Button.class, spec -> spec.withId("create-holiday-button"));
    assertTrue(createButton.isVisible());
  }
}
