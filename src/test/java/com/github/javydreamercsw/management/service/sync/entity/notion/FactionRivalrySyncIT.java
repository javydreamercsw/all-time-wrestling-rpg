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

import com.github.javydreamercsw.base.ai.notion.FactionRivalryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("Faction Rivalry Sync Integration Tests")
class FactionRivalrySyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private FactionRivalryPage factionRivalryPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database successfully")
  @Transactional
  void shouldSyncFactionRivalriesFromNotionToDatabaseSuccessfully() {
    // Given
    String faction1Name = "Test Faction 1";
    Wrestler member1Faction1 =
        createTestWrestler("Member 1 for Faction 1"); // This creates and saves managed wrestler
    Faction f1 = new Faction();
    f1.setName(faction1Name);
    f1.setIsActive(true);
    f1.addMember(member1Faction1); // Add managed wrestler to faction
    factionRepository.saveAndFlush(f1); // Save or merge the faction with its managed members

    String faction2Name = "Test Faction 2";
    Wrestler member1Faction2 = createTestWrestler("Member 1 for Faction 2");
    Faction f2 = new Faction();
    f2.setName(faction2Name);
    f2.setIsActive(true);
    f2.addMember(member1Faction2);
    factionRepository.saveAndFlush(f2);

    String rivalryId = UUID.randomUUID().toString();
    when(factionRivalryPage.getId()).thenReturn(rivalryId);
    when(factionRivalryPage.getRawProperties())
        .thenReturn(Map.of("Faction 1", faction1Name, "Faction 2", faction2Name, "Heat", "10"));
    when(notionHandler.loadAllFactionRivalries()).thenReturn(List.of(factionRivalryPage));

    // When - Sync faction rivalries from real Notion database
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactionRivalries("test-operation-faction-rivalry-123");

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
    assertThat(rivalry.getExternalId()).isEqualTo(rivalryId);
    assertThat(rivalry.getHeat()).isEqualTo(10);
    assertThat(rivalry.getFaction1().getName()).isEqualTo(faction1Name);
    assertThat(rivalry.getFaction2().getName()).isEqualTo(faction2Name);
  }
}
