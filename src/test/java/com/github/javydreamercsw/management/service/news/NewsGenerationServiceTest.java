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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsGenerationServiceTest {

  private NewsService newsService;
  private SegmentNarrationServiceFactory aiFactory;
  private SegmentNarrationService aiService;
  private NewsGenerationService newsGenerationService;
  private ObjectMapper objectMapper;
  private GameSettingService gameSettingService;
  private InjuryRepository injuryRepository;
  private SegmentRepository segmentRepository;

  @BeforeEach
  void setUp() {
    newsService = mock(NewsService.class);
    aiFactory = mock(SegmentNarrationServiceFactory.class);
    aiService = mock(SegmentNarrationService.class);
    gameSettingService = mock(GameSettingService.class);
    injuryRepository = mock(InjuryRepository.class);
    segmentRepository = mock(SegmentRepository.class);
    objectMapper = new ObjectMapper();
    newsGenerationService =
        new NewsGenerationService(
            newsService,
            aiFactory,
            objectMapper,
            gameSettingService,
            injuryRepository,
            segmentRepository);

    when(aiFactory.getBestAvailableService()).thenReturn(aiService);
    when(aiService.isAvailable()).thenReturn(true);
    when(gameSettingService.isAiNewsEnabled()).thenReturn(true);
  }

  @Test
  void testGenerateNewsForSegment() {
    Wrestler winner = Wrestler.builder().name("Winner").build();
    Wrestler loser = Wrestler.builder().name("Loser").build();

    Show show = new Show();
    show.setName("Test Show");

    SegmentType type = new SegmentType();
    type.setName("One on One");

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(type);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segment.setIsTitleSegment(false);

    String aiResponse =
        "{\"headline\": \"Winner Defeats Loser!\", \"content\": \"What a match.\", \"category\":"
            + " \"BREAKING\", \"isRumor\": false, \"importance\": 3}";
    when(aiService.generateText(anyString())).thenReturn(aiResponse);

    newsGenerationService.generateNewsForSegment(segment);

    verify(newsService, times(1))
        .createNewsItem(
            eq("Winner Defeats Loser!"),
            eq("What a match."),
            eq(NewsCategory.BREAKING),
            eq(false),
            eq(3));
  }

  @Test
  void testGenerateNewsWithJsonWrapping() {
    Wrestler winner = Wrestler.builder().name("Winner").build();
    Segment segment = new Segment();
    segment.setShow(new Show());
    segment.setSegmentType(new SegmentType());
    segment.getSegmentType().setName("Match");
    segment.setWinners(List.of(winner));
    segment.setIsTitleSegment(false);

    String aiResponse =
        """
        ```json
        {"headline": "Wrapped JSON", "content": "Content", "category": "BREAKING", "isRumor": false, "importance": 3}
        ```\
        """;
    when(aiService.generateText(anyString())).thenReturn(aiResponse);

    newsGenerationService.generateNewsForSegment(segment);

    verify(newsService)
        .createNewsItem(eq("Wrapped JSON"), anyString(), any(), anyBoolean(), anyInt());
  }

  @Test
  void testFallbackNews() {
    Wrestler winner = Wrestler.builder().name("Winner").build();
    Segment segment = new Segment();
    segment.setShow(new Show());
    segment.getShow().setName("Show");
    segment.setSegmentType(new SegmentType());
    segment.getSegmentType().setName("Match");
    segment.addParticipant(winner);
    segment.setWinners(List.of(winner));
    segment.setIsTitleSegment(true);

    // Force failure in AI service
    when(aiService.generateText(anyString())).thenThrow(new RuntimeException("AI Down"));

    newsGenerationService.generateNewsForSegment(segment);

    // Should create fallback news
    verify(newsService)
        .createNewsItem(
            eq("Winner Victorious at Show"),
            anyString(),
            eq(NewsCategory.BREAKING),
            eq(false),
            eq(5));
  }
}
