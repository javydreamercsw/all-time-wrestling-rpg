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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.data.provider.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

class EditSegmentDialogTest extends ManagementIntegrationTest {

  @MockBean private WrestlerService wrestlerService;
  @MockBean private TitleService titleService;
  @MockBean private DataInitializer dataInitializer; // Exclude DataInitializer
  private ProposedSegment segment;
  private Runnable onSave;

  @BeforeEach
  void setUp() {
    segment = new ProposedSegment();
    segment.setDescription("Old Description");
    segment.setParticipants(new ArrayList<>(List.of("Wrestler 1")));

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");
    List<Wrestler> allWrestlers = Arrays.asList(wrestler1, wrestler2);

    when(wrestlerService.findAll()).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));

    // Mock TitleService and available titles
    Title title1 = new Title(); // Use no-arg constructor
    title1.setId(1L);
    title1.setName("Test Title 1");
    Title title2 = new Title(); // Use no-arg constructor
    title2.setId(2L);
    title2.setName("Test Title 2");
    when(titleService.findAll()).thenReturn(List.of(title1, title2));

    onSave = mock(Runnable.class);
  }

  @Test
  void testSave() {
    EditSegmentDialog dialog =
        new EditSegmentDialog(segment, wrestlerService, titleService, onSave);
    dialog.open();

    // Simulate user input
    dialog.getDescriptionArea().setValue("New Description");
    segment.setDescription("New Description");
    Set<Wrestler> selectedParticipants = Set.of(wrestlerService.findByName("Wrestler 2").get());
    dialog.getParticipantsCheckboxGroup().setValue(selectedParticipants);
    // Trigger save
    dialog.save();

    // Verify segment is updated
    ProposedSegment updatedSegment = dialog.getSegment();
    assertEquals("New Description", updatedSegment.getDescription());
    assertEquals(1, updatedSegment.getParticipants().size());
    assertEquals("Wrestler 2", updatedSegment.getParticipants().get(0));
    // Verify that no titles were selected if it's not a title segment
    assertTrue(updatedSegment.getTitles().isEmpty());
    assertEquals(false, updatedSegment.getIsTitleSegment());

    // Verify onSave is called and dialog is closed
    verify(onSave).run();
  }

  @Test
  void testSaveWithTitles() {
    // Set segment to be a title segment and pre-select titles
    Title title1 = new Title(); // Use no-arg constructor
    title1.setId(1L);
    title1.setName("Test Title 1");
    Title title2 = new Title(); // Use no-arg constructor
    title2.setId(2L);
    title2.setName("Test Title 2");
    segment.setTitles(Set.of(title1, title2)); // Set initial titles

    EditSegmentDialog dialog =
        new EditSegmentDialog(segment, wrestlerService, titleService, onSave);
    dialog.open();

    // Verify title MultiSelectComboBox is visible and populated
    assertTrue(dialog.getTitleMultiSelectComboBox().isVisible());
    assertEquals(2, dialog.getTitleMultiSelectComboBox().getDataProvider().size(new Query<>()));

    // Simulate user selecting only title1
    dialog.getTitleMultiSelectComboBox().setValue(Set.of(title1));
    segment.setTitles(Set.of(title1));

    // Trigger save
    dialog.save();

    // Verify segment is updated with selected titles
    ProposedSegment updatedSegment = dialog.getSegment();
    assertEquals(1, updatedSegment.getTitles().size());
    assertEquals("Test Title 1", updatedSegment.getTitles().iterator().next().getName());
    assertEquals(true, updatedSegment.getIsTitleSegment());

    // Verify onSave is called and dialog is closed
    verify(onSave).run();
  }
}
