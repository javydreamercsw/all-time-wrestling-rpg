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
package com.github.javydreamercsw.management.ui.view.ranking;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.provider.Query;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RankingViewTest extends AbstractViewTest {

  @Mock private RankingService rankingService;
  @Mock private TierBoundaryService tierBoundaryService;
  @MockitoBean private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private ChampionshipDTO championshipDTO;

  @BeforeEach
  void setUp() {
    championshipDTO =
        ChampionshipDTO.builder().id(1L).name("Test Title").imageName("test.png").build();
    when(rankingService.getChampionships()).thenReturn(List.of(championshipDTO));
    when(rankingService.getCurrentChampions(championshipDTO.getId()))
        .thenReturn(
            List.of(
                ChampionDTO.builder().id(1L).name("Champion").fans(1000L).reignDays(1L).build()));
    List<RankedWrestlerDTO> contenders = new ArrayList<>();
    contenders.add(
        RankedWrestlerDTO.builder().id(2L).name("Contender 2").fans(700L).rank(1).build());
    contenders.add(
        RankedWrestlerDTO.builder().id(3L).name("Contender 1").fans(500L).rank(2).build());
    when(rankingService.getRankedContenders(championshipDTO.getId()))
        .thenAnswer(invocation -> contenders);
  }

  @Test
  void testViewLoadsAndPopulates() {
    RankingView view = new RankingView(rankingService, tierBoundaryService);
    assertNotNull(view);

    ComboBox<ChampionshipDTO> comboBox = _get(view, ComboBox.class);
    assertEquals(1, comboBox.getDataProvider().size(new Query<>()));

    Image image = _get(view, Image.class);
    assertEquals("championship-image", image.getId().get());
    assertNotNull(image);
    assertEquals("images/championships/test.png", image.getSrc());

    comboBox.setValue(championshipDTO);

    Grid<RankedWrestlerDTO> grid =
        _get(view, Grid.class, spec -> spec.withId("wrestler-contenders-grid"));
    List<RankedWrestlerDTO> items = grid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertEquals("Contender 2", items.get(0).getName());
    assertEquals("Contender 1", items.get(1).getName());
  }

  @Test
  void testTeamRankings() {
    List<RankedTeamDTO> teamContenders = new ArrayList<>();
    teamContenders.add(RankedTeamDTO.builder().id(1L).name("Team 1").fans(1500L).rank(1).build());
    teamContenders.add(RankedTeamDTO.builder().id(2L).name("Team 2").fans(1200L).rank(2).build());
    when(rankingService.getRankedContenders(championshipDTO.getId()))
        .thenAnswer(invocation -> teamContenders);

    RankingView view = new RankingView(rankingService, tierBoundaryService);
    ComboBox<ChampionshipDTO> comboBox = _get(view, ComboBox.class);
    comboBox.setValue(championshipDTO);

    Grid<RankedTeamDTO> teamGrid =
        _get(view, Grid.class, spec -> spec.withId("team-contenders-grid"));

    assertNotNull(teamGrid);
    List<RankedTeamDTO> items = teamGrid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertEquals("Team 1", items.get(0).getName());
    assertEquals("Team 2", items.get(1).getName());
  }

  @Test
  void testShowTierBoundariesButton() {
    RankingView view = new RankingView(rankingService, tierBoundaryService);
    Button button = _get(view, Button.class, spec -> spec.withId("show-tier-boundaries-button"));
    assertNotNull(button);
    button.click();
    assertNotNull(_get(Dialog.class));
  }
}
