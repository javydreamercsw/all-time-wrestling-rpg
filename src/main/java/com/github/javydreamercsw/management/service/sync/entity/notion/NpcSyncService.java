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
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NpcSyncService extends BaseSyncService {

  private final NpcService npcService;

  @Autowired
  public NpcSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NpcService npcService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.npcService = npcService;
  }

  public SyncResult syncNpcs(@NonNull String operationId, @NonNull SyncDirection direction) {
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession(SyncEntityType.NPCS.getKey())) {
      return SyncResult.success(SyncEntityType.NPCS.getKey(), 0, 0, 0);
    }

    log.info("🤖 Starting NPCs synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      List<NpcPage> npcPages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllNpcs());
      log.info("✅ Retrieved {} NPCs from Notion", npcPages.size());

      int savedCount = 0;
      for (NpcPage npcPage : npcPages) {
        Map<String, Object> rawProperties = npcPage.getRawProperties();
        String npcName =
            syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(npcPage);
        Object roleObj = rawProperties.get("Role");
        String npcType = roleObj instanceof String ? (String) roleObj : null;

        if (npcName != null && !npcName.isEmpty() && npcType != null && !npcType.isEmpty()) {
          Npc npc = npcService.findByExternalId(npcPage.getId()).orElse(null);
          if (npc == null) {
            npc = npcService.findByName(npcName);
            if (npc == null) {
              npc = new Npc();
            }
          }
          npc.setName(npcName);
          npc.setNpcType(npcType);
          npc.setExternalId(npcPage.getId());

          npc.setDescription(
              syncServiceDependencies
                  .getNotionPageDataExtractor()
                  .extractDescriptionFromNotionPage(npcPage));

          // Alignment
          Object alignmentObj = rawProperties.get("Alignment");
          if (alignmentObj instanceof String) {
            try {
              npc.setAlignment(AlignmentType.valueOf(((String) alignmentObj).toUpperCase()));
            } catch (IllegalArgumentException e) {
              log.warn("Invalid alignment '{}' for NPC '{}'", alignmentObj, npcName);
            }
          }

          // Gender (Sex)
          Object sexObj = rawProperties.get("Sex");
          if (sexObj instanceof String) {
            try {
              npc.setGender(Gender.valueOf(((String) sexObj).toUpperCase()));
            } catch (IllegalArgumentException e) {
              log.warn("Invalid sex '{}' for NPC '{}'", sexObj, npcName);
            }
          }

          // Additional Attributes
          Map<String, Object> attrs = npc.getAttributes();

          Object likenessObj = rawProperties.get("Likeness");
          if (likenessObj instanceof String) attrs.put("likeness", likenessObj);

          Object originObj = rawProperties.get("Origin");
          if (originObj instanceof String) attrs.put("origin", originObj);

          Object catchphraseObj = rawProperties.get("Catchphrase");
          if (catchphraseObj instanceof String) attrs.put("catchphrase", catchphraseObj);

          Object signatureStyleObj = rawProperties.get("Signature Style");
          if (signatureStyleObj instanceof String) attrs.put("signatureStyle", signatureStyleObj);

          Object statusObj = rawProperties.get("Status");
          if (statusObj instanceof String) attrs.put("status", statusObj);

          npc.setAttributes(attrs);
          npcService.save(npc);
          savedCount++;
        }
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("✅ Successfully synchronized {} NPCs in {}ms", savedCount, totalTime);

      syncServiceDependencies
          .getSyncSessionManager()
          .markAsSyncedInSession(SyncEntityType.NPCS.getKey());
      return SyncResult.success(SyncEntityType.NPCS.getKey(), savedCount, 0, 0);

    } catch (Exception e) {
      log.error("❌ Failed to synchronize NPCs from Notion", e);
      return SyncResult.failure(SyncEntityType.NPCS.getKey(), e.getMessage());
    }
  }

  /** DTO for Npc data from Notion. */
  @Setter
  @Getter
  public static class NpcDTO {
    private String name;
    private String npcType;
    private String externalId; // Notion page ID
    private String alignment;
    private String gender;
    private String status;
    private String likeness;
    private String origin;
    private String catchphrase;
    private String signatureStyle;
    private Integer awareness;
  }
}
