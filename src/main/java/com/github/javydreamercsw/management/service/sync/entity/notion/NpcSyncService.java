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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
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

  @Autowired
  public NpcSyncService(
      ObjectMapper objectMapper,
      NotionSyncProperties syncProperties,
      SyncServiceDependencies syncServiceDependencies) {
    super(objectMapper, syncProperties, syncServiceDependencies);
  }

  public SyncResult syncNpcs(@NonNull String operationId, @NonNull SyncDirection direction) {
    if (direction == SyncDirection.OUTBOUND) {
      return SyncResult.success("NPCs", 0, 0, 0);
    }
    if (syncServiceDependencies.syncSessionManager.isAlreadySyncedInSession("npcs")) {
      log.info("⏭️ NPCs already synced in current session, skipping");
      return SyncResult.success("NPCs", 0, 0, 0);
    }

    log.info("Starting NPCs synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      if (!syncProperties.isEntityEnabled("npcs")) {
        log.info("NPCs sync is disabled in configuration");
        return SyncResult.success("NPCs", 0, 0, 0);
      }

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync NPCs from Notion.");
        return SyncResult.failure("NPCs", "NotionHandler is not available for sync operations");
      }

      List<NpcPage> npcPages =
          executeWithRateLimit(syncServiceDependencies.notionHandler::loadAllNpcs);
      log.info("Retrieved {} NPCs from Notion", npcPages.size());

      int savedCount = 0;
      for (NpcPage npcPage : npcPages) {
        // Assuming the Notion database has properties "Name" and "Role"
        String npcName =
            syncServiceDependencies.notionPageDataExtractor.extractNameFromNotionPage(npcPage);
        Object roleObj = npcPage.getRawProperties().get("Role");
        String npcType = null;
        if (roleObj instanceof String) {
          npcType = (String) roleObj;
        }

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

      syncServiceDependencies.syncSessionManager.markAsSyncedInSession("npcs");
      return SyncResult.success("NPCs", savedCount, 0, 0);

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
