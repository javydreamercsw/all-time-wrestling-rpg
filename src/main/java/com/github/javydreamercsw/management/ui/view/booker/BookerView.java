/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.booker;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.BOOKER_ROLE;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.rivalry.RivalryListView;
import com.github.javydreamercsw.management.ui.view.show.ShowListView;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerListView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "booker", layout = MainLayout.class)
@PageTitle("Booker Dashboard | ATW RPG")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE})
public class BookerView extends VerticalLayout {

  private final ShowService showService;
  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;

  @Autowired
  public BookerView(
      ShowService showService, RivalryService rivalryService, WrestlerService wrestlerService) {
    this.showService = showService;
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;

    add(new ViewToolbar("Booker Dashboard"));
    buildDashboard();
  }

  private void buildDashboard() {
    HorizontalLayout mainContent = new HorizontalLayout();
    mainContent.setSizeFull();

    VerticalLayout leftColumn = new VerticalLayout();
    leftColumn.setWidth("50%");
    VerticalLayout rightColumn = new VerticalLayout();
    rightColumn.setWidth("50%");

    leftColumn.add(createQuickActions());
    leftColumn.add(createRosterOverview());
    rightColumn.add(createUpcomingShows());
    rightColumn.add(createActiveRivalries());

    mainContent.add(leftColumn, rightColumn);
    add(mainContent);
  }

  private VerticalLayout createQuickActions() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Quick Actions"));
    HorizontalLayout buttons = new HorizontalLayout();

    Button createShow = new Button("Create Show");
    createShow.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ShowListView.class)));
    buttons.add(createShow);

    Button createWrestler = new Button("Create Wrestler");
    createWrestler.addClickListener(
        e -> getUI().ifPresent(ui -> ui.navigate(WrestlerListView.class)));
    buttons.add(createWrestler);

    Button createRivalry = new Button("Create Rivalry");
    createRivalry.addClickListener(
        e -> getUI().ifPresent(ui -> ui.navigate(RivalryListView.class)));
    buttons.add(createRivalry);

    layout.add(buttons);
    return layout;
  }

  private VerticalLayout createRosterOverview() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Roster Overview"));

    Grid<Wrestler> grid = new Grid<>();
    grid.setId("roster-overview-grid");
    grid.addColumn(Wrestler::getName).setHeader("Name");
    grid.addColumn(Wrestler::getTier).setHeader("Tier");
    grid.addColumn(Wrestler::getGender).setHeader("Gender");
    grid.addColumn(Wrestler::getIsPlayer).setHeader("Is Player?");

    grid.setItems(wrestlerService.findAll());
    layout.add(grid);

    RouterLink rosterLink = new RouterLink("View Full Roster", WrestlerListView.class);
    layout.add(rosterLink);

    return layout;
  }

  private VerticalLayout createUpcomingShows() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Upcoming Shows"));

    Grid<Show> grid = new Grid<>();
    grid.setId("upcoming-shows-grid");
    grid.addColumn(Show::getName).setHeader("Show");
    grid.addColumn(Show::getShowDate).setHeader("Date");
    grid.addColumn(show -> show.getType().getName()).setHeader("Brand");

    grid.setItems(showService.getUpcomingShows(5));
    grid.addComponentColumn(
            show -> {
              Button viewDetailsButton = new Button("View Details");
              viewDetailsButton.addClickListener(
                  e ->
                      getUI()
                          .ifPresent(
                              ui -> {
                                String showDetailPath =
                                    "show-detail/" + show.getId() + "?ref=booker";
                                ui.navigate(showDetailPath);
                              }));
              return viewDetailsButton;
            })
        .setHeader("Actions");

    layout.add(grid);

    RouterLink calendarLink = new RouterLink("View Full Calendar", ShowListView.class);
    layout.add(calendarLink);
    return layout;
  }

  private VerticalLayout createActiveRivalries() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Active Rivalries"));
    Grid<Rivalry> grid = new Grid<>();
    grid.setId("active-rivalries-grid");
    grid.addColumn(Rivalry::getDisplayName).setHeader("Rivalry").setSortable(true);
    grid.addColumn(Rivalry::getHeat).setHeader("Heat").setSortable(true);

    grid.setItems(rivalryService.getActiveRivalries());

    layout.add(grid);

    RouterLink rivalryLink = new RouterLink("View All Rivalries", RivalryListView.class);
    layout.add(rivalryLink);

    return layout;
  }
}
