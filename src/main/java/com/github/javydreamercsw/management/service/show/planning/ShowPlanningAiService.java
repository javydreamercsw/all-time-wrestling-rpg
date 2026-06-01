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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.AiGeneratedSegmentDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShowPlanningAiService {

  private final SegmentNarrationServiceFactory narrationServiceFactory;
  private final ObjectMapper objectMapper;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final HolidayService holidayService;
  private final ShowPlanningPromptBuilder promptBuilder;

  @Autowired
  ShowPlanningAiService(
      final SegmentNarrationServiceFactory narrationServiceFactory,
      final ObjectMapper objectMapper,
      final SegmentTypeService segmentTypeService,
      final SegmentRuleService segmentRuleService,
      final HolidayService holidayService) {
    this.narrationServiceFactory = narrationServiceFactory;
    this.objectMapper = objectMapper;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.holidayService = holidayService;
    this.promptBuilder =
        new ShowPlanningPromptBuilder(segmentTypeService, segmentRuleService, holidayService);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public ProposedShow planShow(@NonNull final ShowPlanningContextDTO context) {
    if (narrationServiceFactory.getBestAvailableService() == null) {
      log.warn("No AI service available for show planning.");
      return new ProposedShow();
    }

    String prompt = promptBuilder.build(context);
    log.debug("Sending prompt to AI: {}", prompt);

    String aiResponse = narrationServiceFactory.generateText(prompt);
    log.debug("Received response from AI: {}", aiResponse);

    if (aiResponse == null || aiResponse.trim().isEmpty()) {
      log.warn("AI returned an empty or null response for show planning.");
      return new ProposedShow();
    }

    try {
      // Attempt to extract JSON array from the response, as AI might include conversational text
      String jsonString = extractJsonArray(aiResponse);
      if (jsonString == null) {
        log.error("Could not extract JSON array from AI response. Length: {}", aiResponse.length());
        log.debug("Raw response: {}", aiResponse);
        throw new ShowPlanningException("Could not extract JSON array from AI response");
      }

      log.debug("Extracted JSON (length {}): {}", jsonString.length(), jsonString);

      List<AiGeneratedSegmentDTO> aiSegments =
          objectMapper.readValue(
              jsonString,
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, AiGeneratedSegmentDTO.class));
      log.info("Successfully parsed {} segments from AI response", aiSegments.size());

      List<ProposedSegment> proposedSegments =
          aiSegments.stream()
              .map(
                  dto -> {
                    ProposedSegment segment = new ProposedSegment();
                    segment.setType(dto.getType());
                    segment.setNarration(dto.getDescription());
                    segment.setSummary(dto.getOutcome());
                    segment.setNotes(dto.getNotes());
                    segment.setRivalryId(dto.getRivalryId());
                    if (dto.getTeams() != null && !dto.getTeams().isEmpty()) {
                      segment.setTeams(dto.getTeams());
                      segment.setParticipants(
                          dto.getTeams().stream()
                              .flatMap(java.util.List::stream)
                              .collect(java.util.stream.Collectors.toList()));
                    } else {
                      segment.setParticipants(dto.getParticipants());
                    }
                    if (dto.getTeamIds() != null && !dto.getTeamIds().isEmpty()) {
                      segment.setTeamIds(dto.getTeamIds());
                      segment.setParticipantIds(
                          dto.getTeamIds().stream()
                              .flatMap(java.util.List::stream)
                              .collect(java.util.stream.Collectors.toList()));
                    } else if (dto.getParticipantIds() != null) {
                      segment.setParticipantIds(dto.getParticipantIds());
                    }
                    return segment;
                  })
              .collect(java.util.stream.Collectors.toList());

      ProposedShow proposedShow = new ProposedShow();
      proposedShow.setSegments(proposedSegments);
      return proposedShow;
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to parse AI response into ProposedShow object. AI Response length: {}",
          aiResponse.length());
      log.error("Full AI response that failed to parse:\n{}", aiResponse);
      throw new ShowPlanningException("Failed to parse AI response", e);
    } catch (Exception e) {
      log.error("An unexpected error occurred during show planning: {}", e.getMessage(), e);
      throw new ShowPlanningException("An unexpected error occurred during show planning", e);
    }
  }

  /**
   * Extracts a JSON array from a given string. This is useful when the AI might include
   * conversational text around the JSON output.
   *
   * @param input The string potentially containing a JSON array.
   * @return The extracted JSON array string, or null if not found.
   */
  private String extractJsonArray(final String input) {
    if (input == null || input.trim().isEmpty()) {
      return null;
    }

    String cleaned = input.trim();

    // Remove markdown code blocks if present
    if (cleaned.startsWith("```")) {
      // Find the first newline or the end of the first line
      int firstNewline = cleaned.indexOf('\n');
      if (firstNewline != -1) {
        cleaned = cleaned.substring(firstNewline).trim();
      } else {
        cleaned = cleaned.substring(3).trim();
      }

      if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
      }
    }

    // Search for the array start and end in the cleaned string
    int startIndex = cleaned.indexOf('[');
    int endIndex = cleaned.lastIndexOf(']');

    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
      return cleaned.substring(startIndex, endIndex + 1);
    }

    return null;
  }
}
