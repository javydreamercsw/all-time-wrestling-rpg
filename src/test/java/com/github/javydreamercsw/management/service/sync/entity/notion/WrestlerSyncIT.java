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

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@TestPropertySource(properties = "notion.sync.load-from-json=false")
class WrestlerSyncIT extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;
  @Autowired private SyncSessionManager syncSessionManager;
  @Autowired private TierRecalculationService tierRecalculationService;

  @MockitoBean private NotionHandler notionHandler;

  private WrestlerPage wrestlerPage;

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
    wrestlerPage = Mockito.mock(WrestlerPage.class);

    // Create some wrestlers to establish tiers
    Stream.of(
            TestUtils.createWrestler("W1", 200_000L),
            TestUtils.createWrestler("W2", 120_000L),
            TestUtils.createWrestler("W3", 80_000L),
            TestUtils.createWrestler("W4", 50_000L),
            TestUtils.createWrestler("W5", 30_000L),
            TestUtils.createWrestler("W6", 10_000L))
        .forEach(wrestlerRepository::save);

    tierRecalculationService.recalculateRanking(new ArrayList<>(wrestlerRepository.findAll()));
  }

  @Test
  @DisplayName("Should sync wrestlers from Notion")
  void shouldSyncWrestlersFromNotion() {
    log.info("🧪 Verifying wrestler sync from Notion...");

    // Given
    String wrestlerId = UUID.randomUUID().toString();
    when(wrestlerPage.getId()).thenReturn(wrestlerId);
    when(wrestlerPage.getRawProperties())
        .thenReturn(Map.of("Name", "Test Wrestler", "Fans", 100_000L));

    when(notionHandler.loadAllWrestlers()).thenReturn(List.of(wrestlerPage));

    // When
    BaseSyncService.SyncResult result = wrestlerSyncService.syncWrestlers("wrestler-sync-test");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify the wrestler in the database
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findByExternalId(wrestlerId);
    assertThat(wrestlerOpt).isPresent();
    Wrestler wrestler = wrestlerOpt.get();
    assertThat(wrestler.getName()).isEqualTo("Test Wrestler");
    assertThat(wrestler.getFans()).isEqualTo(100_000L);

    // Test update
    syncSessionManager.clearSyncSession(); // Reset session to allow second sync
    when(wrestlerPage.getRawProperties())
        .thenReturn(Map.of("Name", "Test Wrestler Updated", "Fans", 120_000L));

    wrestlerSyncService.syncWrestlers("wrestler-sync-test-2");

    assertThat(wrestlerRepository.findAll()).hasSize(7);
    Optional<Wrestler> updatedWrestlerOpt = wrestlerRepository.findByExternalId(wrestlerId);
    assertThat(updatedWrestlerOpt).isPresent();
    Wrestler updatedWrestler = updatedWrestlerOpt.get();
    assertThat(updatedWrestler.getName()).isEqualTo("Test Wrestler Updated");
    assertThat(updatedWrestler.getFans()).isEqualTo(120_000L);
  }
}
