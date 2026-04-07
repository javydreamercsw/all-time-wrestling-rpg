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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsGenerationServiceLazyInitTest {

  private NewsService newsService;
  private SegmentNarrationServiceFactory aiFactory;
  private ObjectMapper objectMapper;
  private GameSettingService gameSettingService;
  private InjuryRepository injuryRepository;
  private SegmentRepository segmentRepository;
  private EventAggregationService aggregationService;
  private WrestlerRepository wrestlerRepository;
  private NpcRepository npcRepository;
  private NewsGenerationService newsGenerationService;

  @BeforeEach
  void setUp() {
    newsService = mock(NewsService.class);
    aiFactory = mock(SegmentNarrationServiceFactory.class);
    objectMapper = new ObjectMapper();
    gameSettingService = mock(GameSettingService.class);
    injuryRepository = mock(InjuryRepository.class);
    segmentRepository = mock(SegmentRepository.class);
    aggregationService = mock(EventAggregationService.class);
    wrestlerRepository = mock(WrestlerRepository.class);
    npcRepository = mock(NpcRepository.class);

    newsGenerationService =
        new NewsGenerationService(
            newsService,
            aiFactory,
            objectMapper,
            gameSettingService,
            injuryRepository,
            segmentRepository,
            aggregationService,
            wrestlerRepository,
            npcRepository);
  }

  @Test
  void testGenerateMonthlySynthesis_ReturnsFalseWhenAiDisabled() {
    when(gameSettingService.isAiNewsEnabled()).thenReturn(false);

    boolean result = newsGenerationService.generateMonthlySynthesis();

    assertThat(result).isFalse();
  }

  @Test
  void testGenerateMonthlySynthesis_ReturnsFalseWhenNoServiceAvailable() {
    when(gameSettingService.isAiNewsEnabled()).thenReturn(true);
    when(aiFactory.getBestAvailableService()).thenReturn(null);

    boolean result = newsGenerationService.generateMonthlySynthesis();

    assertThat(result).isFalse();
  }
}
