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
package com.github.javydreamercsw.base.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.TitleContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;

/** Generates prompts for AI services. */
public class PromptGenerator {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Generates a prompt for narrating a match.
   *
   * @param segmentContext The context of the segment to narrate.
   * @return The generated prompt.
   */
  public String generateMatchNarrationPrompt(@NonNull SegmentNarrationContext segmentContext) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        "Narrate the following wrestling segment based on the provided JSON context.\n\n");

    // CRITICAL INSTRUCTIONS - These must be followed
    prompt.append("CRITICAL INSTRUCTIONS:\n");

    // 1. Participants
    if (segmentContext.getWrestlers() != null && !segmentContext.getWrestlers().isEmpty()) {
      List<String> participantNames =
          segmentContext.getWrestlers().stream()
              .map(
                  w -> {
                    if (w.getManagerName() != null && !w.getManagerName().isEmpty()) {
                      return w.getName() + " accompanied to the ring by " + w.getManagerName();
                    }
                    return w.getName();
                  })
              .collect(Collectors.toList());
      prompt
          .append(
              "1. PARTICIPANTS: The following wrestlers are participating in this match and MUST"
                  + " ALL be mentioned and involved in the action: ")
          .append(String.join(", ", participantNames))
          .append(".\n");
    }

    // 2. Outcome
    if (segmentContext.getDeterminedOutcome() != null
        && !segmentContext.getDeterminedOutcome().isEmpty()) {
      prompt
          .append("2. OUTCOME: The match MUST end with the following outcome: ")
          .append(segmentContext.getDeterminedOutcome())
          .append("\n");
    }

    // 3. Match Type & Rules
    SegmentTypeContext segmentType = segmentContext.getSegmentType();
    if (segmentType != null) {
      prompt
          .append("3. MATCH TYPE: This is a '")
          .append(segmentType.getSegmentType())
          .append("'.\n");
      if (segmentType.getStipulation() != null && !segmentType.getStipulation().isEmpty()) {
        prompt.append("   Stipulation: ").append(segmentType.getStipulation()).append(".\n");
      }
      if (segmentType.getRules() != null && !segmentType.getRules().isEmpty()) {
        prompt.append("   Rules: ").append(String.join(", ", segmentType.getRules())).append(".\n");
      }
    }
    prompt.append(
        "Generate a compelling wrestling narration as a DIALOGUE between the commentators provided"
            + " in the JSON.\n");
    prompt.append(
        "Each line of dialogue MUST start with a tag identifying the speaker in the following"
            + " format: '[SPEAKER:Commentator Name]'.\n");
    prompt.append("Follow the tag immediately with a colon and the commentator's dialogue.\n");
    prompt.append("DO NOT include any text that is not part of a tagged dialogue line.\n\n");

    prompt.append("Here is the full context for the segment:\n");
    try {
      prompt.append(
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(segmentContext));
    } catch (JsonProcessingException e) {
      // Fallback to toString() if JSON serialization fails
      prompt.append(segmentContext.toString());
    }

    return prompt.toString();
  }

  /**
   * Generates a simplified prompt for narrating a match, optimized for smaller models. Strips out
   * move sets, feuds, and other detailed context to reduce noise.
   *
   * @param segmentContext The context of the segment to narrate.
   * @return The generated prompt.
   */
  public String generateSimplifiedMatchNarrationPrompt(SegmentNarrationContext segmentContext) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Narrate a wrestling match based on these details:\n\n");

    // 1. Participants (Simplified)
    if (segmentContext.getWrestlers() != null && !segmentContext.getWrestlers().isEmpty()) {
      prompt.append("PARTICIPANTS:\n");
      for (WrestlerContext wrestler : segmentContext.getWrestlers()) {
        prompt.append("- ").append(wrestler.getName());
        if (wrestler.getManagerName() != null && !wrestler.getManagerName().isEmpty()) {
          prompt.append(" (with ").append(wrestler.getManagerName()).append(")");
        }
        if (wrestler.getDescription() != null) {
          String desc = wrestler.getDescription();
          // Truncate description if it's too long, but try to keep it coherent
          int limit = 200;
          if (desc.length() > limit) {
            int cutOff = desc.lastIndexOf(' ', limit);
            if (cutOff != -1) {
              desc = desc.substring(0, cutOff) + "...";
            } else {
              desc = desc.substring(0, limit) + "...";
            }
          }
          prompt.append(": ").append(desc);
        }
        prompt.append("\n");
      }
      prompt.append("\n");
    }

    // 2. Non-Wrestler Characters (Referee, Commentators, etc.)
    if (segmentContext.getReferee() != null) {
      RefereeContext referee = segmentContext.getReferee();
      prompt.append("REFEREE: ").append(referee.getName()).append("\n");
    }

    if (segmentContext.getNpcs() != null && !segmentContext.getNpcs().isEmpty()) {
      prompt.append("OTHER CHARACTERS:\n");
      for (NPCContext npc : segmentContext.getNpcs()) {
        prompt.append("- ").append(npc.getName());
        if (npc.getRole() != null) {
          prompt.append(" (").append(npc.getRole()).append(")");
        }
        prompt.append("\n");
      }
      prompt.append("\n");
    }

    // 3. Match Type
    SegmentTypeContext segmentType = segmentContext.getSegmentType();
    if (segmentType != null) {
      prompt.append("MATCH TYPE: ").append(segmentType.getSegmentType()).append("\n");
      if (segmentType.getRules() != null && !segmentType.getRules().isEmpty()) {
        prompt.append("RULES: ").append(String.join(", ", segmentType.getRules())).append("\n");
      }
      prompt.append("\n");
    }

    // 4. Titles
    if (segmentContext.getTitles() != null && !segmentContext.getTitles().isEmpty()) {
      String titles =
          segmentContext.getTitles().stream()
              .map(TitleContext::getName)
              .collect(Collectors.joining(", "));
      prompt.append("TITLES ON THE LINE: ").append(titles).append("\n\n");
    }

    // 5. Outcome
    if (segmentContext.getDeterminedOutcome() != null
        && !segmentContext.getDeterminedOutcome().isEmpty()) {
      prompt
          .append("OUTCOME (MUST FOLLOW): ")
          .append(segmentContext.getDeterminedOutcome())
          .append("\n");
    }

    prompt.append(
        "\n"
            + "INSTRUCTIONS: Write a commentary for this match as a dialogue between the"
            + " commentators. Each line MUST start with '[SPEAKER:Commentator Name]:'. Include all"
            + " participants. Mention the referee and commentators. Follow the outcome exactly.");

    return prompt.toString();
  }

  /**
   * Generates a prompt for summarizing a narration.
   *
   * @param narration The narration to summarize.
   * @return The generated prompt.
   */
  public String generateSummaryPrompt(String narration) {
    return "Summarize the following wrestling narration in 2-3 sentences, focusing on the key"
        + " moments and the outcome:\n\n"
        + narration;
  }
}
