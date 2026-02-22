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
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsGenerationService {

  private final NewsService newsService;
  private final SegmentNarrationServiceFactory aiFactory;
  private final ObjectMapper objectMapper;
  private final GameSettingService gameSettingService;
  private final InjuryRepository injuryRepository;
  private final SegmentRepository segmentRepository;
  private final EventAggregationService aggregationService;
  private final WrestlerRepository wrestlerRepository;
  private final NpcRepository npcRepository;
  private final Random random = new Random();

  private static final String SYSTEM_PROMPT =
      """
      You are a professional wrestling sports journalist.
      Based on the provided match results (can be a single segment or a whole show roundup), generate a news item or a backstage rumor.

      For show roundups, synthesize multiple results into 1-2 major headlines that highlight the most important events.

      Output MUST be a valid JSON object with the following fields:
      - headline: A catchy, sports-journalism style headline (max 255 chars).
      - content: A detailed summary of the event or rumor (max 2000 chars).
      - category: One of [BREAKING, RUMOR, ANALYSIS, INJURY, CONTRACT].
      - isRumor: Boolean, true if this is speculative or backstage gossip.
      - importance: Integer 1-5, where 5 is a major event like a title change or injury.
      """;

  private static final String RUMOR_PROMPT_ENHANCEMENT =
      """

      IMPORTANT INSTRUCTIONS FOR RUMORS:
      - You MUST ONLY use the names of wrestlers and NPCs provided in the 'Current Roster' list below.
      - DO NOT hallucinate or invent names of wrestlers not present in the provided list.
      - Focus on backstage drama, contract negotiations, or potential future matches between these specific individuals.
      """;

  private static final String MONTHLY_SYSTEM_PROMPT =
      """
      You are the Lead Analyst for the Wrestling World.
      You have been provided with a summary of the entire month's major events, including title changes and key match results.

      Your task is to write a 'Monthly State of the World' report. This should be a long-form, analytical piece that synthesizes these events into a cohesive narrative.

      - What were the defining moments?
      - Who are the rising stars?
      - What is the current landscape of the championships?

      Output MUST be a valid JSON object with the following fields:
      - headline: A formal, powerful headline for the monthly report (e.g., 'THE JANUARY RECAP: A Month of Betrayal and New Kings').
      - content: A comprehensive, multi-paragraph analysis (max 2000 chars).
      - category: 'ANALYSIS'.
      - isRumor: false.
      - importance: 5.
      """;

  @Transactional
  public void generateMonthlySynthesis() {
    if (!gameSettingService.isAiNewsEnabled()) return;

    SegmentNarrationService aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) return;

    EventAggregationService.MonthlySummary summary = aggregationService.getMonthlySummary();
    String context = aggregationService.formatMonthlySummary(summary);

    try {
      String response =
          aiService.generateText(
              MONTHLY_SYSTEM_PROMPT + "\n\nMonthly Summary Context:\n" + context);
      parseAndCreateNews(response);
    } catch (Exception e) {
      log.error("Failed to generate monthly synthesis news", e);
    }
  }

  @Transactional
  public void generateNewsForSegment(@NonNull Segment segment) {
    if (!gameSettingService.isAiNewsEnabled()) {
      log.debug("AI news generation is disabled. Creating fallback news.");
      createFallbackNews(segment);
      return;
    }

    if ("SEGMENT".equals(gameSettingService.getNewsStrategy()) && !isNewsWorthy(segment)) {
      log.debug("Segment is not news-worthy. Skipping news generation.");
      return;
    }

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
      parseAndCreateNews(response);
    } catch (Exception e) {
      log.error("Failed to generate AI news item", e);
      createFallbackNews(segment);
    }
  }

  @Transactional
  public void generateNewsForShow(@NonNull Show show) {
    if (!gameSettingService.isAiNewsEnabled()) return;

    SegmentNarrationService aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) return;

    StringBuilder context = new StringBuilder();
    context.append("Show: ").append(show.getName()).append("\n");
    context.append("Results Roundup:\n");

    List<Segment> segments = segmentRepository.findByShow(show);
    for (Segment s : segments) {
      String winners =
          s.getWinners().stream().map(Wrestler::getName).collect(Collectors.joining(", "));
      context
          .append("- ")
          .append(s.getSegmentType().getName())
          .append(": ")
          .append(winners)
          .append(" won");
      if (s.getIsTitleSegment()) context.append(" (TITLE MATCH)");
      context.append("\n");
    }

    try {
      String response =
          aiService.generateText(SYSTEM_PROMPT + "\n\nContext:\n" + context.toString());
      parseAndCreateNews(response);
    } catch (Exception e) {
      log.error("Failed to generate show roundup news", e);
    }
  }

  @Transactional(readOnly = true)
  public void rollForRumor() {
    if (!gameSettingService.isAiNewsEnabled()) return;

    int chance = gameSettingService.getNewsRumorChance();
    if (random.nextInt(100) < chance) {
      log.info("Rumor roll success! Generating rumor...");
      SegmentNarrationService aiService = aiFactory.getBestAvailableService();
      if (aiService != null && aiService.isAvailable()) {
        try {
          String rosterContext =
              Stream.concat(
                      wrestlerRepository.findAll().stream().map(Wrestler::getName),
                      npcRepository.findAll().stream().map(Npc::getName))
                  .collect(Collectors.joining(", "));

          String response =
              aiService.generateText(
                  SYSTEM_PROMPT
                      + RUMOR_PROMPT_ENHANCEMENT
                      + "\n\nContext: Generate a plausible backstage rumor about the current"
                      + " wrestling roster. It should be speculative and interesting."
                      + "\n\nCurrent Roster: "
                      + rosterContext);
          parseAndCreateNews(response);
        } catch (Exception e) {
          log.error("Failed to generate random rumor", e);
        }
      }
    }
  }

  private void parseAndCreateNews(String response) throws Exception {
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

  public boolean isNewsWorthy(@NonNull Segment segment) {
    // 1. Title matches are always news-worthy
    if (segment.getIsTitleSegment()) return true;

    // 2. Main events are news-worthy
    if (segment.isMainEvent()) return true;

    // 3. New injuries are news-worthy
    boolean hasInjuries =
        segment.getWrestlers().stream()
            .anyMatch(
                w ->
                    !injuryRepository
                        .findByWrestlerAndInjuryDate(w, segment.getSegmentDate())
                        .isEmpty());
    if (hasInjuries) return true;

    // 4. Rivalry conclusions or high heat could be added here later

    return false;
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
