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
package com.github.javydreamercsw.management.ui.view.season;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonAward;
import com.github.javydreamercsw.management.domain.season.SeasonAwardType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.season.SeasonAwardsService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowContextFacade;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonDetailViewTest extends AbstractViewTest {

  @Mock private ShowContextFacade showContextFacade;
  @Mock private SeasonService seasonService;
  @Mock private SeasonAwardsService seasonAwardsService;
  @Mock private ViewContext viewContext;

  private SeasonDetailView view;

  @BeforeEach
  public void setup() {
    lenient().when(showContextFacade.getSeasonService()).thenReturn(seasonService);
    lenient().when(showContextFacade.getSeasonAwardsService()).thenReturn(seasonAwardsService);
    view = new SeasonDetailView(showContextFacade, viewContext);
  }

  @Test
  @DisplayName("beforeEnter — missing seasonId param forwards to SeasonListView")
  void beforeEnter_missingParam_redirectsToSeasonList() {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    RouteParameters emptyParams = mock(RouteParameters.class);
    when(emptyParams.get("seasonId")).thenReturn(Optional.empty());
    when(event.getRouteParameters()).thenReturn(emptyParams);

    view.beforeEnter(event);

    verify(event).forwardTo(SeasonListView.class);
  }

  @Test
  @DisplayName("beforeEnter — non-numeric seasonId forwards to SeasonListView")
  void beforeEnter_invalidId_redirectsToSeasonList() {
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("seasonId", "not-a-number"));

    view.beforeEnter(event);

    verify(event).forwardTo(SeasonListView.class);
  }

  @Test
  @DisplayName("beforeEnter — season not found forwards to SeasonListView")
  void beforeEnter_seasonNotFound_redirectsToSeasonList() {
    when(seasonService.getSeasonById(99L)).thenReturn(Optional.empty());
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("seasonId", "99"));

    view.beforeEnter(event);

    verify(event).forwardTo(SeasonListView.class);
  }

  @Test
  @DisplayName("beforeEnter — active season with no awards shows placeholder paragraph")
  void beforeEnter_activeSeason_showsAwardsPlaceholder() {
    Season season = activeSeason();
    when(seasonService.getSeasonById(1L)).thenReturn(Optional.of(season));
    when(seasonAwardsService.getAwards(any())).thenReturn(List.of());

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("seasonId", "1"));

    UI.getCurrent().add(view);
    view.beforeEnter(event);

    assertThat(_find(Paragraph.class))
        .anyMatch(p -> p.getText().contains("Awards will be presented when the season ends"));
    assertThat(_find(Grid.class)).isEmpty();
  }

  @Test
  @DisplayName("beforeEnter — ended season with awards shows awards grid")
  void beforeEnter_endedSeason_withAwards_showsGrid() {
    Season season = endedSeason();
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Champ");

    SeasonAward award =
        SeasonAward.builder()
            .season(season)
            .wrestler(wrestler)
            .awardType(SeasonAwardType.WRESTLER_OF_THE_YEAR)
            .awardValue("5 wins")
            .build();

    when(seasonService.getSeasonById(1L)).thenReturn(Optional.of(season));
    when(seasonAwardsService.getAwards(any())).thenReturn(List.of(award));

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("seasonId", "1"));

    UI.getCurrent().add(view);
    view.beforeEnter(event);

    assertThat(_find(Grid.class)).isNotEmpty();
    assertThat(_find(Paragraph.class))
        .noneMatch(p -> p.getText().contains("Awards will be presented"));
  }

  private Season activeSeason() {
    Season s = new Season();
    s.setId(1L);
    s.setName("Active Season");
    s.setIsActive(true);
    s.setStartDate(Instant.now().minus(10, ChronoUnit.DAYS));
    return s;
  }

  private Season endedSeason() {
    Season s = new Season();
    s.setId(1L);
    s.setName("Ended Season");
    s.setIsActive(false);
    s.setStartDate(Instant.now().minus(30, ChronoUnit.DAYS));
    s.setEndDate(Instant.now());
    return s;
  }
}
