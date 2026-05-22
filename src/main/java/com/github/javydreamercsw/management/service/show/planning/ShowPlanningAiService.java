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
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.AiGeneratedSegmentDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
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
  private final HolidayService holidayService;

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public ProposedShow planShow(@NonNull final ShowPlanningContextDTO context) {
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
                    segment.setParticipants(dto.getParticipants());
                    segment.setRivalryId(dto.getRivalryId());
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

  private String buildShowPlanningPrompt(@NonNull final ShowPlanningContextDTO context) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        """
        You are a professional wrestling show planner. Your task is to create a compelling and\
         coherent show by generating a list of segments in JSON format.

        """);
    if (context.isPremiumLiveEvent()) {
      prompt.append(
          """
          **THIS IS A PREMIUM LIVE EVENT (PLE).** Apply PLE-specific booking rules (see below).

          """);
    }
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
      Optional<String> holidayTheme = holidayService.getHolidayTheme(context.getShowDate());
      holidayTheme.ifPresent(
          theme ->
              prompt
                  .append("\nHoliday Theme: ")
                  .append(theme)
                  .append(
                      """
                      . Please incorporate this theme into the show's segments where\
                       appropriate. Including, but not limited to, commentators mentioning\
                       them, wrestlers, referencing them in promos and having matches with\
                       the themes.
                      """));
    }

    if (context.getRecentSegments() != null && !context.getRecentSegments().isEmpty()) {
      prompt.append("Recent Segments (up to 10):\n");
      context.getRecentSegments().stream()
          .limit(10)
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
      prompt.append("Recent Promos (up to 10):\n");
      context.getRecentPromos().stream()
          .limit(10)
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
                      .append("- Id: ")
                      .append(rivalry.getId())
                      .append(", Name: ")
                      .append(rivalry.getName())
                      .append(", Participants: ")
                      .append(String.join(", ", rivalry.getParticipants()))
                      .append(", Heat: ")
                      .append(rivalry.getHeat())
                      .append(", Priority: ")
                      .append(rivalry.getPriority())
                      .append("\n"));
      prompt.append("\n**Rivalry Resolution Rules:**\n");
      prompt.append("- At 10 Heat: They must wrestle at the next PLE show\n");
      prompt.append("- At 30 Heat → forced into High Heat Rule Match\n");
      List<SegmentRule> highHeatRules = segmentRuleService.getHighHeatRules();
      List<String> highHeatRuleDescriptions =
          highHeatRules.stream()
              .map(rule -> "%s (%s)".formatted(rule.getName(), rule.getDescription()))
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
                      .append(", Defense Frequency: ")
                      .append(championship.getDefenseFrequency())
                      .append(" days, Days since last defense: ")
                      .append(championship.getDaysSinceLastDefense())
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
                      .append(", Injured: ")
                      .append(wrestler.isInjured())
                      .append("\n"));
    }

    if (context.getFactions() != null && !context.getFactions().isEmpty()) {
      prompt.append("\nFactions:\n");
      context
          .getFactions()
          .forEach(
              faction -> {
                prompt.append("- Name: ").append(faction.getName());
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

    prompt.append("\n**Booking Rules & Participation Goal:**\n");
    prompt.append(
        """
        - Goal: Every healthy (non-injured) wrestler MUST participate in at least one segment\
         per week.
        """);
    prompt.append(
        """
        - Prioritize active feuds (especially high priority ones) and title defenses if the days\
         since last defense exceeds the frequency.
        """);
    prompt.append(
        """
        - To ensure 100% participation, use multi-man matches (Triple Threat, Fatal Four-Way,\
         Battle Royale) or Faction-based tag matches to consolidate many wrestlers into few\
         segments.
        """);
    prompt.append(
        """
        - If healthy wrestlers remain after booking matches, assign them to Promos or Backstage\
         segments.
        """);
    prompt.append(
        """
        - Within the same calendar day, avoid having a wrestler in more than one match. They can\
         participate in promos and any other capacity as long as it doesn't involve\
         officially participating in the match.
        """);
    prompt.append(
        """
        - Within the same calendar week, avoid having a wrestler in more than one match. The\
         exception is for Premium Live Event (PLE) where this is not avoidable.
        """);
    if (context.isPremiumLiveEvent()) {
      prompt.append(
          """

          **PLE-Specific Booking Rules:**
          - ALL rivalries at Heat ≥ 10 MUST have a match on this card — no deferral to a future show.
          - ALL rivalries at Heat ≥ 30 MUST use a stipulation match from the Available Stipulation\
           Matches list above.
          - Every active championship MUST be defended on this card.
          - Matches should have clear, decisive finishes — PLE is not the place for count-out or\
           disqualification endings.
          """);
    }

    List<SegmentType> segmentTypes = segmentTypeService.findAll();
    List<String> segmentTypeDescriptions =
        segmentTypes.stream()
            .map(type -> "%s (%s)".formatted(type.getName(), type.getDescription()))
            .collect(Collectors.toList());
    prompt
        .append("\nAvailable Segment Types: ")
        .append(String.join(", ", segmentTypeDescriptions))
        .append("\n");

    prompt.append(
        """

        IMPORTANT: Use the provided context to generate a compelling and coherent show. \
        The segments should build on existing rivalries and championships. \
        If a `Next PLE` is provided, the show should build towards it.

        """);

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
    prompt.append(
        "  \"notes\": \"string\", // Optional instructions/feedback for future AI narration\n");
    prompt.append("  \"participants\": [\"string\"],\n");
    prompt.append(
        "  \"rivalryId\": number // Optional: the Id of the rivalry this match resolves; omit or"
            + " null if not rivalry-driven\n");
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
        .append(
            """
             towards a compelling narrative. **Be concise with descriptions and outcomes to\
             ensure the entire JSON array fits in the response.**

            """)
        .append(
            """
            IMPORTANT: **Be extremely concise with your internal thoughts/reasoning to save\
             output tokens for the JSON.** The 'participants' field MUST be\
            """)
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
