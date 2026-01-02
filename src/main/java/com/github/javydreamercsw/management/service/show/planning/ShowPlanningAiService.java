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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.AiGeneratedSegmentDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.util.HolidayUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowPlanningAiService {

  private final SegmentNarrationServiceFactory narrationServiceFactory;
  private final ObjectMapper objectMapper;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public ProposedShow planShow(@NonNull ShowPlanningContextDTO context) {
    if (narrationServiceFactory.getBestAvailableService() == null) {
      log.warn("No AI service available for show planning.");
      return new ProposedShow();
    }

    String prompt = buildShowPlanningPrompt(context);
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
        log.error("Could not extract JSON array from AI response: {}", aiResponse);
        throw new ShowPlanningException(
            "Could not extract JSON array from AI response: " + aiResponse);
      }

      List<AiGeneratedSegmentDTO> aiSegments =
          objectMapper.readValue(
              jsonString,
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, AiGeneratedSegmentDTO.class));

      List<ProposedSegment> proposedSegments =
          aiSegments.stream()
              .map(
                  dto -> {
                    ProposedSegment segment = new ProposedSegment();
                    segment.setType(dto.getType());
                    segment.setNarration(dto.getDescription());
                    segment.setParticipants(dto.getParticipants());
                    return segment;
                  })
              .collect(java.util.stream.Collectors.toList());

      ProposedShow proposedShow = new ProposedShow();
      proposedShow.setSegments(proposedSegments);
      return proposedShow;
    } catch (JsonProcessingException e) {
      log.error("Failed to parse AI response into ProposedShow object: {}", aiResponse, e);
      throw new ShowPlanningException("Failed to parse AI response", e);
    } catch (Exception e) {
      log.error("An unexpected error occurred during show planning: {}", e.getMessage(), e);
      throw new ShowPlanningException("An unexpected error occurred during show planning", e);
    }
  }

  private String buildShowPlanningPrompt(@NonNull ShowPlanningContextDTO context) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a professional wrestling show planner. Your task is to create a compelling and"
            + " coherent show by generating a list of segments in JSON format.\n\n");
    prompt.append("Here is the context for the show:\n");

    if (context.getShowTemplate() != null) {
      prompt
          .append("Show Template Name: ")
          .append(context.getShowTemplate().getShowName())
          .append("\n");
      prompt
          .append("Show Template Description: ")
          .append(context.getShowTemplate().getDescription())
          .append("\n");
      prompt
          .append("Expected Matches: ")
          .append(context.getShowTemplate().getExpectedMatches())
          .append("\n");
      prompt
          .append("Expected Promos: ")
          .append(context.getShowTemplate().getExpectedPromos())
          .append("\n");
    }

    if (context.getShowDate() != null) {
      Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(context.getShowDate());
      holidayTheme.ifPresent(
          theme ->
              prompt
                  .append("\nHoliday Theme: ")
                  .append(theme)
                  .append(
                      ". Please incorporate this theme into the show's segments where"
                          + " appropriate. Including, but not limited to, commentators mentioning"
                          + " them, wrestlers, referencing them in promos and having matches with"
                          + " the themes.\n"));
    }

    if (context.getRecentSegments() != null && !context.getRecentSegments().isEmpty()) {
      prompt.append("Recent Segments:\n");
      context
          .getRecentSegments()
          .forEach(
              segment ->
                  prompt
                      .append("- Name: ")
                      .append(segment.getName())
                      .append(", Summary: ")
                      .append(segment.getSummary())
                      .append(", Participants: ")
                      .append(String.join(", ", segment.getParticipants()))
                      .append(", Show: ")
                      .append(segment.getShowName())
                      .append(", Date: ")
                      .append(segment.getShowDate())
                      .append("\n"));
    }

    if (context.getRecentPromos() != null && !context.getRecentPromos().isEmpty()) {
      prompt.append("Recent Promos:\n");
      context
          .getRecentPromos()
          .forEach(
              promo ->
                  prompt
                      .append("- Name: ")
                      .append(promo.getName())
                      .append(", Summary: ")
                      .append(promo.getSummary())
                      .append(", Participants: ")
                      .append(String.join(", ", promo.getParticipants()))
                      .append(", Show: ")
                      .append(promo.getShowName())
                      .append(", Date: ")
                      .append(promo.getShowDate())
                      .append("\n"));
    }

    if (context.getCurrentRivalries() != null && !context.getCurrentRivalries().isEmpty()) {
      prompt.append("Current Rivalries:\n");
      context
          .getCurrentRivalries()
          .forEach(
              rivalry ->
                  prompt
                      .append("- Name: ")
                      .append(rivalry.getName())
                      .append(", Participants: ")
                      .append(String.join(", ", rivalry.getParticipants()))
                      .append(", Heat: ")
                      .append(rivalry.getHeat())
                      .append("\n"));
      prompt.append("\n**Rivalry Resolution Rules:**\n");
      prompt.append("- At 10 Heat: They must wrestle at the next PLE show\n");
      prompt.append("- At 30 Heat → forced into High Heat Rule Match\n");
      List<SegmentRule> highHeatRules = segmentRuleService.getHighHeatRules();
      List<String> highHeatRuleDescriptions =
          highHeatRules.stream()
              .map(rule -> String.format("%s (%s)", rule.getName(), rule.getDescription()))
              .collect(Collectors.toList());
      prompt
          .append("Available Stipulation Matches: ")
          .append(String.join(", ", highHeatRuleDescriptions))
          .append("\n\n");
    }

    if (context.getWrestlerHeats() != null && !context.getWrestlerHeats().isEmpty()) {
      prompt.append("Wrestler Heat:\n");
      context
          .getWrestlerHeats()
          .forEach(
              heat ->
                  prompt
                      .append("- Wrestler: ")
                      .append(heat.getWrestlerName())
                      .append(", Heat: ")
                      .append(heat.getHeat())
                      .append("\n"));
    }

    if (context.getChampionships() != null && !context.getChampionships().isEmpty()) {
      prompt.append("Championships:\n");
      context
          .getChampionships()
          .forEach(
              championship ->
                  prompt
                      .append("- Name: ")
                      .append(championship.getChampionshipName())
                      .append(", Champion: ")
                      .append(championship.getChampionName())
                      .append(", Contender: ")
                      .append(championship.getContenderName())
                      .append("\n"));
    }

    if (context.getFullRoster() != null && !context.getFullRoster().isEmpty()) {
      prompt.append("\nFull Roster:\n");
      context
          .getFullRoster()
          .forEach(
              wrestler ->
                  prompt
                      .append("- Name: ")
                      .append(wrestler.getName())
                      .append(", Gender: ")
                      .append(wrestler.getGender())
                      .append(", Tier: ")
                      .append(wrestler.getTier())
                      .append(", Description: ")
                      .append(wrestler.getDescription())
                      .append("\n"));
    }

    if (context.getFactions() != null && !context.getFactions().isEmpty()) {
      prompt.append("\nFactions:\n");
      context
          .getFactions()
          .forEach(
              faction -> {
                prompt
                    .append("- Name: ")
                    .append(faction.getName())
                    .append(", Description: ")
                    .append(faction.getDescription());
                if (faction.getLeader() != null) {
                  prompt.append(", Leader: ").append(faction.getLeader());
                }
                if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
                  prompt.append(", Members: ").append(String.join(", ", faction.getMembers()));
                }
                prompt.append("\n");
              });
    }

    if (context.getNextPle() != null) {
      prompt.append("Next PLE (Premium Live Event):\n");
      prompt.append("- Name: ").append(context.getNextPle().getPleName()).append("\n");
      prompt.append("- Date: ").append(context.getNextPle().getPleDate()).append("\n");
      prompt.append("- Summary: ").append(context.getNextPle().getSummary()).append("\n");
      if (context.getNextPle().getMatches() != null
          && !context.getNextPle().getMatches().isEmpty()) {
        prompt.append("  Scheduled Matches:\n");
        context
            .getNextPle()
            .getMatches()
            .forEach(
                match ->
                    prompt
                        .append("  - Name: ")
                        .append(match.getName())
                        .append(", Participants: ")
                        .append(String.join(", ", match.getParticipants()))
                        .append("\n"));
      }
    }

    prompt.append("\n**Other considerations:**\n");
    prompt.append(
        "- Within the same calendar day, avoid having a wrestler in more than one match. They can"
            + " participate in promos and any other capacity as long as it doesn't involve"
            + " officially participating in the match.\n");
    prompt.append(
        "- Within the same calendar week, avoid having a wrestler in more than one match. Instead"
            + " focus on giving all wrestlers a change to perform in a match every week. The"
            + " exception is for Premium Live Event (PLE) where this is not avoidable.\n");

    List<SegmentType> segmentTypes = segmentTypeService.findAll();
    List<String> segmentTypeDescriptions =
        segmentTypes.stream()
            .map(type -> String.format("%s (%s)", type.getName(), type.getDescription()))
            .collect(Collectors.toList());
    prompt
        .append("\nAvailable Segment Types: ")
        .append(String.join(", ", segmentTypeDescriptions))
        .append("\n");

    prompt.append(
        "\nIMPORTANT: Use the provided context to generate a compelling and coherent show. "
            + "The segments should build on existing rivalries and championships. "
            + "If a `Next PLE` is provided, the show should build towards it.\n\n");

    prompt.append("\nHere is the JSON schema for a single segment:\n");
    prompt.append("```json\n");
    prompt.append("{\n");
    prompt.append("  \"segmentId\": \"string\",\n");
    if (!segmentTypes.isEmpty()) {
      prompt
          .append("  \"type\": \"string\", // e.g., \"")
          .append(segmentTypes.get(0).getName())
          .append("\"\n");
    } else {
      prompt.append("  \"type\": \"string\", // e.g., \"Match\"\n");
    }
    prompt.append("  \"description\": \"string\",\n");
    prompt.append("  \"outcome\": \"string\",\n");
    prompt.append("  \"participants\": [\"string\"]\n");
    prompt.append("}\n");
    prompt.append("```\n\n");
    prompt
        .append("Generate a JSON array of exactly ")
        .append(context.getShowTemplate().getExpectedMatches())
        .append(" matches and ")
        .append(context.getShowTemplate().getExpectedPromos())
        .append(" promos for the show. Each segment")
        .append(
            " should adhere to the provided schema. Ensure the segments flow logically and build")
        .append(" towards a compelling narrative. IMPORTANT: The 'participants' field MUST be")
        .append(
            " populated with relevant wrestler names from the provided context. The response MUST")
        .append(
            " be a valid JSON array, and ONLY the JSON array. Do not include any conversational")
        .append(" text or explanations outside the JSON.\n\n");
    prompt.append("JSON:\n");
    return prompt.toString();
  }

  /**
   * Extracts a JSON array from a given string. This is useful when the AI might include
   * conversational text around the JSON output.
   *
   * @param input The string potentially containing a JSON array.
   * @return The extracted JSON array string, or null if not found.
   */
  private String extractJsonArray(String input) {
    String jsonMarker = "```json";
    int jsonStart = input.indexOf(jsonMarker);
    if (jsonStart != -1) {
      // Find the opening bracket after the marker
      int startIndex = input.indexOf('[', jsonStart + jsonMarker.length());
      if (startIndex != -1) {
        // Find the corresponding closing bracket
        int balance = 1;
        int endIndex = -1;
        for (int i = startIndex + 1; i < input.length(); i++) {
          char c = input.charAt(i);
          if (c == '[') {
            balance++;
          } else if (c == ']') {
            balance--;
          }
          if (balance == 0) {
            endIndex = i;
            break;
          }
        }
        if (endIndex != -1) {
          return input.substring(startIndex, endIndex + 1);
        }
      }
    }

    // Fallback for cases where the marker is not present or the extraction fails
    int startIndex = input.indexOf('[');
    if (startIndex != -1) {
      int balance = 1;
      int endIndex = -1;
      for (int i = startIndex + 1; i < input.length(); i++) {
        char c = input.charAt(i);
        if (c == '[') {
          balance++;
        } else if (c == ']') {
          balance--;
        }
        if (balance == 0) {
          endIndex = i;
          break;
        }
      }
      if (endIndex != -1) {
        return input.substring(startIndex, endIndex + 1);
      }
    }

    return null;
  }
}
