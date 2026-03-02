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
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NpcSyncService extends BaseSyncService {

  private final NpcService npcService;

  @Autowired @Lazy private NpcSyncService self;

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
        if (self.processSingleNpc(npcPage)) {
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleNpc(NpcPage npcPage) {
    try {
      Map<String, Object> rawProperties = npcPage.getRawProperties();
      String npcName =
          syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(npcPage);
      Object roleObj = rawProperties.get("Role");
      String npcType = roleObj instanceof String ? (String) roleObj : null;

      if (npcName == null || npcName.isEmpty() || npcType == null || npcType.isEmpty()) {
        return false;
      }

      Npc npc = npcService.findByExternalId(npcPage.getId()).orElse(null);
      if (npc == null) {
        npc = npcService.findByName(npcName);
        if (npc == null) {
          npc = new Npc();
        }
      }

      boolean changed = false;

      if (!Objects.equals(npc.getName(), npcName)) {
        npc.setName(npcName);
        changed = true;
      }
      if (!Objects.equals(npc.getNpcType(), npcType)) {
        npc.setNpcType(npcType);
        changed = true;
      }
      if (!Objects.equals(npc.getExternalId(), npcPage.getId())) {
        npc.setExternalId(npcPage.getId());
        changed = true;
      }

      String description =
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(npcPage);
      if (!Objects.equals(npc.getDescription(), description)) {
        npc.setDescription(description);
        changed = true;
      }

      // Alignment
      Object alignmentObj = rawProperties.get("Alignment");
      if (alignmentObj instanceof String) {
        try {
          AlignmentType type = AlignmentType.valueOf(((String) alignmentObj).toUpperCase());
          if (!Objects.equals(npc.getAlignment(), type)) {
            npc.setAlignment(type);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn("Invalid alignment '{}' for NPC '{}'", alignmentObj, npcName);
        }
      }

      // Gender (Sex)
      Object sexObj = rawProperties.get("Sex");
      if (sexObj instanceof String) {
        try {
          Gender gender = Gender.valueOf(((String) sexObj).toUpperCase());
          if (!Objects.equals(npc.getGender(), gender)) {
            npc.setGender(gender);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn("Invalid sex '{}' for NPC '{}'", sexObj, npcName);
        }
      }

      // Additional Attributes
      Map<String, Object> attrs = new java.util.HashMap<>(npc.getAttributes());
      boolean attrsChanged = false;

      Object likenessObj = rawProperties.get("Likeness");
      if (likenessObj instanceof String && !Objects.equals(attrs.get("likeness"), likenessObj)) {
        attrs.put("likeness", likenessObj);
        attrsChanged = true;
      }

      Object originObj = rawProperties.get("Origin");
      if (originObj instanceof String && !Objects.equals(attrs.get("origin"), originObj)) {
        attrs.put("origin", originObj);
        attrsChanged = true;
      }

      Object catchphraseObj = rawProperties.get("Catchphrase");
      if (catchphraseObj instanceof String
          && !Objects.equals(attrs.get("catchphrase"), catchphraseObj)) {
        attrs.put("catchphrase", catchphraseObj);
        attrsChanged = true;
      }

      Object signatureStyleObj = rawProperties.get("Signature Style");
      if (signatureStyleObj instanceof String
          && !Objects.equals(attrs.get("signatureStyle"), signatureStyleObj)) {
        attrs.put("signatureStyle", signatureStyleObj);
        attrsChanged = true;
      }

      Object statusObj = rawProperties.get("Status");
      if (statusObj instanceof String && !Objects.equals(attrs.get("status"), statusObj)) {
        attrs.put("status", statusObj);
        attrsChanged = true;
      }

      if (attrsChanged) {
        npc.setAttributes(attrs);
        changed = true;
      }

      if (changed || npc.getId() == null) {
        npcService.save(npc);
        return true;
      }
      return false;
    } catch (Exception e) {
      log.error("❌ Failed to process single NPC: {}", npcPage.getId(), e);
      return false;
    }
  }

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
