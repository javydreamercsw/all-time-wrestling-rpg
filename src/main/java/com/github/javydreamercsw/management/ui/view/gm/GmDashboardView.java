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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Route(value = "gm-dashboard", layout = MainLayout.class)
@PageTitle("GM Dashboard")
@RolesAllowed({"ADMIN", "BOOKER"})
@Slf4j
public class GmDashboardView extends VerticalLayout {

  private final LeagueService leagueService;
  private final SecurityUtils securityUtils;

  private final ComboBox<League> leagueSelector = new ComboBox<>("Select League");
  private final VerticalLayout statsLayout = new VerticalLayout();
  private final Grid<Wrestler> rosterGrid = new Grid<>(Wrestler.class, false);
  private final Span budgetLabel = new Span();

  public GmDashboardView(LeagueService leagueService, SecurityUtils securityUtils) {
    this.leagueService = leagueService;
    this.securityUtils = securityUtils;

    initializeUI();
  }

  private void initializeUI() {
    setSizeFull();
    setPadding(true);

    add(new H2("General Manager Dashboard"));

    configureLeagueSelector();
    add(leagueSelector);

    HorizontalLayout summaryCards = new HorizontalLayout();
    summaryCards.setWidthFull();

    VerticalLayout budgetCard = createCard("Total Budget", budgetLabel);
    summaryCards.add(budgetCard);

    add(summaryCards);

    add(new H3("Roster Health & Morale"));
    configureRosterGrid();
    add(rosterGrid);

    loadLeagues();
  }

  private void configureLeagueSelector() {
    leagueSelector.setItemLabelGenerator(League::getName);
    leagueSelector.addValueChangeListener(e -> updateDashboard(e.getValue()));
    leagueSelector.setWidth("300px");
  }

  private void configureRosterGrid() {
    Grid.Column<Wrestler> nameColumn =
        rosterGrid.addColumn(Wrestler::getName).setHeader("Wrestler").setSortable(true);
    rosterGrid.addColumn(w -> w.getTier().getDisplayName()).setHeader("Tier");

    rosterGrid
        .addColumn(w -> w.getManagementStamina() + "%")
        .setHeader("Stamina")
        .setPartNameGenerator(w -> w.getManagementStamina() < 40 ? "danger" : "");

    rosterGrid
        .addColumn(w -> w.getMorale() + "%")
        .setHeader("Morale")
        .setPartNameGenerator(w -> w.getMorale() < 50 ? "warning" : "");

    rosterGrid
        .addColumn(w -> String.format("%,d", w.getFans()))
        .setHeader("Fans")
        .setSortable(true);

    rosterGrid.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);

    // Default sorting by Name
    rosterGrid.sort(GridSortOrder.asc(nameColumn).build());
  }

  private VerticalLayout createCard(String title, Span valueSpan) {
    VerticalLayout card = new VerticalLayout();
    card.setPadding(true);
    card.setSpacing(false);
    card.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM);

    Span titleSpan = new Span(title);
    titleSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

    card.add(titleSpan, valueSpan);
    return card;
  }

  private void loadLeagues() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              List<League> leagues = leagueService.getLeaguesForUser(user.getAccount());
              leagueSelector.setItems(leagues);
              if (!leagues.isEmpty()) {
                leagueSelector.setValue(leagues.get(0));
              }
            });
  }

  private void updateDashboard(League league) {
    if (league == null) return;

    budgetLabel.setText(
        String.format("$%,.2f", league.getBudget() != null ? league.getBudget() : 0.0));

    List<Wrestler> roster =
        leagueService.getRoster(league.getId()).stream()
            .map(com.github.javydreamercsw.management.domain.league.LeagueRoster::getWrestler)
            .toList();

    rosterGrid.setItems(roster);
  }
}
