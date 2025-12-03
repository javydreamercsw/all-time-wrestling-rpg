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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TitleSyncServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private NotionRateLimitService rateLimitService;

  @Mock
  private com.github.javydreamercsw.management.domain.title.TitleReignRepository
      titleReignRepository;

  private TitleSyncService titleSyncService;

  @BeforeEach
  void setUp() throws Exception {
    when(syncProperties.getParallelThreads()).thenReturn(1);
    titleSyncService = new TitleSyncService(new ObjectMapper(), syncProperties, notionHandler);

    // Manually inject the mocks using reflection
    ReflectionTestUtils.setField(titleSyncService, "titleRepository", titleRepository);
    ReflectionTestUtils.setField(titleSyncService, "wrestlerRepository", wrestlerRepository);
    ReflectionTestUtils.setField(titleSyncService, "titleService", titleService);
    ReflectionTestUtils.setField(titleSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(titleSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(titleSyncService, "rateLimitService", rateLimitService);
    ReflectionTestUtils.setField(titleSyncService, "titleReignRepository", titleReignRepository);
  }

  @Test
  void syncTitles_shouldUpdateChampionAndContender_whenRelationsExist() {
    // Arrange
    Wrestler champion = Wrestler.builder().build();
    champion.setId(1L);
    champion.setName("Champion Wrestler");

    Wrestler contender = Wrestler.builder().build();
    contender.setId(2L);
    contender.setName("Contender Wrestler");

    Title existingTitle = new Title();
    existingTitle.setId(100L);
    existingTitle.setName("ATW World");

    TitlePage titlePage = mock(TitlePage.class);
    when(titlePage.getChampionRelationIds())
        .thenReturn(Collections.singletonList("champion-ext-id"));
    when(titlePage.getContenderRelationIds())
        .thenReturn(Collections.singletonList("contender-ext-id"));

    when(syncProperties.isEntityEnabled("titles")).thenReturn(true);
    when(notionHandler.loadAllTitles()).thenReturn(Collections.singletonList(titlePage));
    when(titleService.findByName(any())).thenReturn(Optional.of(existingTitle));
    when(wrestlerRepository.findByExternalId("champion-ext-id")).thenReturn(Optional.of(champion));
    when(wrestlerRepository.findByExternalId("contender-ext-id"))
        .thenReturn(Optional.of(contender));

    // Act
    titleSyncService.syncTitles("test-op");

    // Assert
    ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
    verify(titleRepository, atLeastOnce()).saveAndFlush(titleCaptor.capture());

    Title finalSave = titleCaptor.getValue();

    assertFalse(finalSave.isVacant());
    assertEquals(1, finalSave.getCurrentChampions().size());
    assertEquals("Champion Wrestler", finalSave.getCurrentChampions().get(0).getName());
    assertNotNull(finalSave.getContender());
    assertEquals("Contender Wrestler", finalSave.getContender().get(0).getName());
  }
}
