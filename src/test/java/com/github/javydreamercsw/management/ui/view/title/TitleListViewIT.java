package com.github.javydreamercsw.management.ui.view.title;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TitleListViewIT extends AbstractIntegrationTest {

  @Autowired private TitleService titleService;

  @Autowired private WrestlerService wrestlerService;

  private TitleListView titleListView;

  @BeforeEach
  void setUp() {
    titleRepository.deleteAll();
    wrestlerRepository.deleteAll();

    Wrestler wrestler = createTestWrestler("Test Wrestler");
    wrestlerService.save(wrestler);

    Title title = new Title();
    title.setName("Test Title");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setIsActive(true);
    title.setChampion(List.of(wrestler));
    titleService.save(title);

    titleListView = new TitleListView(titleService, wrestlerService);
  }

  @Test
  void testGridIsPopulated() {
    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Test Title", items.get(0).getName());
    assertEquals("Test Wrestler", items.get(0).getChampionNames());
  }

  @Test
  void testCreateTitle() {
    Title newTitle = new Title();
    newTitle.setName("New Title");
    newTitle.setTier(WrestlerTier.MIDCARDER);
    newTitle.setIsActive(true);
    titleService.save(newTitle);

    titleListView.refreshGrid();

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertTrue(items.stream().anyMatch(t -> t.getName().equals("New Title")));
  }

  @Test
  void testUpdateTitle() {
    Title title = titleService.findByName("Test Title").get();
    title.setName("Updated Title");
    titleService.save(title);

    titleListView.refreshGrid();

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Updated Title", items.get(0).getName());
  }

  @Test
  void testDeleteTitle() {
    // Create a title that is eligible for deletion (inactive and vacant)
    Title deletableTitle = new Title();
    deletableTitle.setName("Deletable Title");
    deletableTitle.setTier(WrestlerTier.MAIN_EVENTER);
    deletableTitle.setIsActive(false);
    deletableTitle.vacateTitle();
    titleService.save(deletableTitle);

    titleListView.refreshGrid(); // Refresh to include the deletable title

    // Verify the deletable title exists before deletion
    assertTrue(titleService.findByName("Deletable Title").isPresent());

    titleService.deleteTitle(deletableTitle.getId());

    titleListView.refreshGrid();

    // Verify the deletable title is no longer present
    assertTrue(titleService.findByName("Deletable Title").isEmpty());
  }
}
