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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class BookerViewTest extends AbstractViewTest {

  @Mock private ShowService showService;
  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private NewsService newsService;
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
            showService, rivalryService, wrestlerService, newsService, universeContextService);
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
}
