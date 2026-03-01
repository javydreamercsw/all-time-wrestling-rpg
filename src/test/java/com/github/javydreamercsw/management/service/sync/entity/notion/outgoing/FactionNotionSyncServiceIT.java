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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionNotionSyncService;
import dev.failsafe.FailsafeException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
@TestPropertySource(properties = "test.mock.notion-handler=false")
class FactionNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private FactionRepository factionRepository;
  @Autowired private FactionNotionSyncService factionNotionSyncService;
  @Autowired private NotionHandler notionHandler;

  @Test
  void testSyncToNotion() {
    Faction faction = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      // Create a new faction
      faction = new Faction();
      faction.setName("Test Faction " + UUID.randomUUID());
      faction.setCreationDate(Instant.now());
      factionRepository.save(faction);

      // Sync to Notion for the first time
      BaseSyncService.SyncResult result = factionNotionSyncService.syncToNotion("test-op-1");
      assertTrue(result.isSuccess(), "Sync failed: " + result.getMessage());

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(faction.getId());
      Faction updatedFaction = factionRepository.findById(faction.getId()).get();
      assertNotNull(updatedFaction.getExternalId());
      assertNotNull(updatedFaction.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedFaction.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedFaction.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());

      // Sync to Notion again
      updatedFaction.setName("Test Faction Updated " + UUID.randomUUID());
      factionRepository.save(updatedFaction);
      factionNotionSyncService.syncToNotion("test-op-2");
      Faction updatedFaction2 = factionRepository.findById(faction.getId()).get();
      assertTrue(updatedFaction2.getLastSync().isAfter(updatedFaction.getLastSync()));

      // Verify updated name
      page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedFaction.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedFaction2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
    } finally {
      if (faction != null && faction.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(faction.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        factionRepository.delete(faction);
      } else if (faction != null && faction.getId() != null) {
        factionRepository.delete(faction);
      }
    }
  }
}
