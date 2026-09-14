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
package com.github.javydreamercsw.management.ui.view.rivalry;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.mapper.RivalryMapper;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.Query;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class RivalryListViewTest extends AbstractViewTest {

  @Mock private RivalryService rivalryService;
  @Mock private RivalryRepository rivalryRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private SecurityUtils securityUtils;
  @Mock private RivalryMapper rivalryMapper;

  private RivalryListView view;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    doReturn(Collections.emptyList()).when(wrestlerService).getAllWrestlers();
    when(rivalryService.getAllRivalriesWithWrestlers(any(Pageable.class), any()))
        .thenReturn(Page.empty());
    when(rivalryService.getAllRivalriesWithWrestlers(any(Pageable.class), isNull()))
        .thenReturn(Page.empty());
    when(rivalryService.getRivalryMapper()).thenReturn(rivalryMapper);

    view = new RivalryListView(rivalryService, rivalryRepository, wrestlerService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Rivalry List toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName("Status filter defaults to Active (ATW-aeib)")
  void statusFilterDefaultsToActive() {
    assertEquals(RivalryListView.RivalryStatusFilter.ACTIVE, view.statusFilter.getValue());
  }

  @Test
  @DisplayName("Switching the status filter queries with the matching tri-state (ATW-aeib)")
  void statusFilterDrivesQuery() {
    // Fetching from the provider exercises the grid's items callback directly —
    // refreshAll() alone does not pull pages without an attached UI render.
    view.rivalryGrid.getDataProvider().fetch(new Query<>());
    verify(rivalryService).getAllRivalriesWithWrestlers(any(Pageable.class), any());

    // ALL -> null tri-state.
    view.statusFilter.setValue(RivalryListView.RivalryStatusFilter.ALL);
    view.rivalryGrid.getDataProvider().fetch(new Query<>());
    verify(rivalryService).getAllRivalriesWithWrestlers(any(Pageable.class), isNull());
  }
}
