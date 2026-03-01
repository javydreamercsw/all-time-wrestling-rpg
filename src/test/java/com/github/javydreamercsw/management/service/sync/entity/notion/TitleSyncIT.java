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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
class TitleSyncIT extends ManagementIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private TitlePage titlePage;

  private static MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeAll
  static void beforeAll() {
    mockedEnvironmentVariableUtil = Mockito.mockStatic(EnvironmentVariableUtil.class);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::getNotionToken)
        .thenReturn("test-token");
  }

  @AfterAll
  static void afterAll() {
    if (mockedEnvironmentVariableUtil != null) {
      mockedEnvironmentVariableUtil.close();
    }
  }

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should Sync Titles From Notion with all properties")
  void shouldSyncTitlesFromNotion() {
    log.info("🚀 Starting real title sync integration test...");

    // Given
    String titleId = UUID.randomUUID().toString();
    when(titlePage.getId()).thenReturn(titleId);
    when(titlePage.getRawProperties()).thenReturn(Map.of("Name", "Test Title"));
    when(titlePage.getTier()).thenReturn("Main Event");
    when(titlePage.getGender()).thenReturn("MALE");
    when(titlePage.getChampionshipType()).thenReturn("SINGLE");
    when(titlePage.getIsActive()).thenReturn(true);
    when(titlePage.getDefenseFrequency()).thenReturn(30);

    when(notionHandler.loadAllTitles()).thenReturn(List.of(titlePage));

    // Act
    BaseSyncService.SyncResult result =
        notionSyncService.syncTitles("integration-test-titles", SyncDirection.INBOUND);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo(SyncEntityType.TITLES.getKey());
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<Title> titles = titleRepository.findAll();
    assertThat(titles).hasSize(1);
    Title title = titles.get(0);
    assertThat(title.getName()).isEqualTo("Test Title");
    assertThat(title.getExternalId()).isEqualTo(titleId);
    assertThat(title.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
    assertThat(title.getDescription()).isEqualTo("Test Description");
    assertThat(title.getChampionshipType()).isEqualTo(ChampionshipType.SINGLE);
    assertThat(title.getGender()).isEqualTo(Gender.MALE);
    assertThat(title.getIncludeInRankings()).isTrue();
    assertThat(title.getIsActive()).isTrue();
    assertThat(title.getDefenseFrequency()).isEqualTo(30);

    log.info("✅ Title sync completed successfully!");
  }
}
