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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionNotionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TeamNotionSyncService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
@TestPropertySource(properties = "test.mock.notion-handler=false")
class TeamNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamNotionSyncService teamNotionSyncService;
  @Autowired private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Autowired private FactionNotionSyncService factionNotionSyncService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    Team team = null;
    Wrestler testWrestler1 = null;
    Wrestler testWrestler2 = null;
    Faction testFaction = null;

    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isEmpty()) {
      Assertions.fail("Unable to create Notion client, skipping test.");
    }
    try (NotionClient client = clientOptional.get()) {
      testWrestler1 = createTestWrestler("Wrestler One " + UUID.randomUUID());
      wrestlerRepository.save(testWrestler1);

      testWrestler2 = createTestWrestler("Wrestler Two " + UUID.randomUUID());
      wrestlerRepository.save(testWrestler2);

      testFaction = new Faction();
      testFaction.setName("Test Faction Team " + UUID.randomUUID());
      factionRepository.save(testFaction);

      // Sync dependencies to Notion to get external IDs
      wrestlerNotionSyncService.syncToNotion("test-prep-wrestlers");
      factionNotionSyncService.syncToNotion("test-prep-factions");

      // Create a new Team
      team = new Team();
      team.setName("Test Team " + UUID.randomUUID());
      team.setDescription("A test wrestling team");
      team.setWrestler1(testWrestler1);
      team.setWrestler2(testWrestler2);
      team.setFaction(testFaction);
      team.setStatus(TeamStatus.ACTIVE);
      team.setFormedDate(Instant.now().minusSeconds(3600));
      teamRepository.save(team);

      // Sync to Notion for the first time
      teamNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(team.getId());
      Team updatedTeam = teamRepository.findById(team.getId()).get();
      assertNotNull(updatedTeam.getExternalId());
      assertNotNull(updatedTeam.getLastSync());

      // Retrieve the page from Notion and verify properties
      Page page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedTeam.getExternalId(), Collections.emptyList()));
      Map<String, PageProperty> props = page.getProperties();
      assertEquals(
          updatedTeam.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertNotNull(props.get("Member 1").getRelation());
      assertFalse(props.get("Member 1").getRelation().isEmpty());
      assertEquals(
          testWrestler1.getExternalId(), props.get("Member 1").getRelation().get(0).getId());
      assertNotNull(props.get("Member 2").getRelation());
      assertFalse(props.get("Member 2").getRelation().isEmpty());
      assertEquals(
          testWrestler2.getExternalId(), props.get("Member 2").getRelation().get(0).getId());
      assertNotNull(props.get("Faction").getRelation());
      assertFalse(props.get("Faction").getRelation().isEmpty());
      assertEquals(testFaction.getExternalId(), props.get("Faction").getRelation().get(0).getId());
      assertTrue(Objects.requireNonNull(props.get("Status").getCheckbox()));
      assertNotNull(props.get("Formed Date").getDate());
      assertNull(props.get("Disbanded Date").getDate());

      // Sync to Notion again with updates
      updatedTeam.setName("Test Team Updated " + UUID.randomUUID());
      updatedTeam.setStatus(TeamStatus.DISBANDED);
      updatedTeam.setDisbandedDate(Instant.now());
      teamRepository.save(updatedTeam);
      teamNotionSyncService.syncToNotion("test-op-2");
      Team updatedTeam2 = teamRepository.findById(team.getId()).get();
      assertTrue(updatedTeam2.getLastSync().isAfter(updatedTeam.getLastSync()));

      // Verify updated properties
      page =
          notionHandler.executeWithRetry(
              () -> client.retrievePage(updatedTeam.getExternalId(), Collections.emptyList()));
      props = page.getProperties();
      assertEquals(
          updatedTeam2.getName(),
          Objects.requireNonNull(
                  Objects.requireNonNull(props.get("Name").getTitle()).get(0).getText())
              .getContent());
      assertFalse(Objects.requireNonNull(props.get("Status").getCheckbox()));
      assertNotNull(props.get("Disbanded Date").getDate());

    } finally {
      if (team != null && team.getExternalId() != null) {
        // Clean up
        try (NotionClient client = clientOptional.get()) {
          UpdatePageRequest request =
              new UpdatePageRequest(team.getExternalId(), new HashMap<>(), true, null, null);
          notionHandler.executeWithRetry(() -> client.updatePage(request));
        } catch (FailsafeException e) {
          // Ignore timeout on cleanup
        }
      }
      if (team != null) {
        teamRepository.delete(team);
      }
      if (testWrestler1 != null) {
        wrestlerRepository.delete(testWrestler1);
      }
      if (testWrestler2 != null) {
        wrestlerRepository.delete(testWrestler2);
      }
      if (testFaction != null) {
        factionRepository.delete(testFaction);
      }
    }
  }
}
