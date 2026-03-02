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

import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.Instant;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@DisplayName("Injury Sync Integration Tests")
class InjurySyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private InjuryRepository injuryRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @MockitoBean private NotionHandler notionHandler;

  private InjuryPage injuryPage;
  private Wrestler wrestler;

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
    injuryPage = Mockito.mock(InjuryPage.class);

    wrestler = createTestWrestler("Test Wrestler");
    wrestler.setExternalId("test-wrestler-id");
    wrestlerRepository.saveAndFlush(wrestler);
  }

  @Test
  @DisplayName("Should sync individual injuries from Notion")
  void shouldSyncInjuriesFromNotion() {
    log.info("🏥 Testing individual injuries sync from Notion");

    // Given
    String injuryId = UUID.randomUUID().toString();
    when(injuryPage.getId()).thenReturn(injuryId);

    Instant injuryDate = Instant.now();

    when(injuryPage.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Broken Ribs",
                "Wrestler",
                Map.of("relation", List.of(Map.of("id", "test-wrestler-id"))),
                "Severity",
                "MODERATE",
                "Active",
                true,
                "Injury Date",
                injuryDate.toString(),
                "Health Penalty",
                2.0,
                "Stamina Penalty",
                1.0,
                "Healing Cost",
                5000.0,
                "Injury Notes",
                "Sustained during a ladder match."));

    when(notionHandler.loadAllInjuries()).thenReturn(List.of(injuryPage));

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncInjuries("test-injuries-op", SyncDirection.INBOUND);

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    transactionTemplate.executeWithoutResult(
        status -> {
          Optional<Injury> injuryOpt = injuryRepository.findByExternalId(injuryId);
          assertThat(injuryOpt).isPresent();
          Injury injury = injuryOpt.get();

          assertThat(injury.getName()).isEqualTo("Broken Ribs");
          assertThat(injury.getWrestler().getName()).isEqualTo("Test Wrestler");
          assertThat(injury.getSeverity()).isEqualTo(InjurySeverity.MODERATE);
          assertThat(injury.getIsActive()).isTrue();
          assertThat(injury.getHealthPenalty()).isEqualTo(2);
          assertThat(injury.getStaminaPenalty()).isEqualTo(1);
          assertThat(injury.getHealingCost()).isEqualTo(5000L);
          assertThat(injury.getInjuryNotes()).isEqualTo("Sustained during a ladder match.");
        });
  }
}
