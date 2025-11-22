package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class FactionNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private FactionRepository factionRepository;
  @Autowired private FactionNotionSyncService factionNotionSyncService;

  @Test
  void testSyncToNotion() {
    Faction faction = null;
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
      // Create a new faction
      faction = new Faction();
      faction.setName("Test Faction");
      faction.setCreationDate(Instant.now());
      factionRepository.save(faction);

      // Sync to Notion for the first time
      factionNotionSyncService.syncToNotion(faction);

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(faction.getId());
      Faction updatedFaction = factionRepository.findById(faction.getId()).get();
      assertNotNull(updatedFaction.getExternalId());
      assertNotNull(updatedFaction.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedFaction.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          "Test Faction",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());

      // Sync to Notion again
      updatedFaction.setName("Test Faction Updated");
      factionNotionSyncService.syncToNotion(updatedFaction);
      Faction updatedFaction2 = factionRepository.findById(faction.getId()).get();
      Assertions.assertEquals(updatedFaction2.getLastSync(), updatedFaction.getLastSync());

      // Verify updated name
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedFaction.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          "Test Faction Updated",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
    } finally {
      if (faction != null && faction.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(faction.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        factionRepository.delete(faction);
      } else if (faction != null) {
        factionRepository.delete(faction);
      }
    }
  }
}
