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

import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WrestlerRelationshipManagementViewTest extends AbstractViewTest {

  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private WrestlerRelationshipRepository relationshipRepository;
  @Mock private WrestlerService wrestlerService;

  private WrestlerRelationshipManagementView view;

  @BeforeEach
  void setup() {
    when(relationshipRepository.findAll()).thenReturn(Collections.emptyList());
    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    view =
        new WrestlerRelationshipManagementView(
            relationshipService, relationshipRepository, wrestlerService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Wrestler Relationships heading")
  void shouldRenderHeading() {
    H3 heading = _get(view, H3.class, spec -> spec.withText("Wrestler Relationships"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the relationships grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the Add Relationship button")
  void shouldRenderAddButton() {
    Button btn = _get(view, Button.class, spec -> spec.withText("Add Relationship"));
    assertTrue(btn.isVisible());
  }
}
