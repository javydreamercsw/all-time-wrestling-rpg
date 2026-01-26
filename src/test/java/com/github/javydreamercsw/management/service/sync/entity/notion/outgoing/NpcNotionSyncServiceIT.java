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
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.NpcNotionSyncService;
import dev.failsafe.FailsafeException;
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
class NpcNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private NpcRepository npcRepository;
  @Autowired private NpcNotionSyncService npcNotionSyncService;
  @Autowired private NotionHandler notionHandler;

  @Test
  void testSyncToNotion() {
    Npc npc = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      // Create a new Npc
      npc = new Npc();
      npc.setName("Test NPC " + UUID.randomUUID());
      npc.setNpcType("Referee");
      npcRepository.save(npc);

      // Sync to Notion for the first time
      npcNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(npc.getId());
      Npc updatedNpc = npcRepository.findById(npc.getId()).get();
      assertNotNull(updatedNpc.getExternalId());
      assertNotNull(updatedNpc.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedNpc.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedNpc.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals("Referee", Objects.requireNonNull(props.get("Role").getSelect()).getName());

      // Sync to Notion again
      updatedNpc.setName("Test NPC Updated " + UUID.randomUUID());
      updatedNpc.setNpcType("Commentator");
      npcRepository.save(updatedNpc);
      npcNotionSyncService.syncToNotion("test-op-2");
      Npc updatedNpc2 = npcRepository.findById(npc.getId()).get();
      assertTrue(updatedNpc2.getLastSync().isAfter(updatedNpc.getLastSync()));

      // Verify updated properties
      page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedNpc.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedNpc2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals("Commentator", Objects.requireNonNull(props.get("Role").getSelect()).getName());

    } finally {
      if (npc != null && npc.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(npc.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        npcRepository.delete(npc);
      } else if (npc != null && npc.getId() != null) {
        npcRepository.delete(npc);
      }
    }
  }
}
