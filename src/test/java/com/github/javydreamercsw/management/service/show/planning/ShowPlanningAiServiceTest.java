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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowPlanningAiServiceTest {

  private ShowPlanningAiService showPlanningAiService;
  private SegmentNarrationServiceFactory narrationServiceFactory;
  private SegmentNarrationService segmentNarrationService;
  private SegmentRuleService segmentRuleService;

  @BeforeEach
  void setUp() {
    narrationServiceFactory = mock(SegmentNarrationServiceFactory.class);
    segmentNarrationService = mock(SegmentNarrationService.class);
    segmentRuleService = mock(SegmentRuleService.class);
    ObjectMapper objectMapper = new ObjectMapper(); // Use real ObjectMapper for JSON parsing
    SegmentTypeService segmentTypeService = mock(SegmentTypeService.class);

    when(narrationServiceFactory.getBestAvailableService()).thenReturn(segmentNarrationService);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("One on One");
    when(segmentTypeService.findAll()).thenReturn(java.util.List.of(segmentType));

    showPlanningAiService =
        new ShowPlanningAiService(
            narrationServiceFactory, objectMapper, segmentTypeService, segmentRuleService);
  }

  @Test
  void planShow() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);

    // Mock AI response
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Main Event: John Cena vs Randy Orton",
            "outcome": "John Cena wins"
          },
          {
            "segmentId": "seg2",
            "type": "Promo",
            "description": "CM Punk cuts a promo on the Authority",
            "outcome": "Crowd boos Authority"
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
    assertFalse(proposedShow.getSegments().isEmpty());
    assertEquals(2, proposedShow.getSegments().size());

    ProposedSegment segment1 = proposedShow.getSegments().get(0);
    assertEquals("One on One", segment1.getType());
    assertEquals("Main Event: John Cena vs Randy Orton", segment1.getDescription());

    ProposedSegment segment2 = proposedShow.getSegments().get(1);
    assertEquals("Promo", segment2.getType());
    assertEquals("CM Punk cuts a promo on the Authority", segment2.getDescription());

    // Verify that the AI service was called
    verify(segmentNarrationService, times(1)).generateText(anyString());
  }

  @Test
  void planShow_noAiServiceAvailable_returnsEmptyShow() {
    // Given
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(null);
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
    assertTrue(proposedShow.getSegments().isEmpty());
    verify(segmentNarrationService, never()).generateText(anyString());
  }

  @Test
  void planShow_aiReturnsEmptyResponse_returnsEmptyShow() {
    // Given
    when(segmentNarrationService.generateText(anyString())).thenReturn("");
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
    assertTrue(proposedShow.getSegments().isEmpty());
    verify(segmentNarrationService, times(1)).generateText(anyString());
  }

  @Test
  void planShow_aiReturnsInvalidJson_returnsEmptyShow() {
    // Given
    when(segmentNarrationService.generateText(anyString())).thenReturn("invalid json");
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
    assertTrue(proposedShow.getSegments().isEmpty());
    verify(segmentNarrationService, times(1)).generateText(anyString());
  }
}
