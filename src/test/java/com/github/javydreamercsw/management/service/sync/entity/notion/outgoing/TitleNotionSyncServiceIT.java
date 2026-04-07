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
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleNotionSyncService;
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

class TitleNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleNotionSyncService titleNotionSyncService;

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
    when(notionHandler.getDatabaseId("Championships")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a new Title
    Title title = new Title();
    title.setName("Test Title " + UUID.randomUUID());
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
    title.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MAIN_EVENTER);
    titleRepository.save(title);

    // Sync to Notion for the first time
    titleNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(title.getId());
    Title updatedTitle = titleRepository.findById(title.getId()).get();
    assertNotNull(updatedTitle.getExternalId());
    assertEquals(newPageId, updatedTitle.getExternalId());
    assertNotNull(updatedTitle.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        title.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    // Sync to Notion again with updates
    updatedTitle.setName("Test Title Updated " + UUID.randomUUID());
    titleRepository.save(updatedTitle);
    titleNotionSyncService.syncToNotion("test-op-2");
    Title updatedTitle2 = titleRepository.findById(title.getId()).get();
    assertTrue(updatedTitle2.getLastSync().isAfter(updatedTitle.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedTitle2.getName(),
        capturedUpdateRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
  }
}
