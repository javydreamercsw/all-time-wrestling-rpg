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
import com.github.javydreamercsw.base.ai.notion.RivalryPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("Rivalry Sync Integration Tests")
class RivalrySyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockitoBean private NotionHandler notionHandler;
  @Mock private RivalryPage rivalryPage;

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

  @Test
  @DisplayName("Should sync rivalries from Notion to database successfully")
  @Transactional
  void shouldSyncRivalriesFromNotionToDatabaseSuccessfully() {
    // Given
    String wrestler1Name = "Test Wrestler 1";
    String wrestler2Name = "Test Wrestler 2";
    wrestlerRepository.saveAndFlush(createTestWrestler(wrestler1Name));
    wrestlerRepository.saveAndFlush(createTestWrestler(wrestler2Name));

    String rivalryId = UUID.randomUUID().toString();
    when(rivalryPage.getId()).thenReturn(rivalryId);
    when(rivalryPage.getRawProperties())
        .thenReturn(Map.of("Wrestler 1", wrestler1Name, "Wrestler 2", wrestler2Name, "Heat", "10"));
    when(notionHandler.loadAllRivalries()).thenReturn(List.of(rivalryPage));

    // When - Sync rivalries from real Notion database
    BaseSyncService.SyncResult result =
        notionSyncService.syncRivalries("test-operation-rivalry-123", SyncDirection.INBOUND);

    // Then - Verify sync completed successfully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify database state is consistent
    List<Rivalry> finalRivalries = rivalryRepository.findAll();
    assertThat(finalRivalries).hasSize(1);
    Rivalry rivalry = finalRivalries.get(0);
    assertThat(rivalry.getExternalId()).isEqualTo(rivalryId);
    assertThat(rivalry.getHeat()).isEqualTo(10);
    assertThat(rivalry.getWrestler1().getName()).isEqualTo(wrestler1Name);
    assertThat(rivalry.getWrestler2().getName()).isEqualTo(wrestler2Name);
  }
}
