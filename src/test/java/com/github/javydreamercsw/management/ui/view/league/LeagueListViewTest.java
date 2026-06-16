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
package com.github.javydreamercsw.management.ui.view.league;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class LeagueListViewTest extends AbstractViewTest {

  @Mock private LeagueService leagueService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseRepository universeRepository;

  private LeagueListView view;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    view =
        new LeagueListView(
            leagueService,
            accountService,
            securityUtils,
            wrestlerRepository,
            leagueMembershipRepository,
            universeContextService,
            universeRepository);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the league grid")
  void shouldRenderLeagueGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("league-grid"));
    assertTrue(grid.isVisible());
  }
}
