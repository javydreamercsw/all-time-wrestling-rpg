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
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@DisplayName("Injury Sync Integration Tests")
class InjurySyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private InjuryTypeRepository injuryTypeRepository;
  @Autowired private InjurySyncService injurySyncService;
  @MockitoBean private NotionHandler notionHandler;

  @Mock private InjuryPage injuryPage1;
  @Mock private InjuryPage injuryPage2;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync injury types from Notion")
  void shouldSyncInjuryTypesFromNotion() {
    log.info("🏥 Testing injury types sync from Notion");

    // Given
    String injury1Id = UUID.randomUUID().toString();
    when(injuryPage1.getId()).thenReturn(injury1Id);
    when(injuryPage1.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Head Injury",
                "Health Effect",
                -1,
                "Stamina Effect",
                0,
                "Card Effect",
                0,
                "Special Effects",
                "Cannot use headbutts."));

    String injury2Id = UUID.randomUUID().toString();
    when(injuryPage2.getId()).thenReturn(injury2Id);
    when(injuryPage2.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Leg Injury",
                "Health Effect",
                0,
                "Stamina Effect",
                -1,
                "Card Effect",
                0,
                "Special Effects",
                "Cannot use leg attacks."));

    when(notionHandler.loadAllInjuries()).thenReturn(List.of(injuryPage1, injuryPage2));

    // When - Sync injury types from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncInjuryTypes("integration-test-operation", SyncDirection.INBOUND);

    // Then - Verify sync was successful
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Injuries");
    assertThat(result.getSyncedCount()).isEqualTo(2);
    assertThat(result.getErrorCount()).isEqualTo(0);
    assertThat(result.getErrorMessage()).isNull();

    log.info("✅ Sync result: {} injury types synced", result.getSyncedCount());

    // Verify database state
    List<InjuryType> allInjuryTypes = injuryTypeRepository.findAll();
    assertThat(allInjuryTypes).hasSize(2);

    InjuryType headInjury =
        allInjuryTypes.stream()
            .filter(it -> it.getInjuryName().equals("Head Injury"))
            .findFirst()
            .get();
    assertThat(headInjury.getExternalId()).isEqualTo(injury1Id);
    assertThat(headInjury.getHealthEffect()).isEqualTo(-1);
    assertThat(headInjury.getSpecialEffects()).isEqualTo("Cannot use headbutts.");

    // Run sync again to test updates and no duplicates
    when(injuryPage1.getRawProperties())
        .thenReturn(
            Map.of(
                "Name",
                "Head Injury",
                "Health Effect",
                -2, // Changed
                "Stamina Effect",
                0,
                "Card Effect",
                0,
                "Special Effects",
                "Cannot use headbutts. Dizzy."));
    injurySyncService.clearSyncSession();
    BaseSyncService.SyncResult secondResult =
        notionSyncService.syncInjuryTypes("second-sync-operation", SyncDirection.INBOUND);
    assertThat(secondResult.isSuccess()).isTrue();
    assertThat(injuryTypeRepository.findAll()).hasSize(2); // No new injuries

    InjuryType updatedHeadInjury = injuryTypeRepository.findByExternalId(injury1Id).get();
    assertThat(updatedHeadInjury.getHealthEffect()).isEqualTo(-2);
    assertThat(updatedHeadInjury.getSpecialEffects()).isEqualTo("Cannot use headbutts. Dizzy.");
  }
}
