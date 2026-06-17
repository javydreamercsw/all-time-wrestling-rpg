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

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import java.util.ArrayList;
import java.util.List;
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
  @Mock private InviteService inviteService;

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
            universeRepository,
            inviteService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the league grid")
  void shouldRenderLeagueGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("league-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Commissioner sees invite button for league with a universe")
  void commissionerSeesInviteButton() {
    Account commissioner = new Account();
    commissioner.setUsername("comm-user");
    CustomUserDetails userDetails = new CustomUserDetails(commissioner);

    Universe universe = new Universe();
    universe.setId(1L);

    League league = new League();
    league.setId(1L);
    league.setCommissioner(commissioner);
    league.setUniverse(universe);

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(leagueService.getLeaguesForUser(commissioner))
        .thenReturn(new ArrayList<>(List.of(league)));

    LeagueListView commView =
        new LeagueListView(
            leagueService,
            accountService,
            securityUtils,
            wrestlerRepository,
            leagueMembershipRepository,
            universeContextService,
            universeRepository,
            inviteService);
    UI.getCurrent().add(commView);

    @SuppressWarnings("unchecked")
    Grid<League> grid = _get(commView, Grid.class, spec -> spec.withId("league-grid"));
    Component actionsCell = GridKt._getCellComponent(grid, 0, "actions");
    List<Button> inviteBtns =
        _find(actionsCell, Button.class, spec -> spec.withId("invite-league-1"));
    assertThat(inviteBtns).isNotEmpty();
  }

  @Test
  @DisplayName("Clicking invite button opens dialog without LazyInitializationException")
  void clickingInviteButtonOpensDialog() {
    Account commissioner = new Account();
    commissioner.setUsername("comm-user");
    CustomUserDetails userDetails = new CustomUserDetails(commissioner);

    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Test Universe");

    League league = new League();
    league.setId(1L);
    league.setCommissioner(commissioner);
    league.setUniverse(universe);

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(leagueService.getLeaguesForUser(commissioner))
        .thenReturn(new ArrayList<>(List.of(league)));
    // Simulate the repository re-fetch that the fix performs on click
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));

    LeagueListView commView =
        new LeagueListView(
            leagueService,
            accountService,
            securityUtils,
            wrestlerRepository,
            leagueMembershipRepository,
            universeContextService,
            universeRepository,
            inviteService);
    UI.getCurrent().add(commView);

    @SuppressWarnings("unchecked")
    Grid<League> grid = _get(commView, Grid.class, spec -> spec.withId("league-grid"));
    Component actionsCell = GridKt._getCellComponent(grid, 0, "actions");
    Button inviteBtn = _get(actionsCell, Button.class, spec -> spec.withId("invite-league-1"));
    _click(inviteBtn);

    assertNotNull(_get(Dialog.class));
  }

  @Test
  @DisplayName("Non-commissioner does not see invite button")
  void nonCommissionerDoesNotSeeInviteButton() {
    Account otherCommissioner = new Account();
    otherCommissioner.setUsername("other-comm");

    Account viewer = new Account();
    viewer.setUsername("viewer-user");
    CustomUserDetails userDetails = new CustomUserDetails(viewer);

    Universe universe = new Universe();
    universe.setId(1L);

    League league = new League();
    league.setId(2L);
    league.setCommissioner(otherCommissioner);
    league.setUniverse(universe);

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.isAdmin()).thenReturn(false);
    when(leagueService.getLeaguesForUser(viewer)).thenReturn(new ArrayList<>(List.of(league)));

    LeagueListView viewerView =
        new LeagueListView(
            leagueService,
            accountService,
            securityUtils,
            wrestlerRepository,
            leagueMembershipRepository,
            universeContextService,
            universeRepository,
            inviteService);
    UI.getCurrent().add(viewerView);

    @SuppressWarnings("unchecked")
    Grid<League> grid = _get(viewerView, Grid.class, spec -> spec.withId("league-grid"));
    Component actionsCell = GridKt._getCellComponent(grid, 0, "actions");
    List<Button> inviteBtns =
        _find(actionsCell, Button.class, spec -> spec.withId("invite-league-2"));
    assertThat(inviteBtns).isEmpty();
  }
}
