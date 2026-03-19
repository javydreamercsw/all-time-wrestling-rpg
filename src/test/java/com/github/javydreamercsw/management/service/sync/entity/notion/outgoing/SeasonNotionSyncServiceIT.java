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
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonNotionSyncService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

class SeasonNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private SeasonRepository seasonRepository;
  @Autowired private SeasonNotionSyncService seasonNotionSyncService;

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
    when(notionHandler.getDatabaseId("Seasons")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a new Season
    Season season = new Season();
    season.setName("Test Season Sync " + UUID.randomUUID());
    season.setDescription("A test season for Notion sync operations");
    season.setStartDate(Instant.now().minus(7, ChronoUnit.DAYS));
    season.setIsActive(true);
    season.setShowsPerPpv(5);
    seasonRepository.save(season);

    // Sync to Notion for the first time
    seasonNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    Season updatedSeason = seasonRepository.findById(season.getId()).get();
    assertNotNull(updatedSeason.getExternalId());
    assertEquals(newPageId, updatedSeason.getExternalId());
    assertNotNull(updatedSeason.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        updatedSeason.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    // Sync to Notion again with updates
    updatedSeason.setName("Test Season Updated " + UUID.randomUUID());
    updatedSeason.setEndDate(Instant.now());
    updatedSeason.setIsActive(false);
    seasonRepository.save(updatedSeason);

    seasonNotionSyncService.syncToNotion("test-op-2");
    Season updatedSeason2 = seasonRepository.findById(season.getId()).get();
    assertTrue(updatedSeason2.getLastSync().isAfter(updatedSeason.getLastSync()));

    // Verify updated properties
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedSeason2.getName(),
        capturedUpdateRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
  }
}
