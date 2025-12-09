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
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@TestPropertySource(properties = "notion.sync.load-from-json=false")
class WrestlerSyncIT extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private WrestlerPage wrestlerPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync wrestlers from Notion")
  void shouldSyncWrestlersFromNotion() {
    log.info("🧪 Verifying wrestler sync from Notion...");

    // Given
    String wrestlerId = UUID.randomUUID().toString();
    when(wrestlerPage.getId()).thenReturn(wrestlerId);
    when(wrestlerPage.getRawProperties())
        .thenReturn(Map.of("Name", "Test Wrestler", "Fans", 100000L));

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
    assertThat(wrestler.getFans()).isEqualTo(100000L);

    // Test update
    when(wrestlerPage.getRawProperties())
        .thenReturn(Map.of("Name", "Test Wrestler Updated", "Fans", 120000L));

    wrestlerSyncService.syncWrestlers("wrestler-sync-test-2");

    assertThat(wrestlerRepository.findAll()).hasSize(1);
    Optional<Wrestler> updatedWrestlerOpt = wrestlerRepository.findByExternalId(wrestlerId);
    assertThat(updatedWrestlerOpt).isPresent();
    Wrestler updatedWrestler = updatedWrestlerOpt.get();
    assertThat(updatedWrestler.getName()).isEqualTo("Test Wrestler Updated");
    assertThat(updatedWrestler.getFans()).isEqualTo(120000L);
  }
}
