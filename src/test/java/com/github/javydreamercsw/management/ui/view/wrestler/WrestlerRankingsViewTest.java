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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WrestlerRankingsViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private TierBoundaryService tierBoundaryService;
  @Mock private UniverseContextService universeContextService;

  private WrestlerRankingsView view;

  @BeforeEach
  void setup() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(titleService.findAll()).thenReturn(Collections.emptyList());
    when(wrestlerService.findAllIncludingInactive()).thenReturn(Collections.emptyList());

    view =
        new WrestlerRankingsView(
            wrestlerService,
            wrestlerRepository,
            titleService,
            tierBoundaryService,
            universeContextService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the wrestler rankings grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-rankings-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Fans column should sort numerically, not lexicographically")
  void fansColumnShouldSortNumerically() {
    Wrestler lowFans = Wrestler.builder().id(1L).name("Low Fans").build();
    Wrestler midFans = Wrestler.builder().id(2L).name("Mid Fans").build();
    Wrestler highFans = Wrestler.builder().id(3L).name("High Fans").build();

    when(wrestlerService.findAllIncludingInactive())
        .thenReturn(List.of(lowFans, midFans, highFans));
    when(wrestlerService.getStateMapByUniverseId(1L))
        .thenReturn(
            Map.of(
                1L, WrestlerState.builder().fans(6_720L).build(),
                2L, WrestlerState.builder().fans(56_100L).build(),
                3L, WrestlerState.builder().fans(57_630L).build()));

    WrestlerRankingsView sortView =
        new WrestlerRankingsView(
            wrestlerService,
            wrestlerRepository,
            titleService,
            tierBoundaryService,
            universeContextService);
    UI.getCurrent().add(sortView);

    @SuppressWarnings("unchecked")
    Grid<Wrestler> grid = _get(sortView, Grid.class, spec -> spec.withId("wrestler-rankings-grid"));
    Grid.Column<Wrestler> fansColumn =
        grid.getColumns().stream()
            .filter(col -> "Fans".equals(col.getHeaderText()))
            .findFirst()
            .orElseThrow();

    grid.sort(GridSortOrder.desc(fansColumn).build());

    List<Wrestler> items = grid.getListDataView().getItems().toList();
    assertEquals("High Fans", items.get(0).getName(), "Highest fan count should sort first");
    assertEquals(
        "Low Fans",
        items.get(items.size() - 1).getName(),
        "If sorting were lexicographic, '56,100' and '57,630' would order before '6,720'");
  }
}
