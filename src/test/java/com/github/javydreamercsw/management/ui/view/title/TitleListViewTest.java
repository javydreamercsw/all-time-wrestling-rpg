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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
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
import org.springframework.test.util.ReflectionTestUtils;

class TitleListViewTest extends AbstractViewTest {

  @Mock private TitleService titleService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private TierRecalculationService tierRecalculationService;

  private TitleListView titleListView;
  private Title testTitle;
  private Wrestler testWrestler;
  private Wrestler otherWrestler;

  @BeforeEach
  void setUp() {
    // Mock Wrestlers
    testWrestler = new Wrestler();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");
    testWrestler.setTier(WrestlerTier.MAIN_EVENTER);

    otherWrestler = new Wrestler();
    otherWrestler.setId(2L);
    otherWrestler.setName("Other Wrestler");
    otherWrestler.setTier(WrestlerTier.MIDCARDER);

    List<Wrestler> allWrestlers = Arrays.asList(testWrestler, otherWrestler);
    when(wrestlerRepository.findAll()).thenReturn(allWrestlers);
    when(wrestlerService.findAll()).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Test Wrestler")).thenReturn(Optional.of(testWrestler));
    when(wrestlerService.findByName("Other Wrestler")).thenReturn(Optional.of(otherWrestler));

    // Mock Title
    testTitle = new Title();
    testTitle.setId(1L);
    testTitle.setName("Test Title");
    testTitle.setTier(WrestlerTier.MAIN_EVENTER);
    testTitle.setIsActive(true);
    testTitle.awardTitleTo(new ArrayList<>(List.of(testWrestler)), Instant.now());

    when(titleService.findAll()).thenReturn(List.of(testTitle));
    when(titleService.findByName("Test Title")).thenReturn(Optional.of(testTitle));
    when(titleService.deleteTitle(anyLong())).thenReturn(true);
    when(titleService.isWrestlerEligible(any(Wrestler.class), any(Title.class)))
        .thenAnswer(
            invocation -> {
              Wrestler w = invocation.getArgument(0);
              Title t = invocation.getArgument(1);
              if (w.getTier() == null || t.getTier() == null) {
                return false;
              }
              return w.getTier().ordinal() >= t.getTier().ordinal();
            });

    // Mock SecurityUtils
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);

    // Mock UI
    UI.setCurrent(new UI());

    titleListView =
        new TitleListView(
            titleService,
            wrestlerService,
            wrestlerRepository,
            tierRecalculationService,
            securityUtils);
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
  void testCreateTitleWithChampion() {
    TitleFormDialog dialog = titleListView.openCreateDialog();
    dialog.open();

    // Simulate user input
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "name");
    assertNotNull(nameField);
    nameField.setValue("New Title");

    ComboBox<WrestlerTier> tierComboBox =
        (ComboBox<WrestlerTier>) ReflectionTestUtils.getField(dialog, "tier");
    assertNotNull(tierComboBox);
    tierComboBox.setValue(WrestlerTier.MIDCARDER);

    MultiSelectComboBox<Wrestler> championComboBox =
        (MultiSelectComboBox<Wrestler>) ReflectionTestUtils.getField(dialog, "champion");
    assertNotNull(championComboBox);
    championComboBox.setValue(Set.of(otherWrestler));

    Button saveButton = (Button) ReflectionTestUtils.getField(dialog, "saveButton");
    assertNotNull(saveButton);

    // Mocking the save operation
    Title newTitle = new Title();
    newTitle.setId(2L);
    newTitle.setName("New Title");
    newTitle.setTier(WrestlerTier.MIDCARDER);
    newTitle.setIsActive(true);
    newTitle.awardTitleTo(new ArrayList<>(List.of(otherWrestler)), Instant.now());

    when(titleService.save(any(Title.class))).thenReturn(newTitle);
    when(titleService.findAll()).thenReturn(List.of(testTitle, newTitle));

    saveButton.click();

