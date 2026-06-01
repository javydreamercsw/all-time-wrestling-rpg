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
package com.github.javydreamercsw.management.ui.view.segment.rule;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class SegmentRuleListViewTest extends AbstractViewTest {

  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SecurityUtils securityUtils;

  private SegmentRuleListView view;

  @BeforeEach
  void setup() {
    when(segmentRuleService.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    view = new SegmentRuleListView();
    ReflectionTestUtils.setField(view, "segmentRuleService", segmentRuleService);
    ReflectionTestUtils.setField(view, "securityUtils", securityUtils);
    ReflectionTestUtils.invokeMethod(view, "initializeUI");
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the segment rule grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the create segment rule button")
  void shouldRenderCreateButton() {
    Button createButton = _get(view, Button.class, spec -> spec.withText("Create Segment Rule"));
    assertTrue(createButton.isVisible());
  }
}
