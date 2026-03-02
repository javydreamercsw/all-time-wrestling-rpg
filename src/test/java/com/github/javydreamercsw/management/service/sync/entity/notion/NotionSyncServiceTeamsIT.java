/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotionSyncServiceTeamsIT extends ManagementIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @MockitoBean private NotionHandler notionHandler;
  @MockitoBean private NotionPageDataExtractor notionPageDataExtractor;
  @MockitoBean private WrestlerService wrestlerService;

  private MockedStatic<EnvironmentVariableUtil> mockedEnv;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    mockedEnv = Mockito.mockStatic(EnvironmentVariableUtil.class);
    mockedEnv.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
  }

  @AfterEach
  public void tearDown() {
    if (mockedEnv != null) {
      mockedEnv.close();
    }
  }

  @Test
  void testSyncTeams() {
    // Given
    String teamId = UUID.randomUUID().toString();
    TeamPage teamPage = new TeamPage();
    teamPage.setId(teamId);

    // Create required wrestlers for a new team
    Wrestler w1 = new Wrestler();
    w1.setName("Wrestler 1");
    w1.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(w1);

    Wrestler w2 = new Wrestler();
    w2.setName("Wrestler 2");
    w2.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(w2);

    // Mock TeamPage properties
    java.util.Map<String, Object> props = new java.util.HashMap<>();
    props.put("Member 1", w1.getExternalId());
    props.put("Member 2", w2.getExternalId());
    props.put("Status", Boolean.TRUE);
    teamPage.setRawProperties(props);

    when(notionHandler.isNotionTokenAvailable()).thenReturn(true);
    when(notionHandler.loadAllTeams()).thenReturn(List.of(teamPage));
    when(notionPageDataExtractor.extractNameFromNotionPage(any())).thenReturn("Test Team");
    when(notionPageDataExtractor.extractIdFromNotionPage(any())).thenReturn(teamId);

    when(wrestlerService.findByExternalId(w1.getExternalId())).thenReturn(Optional.of(w1));
    when(wrestlerService.findByExternalId(w2.getExternalId())).thenReturn(Optional.of(w2));

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncTeams("test-op-teams", SyncDirection.INBOUND);

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCreatedCount()).isEqualTo(1);
  }
}
