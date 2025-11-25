package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerNotionSyncService;
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

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class WrestlerNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerNotionSyncService wrestlerNotionSyncService;

  @Test
  void testSyncToNotion() {
    Wrestler wrestler = null;
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
      faction.setName("Test Faction " + UUID.randomUUID());
      factionRepository.save(faction);

      // Create a new wrestler
      wrestler = new Wrestler();
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

      // Sync to Notion for the first time
      wrestlerNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(wrestler.getId());
      Wrestler updatedWrestler = wrestlerRepository.findById(wrestler.getId()).get();
      assertNotNull(updatedWrestler.getExternalId());
      assertNotNull(updatedWrestler.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedWrestler.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedWrestler.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(1000, Objects.requireNonNull(props.get("Fans").getNumber()).longValue());
      Assertions.assertTrue(
          wrestler
              .getTier()
              .getDisplayName()
              .equalsIgnoreCase(Objects.requireNonNull(props.get("Tier").getSelect()).getName()),
          "Tier does not match!");
      Assertions.assertTrue(
          wrestler
              .getGender()
              .name()
              .equalsIgnoreCase(Objects.requireNonNull(props.get("Gender").getSelect()).getName()),
          "Gender does not match!");
      assertEquals(1, Objects.requireNonNull(props.get("Bumps").getNumber()).intValue());
      assertEquals(
          16, Objects.requireNonNull(props.get("Starting Stamina").getNumber()).intValue());
      assertEquals(15, Objects.requireNonNull(props.get("Starting Health").getNumber()).intValue());
      assertEquals(2, Objects.requireNonNull(props.get("Low Stamina").getNumber()).intValue());
      assertEquals(4, Objects.requireNonNull(props.get("Low Health").getNumber()).intValue());
      assertEquals(15, Objects.requireNonNull(props.get("Deck Size").getNumber()).intValue());

      // Sync to Notion again
      updatedWrestler.setName("Test Wrestler Updated " + UUID.randomUUID());
      wrestlerRepository.save(updatedWrestler);
      wrestlerNotionSyncService.syncToNotion("test-op-2");
      Wrestler updatedWrestler2 = wrestlerRepository.findById(wrestler.getId()).get();
      assertTrue(updatedWrestler2.getLastSync().isAfter(updatedWrestler.getLastSync()));

      // Verify updated name
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedWrestler.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedWrestler2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
    } finally {
      if (wrestler != null && wrestler.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(wrestler.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      if (wrestler != null && wrestler.getId() != null) {
        wrestlerRepository.delete(wrestler);
      }
      if (faction != null && faction.getId() != null) {
        factionRepository.delete(faction);
      }
    }
  }
}
