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
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowNotionSyncService;
import java.time.Instant;
import java.time.LocalDate;
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

class ShowNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowNotionSyncService showNotionSyncService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;

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
    when(notionHandler.getDatabaseId("Shows")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create dependencies
    ShowType testShowType = new ShowType();
    testShowType.setName("Test ShowType " + UUID.randomUUID());
    testShowType.setDescription("Test ShowType Description");
    testShowType.setExternalId(UUID.randomUUID().toString());
    showTypeRepository.save(testShowType);

    Season testSeason = new Season();
    testSeason.setName("Test Season " + UUID.randomUUID());
    testSeason.setStartDate(Instant.now());
    testSeason.setIsActive(true);
    testSeason.setExternalId(UUID.randomUUID().toString());
    seasonRepository.save(testSeason);

    ShowTemplate testShowTemplate = new ShowTemplate();
    testShowTemplate.setName("Test Template " + UUID.randomUUID());
    testShowTemplate.setShowType(testShowType);
    testShowTemplate.setExternalId(UUID.randomUUID().toString());
    showTemplateRepository.save(testShowTemplate);

    // Create a new Show
    Show show = new Show();
    show.setName("Test Show " + UUID.randomUUID());
    show.setDescription("A test wrestling show");
    show.setType(testShowType);
    show.setSeason(testSeason);
    show.setTemplate(testShowTemplate);
    show.setShowDate(LocalDate.now());
    showRepository.save(show);

    // Sync to Notion for the first time
    showNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    Show updatedShow = showRepository.findById(show.getId()).get();
    assertNotNull(updatedShow.getExternalId());
    assertEquals(newPageId, updatedShow.getExternalId());
    assertNotNull(updatedShow.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        updatedShow.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    // Sync to Notion again with updates
    updatedShow.setName("Test Show Updated " + UUID.randomUUID());
    showRepository.save(updatedShow);

    showNotionSyncService.syncToNotion("test-op-2");
    Show updatedShow2 = showRepository.findById(show.getId()).get();
    assertTrue(updatedShow2.getLastSync().isAfter(updatedShow.getLastSync()));

    // Verify updated properties
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedShow2.getName(),
        capturedUpdateRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
  }
}
