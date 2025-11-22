package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.RivalryNotionSyncService;
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
class RivalryNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private RivalryNotionSyncService rivalryNotionSyncService;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testSyncToNotion() {
    Rivalry rivalry = null;
    Wrestler wrestler1 = null;
    Wrestler wrestler2 = null;
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
      // Create a new wrestler
      wrestler1 = createTestWrestler("Test Wrestler 1");
      wrestlerRepository.save(wrestler1);
      // Create a new wrestler
      wrestler2 = createTestWrestler("Test Wrestler 2");
      wrestlerRepository.save(wrestler2);

      // Create a new rivalry
      rivalry = new Rivalry();
      rivalry.setWrestler1(wrestler1);
      rivalry.setWrestler2(wrestler2);
      rivalry.setCreationDate(Instant.now());
      rivalryRepository.save(rivalry);

      // Sync to Notion for the first time
      rivalryNotionSyncService.syncToNotion(rivalry);

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(rivalry.getId());
      Rivalry updatedRivalry = rivalryRepository.findById(rivalry.getId()).get();
      assertNotNull(updatedRivalry.getExternalId());
      assertNotNull(updatedRivalry.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedRivalry.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          rivalry.getDisplayName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());

    } finally {
      if (rivalry != null && rivalry.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(rivalry.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        rivalryRepository.delete(rivalry);
      } else if (rivalry != null) {
        rivalryRepository.delete(rivalry);
      }
      if (wrestler1 != null) {
        wrestlerRepository.delete(wrestler1);
      }
      if (wrestler2 != null) {
        wrestlerRepository.delete(wrestler2);
      }
    }
  }
}
