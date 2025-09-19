package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowPlanningAiServiceTest {

  private ShowPlanningAiService showPlanningAiService;
  private SegmentNarrationServiceFactory narrationServiceFactory;
  private SegmentNarrationService segmentNarrationService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    narrationServiceFactory = mock(SegmentNarrationServiceFactory.class);
    segmentNarrationService = mock(SegmentNarrationService.class);
    objectMapper = new ObjectMapper(); // Use real ObjectMapper for JSON parsing

    when(narrationServiceFactory.getBestAvailableService()).thenReturn(segmentNarrationService);

    showPlanningAiService = new ShowPlanningAiService(narrationServiceFactory, objectMapper);
  }

  @Test
  void planShow() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    // Populate context with some dummy data if needed for prompt building
    // context.setShowName("Test Show");
    // context.setShowDescription("A test show description");

    // Mock AI response
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "match",
            "description": "Main Event: John Cena vs Randy Orton",
            "outcome": "John Cena wins"
          },
          {
            "segmentId": "seg2",
            "type": "promo",
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
    assertEquals("match", segment1.getType());
    assertEquals("Main Event: John Cena vs Randy Orton", segment1.getDescription());

    ProposedSegment segment2 = proposedShow.getSegments().get(1);
    assertEquals("promo", segment2.getType());
    assertEquals("CM Punk cuts a promo on the Authority", segment2.getDescription());

    // Verify that the AI service was called
    verify(segmentNarrationService, times(1)).generateText(anyString());
  }

  @Test
  void planShow_noAiServiceAvailable_returnsEmptyShow() {
    // Given
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(null);
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();

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

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
    assertTrue(proposedShow.getSegments().isEmpty());
    verify(segmentNarrationService, times(1)).generateText(anyString());
  }
}
