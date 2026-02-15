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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoService {

  private final SegmentNarrationServiceFactory aiFactory;

  /**
   * Generates a retort from an opponent based on the player's promo text.
   *
   * @param playerText The text spoken by the player.
   * @param segment The current segment context.
   * @param opponent The wrestler responding to the player.
   * @return The AI-generated retort.
   */
  public String generateRetort(String playerText, Segment segment, Wrestler opponent) {
    SegmentNarrationService aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) {
      log.warn("No AI service available for promo generation.");
      return "The opponent stares at you in silence.";
    }

    String prompt = buildPromoPrompt(playerText, segment, opponent);
    return aiService.generateText(prompt);
  }

  private String buildPromoPrompt(String playerText, Segment segment, Wrestler opponent) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("You are roleplaying as professional wrestler ")
        .append(opponent.getName())
        .append(".\n");
    prompt.append("Your personality/gimmick: ").append(opponent.getDescription()).append("\n");
    prompt.append("Your alignment: ").append(opponent.getAlignment()).append("\n");

    // Add context about the show/feud if available
    if (segment.getShow() != null) {
      prompt.append("Current Show: ").append(segment.getShow().getName()).append("\n");
    }

    prompt.append("\nThe player (your opponent) just said:\n\"").append(playerText).append("\"\n");
    prompt.append("\nRespond directly to them with a short, impactful retort (2-3 sentences). ");
    prompt.append("Stay in character. Do not use stage directions unless necessary.");

    return prompt.toString();
  }
}
