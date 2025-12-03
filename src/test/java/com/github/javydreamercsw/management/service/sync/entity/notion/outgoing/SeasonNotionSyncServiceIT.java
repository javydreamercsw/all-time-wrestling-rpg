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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonNotionSyncService;
import dev.failsafe.FailsafeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class SeasonNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private SeasonRepository seasonRepository;
  @Autowired private SeasonNotionSyncService seasonNotionSyncService;

  @Test
  void testSyncToNotion() {
    Season season = null;
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
      // Create a new Season (separate from testSeason from setUp)
      season = new Season();
      season.setName("Test Season Sync " + UUID.randomUUID());
      season.setDescription("A test season for Notion sync operations");
      season.setStartDate(Instant.now().minus(7, ChronoUnit.DAYS));
      season.setIsActive(true);
      season.setShowsPerPpv(5);
      seasonRepository.save(season);

      // Sync to Notion for the first time
      seasonNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(season.getId());
      Season updatedSeason = seasonRepository.findById(season.getId()).get();
      assertNotNull(updatedSeason.getExternalId());
      assertNotNull(updatedSeason.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedSeason.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedSeason.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "A test season for Notion sync operations",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertNotNull(props.get("Start Date").getDate());
      assertNull(props.get("End Date").getDate()); // Should be null as season is active

      // Sync to Notion again with updates
      updatedSeason.setName("Test Season Updated " + UUID.randomUUID());
      updatedSeason.setDescription("Updated description");
      updatedSeason.setEndDate(Instant.now());
      updatedSeason.setIsActive(false);
      seasonRepository.save(updatedSeason);
      seasonNotionSyncService.syncToNotion("test-op-2");
      Season updatedSeason2 = seasonRepository.findById(season.getId()).get();
      assertTrue(updatedSeason2.getLastSync().isAfter(updatedSeason.getLastSync()));

      // Verify updated properties
      page =
          handler.executeWithRetry(
              () -> client.retrievePage(updatedSeason.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedSeason2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "Updated description",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertNotNull(props.get("End Date").getDate());

    } finally {
      if (season != null && season.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(season.getExternalId(), new HashMap<>(), true, null, null);
          handler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        seasonRepository.delete(season);
      } else if (season != null && season.getId() != null) {
        seasonRepository.delete(season);
      }
    }
  }
}
