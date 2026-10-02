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
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningPleDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningSegmentDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.TournamentSlotPreviewDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

class ShowPlanningAiServiceTest {

  private ShowPlanningAiService showPlanningAiService;
  private SegmentNarrationServiceFactory narrationServiceFactory;
  private SegmentNarrationService segmentNarrationService;
  private SegmentRuleService segmentRuleService;
  private HolidayService holidayService;
  private SegmentTypeService segmentTypeService;

  @BeforeEach
  public void setUp() {
    narrationServiceFactory = mock(SegmentNarrationServiceFactory.class);
    segmentNarrationService = mock(SegmentNarrationService.class);
    segmentRuleService = mock(SegmentRuleService.class);
    holidayService = mock(HolidayService.class);
    ObjectMapper objectMapper = new ObjectMapper(); // Use real ObjectMapper for JSON parsing
    segmentTypeService = mock(SegmentTypeService.class);

    // Mock the factory to return the service
    when(narrationServiceFactory.getBestAvailableService()).thenReturn(segmentNarrationService);
    // This is the new change to mock the generateText method
    when(narrationServiceFactory.generateText(anyString()))
        .thenAnswer(invocation -> segmentNarrationService.generateText(invocation.getArgument(0)));
    when(holidayService.getHolidayTheme(any(Instant.class)))
        .thenReturn(Optional.of("Christmas Day"));
    when(segmentRuleService.getStandardRules()).thenReturn(List.of());

    SegmentType segmentType = new SegmentType();
    segmentType.setName("One on One");
    segmentType.setDescription("A standard wrestling match between two competitors.");
    when(segmentTypeService.findAll()).thenReturn(List.of(segmentType));
    when(segmentTypeService.findByName(anyString())).thenReturn(Optional.of(segmentType));

    showPlanningAiService =
        new ShowPlanningAiService(
            narrationServiceFactory,
            objectMapper,
            segmentTypeService,
            segmentRuleService,
            holidayService);
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
            "outcome": "John Cena wins",
            "notes": "End with a dramatic finisher"
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
    assertEquals("Main Event: John Cena vs Randy Orton", segment1.getNarration());
    assertEquals("End with a dramatic finisher", segment1.getNotes());

    ProposedSegment segment2 = proposedShow.getSegments().get(1);
    assertEquals("Promo", segment2.getType());
    assertEquals("CM Punk cuts a promo on the Authority", segment2.getNarration());

    // Verify that the AI service was called and capture the prompt
    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());

    String capturedPrompt = promptCaptor.getValue();
    assertTrue(capturedPrompt.contains("\"notes\": \"string\""));
    assertTrue(capturedPrompt.contains("\"rules\""));
    assertTrue(
        capturedPrompt.contains(
            """
            Available Segment Types: One on One (A standard wrestling match between two\
             competitors.)\
            """));
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
    assertTrue(capturedPrompt.contains("Rivalry Classification Rules:"));
    assertTrue(capturedPrompt.contains("MUST_BOOK"));
    assertTrue(capturedPrompt.contains("PLE_RESOLUTION_ELIGIBLE"));
    assertTrue(capturedPrompt.contains("STIPULATION_REQUIRED"));
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
      verify(narrationServiceFactory, never()).generateText(anyString());
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
      when(narrationServiceFactory.generateText(anyString())).thenReturn("");
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
      verify(narrationServiceFactory, times(1)).generateText(anyString());
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
      when(narrationServiceFactory.generateText(anyString())).thenReturn("invalid json");
      ShowPlanningContextDTO context = new ShowPlanningContextDTO();
      ShowTemplate showTemplate = new ShowTemplate();
      showTemplate.setExpectedMatches(2);
      showTemplate.setExpectedPromos(1);
      context.setShowTemplate(showTemplate);

