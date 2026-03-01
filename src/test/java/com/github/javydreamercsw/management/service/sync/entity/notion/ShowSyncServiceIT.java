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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ShowSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeService showTypeService;

  @MockitoBean private NotionHandler notionHandler;
  @MockitoBean private NotionPageDataExtractor notionPageDataExtractor;

  private MockedStatic<EnvironmentVariableUtil> mockedEnv;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    mockedEnv = Mockito.mockStatic(EnvironmentVariableUtil.class);
    mockedEnv.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
  }

  @AfterEach
  public void tearDown() {
    if (mockedEnv != null) {
      mockedEnv.close();
    }
  }

  @Test
  void testSyncShows() {
    // Given
    String showId = UUID.randomUUID().toString();
    ShowPage showPage = new ShowPage();
    showPage.setId(showId);

    // Create a required ShowType
    ShowType testShowType = new ShowType();
    testShowType.setName("Weekly");
    testShowType.setDescription("Weekly show type");
    showTypeService.save(testShowType);

    when(notionHandler.getDatabasePageIds("Shows")).thenReturn(List.of(showId));
    when(notionHandler.loadShowById(showId)).thenReturn(Optional.of(showPage));
    when(notionHandler.loadAllShowsForSync()).thenReturn(List.of(showPage));
    when(notionPageDataExtractor.extractNameFromNotionPage(any())).thenReturn("Test Show");
    when(notionPageDataExtractor.extractDateFromNotionPage(any())).thenReturn(LocalDate.now());
    when(notionPageDataExtractor.extractIdFromNotionPage(any())).thenReturn(showId);
    when(notionPageDataExtractor.extractDescriptionFromNotionPage(any()))
        .thenReturn("Test Description");
    when(notionPageDataExtractor.extractRelationId(any(), Mockito.eq("Show Type")))
        .thenReturn(testShowType.getExternalId());
    when(notionPageDataExtractor.extractPropertyAsString(any(), Mockito.eq("Show Type")))
        .thenReturn("Weekly");

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncShows("test-op-shows", SyncDirection.INBOUND);

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCreatedCount()).isGreaterThanOrEqualTo(1);
  }
}
