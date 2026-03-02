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
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjuryNotionSyncService;
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

class InjuryNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private InjuryRepository injuryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private InjuryNotionSyncService injuryNotionSyncService;

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
    when(notionHandler.getDatabaseId("Injuries")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a new Wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler " + UUID.randomUUID());
    wrestler.setExternalId(UUID.randomUUID().toString());
    wrestlerRepository.save(wrestler);

    // Create a new Injury
    Injury injury = new Injury();
    injury.setName("Concussion");
    injury.setSeverity(InjurySeverity.SEVERE);
    injury.setHealthPenalty(10);
    injury.setStaminaPenalty(5);
    injury.setHandSizePenalty(2);
    injury.setIsActive(true);
    injury.setInjuryDate(Instant.now());
    injury.setInjuryNotes("Got hit with a chair");
    injury.setWrestler(wrestler);
    injuryRepository.save(injury);

    // Sync to Notion for the first time
    injuryNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(injury.getId());
    Injury updatedInjury = injuryRepository.findById(injury.getId()).get();
    assertNotNull(updatedInjury.getExternalId());
    assertEquals(newPageId, updatedInjury.getExternalId());
    assertNotNull(updatedInjury.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        injury.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
    assertEquals(
        wrestler.getExternalId(),
        capturedRequest.getProperties().get("Wrestler").getRelation().get(0).getId());

    // Sync to Notion again
    updatedInjury.setInjuryNotes("Recovery going well");
    injuryRepository.save(updatedInjury);
    injuryNotionSyncService.syncToNotion("test-op-2");
    Injury updatedInjury2 = injuryRepository.findById(injury.getId()).get();
    assertTrue(updatedInjury2.getLastSync().isAfter(updatedInjury.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedInjury2.getInjuryNotes(),
        capturedUpdateRequest
            .getProperties()
            .get("Injury Notes")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
  }
}
