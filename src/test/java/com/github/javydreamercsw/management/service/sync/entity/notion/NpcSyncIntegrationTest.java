package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class NpcSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private NpcService npcService;

  @MockBean private NotionHandler notionHandler;

  @Mock private NpcPage npcPage1;
  @Mock private NpcPage npcPage2;
  
  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync NPCs from Notion")
  void shouldSyncNpcsFromNotion() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      log.info("🚀 Starting real NPC sync integration test...");

      // Given
      String npc1Id = UUID.randomUUID().toString();
      when(npcPage1.getId()).thenReturn(npc1Id);
      when(npcPage1.getRawProperties())
          .thenReturn(
              Map.of(
                  "Name", "Test NPC 1",
                  "Role", "Interviewer"));

      String npc2Id = UUID.randomUUID().toString();
      when(npcPage2.getId()).thenReturn(npc2Id);
      when(npcPage2.getRawProperties())
          .thenReturn(
              Map.of(
                  "Name", "Test NPC 2",
                  "Role", "General Manager"));

      when(notionHandler.loadAllNpcs()).thenReturn(List.of(npcPage1, npcPage2));

      // When - Perform real sync with real services
      BaseSyncService.SyncResult result =
          notionSyncService.syncNpcs("test-operation", SyncDirection.INBOUND);

      // Then - Verify the sync result
      assertNotNull(result, "Sync result should not be null");
      assertEquals("NPCs", result.getEntityType(), "Entity type should be 'NPCs'");
      assertTrue(result.isSuccess(), "Sync should be successful");
      assertEquals(2, result.getSyncedCount(), "Should have synced 2 NPCs");

      // Verify NPCs in database
      Optional<Npc> npc1Opt = npcService.findByName("Test NPC 1");
      assertTrue(npc1Opt.isPresent());
      Npc npc1 = npc1Opt.get();
      assertEquals(npc1Id, npc1.getExternalId());
      assertEquals("Interviewer", npc1.getNpcType());

      Optional<Npc> npc2Opt = npcService.findByName("Test NPC 2");
      assertTrue(npc2Opt.isPresent());
      Npc npc2 = npc2Opt.get();
      assertEquals(npc2Id, npc2.getExternalId());
      assertEquals("General Manager", npc2.getNpcType());

      // Run sync again to test updates and no duplicates
      when(npcPage1.getRawProperties())
          .thenReturn(
              Map.of(
                  "Name", "Test NPC 1 Updated",
                  "Role", "Announcer"));
      
      BaseSyncService.SyncResult secondResult =
          notionSyncService.syncNpcs("second-sync-operation", SyncDirection.INBOUND);
      
      assertEquals(2, npcService.findAll().size());

      Optional<Npc> updatedNpc1Opt = npcService.findByExternalId(npc1Id);
      assertTrue(updatedNpc1Opt.isPresent());
      Npc updatedNpc1 = updatedNpc1Opt.get();
      assertEquals("Test NPC 1 Updated", updatedNpc1.getName());
      assertEquals("Announcer", updatedNpc1.getNpcType());
    }
  }
}