    // Verify grid is refreshed and contains the new title with the correct champion
    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    Optional<Title> createdTitle =
        items.stream().filter(t -> t.getName().equals("New Title")).findFirst();
    assertTrue(createdTitle.isPresent());
    assertEquals("Other Wrestler", createdTitle.get().getChampionNames());
  }

  @Test
  void testUpdateTitleAndChampion() {
    TitleFormDialog dialog = titleListView.openEditDialog(testTitle);
    dialog.open();

    // Simulate user input
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "name");
    assertNotNull(nameField);
    nameField.setValue("Updated Title");

    ComboBox<WrestlerTier> tierComboBox =
        (ComboBox<WrestlerTier>) ReflectionTestUtils.getField(dialog, "tier");
    assertNotNull(tierComboBox);
    tierComboBox.setValue(WrestlerTier.MIDCARDER);

    MultiSelectComboBox<Wrestler> championComboBox =
        (MultiSelectComboBox<Wrestler>) ReflectionTestUtils.getField(dialog, "champion");
    assertNotNull(championComboBox);
    championComboBox.setValue(Set.of(otherWrestler));

    Button saveButton = (Button) ReflectionTestUtils.getField(dialog, "saveButton");
    assertNotNull(saveButton);

    // Mocking the update operation
    Title updatedTitle = new Title();
    updatedTitle.setId(testTitle.getId());
    updatedTitle.setName("Updated Title");
    updatedTitle.setTier(WrestlerTier.MIDCARDER);
    updatedTitle.setIsActive(testTitle.getIsActive());
    updatedTitle.awardTitleTo(new ArrayList<>(List.of(otherWrestler)), Instant.now());

    when(titleService.save(any(Title.class))).thenReturn(updatedTitle);
    when(titleService.findAll()).thenReturn(List.of(updatedTitle));

    saveButton.click();

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Updated Title", items.get(0).getName());
    assertEquals("Other Wrestler", items.get(0).getChampionNames());
  }

  @Test
  void testDeleteTitle() {
    when(titleService.findAll()).thenReturn(new ArrayList<>()); // Return empty list after deletion

    // Simulate deletion
    titleService.deleteTitle(testTitle.getId());
    titleListView.refreshGrid();

    Grid<Title> grid = titleListView.grid;
    List<Title> items = grid.getGenericDataView().getItems().toList();
    assertEquals(0, items.size());
    verify(titleService, times(1)).deleteTitle(testTitle.getId());
  }

  @Test
  void testChallengerSelection() {
    // Find the challenger MultiSelectComboBox in the grid
    Grid.Column<Title> challengerColumn =
        titleListView.grid.getColumns().stream()
            .filter(col -> "Challengers".equals(col.getHeaderText()))
            .findFirst()
            .orElse(null);
    assertNotNull(challengerColumn);

    // Use ComponentRenderer to create the component for the specific item
    ComponentRenderer<MultiSelectComboBox<Wrestler>, Title> renderer =
        (ComponentRenderer<MultiSelectComboBox<Wrestler>, Title>) challengerColumn.getRenderer();
    MultiSelectComboBox<Wrestler> challengerComboBox = renderer.createComponent(testTitle);

    // Simulate selecting a challenger
    challengerComboBox.setValue(Set.of(otherWrestler));

    // Verify that the service method was called
    verify(titleService).addChallengerToTitle(testTitle.getId(), otherWrestler.getId());
  }

  @Test
  void testChampionDropdownIsPopulatedBasedOnTier() {
    TitleFormDialog dialog = titleListView.openCreateDialog();
    dialog.open();

    // Get components from dialog
    ComboBox<WrestlerTier> tierComboBox =
        (ComboBox<WrestlerTier>) ReflectionTestUtils.getField(dialog, "tier");
    assertNotNull(tierComboBox);
    MultiSelectComboBox<Wrestler> championComboBox =
        (MultiSelectComboBox<Wrestler>) ReflectionTestUtils.getField(dialog, "champion");
    assertNotNull(championComboBox);

    // Simulate selecting a tier
    tierComboBox.setValue(WrestlerTier.MIDCARDER);

    // Verify the champion dropdown is populated correctly
    List<Wrestler> championItems =
        new ArrayList<>(championComboBox.getDataProvider().fetch(new Query<>()).toList());
    assertEquals(2, championItems.size()); // Main Eventer and Midcarder
    assertTrue(championItems.contains(testWrestler));
    assertTrue(championItems.contains(otherWrestler));

    // Simulate selecting a lower tier
    tierComboBox.setValue(WrestlerTier.ROOKIE);
    championItems =
        new ArrayList<>(championComboBox.getDataProvider().fetch(new Query<>()).toList());
    assertEquals(2, championItems.size()); // All wrestlers are eligible for a rookie title
  }
}
