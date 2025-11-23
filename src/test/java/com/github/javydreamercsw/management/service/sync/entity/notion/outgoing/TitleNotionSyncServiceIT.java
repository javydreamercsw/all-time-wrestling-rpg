package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleNotionSyncService;
import dev.failsafe.FailsafeException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class TitleNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleNotionSyncService titleNotionSyncService;
  @Autowired private WrestlerRepository wrestlerRepository;

  private Wrestler testChampion;
  private Wrestler testContender;

  @BeforeEach
  void setUp() {
    testChampion = new Wrestler();
    testChampion.setName("Test Champion");
    testChampion.setExternalId(UUID.randomUUID().toString());
    testChampion.setStartingHealth(100);
    testChampion.setLowHealth(20);
    testChampion.setStartingStamina(100);
    testChampion.setLowStamina(20);
    testChampion.setDeckSize(20);
    testChampion.setTier(WrestlerTier.MAIN_EVENTER);
    testChampion.setGender(Gender.MALE);
    wrestlerRepository.save(testChampion);

    testContender = new Wrestler();
    testContender.setName("Test Contender");
    testContender.setExternalId(UUID.randomUUID().toString());
    testContender.setStartingHealth(100);
    testContender.setLowHealth(20);
    testContender.setStartingStamina(100);
    testContender.setLowStamina(20);
    testContender.setDeckSize(20);
    testContender.setTier(WrestlerTier.CONTENDER);
    testContender.setGender(Gender.MALE);
    wrestlerRepository.save(testContender);
  }

  @AfterEach
  public void tearDown() {
    // Clean up created entities
    titleRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  void testSyncToNotion() {
    Title title = null;
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
      // Create a new Title
      title = new Title();
      title.setName("World Championship");
      title.setDescription("The most prestigious title");
      title.setTier(WrestlerTier.MAIN_EVENTER);
      title.setGender(Gender.MALE);
      title.setIsActive(true);
      title.setChampion(List.of(testChampion));
      title.setContender(List.of(testContender));
      titleRepository.save(title);

      // Sync to Notion for the first time
      titleNotionSyncService.syncToNotion(title);

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(title.getId());
      Title updatedTitle = titleRepository.findById(title.getId()).get();
      assertNotNull(updatedTitle.getExternalId());
      assertNotNull(updatedTitle.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedTitle.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          "World Championship",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "The most prestigious title",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertEquals(
          WrestlerTier.MAIN_EVENTER.getDisplayName(), props.get("Tier").getSelect().getName());
      assertEquals(Gender.MALE.name(), props.get("Gender").getSelect().getName());
      assertTrue(props.get("Active").getCheckbox());
      assertNotNull(props.get("Champion").getRelation());
      assertFalse(props.get("Champion").getRelation().isEmpty());
      assertEquals(
          testChampion.getExternalId(), props.get("Champion").getRelation().get(0).getId());
      assertNotNull(props.get("Contender").getRelation());
      assertFalse(props.get("Contender").getRelation().isEmpty());
      assertEquals(
          testContender.getExternalId(), props.get("Contender").getRelation().get(0).getId());

      // Sync to Notion again with updates
      updatedTitle.setName("Unified World Championship");
      updatedTitle.setDescription("The undisputed title");
      updatedTitle.setTier(WrestlerTier.ICON);
      updatedTitle.setGender(Gender.FEMALE);
      updatedTitle.setIsActive(false);
      updatedTitle.setChampion(Collections.emptyList()); // Vacate title
      titleNotionSyncService.syncToNotion(updatedTitle);
      Title updatedTitle2 = titleRepository.findById(title.getId()).get();
      Assertions.assertEquals(updatedTitle2.getLastSync(), updatedTitle.getLastSync());

      // Verify updated properties
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedTitle.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          "Unified World Championship",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "The undisputed title",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertEquals(WrestlerTier.ICON.getDisplayName(), props.get("Tier").getSelect().getName());
      assertEquals(Gender.FEMALE.name(), props.get("Gender").getSelect().getName());
      assertFalse(props.get("Active").getCheckbox());
      // Champion relation should be empty if vacated
      assertTrue(props.get("Champion").getRelation().isEmpty());

    } finally {
      if (title != null && title.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(title.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      if (title != null) {
        titleRepository.delete(title);
      }
      if (testChampion != null) {
        wrestlerRepository.delete(testChampion);
      }
      if (testContender != null) {
        wrestlerRepository.delete(testContender);
      }
    }
  }
}
