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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class ShowPlanningViewTest {

  @Mock private ShowService showService;
  @Mock private ShowPlanningService showPlanningService;
  @Mock private ShowPlanningAiService showPlanningAiService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  private ShowPlanningView showPlanningView;
  @Mock private TitleService titleService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private com.github.javydreamercsw.management.service.npc.NpcService npcService;
  @Mock private ObjectMapper objectMapper;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private ArenaService arenaService;

  @BeforeEach
  void setUp() {
    aiFactory = mock(SegmentNarrationServiceFactory.class);
    MockitoAnnotations.openMocks(this);
    showPlanningView =
        new ShowPlanningView(
            showService,
            showPlanningService,
            showPlanningAiService,
            wrestlerService,
            wrestlerRepository,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            objectMapper,
            aiFactory,
            arenaService);
    // Mock the UI since we are not in a Vaadin environment
    UI ui = mock(UI.class);
    UI.setCurrent(ui);
    when(ui.getUI()).thenReturn(Optional.of(ui));
  }

  @Test
  void testLoadContextWithRivalries() throws Exception {
    // Create a mock Show
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");

    // Mock the ComboBox to return the mock Show
    @SuppressWarnings("unchecked")
    ComboBox<Show> showComboBox = mock(ComboBox.class);
    when(showComboBox.getValue()).thenReturn(show);
    ReflectionTestUtils.setField(showPlanningView, "showComboBox", showComboBox);

    // Create a mock ShowPlanningContext
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    when(showPlanningService.getShowPlanningContext(show)).thenReturn(context);

    // Mock the ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper();
    ReflectionTestUtils.setField(showPlanningView, "objectMapper", objectMapper);

    // Call the method to be tested
    ReflectionTestUtils.invokeMethod(showPlanningView, "loadContext");

    // Verify the results
    TextArea contextArea = (TextArea) ReflectionTestUtils.getField(showPlanningView, "contextArea");
    assertEquals(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context),
        contextArea.getValue());
  }

  @Test
  void loadContextTest() throws Exception {
    // Create a mock Show
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");

    // Mock the ComboBox to return the mock Show
    @SuppressWarnings("unchecked")
    ComboBox<Show> showComboBox = mock(ComboBox.class);
    when(showComboBox.getValue()).thenReturn(show);
    ReflectionTestUtils.setField(showPlanningView, "showComboBox", showComboBox);

    // Create a mock ShowPlanningContext
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    context.setCurrentRivalries(new ArrayList<>());
    context.setRecentPromos(new ArrayList<>());
    context.setRecentSegments(new ArrayList<>());

    // Create a mock ProposedShow
    ProposedShow proposedShow = new ProposedShow();
    ProposedSegment segment1 = new ProposedSegment();
    segment1.setType("Match");
    segment1.setNarration("A vs B");
    segment1.setParticipants(Arrays.asList("A", "B"));
    ProposedSegment segment2 = new ProposedSegment();
    segment2.setType("Promo");
    segment2.setNarration("C talks");
    segment2.setParticipants(List.of("C"));
    proposedShow.setSegments(Arrays.asList(segment1, segment2));

    when(showPlanningService.getShowPlanningContext(show)).thenReturn(context);
    when(showPlanningAiService.planShow(any(ShowPlanningContextDTO.class)))
        .thenReturn(proposedShow);

    // Mock the ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper();
    ReflectionTestUtils.setField(showPlanningView, "objectMapper", objectMapper);

    // Ensure the grid is initialized if not already
    Grid<ProposedSegment> grid =
        (Grid<ProposedSegment>)
            ReflectionTestUtils.getField(showPlanningView, "proposedSegmentsGrid");
    if (grid == null) {
      grid = new Grid<>(ProposedSegment.class);
      ReflectionTestUtils.setField(showPlanningView, "proposedSegmentsGrid", grid);
    }
    // Ensure AI factory returns a non-empty list so proposeSegments() does not return early
    when(aiFactory.getAvailableServicesInPriorityOrder())
        .thenReturn(List.of(mock(SegmentNarrationService.class)));
    // Call the method to be tested
    ReflectionTestUtils.invokeMethod(showPlanningView, "loadContext");
    ReflectionTestUtils.invokeMethod(showPlanningView, "proposeSegments");

    // Verify the results
    TextArea contextArea = (TextArea) ReflectionTestUtils.getField(showPlanningView, "contextArea");
    assertEquals(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context),
        contextArea.getValue());

    @SuppressWarnings("unchecked")
    Grid<ProposedSegment> proposedSegmentsGrid =
        (Grid<ProposedSegment>)
            ReflectionTestUtils.getField(showPlanningView, "proposedSegmentsGrid");
    List<ProposedSegment> items = proposedSegmentsGrid.getGenericDataView().getItems().toList();
    assertEquals(2, items.size());
    assertEquals("Match", items.get(0).getType());
    assertEquals("Promo", items.get(1).getType());
  }

  @Test
  void setParameterTest() throws Exception {
    // Create a mock Show
    long showId = 1L;
    Show show = new Show();
    show.setId(showId);
    show.setName("Test Show");

    // Mock the ShowService to return the mock Show
    when(showService.getShowById(showId)).thenReturn(Optional.of(show));

    // Create a mock ShowPlanningContext
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    context.setCurrentRivalries(new ArrayList<>());
    context.setRecentPromos(new ArrayList<>());
    context.setRecentSegments(new ArrayList<>());

    // Create a mock ProposedShow
    ProposedShow proposedShow = new ProposedShow();
    ProposedSegment segment1 = new ProposedSegment();
    segment1.setType("Match");
    segment1.setNarration("A vs B");
    segment1.setParticipants(Arrays.asList("A", "B"));
    proposedShow.setSegments(List.of(segment1));

    when(showPlanningService.getShowPlanningContext(show)).thenReturn(context);
    when(showPlanningAiService.planShow(any(ShowPlanningContextDTO.class)))
        .thenReturn(proposedShow);

    // Mock the ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper();
    ReflectionTestUtils.setField(showPlanningView, "objectMapper", objectMapper);

    // Ensure the grid is initialized if not already
    Grid<ProposedSegment> grid =
        (Grid<ProposedSegment>)
            ReflectionTestUtils.getField(showPlanningView, "proposedSegmentsGrid");
    if (grid == null) {
      grid = new Grid<>(ProposedSegment.class);
      ReflectionTestUtils.setField(showPlanningView, "proposedSegmentsGrid", grid);
    }
    // Ensure AI factory returns a non-empty list so proposeSegments() does not return early
    when(aiFactory.getAvailableServicesInPriorityOrder())
        .thenReturn(List.of(mock(SegmentNarrationService.class)));
    // Call the method to be tested
    showPlanningView.setParameter(mock(BeforeEvent.class), showId);
    ReflectionTestUtils.invokeMethod(showPlanningView, "proposeSegments");

    // Debug: Print grid contents
    @SuppressWarnings("unchecked")
    Grid<ProposedSegment> debugGrid =
        (Grid<ProposedSegment>)
            ReflectionTestUtils.getField(showPlanningView, "proposedSegmentsGrid");
    List<ProposedSegment> debugItems = debugGrid.getGenericDataView().getItems().toList();
    System.out.println("DEBUG: ProposedSegment grid items after proposeSegments(): " + debugItems);

    // Verify the results
    @SuppressWarnings("unchecked")
    ComboBox<Show> showComboBox =
        (ComboBox<Show>) ReflectionTestUtils.getField(showPlanningView, "showComboBox");
    assertEquals(show, showComboBox.getValue());

    TextArea contextArea = (TextArea) ReflectionTestUtils.getField(showPlanningView, "contextArea");
    assertEquals(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context),
        contextArea.getValue());

    @SuppressWarnings("unchecked")
    Grid<ProposedSegment> proposedSegmentsGrid =
        (Grid<ProposedSegment>)
            ReflectionTestUtils.getField(showPlanningView, "proposedSegmentsGrid");
    List<ProposedSegment> items = proposedSegmentsGrid.getGenericDataView().getItems().toList();
    assertEquals(1, items.size());
    assertEquals("Match", items.get(0).getType());
  }

  @Test
  void testNavigateToShowDetails() {
    // Create a mock Show
    long showId = 1L;
    Show show = new Show();
    show.setId(showId);
    show.setName("Test Show");

    // Mock the ComboBox to return the mock Show
    @SuppressWarnings("unchecked")
    ComboBox<Show> showComboBox =
        (ComboBox<Show>) ReflectionTestUtils.getField(showPlanningView, "showComboBox");
    showComboBox.setValue(show);

    // Mock the UI to capture the navigation call
    UI ui = mock(UI.class);
    UI.setCurrent(ui);

    // Get the button and click it
    Button viewDetailsButton =
        (Button) ReflectionTestUtils.getField(showPlanningView, "viewDetailsButton");
    viewDetailsButton.click();

    // Verify that navigate was called with the correct parameters
    verify(ui).navigate(ShowDetailView.class, showId);
  }
}
