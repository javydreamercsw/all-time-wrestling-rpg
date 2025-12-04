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
  @Autowired private NotionHandler notionHandler;

  @Test
  void testSyncToNotion() {
    Rivalry rivalry = null;
    Wrestler wrestler1 = null;
    Wrestler wrestler2 = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
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
      rivalryNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(rivalry.getId());
      Rivalry updatedRivalry = rivalryRepository.findById(rivalry.getId()).get();
      assertNotNull(updatedRivalry.getExternalId());
      assertNotNull(updatedRivalry.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
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
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        rivalryRepository.delete(rivalry);
      } else if (rivalry != null && rivalry.getId() != null) {
        rivalryRepository.delete(rivalry);
      }
      if (wrestler1 != null && wrestler1.getId() != null) {
        wrestlerRepository.delete(wrestler1);
      }
      if (wrestler2 != null && wrestler2.getId() != null) {
        wrestlerRepository.delete(wrestler2);
      }
    }
  }
}
