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
package com.github.javydreamercsw.management.ui.view.segment.type;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class SegmentTypeListViewTest extends AbstractViewTest {

  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SecurityUtils securityUtils;

  private SegmentTypeListView view;

  @BeforeEach
  void setup() {
    when(segmentTypeService.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    view = new SegmentTypeListView(segmentTypeService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the segment type grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the create segment type button")
  void shouldRenderCreateButton() {
    Button createButton = _get(view, Button.class, spec -> spec.withText("Create Segment Type"));
    assertTrue(createButton.isVisible());
  }
}
