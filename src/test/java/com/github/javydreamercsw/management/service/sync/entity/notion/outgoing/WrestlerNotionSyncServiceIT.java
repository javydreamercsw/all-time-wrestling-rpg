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
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerNotionSyncService;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
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
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerNotionSyncService wrestlerNotionSyncService;

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
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(notionClient));

    String newPageId = UUID.randomUUID().toString();
    when(newPage.getId()).thenReturn(newPageId);

    when(notionClient.createPage(any(CreatePageRequest.class))).thenReturn(newPage);
    when(notionClient.updatePage(any(UpdatePageRequest.class))).thenReturn(newPage);
    when(notionHandler.getDatabaseId("Wrestlers")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  // The argument is a Supplier<Page>
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a new faction
    Faction faction = new Faction();
    faction.setName("Test Faction " + UUID.randomUUID());
    factionRepository.save(faction);

    // Create a new wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler " + UUID.randomUUID());
    wrestler.setStartingStamina(16);
    wrestler.setFans(1000L);
    wrestler.setBumps(1);
    wrestler.setGender(Gender.MALE);
    wrestler.setLowStamina(2);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(4);
    wrestler.setDeckSize(15);
    wrestler.setTier(WrestlerTier.MIDCARDER);
    wrestler.setCreationDate(Instant.now());
    wrestler.setFaction(faction);
    wrestlerRepository.save(wrestler);

    PageProperty nameProperty = Mockito.mock(PageProperty.class);
    PageProperty.RichText richText = Mockito.mock(PageProperty.RichText.class);
    PageProperty.RichText.Text text = Mockito.mock(PageProperty.RichText.Text.class);

    Map<String, PageProperty> propertiesMap = new HashMap<>();
    propertiesMap.put("Name", nameProperty);

    when(newPage.getProperties()).thenReturn(propertiesMap);
    when(nameProperty.getTitle()).thenReturn(Collections.singletonList(richText));
    when(richText.getText()).thenReturn(text);
    when(text.getContent()).thenReturn(wrestler.getName());

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
    assertEquals(
        wrestler.getFans().doubleValue(), capturedRequest.getProperties().get("Fans").getNumber());

    // Sync to Notion again
    updatedWrestler.setName("Test Wrestler Updated " + UUID.randomUUID());
    wrestlerRepository.save(updatedWrestler);
    // Update the mock to return the new name
    when(text.getContent()).thenReturn(updatedWrestler.getName());
    wrestlerNotionSyncService.syncToNotion("test-op-2");
    Wrestler updatedWrestler2 = wrestlerRepository.findById(wrestler.getId()).get();
    assertTrue(updatedWrestler2.getLastSync().isAfter(updatedWrestler.getLastSync()));

    // Verify updated name sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedWrestler2.getName(),
        capturedUpdateRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());

    assertEquals(
        updatedWrestler.getName(),
        newPage.getProperties().get("Name").getTitle().get(0).getText().getContent());
  }
}
