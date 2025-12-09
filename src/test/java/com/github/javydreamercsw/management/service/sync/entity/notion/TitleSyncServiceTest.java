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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.spy;

import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitleSyncServiceTest extends AbstractSyncTest {

  @Mock private TitleService titleService;
  @Mock private TitleNotionSyncService titleNotionSyncService;

  private TitleSyncService titleSyncService;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp();

    titleSyncService =
        new TitleSyncService(
            objectMapper,
            syncServiceDependencies,
            titleService,
            titleNotionSyncService,
            notionApiExecutor);

    lenient()
        .when(titleService.save(any(Title.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
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

    Title existingTitle = spy(new Title());
    existingTitle.setId(100L);
    existingTitle.setGender(Gender.MALE);
    existingTitle.setName("ATW World");

    TitlePage titlePage = mock(TitlePage.class);
    when(titlePage.getChampionRelationIds())
        .thenReturn(Collections.singletonList("champion-ext-id"));
    when(titlePage.getContenderRelationIds())
        .thenReturn(Collections.singletonList("contender-ext-id"));
    Map<String, Object> rawProps = new HashMap<>();
    rawProps.put("Name", "ATW World");
    when(titlePage.getGender()).thenReturn("MALE");

    when(syncProperties.isEntityEnabled("titles")).thenReturn(true);
    lenient().when(notionHandler.loadAllTitles()).thenReturn(Collections.singletonList(titlePage));
    lenient().when(titleService.findByName(any())).thenReturn(Optional.of(existingTitle));
    lenient()
        .when(wrestlerRepository.findByExternalId("champion-ext-id"))
        .thenReturn(Optional.of(champion));
    lenient()
        .when(wrestlerRepository.findByExternalId("contender-ext-id"))
        .thenReturn(Optional.of(contender));

    // Act
    SyncResult result = titleSyncService.syncTitles("test-op");

    // Assert
    assertThat(result.isSuccess()).isTrue();

    ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
    verify(super.titleRepository, atLeastOnce()).saveAndFlush(titleCaptor.capture());

    Title finalSave = titleCaptor.getValue();
    assertFalse(finalSave.isVacant());
    assertEquals(1, finalSave.getCurrentChampions().size());
    assertEquals("Champion Wrestler", finalSave.getCurrentChampions().get(0).getName());
    assertNotNull(finalSave.getContender());
    assertEquals("Contender Wrestler", finalSave.getContender().get(0).getName());
  }
}
