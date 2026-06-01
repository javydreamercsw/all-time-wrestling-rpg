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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.Draft;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.league.DraftBroadcaster;
import com.github.javydreamercsw.management.service.league.DraftService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class DraftViewTest extends AbstractViewTest {

  @Mock private DraftService draftService;
  @Mock private DraftRepository draftRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private DraftPickRepository draftPickRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private DraftBroadcaster draftBroadcaster;

  private DraftView view;

  @BeforeEach
  void setup() {
    League league = new League();
    league.setName("Test League");
    league.setExcludedWrestlers(Collections.emptySet());

    Draft draft = new Draft();
    draft.setId(1L);
    draft.setCurrentRound(1);
    draft.setCurrentPickNumber(1);
    draft.setStatus(Draft.DraftStatus.ACTIVE);

    when(leagueRepository.findByIdWithExcludedWrestlers(1L)).thenReturn(Optional.of(league));
    when(draftRepository.findByLeague(any())).thenReturn(Optional.of(draft));
    when(draftPickRepository.findByDraftOrderByPickNumberAsc(any()))
        .thenReturn(Collections.emptyList());
    when(leagueRosterRepository.findByLeague(any())).thenReturn(Collections.emptyList());
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    view =
        new DraftView(
            draftService,
            draftRepository,
            leagueRepository,
            draftPickRepository,
            wrestlerRepository,
            leagueRosterRepository,
            securityUtils,
            draftBroadcaster);
    view.setParameter(null, 1L);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Draft Room heading")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("Draft Room: Test League"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render the available wrestlers grid")
  void shouldRenderAvailableWrestlersGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("available-wrestlers-grid"));
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the pick history grid")
  void shouldRenderPickHistoryGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("pick-history-grid"));
    assertTrue(grid.isVisible());
  }
}
