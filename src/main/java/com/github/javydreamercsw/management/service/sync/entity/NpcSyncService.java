package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NpcSyncService extends BaseSyncService {

  @Autowired protected NpcService npcService;

  public NpcSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  public SyncResult syncNpcs(@NonNull String operationId) {
    if (isAlreadySyncedInSession("npcs")) {
      log.info("NPCs already synced in current session, skipping");
      return SyncResult.success("NPCs", 0, 0);
    }

    log.info("Starting NPCs synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      if (!syncProperties.isEntityEnabled("npcs")) {
        log.info("NPCs sync is disabled in configuration");
        return SyncResult.success("NPCs", 0, 0);
      }

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync NPCs from Notion.");
        return SyncResult.failure("NPCs", "NotionHandler is not available for sync operations");
      }

      List<NpcPage> npcPages = executeWithRateLimit(notionHandler::loadAllNpcs);
      log.info("Retrieved {} NPCs from Notion", npcPages.size());

      int savedCount = 0;
      for (NpcPage npcPage : npcPages) {
        // Assuming the Notion database has properties "Name" and "Role"
        String npcName = (String) npcPage.getRawProperties().get("Name");
        String npcType = (String) npcPage.getRawProperties().get("Role");
        if (npcName != null && !npcName.isEmpty() && npcType != null && !npcType.isEmpty()) {
          Npc npc = npcService.findByExternalId(npcPage.getId());
          if (npc == null) {
            npc = npcService.findByName(npcName);
            if (npc == null) {
              npc = new Npc();
            }
          }
          npc.setName(npcName);
          npc.setNpcType(npcType);
          npc.setExternalId(npcPage.getId());
          npcService.save(npc);
          savedCount++;
        }
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("Successfully synchronized {} NPCs in {}ms total", savedCount, totalTime);

      markAsSyncedInSession("npcs");
      return SyncResult.success("NPCs", savedCount, 0);
    } catch (Exception e) {
      log.error("Failed to sync NPCs", e);
      return SyncResult.failure("NPCs", e.getMessage());
    }
  }

  /** DTO for Npc data from Notion. */
  @Setter
  @Getter
  public static class NpcDTO {
    private String name;
    private String npcType;
    private String externalId; // Notion page ID
  }
}
