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
package com.github.javydreamercsw.management.ui.view.gm;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H2;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class GmDashboardViewTest extends AbstractViewTest {

  @Mock private LeagueService leagueService;
  @Mock private SecurityUtils securityUtils;
  @Mock private WrestlerService wrestlerService;

  private GmDashboardView view;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    view = new GmDashboardView(leagueService, securityUtils, wrestlerService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the GM Dashboard heading")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("General Manager Dashboard"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the league selector")
  void shouldRenderLeagueSelector() {
    @SuppressWarnings("unchecked")
    ComboBox<Object> selector = _get(view, ComboBox.class);
    assertTrue(selector.isVisible());
  }

  @Test
  @DisplayName("Should render the roster grid")
  void shouldRenderRosterGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Fans column should sort numerically, not lexicographically")
  void fansColumnShouldSortNumerically() {
    Wrestler lowFansWrestler = Wrestler.builder().id(1L).name("Low Fans").build();
    Wrestler midFansWrestler = Wrestler.builder().id(2L).name("Mid Fans").build();
    Wrestler highFansWrestler = Wrestler.builder().id(3L).name("High Fans").build();

    Universe universe = Universe.builder().id(1L).build();
    Account owner = new Account();
    League league = League.builder().id(1L).name("Test League").universe(universe).build();

    LeagueRoster lowRoster = new LeagueRoster();
    lowRoster.setWrestler(lowFansWrestler);
    lowRoster.setOwner(owner);
    LeagueRoster midRoster = new LeagueRoster();
    midRoster.setWrestler(midFansWrestler);
    midRoster.setOwner(owner);
    LeagueRoster highRoster = new LeagueRoster();
    highRoster.setWrestler(highFansWrestler);
    highRoster.setOwner(owner);

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    when(leagueService.getRoster(1L)).thenReturn(List.of(lowRoster, midRoster, highRoster));
    when(wrestlerService.getStateMapByUniverseId(1L))
        .thenReturn(
            Map.of(
                1L, WrestlerState.builder().fans(6_720L).build(),
                2L, WrestlerState.builder().fans(56_100L).build(),
                3L, WrestlerState.builder().fans(57_630L).build()));

    GmDashboardView sortView = new GmDashboardView(leagueService, securityUtils, wrestlerService);
    UI.getCurrent().add(sortView);

    @SuppressWarnings("unchecked")
    ComboBox<League> leagueSelector = _get(sortView, ComboBox.class);
    leagueSelector.setItems(List.of(league));
    leagueSelector.setValue(league);

    @SuppressWarnings("unchecked")
    Grid<Wrestler> grid = _get(sortView, Grid.class);
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
