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

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;

/** Generates prompts for AI services. */
public class PromptGenerator {

  /**
   * Generates a prompt for narrating a match.
   *
   * @param segmentContext The context of the segment to narrate.
   * @return The generated prompt.
   */
  public String generateMatchNarrationPrompt(SegmentNarrationContext segmentContext) {
    // Implementation to generate the match narration prompt
    return "Narrate the following match: " + segmentContext.toString();
  }

  /**
   * Generates a prompt for summarizing a narration.
   *
   * @param narration The narration to summarize.
   * @return The generated prompt.
   */
  public String generateSummaryPrompt(String narration) {
    // Implementation to generate the summary prompt
    return "Summarize the following narration: " + narration;
  }
}
