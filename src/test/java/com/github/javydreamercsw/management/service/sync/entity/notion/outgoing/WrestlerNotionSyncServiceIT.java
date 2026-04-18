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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerNotionSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
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

class WrestlerNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Autowired private WrestlerService wrestlerService;

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
    when(notionHandler.getDatabaseId("Wrestlers")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    Universe defaultUniverse =
        universeRepository.save(Universe.builder().name("Default Universe").build());

    // Create a new Wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler " + UUID.randomUUID());
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setFans(500L);
    wrestlerStateRepository.save(state);

    // Ensure it has unsynced changes by setting updatedAt to the future
    wrestler.setUpdatedAt(java.time.Instant.now().plusSeconds(10));
    wrestlerRepository.saveAndFlush(wrestler);

    // Sync to Notion for the first time

    wrestlerNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(wrestler.getId());
    Wrestler updatedWrestler = wrestlerRepository.findById(wrestler.getId()).get();
    assertNotNull(updatedWrestler.getExternalId());
    assertEquals(newPageId, updatedWrestler.getExternalId());
    assertNotNull(updatedWrestler.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        wrestler.getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    // Sync to Notion again with updates
    WrestlerState updatedState =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    updatedState.setFans(1000L);
    wrestlerStateRepository.saveAndFlush(updatedState);

    // Ensure it's treated as changed
    updatedWrestler.setUpdatedAt(java.time.Instant.now().plusSeconds(10));
    wrestlerRepository.saveAndFlush(updatedWrestler);

    wrestlerNotionSyncService.syncToNotion("test-op-2");

    WrestlerState updatedState2 =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedState2.getFans().doubleValue(),
        capturedUpdateRequest.getProperties().get("Fans").getNumber());
  }
}
