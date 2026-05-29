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
package com.github.javydreamercsw.management.ui.view.datatransfer;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.service.db.DataMigrationService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class DataTransferViewTest extends AbstractViewTest {

  @Mock private DataMigrationService migrationService;

  private DataTransferView view;

  @BeforeEach
  void setup() {
    view = new DataTransferView(migrationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Data Transfer Management toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
