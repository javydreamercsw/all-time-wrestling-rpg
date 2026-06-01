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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContractRepository;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ContractManagementViewTest extends AbstractViewTest {

  @Mock private LeagueService leagueService;
  @Mock private WrestlerContractRepository contractRepository;
  @Mock private SecurityUtils securityUtils;

  private ContractManagementView view;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    view = new ContractManagementView(leagueService, contractRepository, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Contract Management heading")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("Contract Management"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the contract grid")
  void shouldRenderContractGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }
}
