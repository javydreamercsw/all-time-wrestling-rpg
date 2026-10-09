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
package com.github.javydreamercsw.management.ui.view.booker;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class BookerViewTest extends AbstractViewTest {

  @Mock private ShowService showService;
  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private NewsService newsService;
  @Mock private TitleOpportunityService titleOpportunityService;
  @Mock private TitleService titleService;
  @Mock private UniverseContextService universeContextService;

  private BookerView view;

  @BeforeEach
  void setup() {
    when(wrestlerService.findAll()).thenReturn(Collections.emptyList());
    when(showService.getUpcomingShows(5)).thenReturn(Collections.emptyList());
    when(rivalryService.getActiveRivalries()).thenReturn(Collections.emptyList());
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());
    view =
        new BookerView(
            showService,
            rivalryService,
            wrestlerService,
            newsService,
            universeContextService,
            titleOpportunityService,
            titleService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Booker Dashboard heading")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("Quick Actions"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the roster overview grid")
  void shouldRenderRosterGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("roster-overview-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render Create Show button")
  void shouldRenderCreateShowButton() {
    Button btn = _get(view, Button.class, spec -> spec.withText("Create Show"));
    assertTrue(btn.isVisible());
  }

  // ── Held briefcases panel (ATW-3fhh) ─────────────────────────────────────

  @Test
  @DisplayName("No held briefcases shows the empty-state message")
  void heldBriefcases_empty_showsEmptyMessage() {
    when(titleOpportunityService.findHeld()).thenReturn(Collections.emptyList());
    view = buildFreshView();
    selectBriefcasesTab(view);

    assertTrue(
        _find(view, com.vaadin.flow.component.html.Span.class).stream()
            .anyMatch(
                s ->
                    s.getText() != null
                        && s.getText().contains("No briefcases are currently held.")),
        "The empty state must render when no case is held");
  }

  @Test
  @DisplayName("Held briefcases grid lists holders with a Cash In action per row")
  void heldBriefcases_gridWithCashInActions() {
    Wrestler holder = new Wrestler();
    holder.setId(8L);
    holder.setName("Mukundi Shumba");
    TitleOpportunity held = new TitleOpportunity();
    held.setId(10L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(holder);
    held.setEarnedAt(java.time.LocalDate.now().minusDays(30));
    held.setExpiryDate(java.time.LocalDate.now().plusDays(335));
    when(titleOpportunityService.findHeld()).thenReturn(List.of(held));
    when(wrestlerService.findById(8L)).thenReturn(Optional.of(holder));
    view = buildFreshView();
    selectBriefcasesTab(view);

    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("held-briefcases-grid"));
    assertTrue(grid.isVisible(), "The held-briefcases grid must render");
    // Component columns materialize per addressed cell — reach the Actions cell directly
    // through the column's runtime key (Actions is the 6th column of this grid).
    com.vaadin.flow.component.grid.Grid.Column<
            com.github.javydreamercsw.management.domain.title.TitleOpportunity>
        actionsColumn =
            ((Grid<com.github.javydreamercsw.management.domain.title.TitleOpportunity>) grid)
                .getColumns()
                .get(5);
    actionsColumn.setKey("actions");
    String actionsKey = actionsColumn.getKey();
    com.vaadin.flow.component.Component actionsCell = _getCellComponent(grid, 0, actionsKey);
    assertTrue(
        !_find(actionsCell, Button.class, spec -> spec.withText("Cash In")).isEmpty(),
        "Each held briefcase row must expose a Cash In action");
  }

  /** The Held Briefcases panel lives on the fourth tab (hidden by default). */
  private static void selectBriefcasesTab(BookerView target) {
    com.vaadin.flow.component.tabs.Tabs tabs =
        _get(target, com.vaadin.flow.component.tabs.Tabs.class);
    tabs.setSelectedIndex(3);
  }

  private BookerView buildFreshView() {
    BookerView fresh =
        new BookerView(
            showService,
            rivalryService,
            wrestlerService,
            newsService,
            universeContextService,
            titleOpportunityService,
            titleService);
    UI.getCurrent().add(fresh);
    return fresh;
  }
}
