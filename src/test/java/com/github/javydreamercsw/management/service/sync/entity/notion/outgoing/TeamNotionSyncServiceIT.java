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
package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.TeamNotionSyncService;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TeamNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TeamNotionSyncService teamNotionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private NotionClient notionClient;
  @Mock private Page newPage;

  @Captor private ArgumentCaptor<CreatePageRequest> createPageRequestCaptor;
  @Captor private ArgumentCaptor<UpdatePageRequest> updatePageRequestCaptor;

  @BeforeEach
  public void setup() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    when(notionHandler.createNotionClient()).thenReturn(java.util.Optional.of(notionClient));

    String newPageId = UUID.randomUUID().toString();
    when(newPage.getId()).thenReturn(newPageId);

    when(notionClient.createPage(any(CreatePageRequest.class))).thenReturn(newPage);
    when(notionClient.updatePage(any(UpdatePageRequest.class))).thenReturn(newPage);
    when(notionHandler.getDatabaseId("Teams")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create Wrestlers
    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler 1 " + UUID.randomUUID());
    wrestler1.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(wrestler1);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Wrestler 2 " + UUID.randomUUID());
    wrestler2.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(wrestler2);

    // Create a new Team
    Team team = new Team();
    team.setName("Test Team " + UUID.randomUUID());
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    teamRepository.save(team);

    // Sync to Notion for the first time
    teamNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(team.getId());
    Team updatedTeam = teamRepository.findById(team.getId()).get();
    assertNotNull(updatedTeam.getExternalId());
    assertEquals(newPageId, updatedTeam.getExternalId());
    assertNotNull(updatedTeam.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        team.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    // Sync to Notion again with updates
    updatedTeam.setName("Test Team Updated " + UUID.randomUUID());
    teamRepository.save(updatedTeam);
    teamNotionSyncService.syncToNotion("test-op-2");
    Team updatedTeam2 = teamRepository.findById(team.getId()).get();
    assertTrue(updatedTeam2.getLastSync().isAfter(updatedTeam.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedTeam2.getName(),
        capturedUpdateRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
  }
}
