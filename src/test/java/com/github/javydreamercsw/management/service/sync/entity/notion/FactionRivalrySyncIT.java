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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.FactionRivalryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("Faction Rivalry Sync Integration Tests")
class FactionRivalrySyncIT extends ManagementIntegrationTest {

  @MockitoBean private NotionHandler notionHandler;
  @Mock private FactionRivalryPage newPage;
  @Autowired private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    // Mock NotionHandler to return external IDs for factions and rivalries
    String newPageId = UUID.randomUUID().toString();
    when(newPage.getId()).thenReturn(newPageId);
    when(notionHandler.getDatabaseId("Faction Heat")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });
    Mockito.when(notionHandler.getDatabasePageIds("Segments"))
        .thenAnswer(
            invocation -> {
              // Return a mock external ID for any page creation/update
              return List.of(newPage);
            });
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database successfully")
  @Transactional
  void shouldSyncFactionRivalriesFromNotionToDatabaseSuccessfully() {
    // Given
    String faction1Name = "Test Faction 1";
    Wrestler member1Faction1 = createTestWrestler("Member 1 for Faction 1");
    Faction f1 = new Faction();
    f1.setName(faction1Name);
    f1.setIsActive(true);
    f1.addMember(member1Faction1);
    f1.setExternalId("mock-external-id-faction1"); // Set externalId manually
    factionRepository.saveAndFlush(f1);

    String faction2Name = "Test Faction 2";
    Wrestler member1Faction2 = createTestWrestler("Member 1 for Faction 2");
    Faction f2 = new Faction();
    f2.setName(faction2Name);
    f2.setIsActive(true);
    f2.addMember(member1Faction2);
    f2.setExternalId("mock-external-id-faction2"); // Set externalId manually
    factionRepository.saveAndFlush(f2);

    FactionRivalry newRivalry = new FactionRivalry();
    newRivalry.setFaction1(f1);
    newRivalry.setFaction2(f2);
    newRivalry.setHeat(10);
    newRivalry.setIsActive(true);
    newRivalry.setExternalId("mock-external-id-rivalry"); // Set externalId manually
    factionRivalryRepository.saveAndFlush(newRivalry);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Faction 1", f1.getName());
    properties.put("Faction 2", f2.getName());
    properties.put("Heat", newRivalry.getHeat());
    when(newPage.getRawProperties()).thenReturn(properties);

    // When - Sync faction rivalries from real Notion database
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactionRivalries(
            "test-operation-faction-rivalry-123", SyncDirection.INBOUND);

    // Then - Verify sync completed successfully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify database state is consistent
    List<FactionRivalry> finalRivalries = factionRivalryRepository.findAll();
    assertThat(finalRivalries).hasSize(1);
    // Reload the rivalry to ensure its associations are eagerly fetched or within the session
    FactionRivalry rivalry =
        factionRivalryRepository.findById(finalRivalries.get(0).getId()).orElseThrow();
    assertThat(rivalry.getExternalId()).isNotNull();
    assertThat(rivalry.getHeat()).isEqualTo(5);
    assertThat(rivalry.getFaction1().getName()).isEqualTo(faction1Name);
    assertThat(rivalry.getFaction2().getName()).isEqualTo(faction2Name);
  }
}
