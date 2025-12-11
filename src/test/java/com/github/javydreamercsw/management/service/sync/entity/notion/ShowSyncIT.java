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
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class ShowSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockitoBean private NotionHandler notionHandler;
  @Mock private ShowPage showPage;

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
  @DisplayName("Should sync shows from Notion to database successfully")
  void shouldSyncShowsFromNotionToDatabaseSuccessfully() {
    // Given
    Season season = new Season();
    season.setName("Test Season");
    seasonRepository.save(season);

    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("A test show type");
    showTypeRepository.save(showType);

    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setName("Test Show Template");
    showTemplate.setShowType(showType);
    showTemplateRepository.save(showTemplate);

    String showId = UUID.randomUUID().toString();
    when(showPage.getId()).thenReturn(showId);
    when(showPage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Test Show",
                "Description",
                "Test Description",
                "Show Type",
                showType.getName(),
                "Date",
                LocalDate.now().toString(),
                "Season",
                season.getName(),
                "Template",
                showTemplate.getName()));

    when(notionHandler.getDatabasePageIds("Shows")).thenReturn(List.of(showId));
    when(notionHandler.loadShowById(showId)).thenReturn(Optional.of(showPage));

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncShows("test-operation", SyncDirection.INBOUND);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    List<Show> finalShows = showRepository.findAll();
    assertThat(finalShows).hasSize(1);
    Show show_ = finalShows.get(0);
    assertThat(show_.getExternalId()).isEqualTo(showId);
    assertThat(show_.getName()).isEqualTo("Test Show");
    assertThat(show_.getSeason().getName()).isEqualTo(season.getName());
    assertThat(show_.getType().getName()).isEqualTo(showType.getName());
    assertThat(show_.getTemplate().getName()).isEqualTo(showTemplate.getName());
  }
}
