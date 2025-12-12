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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningPleDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

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
    segmentType.setDescription("A standard wrestling match between two competitors.");
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
    showTemplate.setShowName("Test Show");
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 12, 25).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningPleDTO ple = new ShowPlanningPleDTO();
    ple.setPleName("WrestleMania");
    ple.setPleDate(Instant.now());
    ple.setSummary("The biggest event of the year!");
    ShowPlanningSegmentDTO match = new ShowPlanningSegmentDTO();
    match.setName("Championship Match");
    match.setParticipants(List.of("Roman Reigns", "Cody Rhodes"));
    ple.setMatches(List.of(match));
    context.setNextPle(ple);

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setName("Test Rivalry");
    rivalry.setHeat(30);
    rivalry.setParticipants(List.of("Wrestler A", "Wrestler B"));
    context.setCurrentRivalries(List.of(rivalry));

    SegmentRule highHeatRule = new SegmentRule();
    highHeatRule.setName("Steel Cage");
    highHeatRule.setDescription("A match fought within a steel cage.");
    when(segmentRuleService.getHighHeatRules()).thenReturn(List.of(highHeatRule));

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

    // Verify that the AI service was called and capture the prompt
    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    verify(segmentNarrationService, times(1)).generateText(promptCaptor.capture());

    String capturedPrompt = promptCaptor.getValue();
    assertTrue(
        capturedPrompt.contains(
            "Available Segment Types: One on One (A standard wrestling match between two"
                + " competitors.)"));
    assertTrue(capturedPrompt.contains("Next PLE (Premium Live Event):"));
    assertTrue(capturedPrompt.contains("- Name: WrestleMania"));
    assertTrue(capturedPrompt.contains("  Scheduled Matches:"));
    assertTrue(
        capturedPrompt.contains(
            "  - Name: Championship Match, Participants: Roman Reigns, Cody Rhodes"));
    assertTrue(capturedPrompt.contains("Holiday Theme: Christmas Day"));
    assertTrue(
        capturedPrompt.contains(
            "Available Stipulation Matches: Steel Cage (A match fought within a steel cage.)"));
  }

  @Test
  void planShow_noAiServiceAvailable_returnsEmptyShow() {
    Logger logger = (Logger) LoggerFactory.getLogger(ShowPlanningAiService.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
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
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void planShow_aiReturnsEmptyResponse_returnsEmptyShow() {
    Logger logger = (Logger) LoggerFactory.getLogger(ShowPlanningAiService.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
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
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void planShow_aiReturnsInvalidJson_throwsException() {
    Logger logger = (Logger) LoggerFactory.getLogger(ShowPlanningAiService.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      // Given
      when(segmentNarrationService.generateText(anyString())).thenReturn("invalid json");
      ShowPlanningContextDTO context = new ShowPlanningContextDTO();
      ShowTemplate showTemplate = new ShowTemplate();
      showTemplate.setExpectedMatches(2);
      showTemplate.setExpectedPromos(1);
      context.setShowTemplate(showTemplate);

      // When & Then
      assertThrows(ShowPlanningException.class, () -> showPlanningAiService.planShow(context));
      verify(segmentNarrationService, times(1)).generateText(anyString());
    } finally {
      logger.setLevel(originalLevel);
    }
  }
}
