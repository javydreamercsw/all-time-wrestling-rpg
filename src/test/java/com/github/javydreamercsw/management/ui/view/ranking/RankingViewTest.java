package com.github.javydreamercsw.management.ui.view.ranking;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.provider.Query;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RankingViewTest extends ManagementIntegrationTest {

  @Mock private RankingService rankingService;

  private ChampionshipDTO championshipDTO;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    MockVaadin.setup();
    championshipDTO = new ChampionshipDTO(1L, "Test Title", "test.png");
    when(rankingService.getChampionships()).thenReturn(List.of(championshipDTO));
    when(rankingService.getCurrentChampions(championshipDTO.getId()))
        .thenReturn(List.of(new ChampionDTO(1L, "Champion", 1000L, 1L)));
    List<RankedWrestlerDTO> contenders = new ArrayList<>();
    contenders.add(new RankedWrestlerDTO(2L, "Contender 2", 700L, 1));
    contenders.add(new RankedWrestlerDTO(3L, "Contender 1", 500L, 2));
    when(rankingService.getRankedContenders(championshipDTO.getId())).thenReturn(contenders);
  }

  @Test
  void testViewLoadsAndPopulates() {
    RankingView view = new RankingView(rankingService);
    assertNotNull(view);

    ComboBox<ChampionshipDTO> comboBox = _get(view, ComboBox.class);
    assertEquals(1, comboBox.getDataProvider().size(new Query<>()));

    Image image = _get(view, Image.class);
    assertEquals("championship-image", image.getId().get());
    assertNotNull(image);
    assertEquals("images/championships/test.png", image.getSrc());

    comboBox.setValue(championshipDTO);

    Grid<RankedWrestlerDTO> grid = _get(view, Grid.class);
    List<RankedWrestlerDTO> items = grid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertEquals("Contender 2", items.get(0).getName());
    assertEquals("Contender 1", items.get(1).getName());
  }
}
