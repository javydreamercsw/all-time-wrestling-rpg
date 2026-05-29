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
package com.github.javydreamercsw.management.ui.view.faction;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

class FactionRivalryListViewTest extends AbstractViewTest {

  @Mock private FactionRivalryService factionRivalryService;
  @Mock private FactionRivalryRepository factionRivalryRepository;
  @Mock private FactionService factionService;
  @Mock private SecurityUtils securityUtils;

  private FactionRivalryListView view;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    when(factionService.getAllFactions(any())).thenReturn(Page.empty());
    when(factionRivalryService.getAllFactionRivalriesWithFactions(any())).thenReturn(Page.empty());

    view =
        new FactionRivalryListView(
            factionRivalryService, factionRivalryRepository, factionService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Faction Rivalry List toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
