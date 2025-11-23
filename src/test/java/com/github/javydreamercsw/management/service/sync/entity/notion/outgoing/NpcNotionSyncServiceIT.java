package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class NpcNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private NpcRepository npcRepository;
  @Autowired private NpcNotionSyncService npcNotionSyncService;

  @Test
  void testSyncToNotion() {
    Npc npc = null;
    Optional<NotionHandler> handlerOpt = NotionHandler.getInstance();
    if (handlerOpt.isEmpty()) {
      Assertions.fail("Notion credentials not configured, skipping test.");
    }
    NotionHandler handler = handlerOpt.get();
    Optional<NotionClient> clientOptional = handler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      // Create a new Npc
      npc = new Npc();
      npc.setName("Test NPC");
      npc.setNpcType("Referee");
      npcRepository.save(npc);

      // Sync to Notion for the first time
      npcNotionSyncService.syncToNotion(npc);

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(npc.getId());
      Npc updatedNpc = npcRepository.findById(npc.getId()).get();
      assertNotNull(updatedNpc.getExternalId());
      assertNotNull(updatedNpc.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedNpc.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          "Test NPC",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals("Referee", Objects.requireNonNull(props.get("Role").getSelect()).getName());

      // Sync to Notion again
      updatedNpc.setName("Test NPC Updated");
      updatedNpc.setNpcType("Commentator");
      npcNotionSyncService.syncToNotion(updatedNpc);
      Npc updatedNpc2 = npcRepository.findById(npc.getId()).get();
      Assertions.assertEquals(updatedNpc2.getLastSync(), updatedNpc.getLastSync());

      // Verify updated properties
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedNpc.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          "Test NPC Updated",
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
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        npcRepository.delete(npc);
      } else if (npc != null) {
        npcRepository.delete(npc);
      }
    }
  }
}
