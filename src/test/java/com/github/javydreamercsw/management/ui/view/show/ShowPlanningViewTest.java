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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.vaadin.flow.component.UI;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class ShowPlanningViewTest {

  @Mock private ShowService showService;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private ShowPlanningView showPlanningView;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Mock the UI since we are not in a Vaadin environment
    UI ui = mock(UI.class);
    UI.setCurrent(ui);
    when(ui.getUI()).thenReturn(Optional.of(ui));
    ReflectionTestUtils.setField(showPlanningView, "restTemplate", restTemplate);
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
    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setName("Test Rivalry");
    context.setCurrentRivalries(List.of(rivalry));
    context.setRecentPromos(new ArrayList<>());
    context.setRecentSegments(new ArrayList<>());

    // Mock the RestTemplate
    when(restTemplate.getForObject(any(String.class), eq(ShowPlanningContextDTO.class)))
        .thenReturn(context);

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
    segment1.setDescription("A vs B");
    segment1.setParticipants(Arrays.asList("A", "B"));
    ProposedSegment segment2 = new ProposedSegment();
    segment2.setType("Promo");
    segment2.setDescription("C talks");
    segment2.setParticipants(List.of("C"));
    proposedShow.setSegments(Arrays.asList(segment1, segment2));

    // Mock the RestTemplate
    when(restTemplate.getForObject(any(String.class), eq(ShowPlanningContextDTO.class)))
        .thenReturn(context);
    when(restTemplate.postForObject(
            any(String.class), any(ShowPlanningContextDTO.class), eq(ProposedShow.class)))
        .thenReturn(proposedShow);

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
    segment1.setDescription("A vs B");
    segment1.setParticipants(Arrays.asList("A", "B"));
    proposedShow.setSegments(List.of(segment1));

    // Mock the RestTemplate
    when(restTemplate.getForObject(any(String.class), eq(ShowPlanningContextDTO.class)))
        .thenReturn(context);
    when(restTemplate.postForObject(
            any(String.class), any(ShowPlanningContextDTO.class), eq(ProposedShow.class)))
        .thenReturn(proposedShow);

    // Mock the ObjectMapper
    ObjectMapper objectMapper = new ObjectMapper();
    ReflectionTestUtils.setField(showPlanningView, "objectMapper", objectMapper);

    // Call the method to be tested
    showPlanningView.setParameter(mock(BeforeEvent.class), showId);

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
}
