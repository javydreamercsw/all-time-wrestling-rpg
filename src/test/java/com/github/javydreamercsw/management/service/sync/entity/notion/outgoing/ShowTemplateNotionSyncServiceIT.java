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
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTemplateNotionSyncService;
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

class ShowTemplateNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private ShowTemplateNotionSyncService showTemplateNotionSyncService;

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
    when(notionHandler.getDatabaseId("Show Templates")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a Show Type
    ShowType showType = new ShowType();
    showType.setName("Weekly");
    showType.setDescription("A weekly show type.");
    showType.setExpectedMatches(5);
    showType.setExpectedPromos(2);
    showType.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    showTypeRepository.save(showType);

    // Create a new Show Template
    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setName("Raw " + UUID.randomUUID());
    showTemplate.setDescription("Monday Night Raw Template");
    showTemplate.setShowType(showType);
    showTemplate.setNotionUrl("https://www.notion.so/test-url");
    showTemplateRepository.save(showTemplate);

    // Sync to Notion for the first time
    showTemplateNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(showTemplate.getId());
    ShowTemplate updatedShowTemplate = showTemplateRepository.findById(showTemplate.getId()).get();
    assertNotNull(updatedShowTemplate.getExternalId());
    assertEquals(newPageId, updatedShowTemplate.getExternalId());
    assertNotNull(updatedShowTemplate.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        showTemplate.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
    assertEquals(
        showTemplate.getDescription(),
        capturedRequest
            .getProperties()
            .get("Description")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
    assertEquals(
        showTemplate.getShowType().getExternalId(),
        capturedRequest.getProperties().get("Show Type").getRelation().get(0).getId());
    assertEquals(
        showTemplate.getNotionUrl(), capturedRequest.getProperties().get("Notion URL").getUrl());

    // Sync to Notion again
    updatedShowTemplate.setDescription("Updated description " + UUID.randomUUID());
    updatedShowTemplate.setNotionUrl("https://www.notion.so/updated-url");
    showTemplateRepository.save(updatedShowTemplate);
    showTemplateNotionSyncService.syncToNotion("test-op-2");
    ShowTemplate updatedShowTemplate2 = showTemplateRepository.findById(showTemplate.getId()).get();
    assertTrue(updatedShowTemplate2.getLastSync().isAfter(updatedShowTemplate.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedShowTemplate2.getDescription(),
        capturedUpdateRequest
            .getProperties()
            .get("Description")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
    assertEquals(
        updatedShowTemplate2.getNotionUrl(),
        capturedUpdateRequest.getProperties().get("Notion URL").getUrl());
  }
}
