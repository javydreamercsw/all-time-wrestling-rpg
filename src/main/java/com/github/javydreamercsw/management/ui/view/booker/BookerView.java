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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Map;
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

    setHeightFull();
    setPadding(false);
    setSpacing(false);

    add(new ViewToolbar("Booker Dashboard"));
    buildDashboard();
  }

  private void buildDashboard() {
    Component quickActions = createQuickActions();
    Tabs tabs = createTabs();
    Div pages = createPages(tabs);

    VerticalLayout tabsComponent = new VerticalLayout(tabs, pages);
    tabsComponent.setPadding(false);
    tabsComponent.setSpacing(false);
    tabsComponent.setAlignItems(Alignment.STRETCH);
    tabsComponent.setSizeFull();
    tabsComponent.setFlexGrow(1, pages);

    add(quickActions, tabsComponent);

    setFlexGrow(0, quickActions);
    setFlexGrow(1, tabsComponent);
    getStyle().set("padding", "1em");
  }

  private Component createQuickActions() {
    H2 title = new H2("Quick Actions");
    title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.Top.NONE);

    Button createShow = new Button("Create Show");
    createShow.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ShowListView.class)));

    Button createWrestler = new Button("Create Wrestler");
    createWrestler.addClickListener(
        e -> getUI().ifPresent(ui -> ui.navigate(WrestlerListView.class)));

    Button createRivalry = new Button("Create Rivalry");
    createRivalry.addClickListener(
        e -> getUI().ifPresent(ui -> ui.navigate(RivalryListView.class)));

    HorizontalLayout buttons = new HorizontalLayout(createShow, createWrestler, createRivalry);
    buttons.setSpacing(true);

    VerticalLayout card = new VerticalLayout(title, buttons);
    card.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.SMALL);
    card.setSpacing(true);
    return card;
  }

  private Tabs createTabs() {
    Tab rosterTab = new Tab("Roster Overview");
    Tab showsTab = new Tab("Upcoming Shows");
    Tab rivalriesTab = new Tab("Active Rivalries");

    Tabs tabs = new Tabs(rosterTab, showsTab, rivalriesTab);
    tabs.setWidthFull();
    return tabs;
  }

  private Div createPages(Tabs tabs) {
    Grid<Wrestler> rosterGrid = createRosterOverviewGrid();
    Grid<Show> showsGrid = createUpcomingShowsGrid();
    Grid<Rivalry> rivalriesGrid = createActiveRivalriesGrid();

    Div pages = new Div(rosterGrid, showsGrid, rivalriesGrid);
    pages.setSizeFull();
    showsGrid.setVisible(false);
    rivalriesGrid.setVisible(false);

    Map<Tab, Component> tabsToPages =
        Map.of(
            tabs.getTabAt(0), rosterGrid,
            tabs.getTabAt(1), showsGrid,
            tabs.getTabAt(2), rivalriesGrid);

    tabs.addSelectedChangeListener(
        event -> {
          tabsToPages.values().forEach(page -> page.setVisible(false));
          Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
          if (selectedPage != null) {
            selectedPage.setVisible(true);
          }
        });

    return pages;
  }

  private Grid<Wrestler> createRosterOverviewGrid() {
    Grid<Wrestler> grid = new Grid<>();
    grid.setId("roster-overview-grid");
    grid.addColumn(Wrestler::getName).setHeader("Name");
    grid.addColumn(Wrestler::getTier).setHeader("Tier");
    grid.addColumn(Wrestler::getGender).setHeader("Gender");
    grid.addColumn(Wrestler::getIsPlayer).setHeader("Is Player?");

    grid.setItems(wrestlerService.findAll());
    grid.setSizeFull();
    return grid;
  }

  private Grid<Show> createUpcomingShowsGrid() {
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
    grid.setSizeFull();
    return grid;
  }

  private Grid<Rivalry> createActiveRivalriesGrid() {
    Grid<Rivalry> grid = new Grid<>();
    grid.setId("active-rivalries-grid");
    grid.addColumn(Rivalry::getDisplayName).setHeader("Rivalry").setSortable(true);
    grid.addColumn(Rivalry::getHeat).setHeader("Heat").setSortable(true);

    grid.setItems(rivalryService.getActiveRivalries());
    grid.setSizeFull();
    return grid;
  }
}
