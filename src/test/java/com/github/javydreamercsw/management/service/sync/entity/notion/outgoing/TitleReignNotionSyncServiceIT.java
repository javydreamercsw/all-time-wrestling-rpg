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
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleReignNotionSyncService;
import java.time.Instant;
import java.util.List;
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

class TitleReignNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TitleReignNotionSyncService titleReignNotionSyncService;

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
    when(notionHandler.getDatabaseId("Title Reigns")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a Title
    Title title = new Title();
    title.setName("ATW World Championship");
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
    title.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MAIN_EVENTER);
    title.setExternalId(UUID.randomUUID().toString());
    titleRepository.save(title);

    // Create a Wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Champion");
    wrestler.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(wrestler);

    // Create a Title Reign
    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setChampions(List.of(wrestler));
    reign.setReignNumber(1);
    reign.setStartDate(Instant.now());
    reign.setNotes("Historical first reign");
    titleReignRepository.save(reign);

    // Ensure it has unsynced changes by setting updatedAt to the future
    reign.setUpdatedAt(java.time.Instant.now().plusSeconds(10));
    titleReignRepository.saveAndFlush(reign);

    // Sync to Notion for the first time

    titleReignNotionSyncService.syncToNotion("test-op-1");

    // Verify fields updated
    TitleReign updatedReign = titleReignRepository.findById(reign.getId()).get();
    assertNotNull(updatedReign.getExternalId());
    assertEquals(newPageId, updatedReign.getExternalId());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        String.format(
            "%s - Reign #%d (%s)", title.getName(), reign.getReignNumber(), wrestler.getName()),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
    assertEquals(
        title.getExternalId(),
        capturedRequest.getProperties().get("Title").getRelation().get(0).getId());
    assertEquals(
        wrestler.getExternalId(),
        capturedRequest.getProperties().get("Champion").getRelation().get(0).getId());

    // Sync to Notion again with updates
    updatedReign.setNotes("Updated historical notes");
    // Ensure it's treated as changed
    updatedReign.setUpdatedAt(java.time.Instant.now().plusSeconds(10));
    titleReignRepository.saveAndFlush(updatedReign);
    titleReignNotionSyncService.syncToNotion("test-op-2");

    TitleReign updatedReign2 = titleReignRepository.findById(reign.getId()).get();
    assertTrue(updatedReign2.getLastSync().isAfter(updatedReign.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedReign2.getNotes(),
        capturedUpdateRequest
            .getProperties()
            .get("Notes")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
  }
}
