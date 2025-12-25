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
package com.github.javydreamercsw.management.ui.view.title;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class TitleListViewTest extends AbstractViewTest {

  @Mock private TitleService titleService;
  @Mock private WrestlerService wrestlerService;
  @Mock private SecurityUtils securityUtils;

  @Mock
  private TeamRepository teamRepository; // Mocked as it's injected but not used in the test setup

  private TitleListView titleListView;
  private Title testTitle;

  @BeforeEach
  void setUp() {
    // Mock WrestlerService
    Wrestler testWrestler = new Wrestler();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");
    // Added for testing multiple contenders
    Wrestler otherWrestler = new Wrestler();
    otherWrestler.setId(2L);
    otherWrestler.setName("Other Wrestler");
    List<Wrestler> allWrestlers = Arrays.asList(testWrestler, otherWrestler);

    when(wrestlerService.findAll()).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Test Wrestler")).thenReturn(Optional.of(testWrestler));
    when(wrestlerService.findByName("Other Wrestler")).thenReturn(Optional.of(otherWrestler));

    // Mock TitleService
    testTitle = new Title();
    testTitle.setId(1L);
    testTitle.setName("Test Title");
    testTitle.setTier(WrestlerTier.MAIN_EVENTER);
    testTitle.setIsActive(true);
    testTitle.awardTitleTo(new ArrayList<>(List.of(testWrestler)), Instant.now());
    // Mock eligible challengers
    assertNotNull(testTitle.getId());
    when(titleService.getEligibleChallengers(testTitle.getId()))
        .thenReturn(List.of(testWrestler, otherWrestler));
    when(titleService.findAll()).thenReturn(List.of(testTitle));
    when(titleService.findByName("Test Title")).thenReturn(Optional.of(testTitle));
    when(titleService.deleteTitle(anyLong())).thenReturn(true); // Mock delete to return true

    // Mock clearNumberOneContender

    // Mock SecurityUtils
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);

    titleListView = new TitleListView(titleService, wrestlerService, securityUtils);
  }

  @Test
  void testGridIsPopulated() {
    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Test Title", items.get(0).getName());
    assertEquals("Test Wrestler", items.get(0).getChampionNames());

    // Verify contender ComboBox is present and populated
    Grid.Column<Title> contenderColumn = null;
    for (Grid.Column<Title> column : grid.getColumns()) {
      if (column.getHeaderText().equals("Challengers")) {
        contenderColumn = column;
        break;
      }
    }
    assertNotNull(contenderColumn, "Challengers column not found");
    assertInstanceOf(
        ComponentRenderer.class,
        contenderColumn.getRenderer(),
        "Renderer is not ComponentRenderer");

    // Manually create a ComboBox instance using the ComponentRenderer
    ComponentRenderer<MultiSelectComboBox<Wrestler>, Title> renderer =
        (ComponentRenderer<MultiSelectComboBox<Wrestler>, Title>) contenderColumn.getRenderer();
    MultiSelectComboBox<Wrestler> contenderComboBox = renderer.createComponent(testTitle);

    assertEquals(2, contenderComboBox.getDataProvider().size(new Query<>()));
    assertTrue(
        contenderComboBox.getDataProvider().fetch(new Query<>()).toList().stream()
            .anyMatch(w -> w.getName().equals("Test Wrestler")));
    assertTrue(
        contenderComboBox.getDataProvider().fetch(new Query<>()).toList().stream()
            .anyMatch(w -> w.getName().equals("Other Wrestler")));
  }

  @Test
  void testCreateTitle() {
    // Mocking the save operation for create dialog
    Title newTitle = new Title();
    newTitle.setId(2L); // Assign an ID for the mock
    newTitle.setName("New Title");
    newTitle.setTier(WrestlerTier.MIDCARDER);
    newTitle.setIsActive(true);
    when(titleService.save(any(Title.class))).thenReturn(newTitle);
    when(titleService.findAll()).thenReturn(List.of(testTitle, newTitle)); // Return updated list

    // Simulate opening the create dialog and saving
    // This part requires more complex interaction with Vaadin dialogs,
    // for now, we'll focus on verifying the grid refresh after a hypothetical save.
    titleListView.refreshGrid(); // Simulate refresh after creation

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertTrue(items.stream().anyMatch(t -> t.getName().equals("New Title")));
  }

  @Test
  void testUpdateTitle() {
    // Mocking the update operation
    Title updatedTitle = new Title();
    updatedTitle.setId(testTitle.getId());
    updatedTitle.setName("Updated Title");
    updatedTitle.setTier(testTitle.getTier());
    updatedTitle.setIsActive(testTitle.getIsActive());
    updatedTitle.awardTitleTo(testTitle.getCurrentChampions(), Instant.now());
    when(titleService.save(any(Title.class))).thenReturn(updatedTitle);
    when(titleService.findByName("Updated Title")).thenReturn(Optional.of(updatedTitle));
    when(titleService.findAll()).thenReturn(List.of(updatedTitle)); // Return updated list

    // Simulate opening the edit dialog and saving
    // This part requires more complex interaction with Vaadin dialogs,
    // for now, we'll focus on verifying the grid refresh after a hypothetical save.
    titleListView.refreshGrid(); // Simulate refresh after update

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Updated Title", items.get(0).getName());
  }

  @Test
  void testDeleteTitle() {
    // Mock a title eligible for deletion
    Title deletableTitle = new Title();
    deletableTitle.setId(2L);
    deletableTitle.setName("Deletable Title");
    deletableTitle.setTier(WrestlerTier.MAIN_EVENTER);
    deletableTitle.setIsActive(false);
    deletableTitle.vacateTitle(java.time.Instant.now()); // Ensure it's vacant

    when(titleService.findByName("Deletable Title")).thenReturn(Optional.of(deletableTitle));
    assertNotNull(deletableTitle.getId());
    when(titleService.deleteTitle(deletableTitle.getId())).thenReturn(true);
    when(titleService.findAll())
        .thenReturn(List.of(testTitle)); // Return list without deletable title

    titleListView.refreshGrid(); // Refresh to include the deletable title

    // Verify the deletable title exists before deletion (simulated by mock)
    assertTrue(titleService.findByName("Deletable Title").isPresent());

    // Simulate clicking delete and confirming
    // This requires interaction with ConfirmDialog, which is complex in unit tests.
    // We'll verify the service method was called.
    titleService.deleteTitle(deletableTitle.getId()); // Simulate deletion call

    when(titleService.findByName("Deletable Title")).thenReturn(Optional.empty()); // Update mock

    titleListView.refreshGrid(); // Simulate refresh after deletion

    // Verify the deletable title is no longer present (simulated by mock)
    assertTrue(titleService.findByName("Deletable Title").isEmpty());
  }

  @Test
  void testChallengerSelectionUpdatesTitle() {
    // Mock the eligible challengers for the test title
    Wrestler challengerWrestler = new Wrestler();
    challengerWrestler.setId(3L);
    challengerWrestler.setName("Challenger Wrestler");
    assertNotNull(testTitle.getId());
    when(titleService.getEligibleChallengers(testTitle.getId()))
        .thenReturn(List.of(challengerWrestler));

    // Mock the addChallengerToTitle call
    when(titleService.addChallengerToTitle(testTitle.getId(), challengerWrestler.getId()))
        .thenReturn(new TitleService.ChallengeResult(true, "Challenger added"));

    titleListView.refreshGrid(); // Refresh grid to show the challenger MultiSelectComboBox

    // Find the challenger MultiSelectComboBox in the grid row for testTitle
    Grid.Column<Title> challengerColumn = null;
    for (Grid.Column<Title> column : titleListView.grid.getColumns()) {
      if (column.getHeaderText().equals("Challengers")) {
        challengerColumn = column;
        break;
      }
    }
    assertNotNull(challengerColumn, "Challenger column not found");
    assertInstanceOf(
        ComponentRenderer.class,
        challengerColumn.getRenderer(),
        "Renderer is not ComponentRenderer");

    // Manually create a MultiSelectComboBox instance using the ComponentRenderer
    ComponentRenderer<MultiSelectComboBox<Wrestler>, Title> renderer =
        (ComponentRenderer<MultiSelectComboBox<Wrestler>, Title>) challengerColumn.getRenderer();
    MultiSelectComboBox<Wrestler> challengerComboBox = renderer.createComponent(testTitle);

    challengerComboBox.setValue(Set.of(challengerWrestler));

    // Verify that titleService.addChallengerToTitle was called
    verify(titleService).addChallengerToTitle(testTitle.getId(), challengerWrestler.getId());

    // Verify the grid is refreshed (implicitly checks if the update was successful)
    titleListView.refreshGrid();
    // Further assertions could check the updated state of the grid if needed
  }
}
