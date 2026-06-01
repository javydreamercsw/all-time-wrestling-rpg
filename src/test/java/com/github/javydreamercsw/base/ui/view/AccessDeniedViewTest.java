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
package com.github.javydreamercsw.base.ui.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessDeniedViewTest extends AbstractViewTest {

  private AccessDeniedView view;

  @BeforeEach
  void setup() {
    view = new AccessDeniedView();
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render Access Denied heading")
  void shouldRenderHeading() {
    H1 heading = _get(view, H1.class, spec -> spec.withText("Access Denied"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render a home navigation button")
  void shouldRenderHomeButton() {
    Button homeButton = _get(view, Button.class, spec -> spec.withText("Go to Home"));
    assertTrue(homeButton.isVisible());
  }
}
