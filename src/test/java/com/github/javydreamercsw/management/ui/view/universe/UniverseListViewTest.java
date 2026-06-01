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
package com.github.javydreamercsw.management.ui.view.universe;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.export.CsvExportWriter;
import com.github.javydreamercsw.management.service.export.JsonExportWriter;
import com.github.javydreamercsw.management.service.export.UniverseExportService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class UniverseListViewTest extends AbstractViewTest {

  @Mock private UniverseService universeService;
  @Mock private UniverseMembershipService membershipService;
  @Mock private AccountService accountService;
  @Mock private UniverseSettingsService settingsService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseExportService universeExportService;
  @Mock private CsvExportWriter csvExportWriter;
  @Mock private JsonExportWriter jsonExportWriter;
  @Mock private WrestlerStateRepository wrestlerStateRepository;

  private UniverseListView view;

  @BeforeEach
  void setup() {
    when(universeService.findAll()).thenReturn(Collections.emptyList());

    view =
        new UniverseListView(
            universeService,
            membershipService,
            accountService,
            settingsService,
            wrestlerRepository,
            universeExportService,
            csvExportWriter,
            jsonExportWriter,
            wrestlerStateRepository);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Universe List toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
