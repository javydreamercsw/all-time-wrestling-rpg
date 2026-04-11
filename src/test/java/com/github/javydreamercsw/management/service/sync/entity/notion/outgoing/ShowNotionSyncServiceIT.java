/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonNotionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowNotionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTemplateNotionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTypeNotionSyncService;
import dev.failsafe.FailsafeException;
import java.time.Instant;
import java.time.LocalDate;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
@TestPropertySource(properties = "test.mock.notion-handler=false")
class ShowNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowNotionSyncService showNotionSyncService;
  @Autowired private SeasonNotionSyncService seasonNotionSyncService;
  @Autowired private ShowTypeNotionSyncService showTypeNotionSyncService;
  @Autowired private ShowTemplateNotionSyncService showTemplateNotionSyncService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    Show show = null;
    ShowType testShowType = null;
    Season testSeason = null;
    ShowTemplate testShowTemplate = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      testShowType = new ShowType();
      testShowType.setName("Test ShowType " + UUID.randomUUID());
      testShowType.setDescription("Test ShowType Description");
      showTypeRepository.save(testShowType);

      testSeason = new Season();
      testSeason.setName("Test Season " + UUID.randomUUID());
      testSeason.setStartDate(Instant.now());
      testSeason.setIsActive(true);
      seasonRepository.save(testSeason);

      testShowTemplate = new ShowTemplate();
      testShowTemplate.setName("Test Template " + UUID.randomUUID());
      testShowTemplate.setShowType(testShowType); // Set the required ShowType
      showTemplateRepository.save(testShowTemplate);

      // Sync dependencies to Notion to get external IDs

      showTypeNotionSyncService.syncToNotion("test-prep-showtypes", List.of(testShowType.getId()));

      seasonNotionSyncService.syncToNotion("test-prep-seasons", List.of(testSeason.getId()));

      showTemplateNotionSyncService.syncToNotion(
          "test-prep-templates", List.of(testShowTemplate.getId()));

      testShowType = showTypeRepository.findById(testShowType.getId()).get();

      testSeason = seasonRepository.findById(testSeason.getId()).get();

      testShowTemplate = showTemplateRepository.findById(testShowTemplate.getId()).get();

      assertNotNull(testShowType.getExternalId(), "ShowType externalId should not be null");

      assertNotNull(testSeason.getExternalId(), "Season externalId should not be null");

      assertNotNull(testShowTemplate.getExternalId(), "ShowTemplate externalId should not be null");

      // Create a new Show

      show = new Show();

      show.setName("Test Show " + UUID.randomUUID());

      show.setDescription("A test wrestling show");

      show.setType(testShowType);

      show.setSeason(testSeason);

      show.setTemplate(testShowTemplate);

      show.setShowDate(LocalDate.now());

      showRepository.save(show);

      // Sync to Notion for the first time

      showNotionSyncService.syncToNotion("test-op-1", List.of(show.getId()));

      // Verify that the externalId and lastSync fields are updated

      assertNotNull(show.getId());

      Show updatedShow = showRepository.findById(show.getId()).get();

      assertNotNull(updatedShow.getExternalId());

      assertNotNull(updatedShow.getLastSync());

      // Retrieve the page from Notion and verify properties

      Page page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedShow.getExternalId(), Collections.emptyList()));

      Map<String, PageProperty> props = page.getProperties();

      assertEquals(
          updatedShow.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());

      assertEquals(
          testShowType.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Show Type")));

      assertEquals(
          testSeason.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Season")));

      assertEquals(
          testShowTemplate.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Template")));

      assertNotNull(props.get("Date").getDate());

      // Sync to Notion again with updates

      updatedShow.setName("Test Show Updated " + UUID.randomUUID());

      updatedShow.setDescription("Updated description for the show");

      updatedShow.setShowDate(LocalDate.now().plusDays(7));

      showRepository.save(updatedShow);

      showNotionSyncService.syncToNotion("test-op-2", List.of(show.getId()));
      Show updatedShow2 = showRepository.findById(show.getId()).get();
      assertTrue(updatedShow2.getLastSync().isAfter(updatedShow.getLastSync()));

      // Verify updated properties
      page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedShow.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedShow2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      // Relations should remain the same unless explicitly changed
      assertEquals(
          testShowType.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Show Type")));
      assertEquals(
          testSeason.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Season")));
      assertEquals(
          testShowTemplate.getName(),
          com.github.javydreamercsw.base.ai.notion.NotionUtil.getValue(
              client, props.get("Template")));
      assertNotNull(props.get("Date").getDate());

    } finally {
      if (show != null && show.getExternalId() != null) {
        // Clean up Notion page
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(show.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      // Correct order of deletion
      if (show != null) {
        showRepository.delete(show);
      }
      if (testShowTemplate != null) {
        showTemplateRepository.delete(testShowTemplate);
      }
      if (testSeason != null) {
        seasonRepository.delete(testSeason);
      }
      if (testShowType != null) {
        showTypeRepository.delete(testShowType);
      }
    }
  }
}
