/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShowPlanningPromptBuilder {

  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final HolidayService holidayService;

  /** Strips characters that could be used to escape or hijack the AI prompt. */
  static String sanitize(final String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[\\[\\]{}|`\\\\]", "").trim();
  }

  public String build(@NonNull final ShowPlanningContextDTO context) {
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
          .append(sanitize(context.getShowTemplate().getShowName()))
          .append("\n");
      prompt
          .append("Show Template Description: ")
          .append(sanitize(context.getShowTemplate().getDescription()))
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
                  .append(sanitize(theme))
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
                      .append(sanitize(segment.getName()))
                      .append(", Summary: ")
                      .append(sanitize(segment.getSummary()))
                      .append(", Participants: ")
                      .append(
                          segment.getParticipants().stream()
                              .map(ShowPlanningPromptBuilder::sanitize)
                              .collect(Collectors.joining(", ")))
                      .append(", Show: ")
                      .append(sanitize(segment.getShowName()))
                      .append(", Date: ")
                      .append(segment.getShowDate())
                      .append("\n"));
    }

    if (context.getCurrentRivalries() != null && !context.getCurrentRivalries().isEmpty()) {
      prompt.append("Current Rivalries (heat ≥ 10 only):\n");
      context
          .getCurrentRivalries()
          .forEach(
              rivalry -> {
                String classification;
                if (rivalry.getHeat() >= 30) {
                  classification = "STIPULATION_REQUIRED";
                } else if (rivalry.getHeat() >= 20) {
                  classification = "PLE_RESOLUTION_ELIGIBLE";
                } else {
                  classification = "MUST_BOOK";
                }
                prompt
                    .append("- Id: ")
                    .append(rivalry.getId())
                    .append(", Name: ")
                    .append(sanitize(rivalry.getName()))
                    .append(", Participants: ")
                    .append(
                        rivalry.getParticipants().stream()
                            .map(ShowPlanningPromptBuilder::sanitize)
                            .collect(Collectors.joining(", ")))
                    .append(", Heat: ")
                    .append(rivalry.getHeat())
                    .append(", Priority: ")
                    .append(rivalry.getPriority())
                    .append(", Classification: ")
                    .append(classification)
                    .append("\n");
              });
      prompt.append("\n**Rivalry Classification Rules:**\n");
      prompt.append(
          """
          - MUST_BOOK (Heat 10-19): This rivalry MUST have a segment on this show. Book a match \
          or a promo confrontation — do not skip it.
          - PLE_RESOLUTION_ELIGIBLE (Heat 20-29): This rivalry is hot enough to headline a PLE. \
          On a regular show, build tension with a confrontation, brawl, or non-finish match. \
          On a PLE, give it a decisive match.
          - STIPULATION_REQUIRED (Heat ≥ 30): This rivalry has reached maximum intensity. It MUST \
          have a match with a stipulation from the Available Stipulation Matches list below. \
          Use a decisive, no-DQ finish — do not end with a count-out or disqualification.
          """);
      List<SegmentRule> highHeatRules = segmentRuleService.getHighHeatRules();
      List<String> highHeatRuleDescriptions =
          highHeatRules.stream()
              .map(
                  rule ->
                      "%s (%s)"
                          .formatted(sanitize(rule.getName()), sanitize(rule.getDescription())))
              .collect(Collectors.toList());
      prompt
          .append("Available Stipulation Matches: ")
          .append(String.join(", ", highHeatRuleDescriptions))
          .append("\n\n");
    }

    if (context.getChampionships() != null && !context.getChampionships().isEmpty()) {
      prompt.append("Championships:\n");
      context
          .getChampionships()
          .forEach(
              championship ->
                  prompt
                      .append("- Name: ")
                      .append(sanitize(championship.getChampionshipName()))
                      .append(", Champion: ")
                      .append(sanitize(championship.getChampionName()))
                      .append(", Contender: ")
                      .append(sanitize(championship.getContenderName()))
                      .append(", Defense Frequency: ")
                      .append(
                          championship.getDefenseFrequencyType() != null
                              ? championship.getDefenseFrequencyType().getDisplayName()
                              : "None")
                      .append(", Days since last defense: ")
                      .append(championship.getDaysSinceLastDefense())
                      .append(championship.isOverdue() ? " (OVERDUE)" : "")
                      .append("\n"));
    }

    if (context.getFullRoster() != null && !context.getFullRoster().isEmpty()) {
      prompt.append("\nFull Roster:\n");
      context
          .getFullRoster()
          .forEach(
              wrestler ->
                  prompt
                      .append("- Id: ")
                      .append(wrestler.getId())
                      .append(", Name: ")
                      .append(sanitize(wrestler.getName()))
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
                prompt.append("- Name: ").append(sanitize(faction.getName()));
                if (faction.getLeader() != null) {
                  prompt.append(", Leader: ").append(sanitize(faction.getLeader()));
                }
                if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
                  prompt
                      .append(", Members: ")
                      .append(
                          faction.getMembers().stream()
                              .map(ShowPlanningPromptBuilder::sanitize)
                              .collect(Collectors.joining(", ")));
                }
                prompt.append("\n");
              });
    }

    if (context.getNextPle() != null) {
      prompt.append("Next PLE (Premium Live Event):\n");
      prompt.append("- Name: ").append(sanitize(context.getNextPle().getPleName())).append("\n");
      prompt.append("- Date: ").append(context.getNextPle().getPleDate()).append("\n");
      prompt.append("- Summary: ").append(sanitize(context.getNextPle().getSummary())).append("\n");
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
                        .append(sanitize(match.getName()))
                        .append(", Participants: ")
                        .append(
                            match.getParticipants().stream()
                                .map(ShowPlanningPromptBuilder::sanitize)
                                .collect(Collectors.joining(", ")))
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
            .map(
                type ->
                    "%s (%s)".formatted(sanitize(type.getName()), sanitize(type.getDescription())))
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
          .append(sanitize(segmentTypes.getFirst().getName()))
          .append("\"\n");
    } else {
      prompt.append("  \"type\": \"string\", // e.g., \"Match\"\n");
    }
    prompt.append("  \"description\": \"string\",\n");
    prompt.append("  \"outcome\": \"string\",\n");
    prompt.append(
        "  \"notes\": \"string\", // Optional instructions/feedback for future AI narration\n");
    prompt.append(
        "  \"teams\": [[\"string\"]], // List of teams by name; each inner array is one team."
            + " For a 1v1 match: [[\"WrestlerA\"],[\"WrestlerB\"]]."
            + " For a tag match: [[\"A\",\"B\"],[\"C\",\"D\"]]."
            + " For promos/non-match segments with no opposing sides: [[\"A\",\"B\",\"C\"]].\n");
    prompt.append(
        "  \"teamIds\": [[number]], // REQUIRED: same structure as teams but using the wrestler Id"
            + " from the Full Roster. Must match the teams array exactly."
            + " For a 1v1 match: [[101],[202]]. For a tag match: [[101,102],[203,204]].\n");
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
}
