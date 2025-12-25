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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleNotionSyncService;
import dev.failsafe.FailsafeException;
import java.time.Instant;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class TitleNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleNotionSyncService titleNotionSyncService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private NotionHandler notionHandler;

  @Test
  void testSyncToNotion() {
    Title title = null;
    Wrestler testChampion = null;
    Wrestler testContender = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      testChampion = createTestWrestler("Test Champion " + UUID.randomUUID());
      wrestlerRepository.save(testChampion);

      testContender = createTestWrestler("Test Contender " + UUID.randomUUID());
      wrestlerRepository.save(testContender);

      // Create a new Title
      title = new Title();
      title.setName("World Championship " + UUID.randomUUID());
      title.setDescription("The most prestigious title");
      title.setTier(WrestlerTier.MAIN_EVENTER);
      title.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
      title.setIsActive(true);
      title.awardTitleTo(List.of(testChampion), Instant.now());
      title.addChallenger(testContender);
      titleRepository.save(title);

      // Sync to Notion for the first time
      titleNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(title.getId());
      Title updatedTitle = titleRepository.findById(title.getId()).get();
      assertNotNull(updatedTitle.getExternalId());
      assertNotNull(updatedTitle.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedTitle.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedTitle.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "The most prestigious title",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertEquals(
          WrestlerTier.MAIN_EVENTER.getDisplayName(),
          Objects.requireNonNull(props.get("Tier").getSelect()).getName());
      assertEquals(
          com.github.javydreamercsw.base.domain.wrestler.Gender.MALE.name(),
          Objects.requireNonNull(props.get("Gender").getSelect()).getName());
      assertTrue(Objects.requireNonNull(props.get("Active").getCheckbox()));
      assertNotNull(props.get("Champion").getRelation());
      assertFalse(props.get("Champion").getRelation().isEmpty());
      assertEquals(
          testChampion.getExternalId(), props.get("Champion").getRelation().get(0).getId());
      assertNotNull(props.get("Challengers").getRelation());
      assertFalse(props.get("Challengers").getRelation().isEmpty());
      assertEquals(
          testContender.getExternalId(), props.get("Challengers").getRelation().get(0).getId());

      // Sync to Notion again with updates
      updatedTitle.setName("Unified World Championship " + UUID.randomUUID());
      updatedTitle.setDescription("The undisputed title");
      updatedTitle.setTier(WrestlerTier.ICON);
      updatedTitle.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.FEMALE);
      updatedTitle.setIsActive(false);
      updatedTitle.vacateTitle(Instant.now()); // Vacate title
      titleRepository.save(updatedTitle);
      titleNotionSyncService.syncToNotion("test-op-2");
      Title updatedTitle2 = titleRepository.findById(title.getId()).get();
      assertTrue(updatedTitle2.getLastSync().isAfter(updatedTitle.getLastSync()));

      // Verify updated properties
      page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedTitle.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedTitle2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(
          "The undisputed title",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Description").getRichText()).get(0).getText())
              .getContent());
      assertEquals(
          WrestlerTier.ICON.getDisplayName(),
          Objects.requireNonNull(props.get("Tier").getSelect()).getName());
      assertEquals(
          com.github.javydreamercsw.base.domain.wrestler.Gender.FEMALE.name(),
          Objects.requireNonNull(props.get("Gender").getSelect()).getName());
      assertFalse(Objects.requireNonNull(props.get("Active").getCheckbox()));
      // Champion relation should be empty if vacated
      assertTrue(props.get("Champion").getRelation().isEmpty());

    } finally {
      if (title != null && title.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(title.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      if (title != null && title.getId() != null) {
        titleRepository.delete(title);
      }
      if (testChampion != null && testChampion.getId() != null) {
        wrestlerRepository.delete(testChampion);
      }
      if (testContender != null && testContender.getId() != null) {
        wrestlerRepository.delete(testContender);
      }
    }
  }
}
