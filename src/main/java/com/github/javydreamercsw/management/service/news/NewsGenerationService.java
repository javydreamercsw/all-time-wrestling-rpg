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
package com.github.javydreamercsw.management.service.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsGenerationService {

  private final NewsService newsService;
  private final SegmentNarrationServiceFactory aiFactory;
  private final ObjectMapper objectMapper;

  private static final String SYSTEM_PROMPT =
      """
      You are a professional wrestling sports journalist.
      Based on the provided match results, generate a news item or a backstage rumor.

      Output MUST be a valid JSON object with the following fields:
      - headline: A catchy, sports-journalism style headline (max 255 chars).
      - content: A detailed summary of the event or rumor (max 2000 chars).
      - category: One of [BREAKING, RUMOR, ANALYSIS, INJURY, CONTRACT].
      - isRumor: Boolean, true if this is speculative or backstage gossip.
      - importance: Integer 1-5, where 5 is a major event like a title change or injury.
      """;

  public void generateNewsForSegment(@NonNull Segment segment) {
    SegmentNarrationService aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) {
      log.warn("No AI service available for news generation. Creating fallback news.");
      createFallbackNews(segment);
      return;
    }

    String winners =
        segment.getWinners().stream().map(Wrestler::getName).collect(Collectors.joining(", "));

    String losers =
        segment.getWrestlers().stream()
            .filter(w -> !segment.getWinners().contains(w))
            .map(Wrestler::getName)
            .collect(Collectors.joining(", "));

    StringBuilder prompt = new StringBuilder();
    prompt.append("Show: ").append(segment.getShow().getName()).append("\n");
    prompt.append("Match Type: ").append(segment.getSegmentType().getName()).append("\n");
    prompt.append("Winners: ").append(winners).append("\n");
    prompt.append("Losers: ").append(losers).append("\n");
    if (segment.getIsTitleSegment()) {
      prompt.append("This was a TITLE match!\n");
    }
    if (segment.getNarration() != null && !segment.getNarration().isEmpty()) {
      prompt.append("Match Highlights: ").append(segment.getNarration()).append("\n");
    }

    try {
      String response =
          aiService.generateText(SYSTEM_PROMPT + "\n\nContext:\n" + prompt.toString());

      // Clean JSON if needed (some LLMs might wrap it in ```json)
      if (response.contains("```json")) {
        response = response.substring(response.indexOf("```json") + 7);
        response = response.substring(0, response.lastIndexOf("```"));
      } else if (response.contains("```")) {
        response = response.substring(response.indexOf("```") + 3);
        response = response.substring(0, response.lastIndexOf("```"));
      }

      NewsDTO dto = objectMapper.readValue(response, NewsDTO.class);
      newsService.createNewsItem(
          dto.getHeadline(),
          dto.getContent(),
          NewsCategory.valueOf(dto.getCategory().toUpperCase()),
          dto.getIsRumor(),
          dto.getImportance());
    } catch (Exception e) {
      log.error("Failed to generate AI news item", e);
      createFallbackNews(segment);
    }
  }

  private void createFallbackNews(Segment segment) {
    String winners =
        segment.getWinners().stream().map(Wrestler::getName).collect(Collectors.joining(", "));

    String headline = winners + " Victorious at " + segment.getShow().getName();
    String content =
        "In a hard-fought "
            + segment.getSegmentType().getName()
            + ", "
            + winners
            + " emerged victorious.";

    newsService.createNewsItem(
        headline, content, NewsCategory.BREAKING, false, segment.getIsTitleSegment() ? 5 : 3);
  }

  @Data
  @NoArgsConstructor
  public static class NewsDTO {
    private String headline;
    private String content;
    private String category;
    private Boolean isRumor;
    private Integer importance;
  }
}
