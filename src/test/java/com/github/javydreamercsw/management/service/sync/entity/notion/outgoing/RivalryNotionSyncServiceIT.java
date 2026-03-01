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
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.RivalryNotionSyncService;
import java.time.Instant;
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

class RivalryNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private RivalryNotionSyncService rivalryNotionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private NotionClient notionClient;
  @Mock private Page newPage;

  @Captor private ArgumentCaptor<CreatePageRequest> createPageRequestCaptor;
  @Captor private ArgumentCaptor<UpdatePageRequest> updatePageRequestCaptor;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    when(notionHandler.createNotionClient()).thenReturn(java.util.Optional.of(notionClient));

    String newPageId = UUID.randomUUID().toString();
    when(newPage.getId()).thenReturn(newPageId);

    when(notionClient.createPage(any(CreatePageRequest.class))).thenReturn(newPage);
    when(notionClient.updatePage(any(UpdatePageRequest.class))).thenReturn(newPage);
    when(notionHandler.getDatabaseId("Heat")).thenReturn("test-db-id");
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

    // Create a new Rivalry
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(15);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now());
    rivalry.setStorylineNotes("Test rivalry");
    rivalryRepository.save(rivalry);

    // Sync to Notion for the first time
    rivalryNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(rivalry.getId());
    Rivalry updatedRivalry = rivalryRepository.findById(rivalry.getId()).get();
    assertNotNull(updatedRivalry.getExternalId());
    assertEquals(newPageId, updatedRivalry.getExternalId());
    assertNotNull(updatedRivalry.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        rivalry.getDisplayName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
    assertEquals(
        wrestler1.getExternalId(),
        capturedRequest.getProperties().get("Wrestler 1").getRelation().get(0).getId());
    assertEquals(
        wrestler2.getExternalId(),
        capturedRequest.getProperties().get("Wrestler 2").getRelation().get(0).getId());

    // Sync to Notion again with updates
    updatedRivalry.setHeat(25);
    rivalryRepository.save(updatedRivalry);
    rivalryNotionSyncService.syncToNotion("test-op-2");
    Rivalry updatedRivalry2 = rivalryRepository.findById(rivalry.getId()).get();
    assertTrue(updatedRivalry2.getLastSync().isAfter(updatedRivalry.getLastSync()));

    // Verify updated properties
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedRivalry2.getHeat().doubleValue(),
        capturedUpdateRequest.getProperties().get("Heat").getNumber());
  }
}
