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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class TeamNotionSyncServiceTest extends AbstractSyncTest {

  private TeamNotionSyncService teamNotionSyncService;
  @Captor private ArgumentCaptor<Team> teamCaptor;

  @BeforeEach
  public void setUp() {
    super.setUp();
    teamNotionSyncService =
        new TeamNotionSyncService(teamRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Sync new team creates Notion page and saves external ID")
  void syncToNotion_newTeam_createsPageAndSavesExternalId() {
    String operationId = UUID.randomUUID().toString();

    Wrestler w1 = new Wrestler();
    w1.setName("Partner A");
    w1.setExternalId(UUID.randomUUID().toString());

    Wrestler w2 = new Wrestler();
    w2.setName("Partner B");
    w2.setExternalId(UUID.randomUUID().toString());

    Team team = new Team();
    team.setName("The Tag Champs");
    team.setWrestler1(w1);
    team.setWrestler2(w2);

    NotionClient client = mock(NotionClient.class);
    when(teamRepository.findAll()).thenReturn(List.of(team));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_teams");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = teamNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
    verify(teamRepository, times(1)).saveAndFlush(teamCaptor.capture());
    assertNotNull(teamCaptor.getValue().getExternalId());
  }

  @Test
  @DisplayName("Sync with no teams returns zero counts")
  void syncToNotion_empty_returnsZeroCounts() {
    String operationId = UUID.randomUUID().toString();

    NotionClient client = mock(NotionClient.class);
    when(teamRepository.findAll()).thenReturn(Collections.emptyList());
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_teams");

    var result = teamNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("Team with optional finisher and theme song synced without error")
  void syncToNotion_teamWithOptionalFields_syncedSuccessfully() {
    String operationId = UUID.randomUUID().toString();

    Team team = new Team();
    team.setName("The Express");
    team.setThemeSong("All Aboard");
    team.setTeamFinisher("Train Wreck");

    NotionClient client = mock(NotionClient.class);
    when(teamRepository.findAll()).thenReturn(List.of(team));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_teams");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = teamNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
  }
}
