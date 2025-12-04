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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjuryNotionSyncService;
import dev.failsafe.FailsafeException;
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
class InjuryNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private InjuryTypeRepository injuryTypeRepository;
  @Autowired private InjuryNotionSyncService injuryNotionSyncService;
  @Autowired private NotionHandler notionHandler;

  @Test
  void testSyncToNotion() {
    InjuryType injuryType = null;
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      // Create a new InjuryType
      injuryType = new InjuryType();
      injuryType.setInjuryName("Test Injury Type " + UUID.randomUUID());
      injuryType.setHealthEffect(-1);
      injuryType.setStaminaEffect(-2);
      injuryType.setCardEffect(-1);
      injuryType.setSpecialEffects("No reversal ability");
      injuryTypeRepository.save(injuryType);

      // Sync to Notion for the first time
      injuryNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(injuryType.getId());
      InjuryType updatedInjuryType = injuryTypeRepository.findById(injuryType.getId()).get();
      assertNotNull(updatedInjuryType.getExternalId());
      assertNotNull(updatedInjuryType.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
              () ->
                  client.retrievePage(updatedInjuryType.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedInjuryType.getInjuryName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(-1.0, Objects.requireNonNull(props.get("Health Effect").getNumber()));
      assertEquals(-2.0, Objects.requireNonNull(props.get("Stamina Effect").getNumber()));
      assertEquals(-1.0, Objects.requireNonNull(props.get("Card Effect").getNumber()));
      assertEquals(
          "No reversal ability",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Special Effects").getRichText())
                      .get(0)
                      .getText())
              .getContent());

      // Sync to Notion again
      updatedInjuryType.setInjuryName("Test Injury Type Updated " + UUID.randomUUID());
      updatedInjuryType.setHealthEffect(-5);
      updatedInjuryType.setStaminaEffect(-5);
      updatedInjuryType.setCardEffect(-5);
      updatedInjuryType.setSpecialEffects("Cannot perform any moves");
      injuryTypeRepository.save(updatedInjuryType);
      injuryNotionSyncService.syncToNotion("test-op-2");
      InjuryType updatedInjuryType2 = injuryTypeRepository.findById(injuryType.getId()).get();
      assertTrue(updatedInjuryType2.getLastSync().isAfter(updatedInjuryType.getLastSync()));

      // Verify updated properties
      page =
          notionHandler.executeWithRetry(
              () ->
                  client.retrievePage(updatedInjuryType.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedInjuryType2.getInjuryName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertEquals(-5.0, Objects.requireNonNull(props.get("Health Effect").getNumber()));
      assertEquals(-5.0, Objects.requireNonNull(props.get("Stamina Effect").getNumber()));
      assertEquals(-5.0, Objects.requireNonNull(props.get("Card Effect").getNumber()));
      assertEquals(
          "Cannot perform any moves",
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Special Effects").getRichText())
                      .get(0)
                      .getText())
              .getContent());

    } finally {
      if (injuryType != null && injuryType.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(injuryType.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
        injuryTypeRepository.delete(injuryType);
      } else if (injuryType != null && injuryType.getId() != null) {
        injuryTypeRepository.delete(injuryType);
      }
    }
  }
}