      // When & Then
      assertThrows(ShowPlanningException.class, () -> showPlanningAiService.planShow(context));
      verify(narrationServiceFactory, times(1)).generateText(anyString());
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void planShow_rivalryIdMappedFromAiResponse() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(42L);
    rivalry.setName("Blood Feud");
    rivalry.setHeat(25);
    rivalry.setParticipants(List.of("Wrestler A", "Wrestler B"));
    context.setCurrentRivalries(List.of(rivalry));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Blowoff match",
            "outcome": "Wrestler A wins",
            "participants": ["Wrestler A", "Wrestler B"],
            "rivalryId": 42
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(42L, proposedShow.getSegments().get(0).getRivalryId());
  }

  @Test
  void planShow_promptContainsRivalryIdSchema() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    // When
    showPlanningAiService.planShow(context);

    // Then
    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    assertTrue(promptCaptor.getValue().contains("\"rivalryId\""));
  }

  @Test
  void planShow_promptIncludesRivalryIdInRivalryListing() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(7L);
    rivalry.setName("Grudge Match");
    rivalry.setHeat(15);
    rivalry.setParticipants(List.of("Alpha", "Beta"));
    context.setCurrentRivalries(List.of(rivalry));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    // When
    showPlanningAiService.planShow(context);

    // Then
    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("- Id: 7"));
    assertTrue(prompt.contains("Name: Grudge Match"));
  }

  @Test
  void planShow_pleContextAddsBookingRulesInPrompt() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 4, 6).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setPremiumLiveEvent(true);

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    // When
    showPlanningAiService.planShow(context);

    // Then
    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("THIS IS A PREMIUM LIVE EVENT (PLE)"));
    assertTrue(prompt.contains("PLE-Specific Booking Rules"));
    assertTrue(prompt.contains("ALL rivalries at Heat"));
  }

  @Test
  void planShow_nonPleContextOmitsPleBookingRules() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 4, 6).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setPremiumLiveEvent(false);

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    // When
    showPlanningAiService.planShow(context);

    // Then
    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertFalse(prompt.contains("THIS IS A PREMIUM LIVE EVENT (PLE)"));
    assertFalse(prompt.contains("PLE-Specific Booking Rules"));
  }

  @Test
  void planShow_rivalryClassifiedAsMustBook_whenHeatBetween10And19() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(1L);
    rivalry.setName("Brewing Feud");
    rivalry.setHeat(15);
    rivalry.setParticipants(List.of("Alpha", "Beta"));
    context.setCurrentRivalries(List.of(rivalry));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("Classification: MUST_BOOK"));
  }

  @Test
  void planShow_rivalryClassifiedAsPleResolutionEligible_whenHeatBetween20And29() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(2L);
    rivalry.setName("Hot Feud");
    rivalry.setHeat(25);
    rivalry.setParticipants(List.of("Gamma", "Delta"));
    context.setCurrentRivalries(List.of(rivalry));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("Classification: PLE_RESOLUTION_ELIGIBLE"));
  }

  @Test
  void planShow_rivalryClassifiedAsPleResolutionRequired_whenHeatAtOrAbove30OnRegularShow() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(3L);
    rivalry.setName("Blood Feud");
    rivalry.setHeat(35);
    rivalry.setParticipants(List.of("Epsilon", "Zeta"));
    context.setCurrentRivalries(List.of(rivalry));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("Classification: PLE_RESOLUTION_REQUIRED"));
    assertFalse(prompt.contains("Classification: STIPULATION_REQUIRED"));
  }

  @Test
  void planShow_rivalryClassifiedAsStipulationRequired_whenHeatAtOrAbove30OnPle() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setPremiumLiveEvent(true);

    ShowPlanningRivalryDTO rivalry = new ShowPlanningRivalryDTO();
    rivalry.setId(3L);
    rivalry.setName("Blood Feud");
    rivalry.setHeat(35);
    rivalry.setParticipants(List.of("Epsilon", "Zeta"));
    context.setCurrentRivalries(List.of(rivalry));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertTrue(prompt.contains("Classification: STIPULATION_REQUIRED"));
    assertFalse(prompt.contains("Classification: PLE_RESOLUTION_REQUIRED"));
  }

  @Test
  void planShow_promptOmitsWrestlerHeatSection() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertFalse(prompt.contains("Wrestler Heat:"));
  }

  @Test
  void planShow_rulesFromAiResponseMappedToProposedSegment() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Steel cage blowoff",
            "outcome": "Wrestler A wins",
            "teams": [["Wrestler A"], ["Wrestler B"]],
            "teamIds": [[1], [2]],
            "rules": ["Steel Cage"]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(List.of("Steel Cage"), proposedShow.getSegments().get(0).getRules());
  }

  @Test
  void planShow_nullRulesFromAiResponse_proposedSegmentRulesNotSet() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Opening promo",
            "outcome": "Heat generated",
            "teams": [["Wrestler A"]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertEquals(1, proposedShow.getSegments().size());
    List<String> rules = proposedShow.getSegments().get(0).getRules();
    assertTrue(rules == null || rules.isEmpty(), "Rules must be null or empty when AI omits them");
  }

  @Test
  void planShow_promptOmitsRecentPromosSection() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
    showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(promptCaptor.capture());
    String prompt = promptCaptor.getValue();
    assertFalse(prompt.contains("Recent Promos"));
  }

  // ── deterministic scripted-beat enforcement (ATW-k3im) ───────────────────

  @Test
  void planShow_matchWithoutRules_getsNormalDefault() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentRule normalRule = new SegmentRule();
    normalRule.setName("Normal");
    when(segmentRuleService.findByName("Normal")).thenReturn(Optional.of(normalRule));

    // AI omits rules — nondeterministic per prompt guidance
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Big fight",
            "outcome": "Someone wins",
            "teams": [["A"], ["B"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(List.of("Normal"), proposedShow.getSegments().get(0).getRules());
  }

  @Test
  void planShow_promoWithoutRules_getsPromoDefault() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(0);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode("promo");
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));
    SegmentRule promoRule = new SegmentRule();
    promoRule.setName("Promo");
    when(segmentRuleService.findByName("Promo")).thenReturn(Optional.of(promoRule));

    // The prompt tells the AI to omit rules on promos — the default must come from the server.
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Talking segment",
            "outcome": "Crowd reacts",
            "teams": [["A"]],
            "teamIds": [[11]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(List.of("Promo"), proposedShow.getSegments().get(0).getRules());
  }

  @Test
  void planShow_explicitRulesAreKept() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Big fight",
            "outcome": "Someone wins",
            "rules": ["No DQ"],
            "teams": [["A"], ["B"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(List.of("No DQ"), proposedShow.getSegments().get(0).getRules());
  }

  private FeudScriptBeatDTO beat(
      String segmentType,
      String segmentRule,
      String winnerControl,
      String plannedWinner,
      List<Long> participantIds) {
    FeudScriptBeatDTO dto = new FeudScriptBeatDTO();
    dto.setBeatId(1L);
    dto.setScriptName("Lashley Arc");
    dto.setSegmentType(segmentType);
    dto.setSegmentRule(segmentRule);
    dto.setWinnerControl(winnerControl);
    dto.setPlannedWinnerName(plannedWinner);
    dto.setParticipantNames("Shelton Benjamin vs Bobby Lashley");
    dto.setParticipantIds(participantIds);
    dto.setRivalryId(9L);
    return dto;
  }

  @Test
  void planShow_aiBooksPromoForScriptedSlot_beatSegmentReplacesItWithMatchType() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "BOOKER_PICKS", "Bobby Lashley", List.of(11L, 12L))));

    // AI ignored the scripted slot and booked a promo for the arc's wrestlers
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Shelton Benjamin and Bobby Lashley have words",
            "outcome": "Tension rises",
            "teams": [["Shelton Benjamin"], ["Bobby Lashley"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Beat segment replaces the AI's promo: exact match type, stipulation, winner, rivalry
    assertEquals(1, proposedShow.getSegments().size());
    ProposedSegment beatSegment = proposedShow.getSegments().get(0);
    assertEquals("Singles Match", beatSegment.getType());
    assertEquals(List.of("Shelton Benjamin"), beatSegment.getTeams().get(0));
    assertEquals(List.of(11L), beatSegment.getTeamIds().get(0));
    assertEquals(9L, beatSegment.getRivalryId());
    assertEquals(List.of("Bobby Lashley"), beatSegment.getWinners());
    // The scripted slot gets a grid-facing summary like the AI's segments have.
    assertEquals(
        "Scripted beat: Lashley Arc (Singles Match) — planned winner: Bobby Lashley",
        beatSegment.getSummary());
  }

  @Test
  void planShow_aiBooksWrongMatchTypeForScriptedSlot_beatSegmentStillWins() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", "Steel Cage", "AI_PICKS", null, List.of(11L, 12L))));

    // AI booked the right participants but the wrong match type
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Standard match",
            "outcome": "Someone wins",
            "teams": [["Shelton Benjamin"], ["Bobby Lashley"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    ProposedSegment beatSegment = proposedShow.getSegments().get(0);
    assertEquals("Singles Match", beatSegment.getType());
    assertEquals(List.of("Steel Cage"), beatSegment.getRules());
  }

  @Test
  void planShow_aiPicksWinnerForScriptedSlot_bookerPicksOverridden() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(
            beat("Singles Match", null, "BOOKER_PICKS", "Shelton Benjamin", List.of(11L, 12L))));

    // AI agreed on a match but picked the wrong winner
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Big fight",
            "outcome": "Bobby Lashley wins",
            "teams": [["Shelton Benjamin"], ["Bobby Lashley"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(List.of("Shelton Benjamin"), proposedShow.getSegments().get(0).getWinners());
  }

  @Test
  void planShow_noScriptedBeats_cardUnchanged() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "A match",
            "outcome": "A wins",
            "teams": [["A"], ["B"]],
            "teamIds": [[1], [2]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals("One on One", proposedShow.getSegments().get(0).getType());
  }

  @Test
  void planShow_tournamentSlots_injectedAheadOfAiCard() {
    // Show-attached tournament slots (ATW-xbn4) preview on the card like scripted beats: a row
    // per round match / payoff, ahead of the AI's segments, with the Tournament source stamp.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    TournamentSlotPreviewDTO payoff = new TournamentSlotPreviewDTO();
    payoff.setTournamentName("Crown's Cup");
    payoff.setTypeName("Free-for-All");
    payoff.setRuleName("Tables, Ladders and Chairs (TLC)");
    payoff.setShape("Payoff final");
    payoff.setTitleName("ATW World");
    TournamentSlotPreviewDTO rounds = new TournamentSlotPreviewDTO();
    rounds.setTournamentName("Crown's Cup");
    rounds.setTypeName("One on One");
    rounds.setShape("2 round matches");
    context.setTournamentSlots(List.of(payoff, rounds));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "A match",
            "outcome": "A wins",
            "teams": [["A"], ["B"]],
            "teamIds": [[1], [2]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // 3 tournament rows + the AI's single segment.
    assertEquals(4, proposedShow.getSegments().size());
    // Tournament rows land ahead of the AI card.
    List<ProposedSegment> tournamentRows =
        proposedShow.getSegments().stream()
            .filter(s -> "Tournament".equals(s.getSource()))
            .toList();
    assertEquals(3, tournamentRows.size());
    assertEquals("Free-for-All", tournamentRows.get(0).getType());
    assertEquals(List.of("Tables, Ladders and Chairs (TLC)"), tournamentRows.get(0).getRules());
    assertTrue(tournamentRows.get(0).getIsTitleSegment());
    assertTrue(tournamentRows.get(0).getSummary().contains("Crown's Cup"));
    assertTrue(tournamentRows.get(0).getSummary().contains("Payoff final"));
    assertEquals("One on One", tournamentRows.get(1).getType());
    assertEquals("One on One", tournamentRows.get(2).getType());
    // The AI's row survives below the tournament rows.
    assertEquals("One on One", proposedShow.getSegments().get(3).getType());
    assertNull(proposedShow.getSegments().get(3).getSource());
  }

  @Test
  void planShow_tournamentSlot_aiRowsStayOnCard() {
    // Unlike beats, tournament slots do NOT evict AI rows: the bracket's participants are
    // unknown at planning time and type-matching would nuke legitimate One-on-One rows on
    // weekly cards. Both stay — the booker deletes what they don't want before approving.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());

    TournamentSlotPreviewDTO payoff = new TournamentSlotPreviewDTO();
    payoff.setTournamentName("Crown's Cup");
    payoff.setTypeName("Free-for-All");
    payoff.setShape("Payoff final");
    context.setTournamentSlots(List.of(payoff));

    SegmentType ffaType = new SegmentType();
    ffaType.setName("Free-for-All");
    when(segmentTypeService.findByName("Free-for-All")).thenReturn(Optional.of(ffaType));
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Free-for-All",
            "description": "AI's rumble",
            "outcome": "A wins",
            "teams": [["A"], ["B"]],
            "teamIds": [[1], [2]]
          },
          {
            "segmentId": "seg2",
            "type": "One on One",
            "description": "A match",
            "outcome": "B wins",
            "teams": [["C"], ["D"]],
            "teamIds": [[3], [4]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Tournament row first, then both AI rows untouched.
    assertEquals(3, proposedShow.getSegments().size());
    assertEquals("Tournament", proposedShow.getSegments().get(0).getSource());
    assertEquals("Free-for-All", proposedShow.getSegments().get(0).getType());
    assertNull(proposedShow.getSegments().get(1).getSource());
    assertEquals("Free-for-All", proposedShow.getSegments().get(1).getType());
    assertNull(proposedShow.getSegments().get(2).getSource());
    assertEquals("One on One", proposedShow.getSegments().get(2).getType());
  }

  @Test
  void planShow_scriptedBeat_sourceStamped() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "A match",
            "outcome": "A wins",
            "teams": [["A"], ["B"]],
            "teamIds": [[1], [2]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    ProposedSegment beatRow = proposedShow.getSegments().get(0);
    assertEquals("Scripted beat", beatRow.getSource(), "Beat rows carry the Scripted beat stamp");
  }

  @Test
  void planShow_aiPutsBeatWrestlersInUnrelatedPromo_promoKept() {
    // A promo mentioning the arc's wrestlers is NOT a match slot — it must survive
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode("promo");
    promoType.setDescription("A talking segment.");
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));
    when(segmentTypeService.findByName("Singles Match")).thenReturn(Optional.empty());

    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Shelton Benjamin cuts a promo",
            "outcome": "Crowd reacts",
            "teams": [["Shelton Benjamin"]],
            "teamIds": [[11]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Promo kept + beat segment added
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals("Singles Match", proposedShow.getSegments().get(0).getType());
    assertEquals("Promo", proposedShow.getSegments().get(1).getType());
  }

  @Test
  void planShow_aiPromoDuplicateOfScriptedPromoBeat_duplicateDroppedBeatSurvivesOnce() {
    // ATW-978m addendum: the AI echoed a scripted PROMO beat as its own promo — the dedup pass
    // previously only dropped match segments, so the card ended up with the beat twice.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(0);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode("promo");
    promoType.setDescription("A talking segment.");
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));

    context.setUpcomingScriptedBeats(
        List.of(beat("Promo", null, "AI_PICKS", null, List.of(11L, 12L))));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Pre-scripted match slot mandatory inclusion.",
            "outcome": "Tension rises",
            "teams": [["Shelton Benjamin"], ["Bobby Lashley"]],
            "teamIds": [[11], [12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // The AI duplicate is dropped; the local beat segment survives exactly once.
    assertEquals(1, proposedShow.getSegments().size());
    assertEquals("Promo", proposedShow.getSegments().get(0).getType());
    assertEquals(
        "Scripted beat: Lashley Arc (Promo)", proposedShow.getSegments().get(0).getSummary());
  }

  @Test
  void planShow_aiDuplicateNamesOnly_teamIdsNull_stillDropped() {
    // AI duplicates often carry participant NAMES with teamIds null (ids reconcile at approval).
    // Two expected matches: the scripted beat claims one, leaving one free slot so the AI is
    // still consulted (all-slots-claimed cards skip the AI entirely).
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Big fight",
            "outcome": "Someone wins",
            "teams": [["Shelton Benjamin"], ["Bobby Lashley"]]
          },
          {
            "segmentId": "seg2",
            "type": "One on One",
            "description": "Unrelated match",
            "outcome": "Unrelated win",
            "teams": [["Randy Orton"], ["Kevin Owens"]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Name-only duplicate dropped; unrelated segment kept; beat segment survives once.
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals(
        "Scripted beat: Lashley Arc (Singles Match)",
        proposedShow.getSegments().get(0).getSummary());
    assertEquals("One on One", proposedShow.getSegments().get(1).getType());
    assertEquals(
        List.of(List.of("Randy Orton"), List.of("Kevin Owens")),
        proposedShow.getSegments().get(1).getTeams());
  }

  @Test
  void planShow_aiSegmentSharesOneWrestlerWithBeat_notDropped() {
    // Single-wrestler overlap is legitimate (participation goal) — only the beat's FULL
    // participant set (or rivalry) claims a non-match slot.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode("promo");
    promoType.setDescription("A talking segment.");
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "Promo",
            "description": "Bobby Lashley cuts a promo",
            "outcome": "Crowd reacts",
            "teams": [["Bobby Lashley"]],
            "teamIds": [[12]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Beat segment + the AI's unrelated Lashley promo both survive.
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals("Singles Match", proposedShow.getSegments().get(0).getType());
    assertEquals("Promo", proposedShow.getSegments().get(1).getType());
  }

  // ── external beat participants (ATW-iukb) ─────────────────────────────────

  @Test
  void planShow_beatWithExternalOpponent_beatSegmentHasTwoTeams() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    FeudScriptBeatDTO beat = beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L));
    beat.setTeams(List.of(List.of("Shelton Benjamin", "Bobby Lashley"), List.of("Randy Orton")));
    beat.setTeamIds(List.of(List.of(11L, 12L), List.of(30L)));
    context.setUpcomingScriptedBeats(List.of(beat));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    ProposedSegment beatSegment = proposedShow.getSegments().get(0);
    assertEquals(
        List.of(List.of("Shelton Benjamin", "Bobby Lashley"), List.of("Randy Orton")),
        beatSegment.getTeams());
    assertEquals(List.of(List.of(11L, 12L), List.of(30L)), beatSegment.getTeamIds());
  }

  @Test
  void planShow_beatWithExtras_extrasShareOpponentTeam() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    FeudScriptBeatDTO beat = beat("Tag Team Match", null, "AI_PICKS", null, List.of(11L, 12L));
    beat.setTeams(
        List.of(List.of("Shelton Benjamin", "Bobby Lashley"), List.of("Randy Orton", "Extra One")));
    beat.setTeamIds(List.of(List.of(11L, 12L), List.of(30L, 31L)));
    context.setUpcomingScriptedBeats(List.of(beat));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(
        List.of(List.of(30L, 31L)), proposedShow.getSegments().get(0).getTeamIds().subList(1, 2));
  }

  @Test
  void planShow_beatWithoutExternals_teamsUnchanged() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));

    when(segmentNarrationService.generateText(anyString())).thenReturn("[]");

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals(
        List.of(List.of(11L), List.of(12L)), proposedShow.getSegments().get(0).getTeamIds());
  }

  @Test
  void planShow_aiSegmentCoversOnlyFeudParticipants_beatEvictsIt() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    FeudScriptBeatDTO beat = beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L));
    beat.setTeams(List.of(List.of("Shelton Benjamin", "Bobby Lashley"), List.of("Randy Orton")));
    beat.setTeamIds(List.of(List.of(11L, 12L), List.of(30L)));
    context.setUpcomingScriptedBeats(List.of(beat));

    // AI booked Shelton vs Randy — covers a feud participant, must be evicted by the beat slot.
    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Big fight",
            "outcome": "Randy Orton wins",
            "teams": [["Shelton Benjamin"], ["Randy Orton"]],
            "teamIds": [[11], [30]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    assertEquals(1, proposedShow.getSegments().size());
    assertEquals("Singles Match", proposedShow.getSegments().get(0).getType());
  }

  @Test
  void planShow_allSlotsPredetermined_skipsAiCall() {
    // One beat + one real-participant tournament slot = both expected matches claimed. The AI
    // has nothing free to propose, so generateText must never be called.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(2);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setUpcomingScriptedBeats(
        List.of(beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L))));
    context.setTournamentSlots(List.of(tournamentSlot("Shelton", "Bobby")));

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, never()).generateText(anyString());
    // Deterministic passes still build the card: tournament slot + beat (tournament rows are
    // prepended after beats are applied, so they land at the front).
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals("Tournament", proposedShow.getSegments().get(0).getSource());
    assertEquals(
        "Scripted beat: Lashley Arc (Singles Match)",
        proposedShow.getSegments().get(1).getSummary());
    assertEquals("Scripted beat", proposedShow.getSegments().get(1).getSource());
  }

  @Test
  void planShow_placeholderTournamentSlot_doesNotClaimMatchSlot() {
    // Placeholder tournament rows are additive (participants resolve at approval) — they claim
    // nothing, so the AI is still consulted for its expected match.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(0);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    context.setTournamentSlots(List.of(tournamentSlot(null, null)));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Free slot",
            "outcome": "Someone wins",
            "teams": [["Randy Orton"], ["Kevin Owens"]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, times(1)).generateText(anyString());
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals("Tournament", proposedShow.getSegments().get(0).getSource());
    assertEquals("One on One", proposedShow.getSegments().get(1).getType());
  }

  @Test
  void planShow_promoBeatClaimsPromoSlot_matchStillFreedForAi() {
    // A promo-type beat claims a PROMO slot; the expected match slot stays free for the AI.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(1);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    FeudScriptBeatDTO beat = beat("Singles Match", null, "AI_PICKS", null, List.of(11L, 12L));
    beat.setSegmentType("Promo");
    context.setUpcomingScriptedBeats(List.of(beat));

    String aiResponseJson =
        """
        [
          {
            "segmentId": "seg1",
            "type": "One on One",
            "description": "Match slot",
            "outcome": "Someone wins",
            "teams": [["Randy Orton"], ["Kevin Owens"]]
          }
        ]
        """;
    when(segmentNarrationService.generateText(anyString())).thenReturn(aiResponseJson);

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Promo beat → AI still called for the match slot.
    verify(narrationServiceFactory, times(1)).generateText(anyString());
    assertEquals(2, proposedShow.getSegments().size());
    assertEquals("Promo", proposedShow.getSegments().get(0).getType());
    assertEquals("Scripted beat", proposedShow.getSegments().get(0).getSource());
    assertEquals("One on One", proposedShow.getSegments().get(1).getType());
  }

  @Test
  void planShow_promoBeatFillsLastPromoSlot_skipsAiWhenNoMatchExpected() {
    // Template expects no matches; the promo beat claims the only promo slot. Nothing is free
    // for the AI — skip the call and build the card deterministically.
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setExpectedMatches(0);
    showTemplate.setExpectedPromos(1);
    context.setShowTemplate(showTemplate);
    context.setShowDate(LocalDate.of(2025, 6, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setCode("promo");
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.of(promoType));
    FeudScriptBeatDTO beat = beat("Promo", null, "AI_PICKS", null, List.of(11L, 12L));
    context.setUpcomingScriptedBeats(List.of(beat));

    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    verify(narrationServiceFactory, never()).generateText(anyString());
    assertEquals(1, proposedShow.getSegments().size());
    assertEquals("Promo", proposedShow.getSegments().get(0).getType());
  }

  /** Tournament slot preview row with either real team names or placeholders. */
  private TournamentSlotPreviewDTO tournamentSlot(String team1, String team2) {
    TournamentSlotPreviewDTO dto = new TournamentSlotPreviewDTO();
    dto.setTournamentName("Crown Cup");
    dto.setTypeName("One on One");
    dto.setShape("Payoff final");
    if (team1 == null) {
      dto.setTeams(List.of(List.of("Tournament bracket"), List.of("Tournament bracket")));
    } else {
      dto.setTeams(List.of(List.of(team1), List.of(team2)));
    }
    return dto;
  }
}
