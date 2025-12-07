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

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@DisplayName("Faction Sync Integration Tests")
class FactionSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync factions from Notion to database successfully")
  @Transactional
  void shouldSyncFactionsFromNotionToDatabaseSuccessfully() {
    // Given
    String faction1Name = "Test Faction 1";
    Wrestler member1Faction1 =
        createTestWrestler("Member 1 for Faction 1"); // This creates and saves managed wrestler
    Faction f1 = new Faction();
    f1.setName(faction1Name);
    f1.setIsActive(true);
    f1.addMember(member1Faction1); // Add managed wrestler to faction
    factionRepository.saveAndFlush(f1); // Save or merge the faction with its managed members

    // When - Sync factions from real Notion database
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactions("test-operation-faction-123", SyncDirection.OUTBOUND);

    // Then - Verify sync completed successfully
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    // Verify database state is consistent
    List<Faction> factions = factionRepository.findAll();
    assertThat(factions).hasSize(1);
    Faction faction = factions.get(0);
    assertThat(faction.getExternalId()).isNotNull();
    assertThat(faction.getName()).isEqualTo(faction1Name);
  }
}
