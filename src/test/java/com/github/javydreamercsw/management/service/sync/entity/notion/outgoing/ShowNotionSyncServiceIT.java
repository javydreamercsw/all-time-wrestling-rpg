package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowNotionSyncService;
import dev.failsafe.FailsafeException;
import java.time.Instant;
import java.time.LocalDate;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class ShowNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowNotionSyncService showNotionSyncService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;

  private ShowType testShowType;
  private Season testSeason;
  private ShowTemplate testShowTemplate;

  @BeforeEach
  void setUp() {
    testShowType = new ShowType();
    testShowType.setName("Test ShowType");
    testShowType.setDescription("Test ShowType Description");
    testShowType.setExternalId(UUID.randomUUID().toString()); // Mock external ID
    showTypeRepository.save(testShowType);

    testSeason = new Season();
    testSeason.setName("Test Season");
    testSeason.setStartDate(Instant.now());
    testSeason.setIsActive(true);
    testSeason.setExternalId(UUID.randomUUID().toString()); // Mock external ID
    seasonRepository.save(testSeason);

    testShowTemplate = new ShowTemplate();
    testShowTemplate.setName("Test Template");
    testShowTemplate.setExternalId(UUID.randomUUID().toString()); // Mock external ID
    testShowTemplate.setShowType(testShowType); // Set the required ShowType
    showTemplateRepository.save(testShowTemplate);
  }

  @AfterEach
  public void tearDown() {
    // Clean up created entities to avoid conflicts in other tests
    showRepository.deleteAll();
    showTypeRepository.deleteAll();
    seasonRepository.deleteAll();
    showTemplateRepository.deleteAll();
  }

  @Test
  void testSyncToNotion() {
    Show show = null;
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
      // Create a new Show
      show = new Show();
      show.setName("Test Show");
      show.setDescription("A test wrestling show");
      show.setType(testShowType);
      show.setSeason(testSeason);
      show.setTemplate(testShowTemplate);
      show.setShowDate(LocalDate.now());
      showRepository.save(show);

      // Sync to Notion for the first time
      showNotionSyncService.syncToNotion(show);

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(show.getId());
      Show updatedShow = showRepository.findById(show.getId()).get();
      assertNotNull(updatedShow.getExternalId());
      assertNotNull(updatedShow.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedShow.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          "Test Show",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "A test wrestling show",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertNotNull(props.get("Show Type").getRelation());
      assertFalse(props.get("Show Type").getRelation().isEmpty());
      assertEquals(
          testShowType.getExternalId(), props.get("Show Type").getRelation().get(0).getId());
      assertNotNull(props.get("Season").getRelation());
      assertFalse(props.get("Season").getRelation().isEmpty());
      assertEquals(testSeason.getExternalId(), props.get("Season").getRelation().get(0).getId());
      assertNotNull(props.get("Template").getRelation());
      assertFalse(props.get("Template").getRelation().isEmpty());
      assertEquals(
          testShowTemplate.getExternalId(), props.get("Template").getRelation().get(0).getId());
      assertNotNull(props.get("Date").getDate());

      // Sync to Notion again with updates
      updatedShow.setName("Test Show Updated");
      updatedShow.setDescription("Updated description for the show");
      updatedShow.setShowDate(LocalDate.now().plusDays(7));
      showNotionSyncService.syncToNotion(updatedShow);
      Show updatedShow2 = showRepository.findById(show.getId()).get();
      Assertions.assertEquals(updatedShow2.getLastSync(), updatedShow.getLastSync());

      // Verify updated properties
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedShow.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          "Test Show Updated",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "Updated description for the show",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      // Relations should remain the same unless explicitly changed
      assertNotNull(props.get("Show Type").getRelation());
      assertFalse(props.get("Show Type").getRelation().isEmpty());
      assertEquals(
          testShowType.getExternalId(), props.get("Show Type").getRelation().get(0).getId());
      assertNotNull(props.get("Season").getRelation());
      assertFalse(props.get("Season").getRelation().isEmpty());
      assertEquals(testSeason.getExternalId(), props.get("Season").getRelation().get(0).getId());
      assertNotNull(props.get("Template").getRelation());
      assertFalse(props.get("Template").getRelation().isEmpty());
      assertEquals(
          testShowTemplate.getExternalId(), props.get("Template").getRelation().get(0).getId());
      assertNotNull(props.get("Date").getDate());

    } finally {
      if (show != null && show.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(show.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      if (show != null) {
        showRepository.delete(show);
      }
      if (testShowType != null) {
        showTypeRepository.delete(testShowType);
      }
      if (testSeason != null) {
        seasonRepository.delete(testSeason);
      }
      if (testShowTemplate != null) {
        showTemplateRepository.delete(testShowTemplate);
      }
    }
  }
}
