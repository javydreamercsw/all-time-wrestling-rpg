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
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.BriefcaseCashInDialog;
import com.github.javydreamercsw.management.ui.component.news.NewsTickerComponent;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.rivalry.RivalryListView;
import com.github.javydreamercsw.management.ui.view.show.ShowListView;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "booker", layout = MainLayout.class)
@PageTitle("Booker Dashboard | ATW RPG")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE})
public class BookerView extends VerticalLayout {

  private final ShowService showService;
  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final NewsService newsService;
  private final UniverseContextService universeContextService;
  private final TitleOpportunityService titleOpportunityService;
  private final TitleService titleService;

  @Autowired
  public BookerView(
      final ShowService showService,
      final RivalryService rivalryService,
      final WrestlerService wrestlerService,
      final NewsService newsService,
      final UniverseContextService universeContextService,
      final TitleOpportunityService titleOpportunityService,
      final TitleService titleService) {
    this.showService = showService;
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.newsService = newsService;
    this.universeContextService = universeContextService;
    this.titleOpportunityService = titleOpportunityService;
    this.titleService = titleService;

    setHeightFull();
    setPadding(false);
    setSpacing(false);

    add(new ViewToolbar("Booker Dashboard"));
    buildDashboard();
  }

  private void buildDashboard() {
    Component quickActions = createQuickActions();
    NewsTickerComponent newsTicker = new NewsTickerComponent(newsService);
    Tabs tabs = createTabs();
    Div pages = createPages(tabs);

    VerticalLayout tabsComponent = new VerticalLayout(newsTicker, tabs, pages);
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
    // Wrap instead of overflowing on narrow (phone) viewports.
    buttons.addClassNames(LumoUtility.FlexWrap.WRAP);

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
    Tab briefcasesTab = new Tab("Held Briefcases");

    Tabs tabs = new Tabs(rosterTab, showsTab, rivalriesTab, briefcasesTab);
    tabs.setWidthFull();
    return tabs;
  }

  private Div createPages(final Tabs tabs) {
    Grid<Wrestler> rosterGrid = createRosterOverviewGrid();
    Grid<Show> showsGrid = createUpcomingShowsGrid();
    Grid<Rivalry> rivalriesGrid = createActiveRivalriesGrid();
    Div briefcasesPanel = createHeldBriefcasesPanel();

    // Wrap each tab grid in the touch-scroll container so wide grids scroll on phones
    Div rosterWrapper = new Div(rosterGrid);
    rosterWrapper.addClassName("grid-scroll-container");
    Div showsWrapper = new Div(showsGrid);
    showsWrapper.addClassName("grid-scroll-container");
    Div rivalriesWrapper = new Div(rivalriesGrid);
    rivalriesWrapper.addClassName("grid-scroll-container");

    Div pages = new Div(rosterWrapper, showsWrapper, rivalriesWrapper, briefcasesPanel);
    // Flex column so each wrapper's .grid-scroll-container flex-grow:1/min-height:0 governs its
    // height — as a plain block the wrappers collapse and their grids render one row tall.
    pages.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
    pages.setSizeFull();
    showsWrapper.setVisible(false);
    rivalriesWrapper.setVisible(false);
    briefcasesPanel.setVisible(false);

    Map<Tab, Component> tabsToPages =
        Map.of(
            tabs.getTabAt(0), rosterWrapper,
            tabs.getTabAt(1), showsWrapper,
            tabs.getTabAt(2), rivalriesWrapper,
            tabs.getTabAt(3), briefcasesPanel);

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
    Grid.Column<Wrestler> nameColumn =
        grid.addColumn(Wrestler::getName).setHeader("Name").setSortable(true);

    // Resolve tiers once per render pass instead of a per-row service call
    // (the N+1 the audit flagged); rows without a state fall back to UNKNOWN.
    List<Wrestler> roster = wrestlerService.findAll();
    Map<Long, WrestlerState> statesByWrestler =
        roster.stream()
            .collect(
                Collectors.toMap(
                    Wrestler::getId,
                    w ->
                        wrestlerService.getOrCreateState(
                            w.getId(), universeContextService.getCurrentUniverseId())));

    grid.addColumn(
            wrestler -> {
              WrestlerState state = statesByWrestler.get(wrestler.getId());
              return state != null ? state.getTier().getDisplayWithEmoji() : "—";
            })
        .setHeader("Tier")
        .setSortable(true);

    grid.addColumn(Wrestler::getGender).setHeader("Gender").setSortable(true);
    grid.addColumn(Wrestler::getIsPlayer).setHeader("Is Player?").setSortable(true);

    grid.setItems(roster);
    grid.setSizeFull();

    // Default sorting by Name
    grid.sort(GridSortOrder.asc(nameColumn).build());

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

  /**
   * Held briefcases panel (ATW-3fhh): every HELD TitleOpportunity with its holder, expiry and
   * granting tournament, plus a Cash In action opening the shared dialog (same flow as the career
   * page).
   */
  private Div createHeldBriefcasesPanel() {
    Div panel = new Div();
    panel.setId("held-briefcases-panel");
    panel.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
    panel.setSizeFull();

    List<TitleOpportunity> held = titleOpportunityService.findHeld();
    if (held.isEmpty()) {
      Span empty = new Span("No briefcases are currently held.");
      empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
      panel.add(empty);
      return panel;
    }

    Grid<TitleOpportunity> grid = new Grid<>();
    grid.setId("held-briefcases-grid");
    // The holder is a lazy proxy — grid value providers run outside a transaction, so resolve
    // names eagerly into a map (the same discipline as the career view's title grid).
    Map<Long, String> holderNames = new HashMap<>();
    held.forEach(
        o -> {
          Wrestler holder = o.getWrestler();
          if (holder != null && holder.getId() != null) {
            holderNames.put(
                o.getId(),
                wrestlerService.findById(holder.getId()).map(Wrestler::getName).orElse("?"));
          }
        });
    grid.addColumn(o -> holderNames.getOrDefault(o.getId(), "—"))
        .setHeader("Holder")
        .setAutoWidth(true);
    grid.addColumn(TitleOpportunity::getName).setHeader("Briefcase").setAutoWidth(true);
    grid.addColumn(o -> o.getGender() != null ? o.getGender().name() : "—")
        .setHeader("Division")
        .setAutoWidth(true);
    grid.addColumn(TitleOpportunity::getEarnedAt).setHeader("Earned").setAutoWidth(true);
    grid.addColumn(o -> o.getExpiryDate() != null ? o.getExpiryDate() : "never")
        .setHeader("Cashable Until")
        .setAutoWidth(true);
    grid.addComponentColumn(
            opportunity -> {
              Button cashIn = new Button("Cash In");
              cashIn.addClickListener(
                  e ->
                      new BriefcaseCashInDialog(
                              opportunity,
                              titleOpportunityService,
                              titleService,
                              showService,
                              this::buildDashboard)
                          .open());
              return cashIn;
            })
        .setHeader("Actions")
        .setAutoWidth(true);

    grid.setItems(held);
    grid.setAllRowsVisible(true);
    grid.setWidthFull();

    panel.add(grid);
    return panel;
  }
}
