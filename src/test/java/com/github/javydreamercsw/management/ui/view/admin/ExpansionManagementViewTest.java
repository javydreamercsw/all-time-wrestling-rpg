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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ExpansionManagementViewTest extends AbstractViewTest {

  @Mock private ExpansionService expansionService;

  private ExpansionManagementView view;

  @BeforeEach
  void setup() {
    when(expansionService.getExpansions()).thenReturn(Collections.emptyList());
    view = new ExpansionManagementView(expansionService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the expansion packs heading")
  void shouldRenderHeading() {
    H3 heading = _get(view, H3.class, spec -> spec.withText("Expansion Packs"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the expansions grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }
}
