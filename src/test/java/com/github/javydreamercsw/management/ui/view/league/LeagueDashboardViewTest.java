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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.tabs.Tabs;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LeagueDashboardViewTest extends AbstractViewTest {

  @Mock private LeagueService leagueService;
  @Mock private ShowService showService;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;

  private LeagueDashboardView view;

  @BeforeEach
  void setup() {
    League league = new League();
    league.setName("Test League");
    when(leagueService.getLeagueById(1L)).thenReturn(Optional.of(league));
    when(leagueRosterRepository.findByLeague(any())).thenReturn(Collections.emptyList());
    when(showService.getShowsByLeague(any())).thenReturn(Collections.emptyList());

    view =
        new LeagueDashboardView(
            leagueService, showService, leagueRosterRepository, leagueMembershipRepository);
    view.setParameter(null, 1L);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render tabs after loading a league")
  void shouldRenderTabs() {
    Tabs tabs = _get(view, Tabs.class);
    assertTrue(tabs.isVisible());
  }

  @Test
  @DisplayName("Should render standings grid on default tab")
  void shouldRenderStandingsGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Nested
  @DisplayName("Standings sorting")
  class StandingsSorting {

    private League league;

    @BeforeEach
    void setup() {
      MockitoAnnotations.openMocks(LeagueDashboardViewTest.this);
      league = new League();
      league.setName("Sort League");
      when(leagueService.getLeagueById(2L)).thenReturn(Optional.of(league));
      when(showService.getShowsByLeague(any())).thenReturn(Collections.emptyList());
    }

    private LeagueRoster rosterEntry(String name, int wins, int losses, int draws) {
      Wrestler w = new Wrestler();
      w.setName(name);
      Account owner = new Account();
      owner.setUsername(name);
      LeagueRoster r = new LeagueRoster();
      r.setWrestler(w);
      r.setOwner(owner);
      r.setWins(wins);
      r.setLosses(losses);
      r.setDraws(draws);
      return r;
    }

    @Test
    @DisplayName("Standings grid shows highest-wins roster entry first")
    void standingsGridSortsWinsDescending() {
      LeagueRoster loser = rosterEntry("Loser", 0, 5, 0);
      LeagueRoster winner = rosterEntry("Winner", 5, 0, 0);
      LeagueRoster midCard = rosterEntry("MidCard", 2, 3, 0);

      when(leagueRosterRepository.findByLeagueWithWrestlerStates(any()))
          .thenReturn(List.of(loser, midCard, winner));

      LeagueDashboardView sortView =
          new LeagueDashboardView(
              leagueService, showService, leagueRosterRepository, leagueMembershipRepository);
      sortView.setParameter(null, 2L);
      UI.getCurrent().add(sortView);

      @SuppressWarnings("unchecked")
      Grid<LeagueRoster> grid = (Grid<LeagueRoster>) _get(sortView, Grid.class);
      List<LeagueRoster> items = grid.getListDataView().getItems().toList();

      assertEquals(
          "Winner", items.get(0).getWrestler().getName(), "Highest-wins entry should be first");
      assertEquals(
          "Loser",
          items.get(items.size() - 1).getWrestler().getName(),
          "Lowest-wins entry should be last");
    }
  }
}
