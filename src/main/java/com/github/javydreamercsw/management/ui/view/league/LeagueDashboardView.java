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

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.show.ShowDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Route(value = "league-dashboard", layout = MainLayout.class)
@PageTitle("League Dashboard")
@PermitAll
@RequiredArgsConstructor
public class LeagueDashboardView extends Main implements HasUrlParameter<Long> {

  private final LeagueService leagueService;
  private final ShowService showService;
  private final LeagueRosterRepository leagueRosterRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;

  private League league;
  private VerticalLayout contentLayout;
  private Tabs tabs;
  private Div tabsContent;

  @Override
  public void setParameter(BeforeEvent event, Long leagueId) {
    Optional<League> leagueOpt = leagueService.getLeagueById(leagueId);
    if (leagueOpt.isPresent()) {
      this.league = leagueOpt.get();
      initView();
    } else {
      // Handle not found
      event.rerouteTo("leagues");
    }
  }

  private void initView() {
    removeAll();
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    // Header
    Button backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("leagues")));

    H3 title = new H3(league.getName() + " - Dashboard");
    title.addClassNames(LumoUtility.Margin.NONE);

    HorizontalLayout header = new HorizontalLayout(backButton, title);
    header.setAlignItems(HorizontalLayout.Alignment.CENTER);

    add(new ViewToolbar("League Dashboard", ViewToolbar.group(header)));

    // Tabs
    tabs = new Tabs();
    Tab standingsTab = new Tab("Standings");
    Tab rosterTab = new Tab("Rosters");
    Tab historyTab = new Tab("Show History");
    tabs.add(standingsTab, rosterTab, historyTab);

    tabsContent = new Div();
    tabsContent.setSizeFull();
    tabsContent.addClassNames(LumoUtility.Overflow.AUTO);

    Map<Tab, Component> tabComponents = new HashMap<>();
    tabComponents.put(standingsTab, createStandingsTab());
    tabComponents.put(rosterTab, createRosterTab());
    tabComponents.put(historyTab, createHistoryTab());

    tabs.addSelectedChangeListener(
        event -> {
          tabsContent.removeAll();
          Component selectedContent = tabComponents.get(tabs.getSelectedTab());
          if (selectedContent != null) {
            tabsContent.add(selectedContent);
          }
        });

    // Select first tab initially
    tabs.setSelectedTab(standingsTab);
    tabsContent.add(tabComponents.get(standingsTab));

    contentLayout = new VerticalLayout(tabs, tabsContent);
    contentLayout.setSizeFull();
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);

    add(contentLayout);
  }

  private Component createStandingsTab() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();
    layout.setPadding(true);

    // Placeholder for standings logic (can be expanded later with actual points calculation)
    // For now, listing members
    Grid<LeagueRoster> standingsGrid = new Grid<>(LeagueRoster.class, false);
    standingsGrid.addColumn(r -> r.getOwner().getUsername()).setHeader("Player");
    standingsGrid.addColumn(r -> r.getWrestler().getName()).setHeader("Wrestler");
    standingsGrid
        .addColumn(r -> String.format("%d - %d - %d", r.getWins(), r.getLosses(), r.getDraws()))
        .setHeader("Record");

    List<LeagueRoster> rosters = leagueRosterRepository.findByLeague(league);
    standingsGrid.setItems(rosters);
    standingsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    layout.add(new H3("League Standings"), standingsGrid);
    return layout;
  }

  private Component createRosterTab() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();
    layout.setPadding(true);

    Grid<LeagueRoster> rosterGrid = new Grid<>(LeagueRoster.class, false);
    rosterGrid.addColumn(r -> r.getOwner().getUsername()).setHeader("Owner").setSortable(true);
    rosterGrid.addColumn(r -> r.getWrestler().getName()).setHeader("Wrestler").setSortable(true);
    rosterGrid.addColumn(r -> r.getWrestler().getTier()).setHeader("Tier").setSortable(true);
    rosterGrid.addColumn(r -> r.getWrestler().getFans()).setHeader("Fans").setSortable(true);

    List<LeagueRoster> rosters = leagueRosterRepository.findByLeague(league);
    rosterGrid.setItems(rosters);
    rosterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    layout.add(new H3("League Rosters"), rosterGrid);
    return layout;
  }

  private Component createHistoryTab() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();
    layout.setPadding(true);

    Grid<Show> showGrid = new Grid<>(Show.class, false);
    showGrid.addColumn(Show::getName).setHeader("Show Name").setSortable(true);
    showGrid
        .addColumn(
            s ->
                s.getShowDate() != null
                    ? s.getShowDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : "Unscheduled")
        .setHeader("Date")
        .setSortable(true);
    showGrid
        .addComponentColumn(
            show -> {
              Button viewBtn = new Button(new Icon(VaadinIcon.EYE));
              viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              viewBtn.setTooltipText("View Show Details");
              viewBtn.addClickListener(
                  e -> getUI().ifPresent(ui -> ui.navigate(ShowDetailView.class, show.getId())));
              return viewBtn;
            })
        .setHeader("Actions");

    // Fetch shows for this league
    List<Show> leagueShows = showService.getShowsByLeague(league);
    showGrid.setItems(leagueShows);
    showGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

    layout.add(new H3("Show History"), showGrid);
    return layout;
  }
}
