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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class FactionListViewTest extends AbstractViewTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;
  @Mock private NpcService npcService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageStorageService imageStorageService;

  private FactionListView view;

  @BeforeEach
  void setup() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(factionService.findAllByUniverse(anyLong())).thenReturn(Collections.emptyList());
    when(wrestlerService.findAllIncludingInactive()).thenReturn(Collections.emptyList());
    when(npcService.findAllIncludingInactive()).thenReturn(Collections.emptyList());
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);
    when(factionService.resolveFactionImage(any())).thenReturn("");

    view =
        new FactionListView(
            factionService,
            wrestlerService,
            npcService,
            wrestlerRepository,
            securityUtils,
            universeContextService,
            imageStorageService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the faction grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
    assertFalse(grid.getColumns().isEmpty());
  }

  @Test
  @DisplayName("Grid should show Art and Active columns")
  void gridShouldHaveArtAndActiveColumns() {
    Grid<?> grid = _get(view, Grid.class);
    List<String> headers =
        grid.getColumns().stream()
            .map(Grid.Column::getHeaderText)
            .filter(h -> h != null && !h.isEmpty())
            .collect(Collectors.toList());
    assertTrue(headers.contains("Art"));
    assertTrue(headers.contains("Active"));
  }
}
