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
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.base.util.NotionBlocksRetriever;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing wrestlers from Notion to the database. */
@Service
@Slf4j
public class WrestlerSyncService extends BaseSyncService {

  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerNotionSyncService wrestlerNotionSyncService;
  private final TierRecalculationService tierRecalculationService;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final FactionRepository factionRepository;
  private final NpcRepository npcRepository;
  private final InjuryRepository injuryRepository;
  private final TeamRepository teamRepository;
  private final TitleReignRepository titleReignRepository;

  public WrestlerSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor,
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      WrestlerNotionSyncService wrestlerNotionSyncService,
      TierRecalculationService tierRecalculationService,
      WrestlerAlignmentRepository wrestlerAlignmentRepository,
      FactionRepository factionRepository,
      NpcRepository npcRepository,
      InjuryRepository injuryRepository,
      TeamRepository teamRepository,
      TitleReignRepository titleReignRepository) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerNotionSyncService = wrestlerNotionSyncService;
    this.tierRecalculationService = tierRecalculationService;
    this.wrestlerAlignmentRepository = wrestlerAlignmentRepository;
    this.factionRepository = factionRepository;
    this.npcRepository = npcRepository;
    this.injuryRepository = injuryRepository;
    this.teamRepository = teamRepository;
    this.titleReignRepository = titleReignRepository;
  }

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(@NonNull String operationId) {
    // Check if already synced in current session
    if (syncServiceDependencies.getSyncSessionManager().isAlreadySyncedInSession("wrestlers")) {
      log.info("⏭️ Wrestlers already synced in current session, skipping");
      return SyncResult.success("wrestlers", 0, 0, 0);
    }

    log.info("🤼 Starting wrestlers synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performWrestlersSync(operationId, startTime);
      if (result.isSuccess()) {
        syncServiceDependencies.getSyncSessionManager().markAsSyncedInSession("wrestlers");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync wrestlers", e);
      return SyncResult.failure("wrestlers", e.getMessage());
    }
  }

  private SyncResult performWrestlersSync(@NonNull String operationId, long startTime) {
    try {
      // Check if entity is enabled
      if (!syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("wrestlers")) {
        log.info("Wrestlers sync is disabled in configuration");
        return SyncResult.success("Wrestlers", 0, 0, 0);
      }

      // Initialize progress tracking
      syncServiceDependencies.getProgressTracker().startOperation(operationId, "Sync Wrestlers", 3);
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Retrieving wrestlers from Notion...");

      // Retrieve wrestlers from Notion
      log.info("📥 Retrieving wrestlers from Notion...");
      long retrieveStart = System.currentTimeMillis();

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync wrestlers from Notion.");
        return SyncResult.failure(
            "Wrestlers", "NotionHandler is not available for sync operations");
      }

      List<WrestlerPage> wrestlerPages =
          executeWithRateLimit(notionApiExecutor.getNotionHandler()::loadAllWrestlers);
      log.info(
          "✅ Retrieved {} wrestlers in {}ms",
          wrestlerPages.size(),
          System.currentTimeMillis() - retrieveStart);

      // Update progress with retrieval results
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Retrieved %d wrestlers from Notion in %dms",
                  wrestlerPages.size(), System.currentTimeMillis() - retrieveStart));

      // Convert to DTOs and merge with existing data
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              2,
              String.format(
                  "Converting %d wrestlers to DTOs and merging with existing data...",
                  wrestlerPages.size()));
      log.info("🔄 Converting wrestlers to DTOs and merging with existing data...");
      long convertStart = System.currentTimeMillis();
      List<WrestlerDTO> wrestlerDTOs = convertAndMergeWrestlerData(wrestlerPages);
      log.info(
          "✅ Converted and merged {} wrestlers in {}ms",
          wrestlerDTOs.size(),
          System.currentTimeMillis() - convertStart);

      // Update progress with conversion results
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              2,
              String.format(
                  "✅ Converted and merged %d wrestlers in %dms",
                  wrestlerDTOs.size(), System.currentTimeMillis() - convertStart));

      // Save wrestlers to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d wrestlers to database...", wrestlerDTOs.size()));
      log.info("🗄️ Saving wrestlers to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = saveWrestlersToDatabase(wrestlerDTOs, operationId);
      log.info(
          "✅ Saved {} wrestlers to database in {}ms",
          savedCount,
          System.currentTimeMillis() - dbStart);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} wrestlers (JSON + Database) in {}ms total",
          wrestlerDTOs.size(),
          totalTime);

      // Complete progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synced %d wrestlers", wrestlerDTOs.size()),
              wrestlerDTOs.size());

      // Record success in health monitor
      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess("Wrestlers", totalTime, wrestlerDTOs.size());

      return SyncResult.success("Wrestlers", wrestlerDTOs.size(), 0, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize wrestlers from Notion after {}ms", totalTime, e);

      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      syncServiceDependencies.getHealthMonitor().recordFailure("Wrestlers", e.getMessage());

      return SyncResult.failure("Wrestlers", e.getMessage());
    }
  }

  /** Converts WrestlerPage objects to WrestlerDTO objects and merges with existing JSON data. */
  private List<WrestlerDTO> convertAndMergeWrestlerData(@NonNull List<WrestlerPage> wrestlerPages) {
    Map<String, WrestlerDTO> existingWrestlers = new HashMap<>();
    if (syncServiceDependencies.getNotionSyncProperties().isLoadFromJson()) {
      existingWrestlers = loadExistingWrestlersFromJson();
    }

    // Convert Notion pages to DTOs and merge with existing data
    List<WrestlerDTO> mergedWrestlers = new ArrayList<>();

    for (WrestlerPage wrestlerPage : wrestlerPages) {
      WrestlerDTO notionDTO = convertWrestlerPageToDTO(wrestlerPage);

      // Try to find existing wrestler by external ID first, then by name
      WrestlerDTO existingDTO = null;
      if (notionDTO.getExternalId() != null) {
        existingDTO =
            existingWrestlers.values().stream()
                .filter(w -> notionDTO.getExternalId().equals(w.getExternalId()))
                .findFirst()
                .orElse(null);
      }
      if (existingDTO == null && notionDTO.getName() != null) {
        existingDTO = existingWrestlers.get(notionDTO.getName());
      }

      // Merge data: preserve existing game data, update Notion data
      WrestlerDTO mergedDTO = mergeWrestlerData(existingDTO, notionDTO);
      mergedWrestlers.add(mergedDTO);
    }

    // Add any existing wrestlers that weren't found in Notion (preserve local-only wrestlers)
    for (WrestlerDTO existing : existingWrestlers.values()) {
      boolean foundInNotion =
          mergedWrestlers.stream()
              .anyMatch(
                  merged ->
                      (existing.getExternalId() != null
                              && existing.getExternalId().equals(merged.getExternalId()))
                          || (existing.getName() != null
                              && existing.getName().equals(merged.getName())));

      if (!foundInNotion) {
        mergedWrestlers.add(existing);
        log.debug("Preserved local-only wrestler: {}", existing.getName());
      }
    }

    return mergedWrestlers;
  }

  /** Converts a single WrestlerPage to WrestlerDTO. */
  private WrestlerDTO convertWrestlerPageToDTO(@NonNull WrestlerPage wrestlerPage) {
    WrestlerDTO dto = new WrestlerDTO();
    dto.setName(extractNameFromNotionPage(wrestlerPage));

    // Extract and truncate description to fit database constraint (1000 chars)
    String description = extractDescriptionFromPageBody(wrestlerPage);
    if (description.length() > 1000) {
      description = description.substring(0, 997) + "...";
      log.debug(
          "Truncated description for wrestler '{}' from {} to 1000 characters",
          dto.getName(),
          description.length() + 3);
    }
    dto.setDescription(description);

    dto.setFaction(extractFactionFromWrestlerPage(wrestlerPage));
    dto.setExternalId(wrestlerPage.getId());

    Map<String, Object> rawProperties = wrestlerPage.getRawProperties();
    if (rawProperties != null) {
      // Extract Fans
      Object fansObj = rawProperties.get("Fans");
      if (fansObj instanceof Number) {
        try {
          dto.setFans(((Number) fansObj).longValue());
        } catch (NumberFormatException e) {
          log.warn("Invalid 'Fans' value for {}: {}", dto.getName(), fansObj);
        }
      }

      // Extract Gender
      Object genderObj = rawProperties.get("Gender");
      if (genderObj instanceof String) {
        dto.setGender((String) genderObj);
      }

      // Extract Bumps
      Object bumpsObj = rawProperties.get("Bumps");
      if (bumpsObj instanceof Number) {
        try {
          dto.setBumps(((Number) bumpsObj).intValue());
        } catch (NumberFormatException e) {
          log.warn("Invalid 'Bumps' value for {}: {}", dto.getName(), bumpsObj);
        }
      }

      // Extract IsPlayer
      Object isPlayerObj = rawProperties.get("Player");
      dto.setIsPlayer(Boolean.TRUE.equals(isPlayerObj));

      // Extract CreationDate
      Object creationDateObj = rawProperties.get("Created");
      if (creationDateObj instanceof String) {
        dto.setCreationDate((String) creationDateObj);
      }
      // Extract Tier
      Object tierObj = rawProperties.get("Tier");
      if (tierObj instanceof String) {
        dto.setTier((String) tierObj);
      }

      // Extract Starting Health
      Object startingHealthObj = rawProperties.get("Starting Health");
      if (startingHealthObj instanceof Number) {
        dto.setStartingHealth(((Number) startingHealthObj).intValue());
      }
      // Extract Low Health
      Object lowHealthObj = rawProperties.get("Low Health");
      if (lowHealthObj instanceof Number) {
        dto.setLowHealth(((Number) lowHealthObj).intValue());
      }
      // Extract Starting Stamina
      Object startingStaminaObj = rawProperties.get("Starting Stamina");
      if (startingStaminaObj instanceof Number) {
        dto.setStartingStamina(((Number) startingStaminaObj).intValue());
      }
      // Extract Low Stamina
      Object lowStaminaObj = rawProperties.get("Low Stamina");
      if (lowStaminaObj instanceof Number) {
        dto.setLowStamina(((Number) lowStaminaObj).intValue());
      }
      // Extract Deck Size
      Object deckSizeObj = rawProperties.get("Deck Size");
      if (deckSizeObj instanceof Number) {
        dto.setDeckSize(((Number) deckSizeObj).intValue());
      }

      // Extract Alignment
      Object alignmentObj = rawProperties.get("Alignment");
      if (alignmentObj instanceof String) {
        dto.setAlignment((String) alignmentObj);
      }

      // Extract Drive
      Object driveObj = rawProperties.get("Drive");
      if (driveObj instanceof Number) {
        dto.setDrive(((Number) driveObj).intValue());
      }

      // Extract Resilience
      Object resilienceObj = rawProperties.get("Resilience");
      if (resilienceObj instanceof Number) {
        dto.setResilience(((Number) resilienceObj).intValue());
      }

      // Extract Charisma
      Object charismaObj = rawProperties.get("Charisma");
      if (charismaObj instanceof Number) {
        dto.setCharisma(((Number) charismaObj).intValue());
      }

      // Extract Brawl
      Object brawlObj = rawProperties.get("Brawl");
      if (brawlObj instanceof Number) {
        dto.setBrawl(((Number) brawlObj).intValue());
      }

      // Extract Heritage Tag
      Object heritageTagObj = rawProperties.get("Heritage Tag");
      if (heritageTagObj instanceof String) {
        dto.setHeritageTag((String) heritageTagObj);
      }

      // Extract Relationship IDs
      dto.setManagerExternalId(extractRelationId(rawProperties.get("Manager")));
      dto.setInjuryExternalIds(extractRelationIds(rawProperties.get("Injuries")));
      dto.setTeamExternalIds(extractRelationIds(rawProperties.get("Teams")));
      dto.setTitleReignExternalIds(extractRelationIds(rawProperties.get("Titles")));
    }
    return dto;
  }

  /** Extracts a single relation ID from a Notion property. */
  private String extractRelationId(Object property) {
    List<String> ids = extractRelationIds(property);
    return ids.isEmpty() ? null : ids.get(0);
  }

  /** Extracts relation IDs from a Notion property. */
  private List<String> extractRelationIds(Object property) {
    List<String> ids = new java.util.ArrayList<>();
    if (property == null) return ids;

    if (property instanceof String str) {
      if (str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        ids.add(str);
      }
    } else if (property instanceof java.util.List<?> list) {
      for (Object item : list) {
        if (item instanceof String str) {
          if (str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            ids.add(str);
          }
        } else if (item instanceof Map<?, ?> map) {
          Object id = map.get("id");
          if (id instanceof String str) ids.add(str);
        }
      }
    } else if (property instanceof Map<?, ?> map) {
      Object id = map.get("id");
      if (id instanceof String str) ids.add(str);
    }
    return ids;
  }

  /** Extracts description from the page body/content using NotionBlocksRetriever. */
  private String extractDescriptionFromPageBody(@NonNull NotionPage page) {
    if (page.getId() == null) {
      return "";
    }

    try {
      NotionBlocksRetriever blocksRetriever =
          new NotionBlocksRetriever(EnvironmentVariableUtil.getNotionToken());
      String content = blocksRetriever.retrievePageContent(page.getId());

      if (content != null && !content.trim().isEmpty()) {
        // Clean up the content - remove excessive newlines and trim
        return content.replaceAll("\n{3,}", "\n\n").trim();
      }
    } catch (Exception e) {
      log.debug(
          "Failed to retrieve page content for wrestler {}: {}", page.getId(), e.getMessage());
    }

    return "";
  }

  /** Extracts faction from a WrestlerPage. */
  private String extractFactionFromWrestlerPage(@NonNull WrestlerPage wrestlerPage) {
    if (wrestlerPage.getRawProperties() == null) {
      return null;
    }

    // Try different possible property names for faction
    Object factionProperty = wrestlerPage.getRawProperties().get("Faction");
    if (factionProperty == null) {
      factionProperty = wrestlerPage.getRawProperties().get("Team");
    }
    if (factionProperty == null) {
      factionProperty = wrestlerPage.getRawProperties().get("faction");
    }

    if (factionProperty != null) {
      String factionStr = factionProperty.toString().trim();

      // If it shows as "X relations", we need to resolve the relationship
      if (factionStr.matches("\\d+ relations?")) {
        log.debug(
            "Faction shows as relationship count ({}), preserving existing faction", factionStr);
        return null;
      }

      // If it's already a readable name, use it
      if (!factionStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        log.debug("Found faction name: {}", factionStr);
        return factionStr;
      }
    }

    return null;
  }

  /** Loads existing wrestlers from the wrestlers.json file. */
  private Map<String, WrestlerDTO> loadExistingWrestlersFromJson() {
    Map<String, WrestlerDTO> existingWrestlers = new HashMap<>();
    Path wrestlersFile = Paths.get("src/main/resources/wrestlers.json");

    if (!Files.exists(wrestlersFile)) {
      log.debug("No existing wrestlers.json file found");
      return existingWrestlers;
    }

    try {
      List<WrestlerDTO> wrestlers =
          objectMapper.readValue(
              wrestlersFile.toFile(),
              objectMapper.getTypeFactory().constructCollectionType(List.class, WrestlerDTO.class));

      for (WrestlerDTO wrestler : wrestlers) {
        if (wrestler.getName() != null) {
          existingWrestlers.put(wrestler.getName(), wrestler);
          log.debug(
              "Loaded existing wrestler: {} with faction: {} and description: {}",
              wrestler.getName(),
              wrestler.getFaction(),
              wrestler.getDescription());
        }
      }

      log.debug("Loaded {} existing wrestlers from JSON file", existingWrestlers.size());
    } catch (Exception e) {
      log.warn("Failed to load existing wrestlers from JSON file: {}", e.getMessage());
    }

    return existingWrestlers;
  }

  /** Merges Notion data with existing wrestler data, preserving game-specific fields. */
  private WrestlerDTO mergeWrestlerData(WrestlerDTO existing, @NonNull WrestlerDTO notion) {
    WrestlerDTO merged = new WrestlerDTO();

    log.debug("Merging wrestler data for: {}", notion.getName());
    if (existing != null) {
      log.debug("  Existing faction: {}", existing.getFaction());
      log.debug("  Existing description: {}", existing.getDescription());
    } else {
      log.debug("  No existing data found for wrestler: {}", notion.getName());
    }

    // Always use Notion data for these fields (they're the source of truth)
    merged.setName(notion.getName());
    merged.setExternalId(notion.getExternalId());

    // Smart description handling: prefer Notion if available, otherwise preserve existing
    if (notion.getDescription() != null && !notion.getDescription().trim().isEmpty()) {
      merged.setDescription(notion.getDescription());
    } else if (existing != null && existing.getDescription() != null) {
      merged.setDescription(existing.getDescription());
    } else {
      merged.setDescription("Professional wrestler competing in All Time Wrestling");
    }

    // Smart faction handling: prefer Notion if available, otherwise preserve existing
    if (notion.getFaction() != null && !notion.getFaction().trim().isEmpty()) {
      merged.setFaction(notion.getFaction());
    } else if (existing != null && existing.getFaction() != null) {
      merged.setFaction(existing.getFaction());
    } else {
      merged.setFaction(null);
    }

    // Smart fans handling: prefer Notion if available, otherwise preserve existing
    if (notion.getFans() != null) {
      merged.setFans(notion.getFans());
    } else if (existing != null && existing.getFans() != null) {
      merged.setFans(existing.getFans());
    } else {
      merged.setFans(0L);
    }

    // Smart isPlayer handling: prefer Notion if available, otherwise preserve existing
    if (notion.getIsPlayer() != null) {
      merged.setIsPlayer(notion.getIsPlayer());
    } else if (existing != null && existing.getIsPlayer() != null) {
      merged.setIsPlayer(existing.getIsPlayer());
    } else {
      merged.setIsPlayer(false);
    }

    // Smart bumps handling: prefer Notion if available, otherwise preserve existing
    if (notion.getBumps() != null) {
      merged.setBumps(notion.getBumps());
    } else if (existing != null && existing.getBumps() != null) {
      merged.setBumps(existing.getBumps());
    } else {
      merged.setBumps(0);
    }

    // Smart creationDate handling: prefer Notion if available, otherwise preserve existing
    if (notion.getCreationDate() != null && !notion.getCreationDate().trim().isEmpty()) {
      merged.setCreationDate(notion.getCreationDate());
    } else if (existing != null && existing.getCreationDate() != null) {
      merged.setCreationDate(existing.getCreationDate());
    } else {
      merged.setCreationDate(null);
    }

    // Smart gender handling: prefer Notion if available, otherwise preserve existing
    if (notion.getGender() != null && !notion.getGender().trim().isEmpty()) {
      merged.setGender(notion.getGender());
    } else if (existing != null && existing.getGender() != null) {
      merged.setGender(existing.getGender());
    } else {
      merged.setGender(null);
    }

    // Smart tier handling: prefer Notion if available, otherwise preserve existing
    if (notion.getTier() != null && !notion.getTier().trim().isEmpty()) {
      merged.setTier(notion.getTier());
    } else if (existing != null && existing.getTier() != null) {
      merged.setTier(existing.getTier());
    } else {
      merged.setTier(null);
    }

    // Smart startingHealth handling: prefer Notion if available, otherwise preserve existing
    if (notion.getStartingHealth() != null) {
      merged.setStartingHealth(notion.getStartingHealth());
    } else if (existing != null && existing.getStartingHealth() != null) {
      merged.setStartingHealth(existing.getStartingHealth());
    } else {
      merged.setStartingHealth(0);
    }

    // Smart startingStamina handling: prefer Notion if available, otherwise preserve existing
    if (notion.getStartingStamina() != null) {
      merged.setStartingStamina(notion.getStartingStamina());
    } else if (existing != null && existing.getStartingStamina() != null) {
      merged.setStartingStamina(existing.getStartingStamina());
    } else {
      merged.setStartingStamina(0);
    }

    // Smart lowHealth handling: prefer Notion if available, otherwise preserve existing
    if (notion.getLowHealth() != null) {
      merged.setLowHealth(notion.getLowHealth());
    } else if (existing != null && existing.getLowHealth() != null) {
      merged.setLowHealth(existing.getLowHealth());
    } else {
      merged.setLowHealth(0);
    }

    // Smart lowStamina handling: prefer Notion if available, otherwise preserve existing
    if (notion.getLowStamina() != null) {
      merged.setLowStamina(notion.getLowStamina());
    } else if (existing != null && existing.getLowStamina() != null) {
      merged.setLowStamina(existing.getLowStamina());
    } else {
      merged.setLowStamina(0);
    }

    // Smart deckSize handling: prefer Notion if available, otherwise preserve existing
    if (notion.getDeckSize() != null) {
      merged.setDeckSize(notion.getDeckSize());
    } else if (existing != null && existing.getDeckSize() != null) {
      merged.setDeckSize(existing.getDeckSize());
    } else {
      merged.setDeckSize(15);
    }

    // Smart alignment handling
    if (notion.getAlignment() != null && !notion.getAlignment().trim().isEmpty()) {
      merged.setAlignment(notion.getAlignment());
    } else if (existing != null && existing.getAlignment() != null) {
      merged.setAlignment(existing.getAlignment());
    } else {
      merged.setAlignment(null);
    }

    // Smart drive handling
    if (notion.getDrive() != null) {
      merged.setDrive(notion.getDrive());
    } else if (existing != null && existing.getDrive() != null) {
      merged.setDrive(existing.getDrive());
    } else {
      merged.setDrive(1);
    }

    // Smart resilience handling
    if (notion.getResilience() != null) {
      merged.setResilience(notion.getResilience());
    } else if (existing != null && existing.getResilience() != null) {
      merged.setResilience(existing.getResilience());
    } else {
      merged.setResilience(1);
    }

    // Smart charisma handling
    if (notion.getCharisma() != null) {
      merged.setCharisma(notion.getCharisma());
    } else if (existing != null && existing.getCharisma() != null) {
      merged.setCharisma(existing.getCharisma());
    } else {
      merged.setCharisma(1);
    }

    // Smart brawl handling
    if (notion.getBrawl() != null) {
      merged.setBrawl(notion.getBrawl());
    } else if (existing != null && existing.getBrawl() != null) {
      merged.setBrawl(existing.getBrawl());
    } else {
      merged.setBrawl(1);
    }

    // Smart heritageTag handling
    if (notion.getHeritageTag() != null && !notion.getHeritageTag().trim().isEmpty()) {
      merged.setHeritageTag(notion.getHeritageTag());
    } else if (existing != null && existing.getHeritageTag() != null) {
      merged.setHeritageTag(existing.getHeritageTag());
    } else {
      merged.setHeritageTag(null);
    }

    // Merge Relationship External IDs
    if (notion.getManagerExternalId() != null) {
      merged.setManagerExternalId(notion.getManagerExternalId());
    } else if (existing != null) {
      merged.setManagerExternalId(existing.getManagerExternalId());
    }

    if (!notion.getInjuryExternalIds().isEmpty()) {
      merged.setInjuryExternalIds(notion.getInjuryExternalIds());
    } else if (existing != null) {
      merged.setInjuryExternalIds(existing.getInjuryExternalIds());
    }

    if (!notion.getTeamExternalIds().isEmpty()) {
      merged.setTeamExternalIds(notion.getTeamExternalIds());
    } else if (existing != null) {
      merged.setTeamExternalIds(existing.getTeamExternalIds());
    }

    if (!notion.getTitleReignExternalIds().isEmpty()) {
      merged.setTitleReignExternalIds(notion.getTitleReignExternalIds());
    } else if (existing != null) {
      merged.setTitleReignExternalIds(existing.getTitleReignExternalIds());
    }

    merged.setExternalId(notion.getExternalId());
    return merged;
  }

  /** Saves wrestler DTOs to the database. */
  private int saveWrestlersToDatabase(
      @NonNull List<WrestlerDTO> wrestlerDTOs, @NonNull String operationId) {
    log.info("Starting database persistence for {} wrestlers", wrestlerDTOs.size());

    int savedCount = 0;
    int skippedCount = 0;
    int processedCount = 0;

    for (WrestlerDTO dto : wrestlerDTOs) {
      processedCount++;

      // Update progress every 5 wrestlers
      if (processedCount % 5 == 0) {
        syncServiceDependencies
            .getProgressTracker()
            .updateProgress(
                operationId,
                4,
                String.format(
                    "Saving wrestlers to database... (%d/%d processed)",
                    processedCount, wrestlerDTOs.size()));
      }

      try {
        // Smart duplicate handling - prefer external ID, fallback to name
        Wrestler wrestler = null;
        boolean isNewWrestler = false;

        // 1. Try to find by external ID first (most reliable)
        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          log.debug("Searching for existing wrestler with external ID: {}", dto.getExternalId());
          wrestler = wrestlerService.findByExternalId(dto.getExternalId()).orElse(null);
          if (wrestler != null) {
            log.debug(
                "Found existing wrestler by external ID: {} for wrestler: {}",
                dto.getExternalId(),
                dto.getName());
          } else {
            log.debug("No existing wrestler found with external ID: {}", dto.getExternalId());
          }
        }

        // 2. Fallback to name matching if external ID didn't work
        if (wrestler == null && dto.getName() != null && !dto.getName().trim().isEmpty()) {
          log.debug("Searching for existing wrestler with name: {}", dto.getName());
          wrestler = wrestlerService.findByName(dto.getName()).orElse(null);
          if (wrestler != null) {
            log.debug("Found existing wrestler by name: {}", dto.getName());
          } else {
            log.debug("No existing wrestler found with name: {}", dto.getName());
          }
        }

        // 3. Create new wrestler if no segment found
        if (wrestler == null) {
          wrestler = Wrestler.builder().build();
          isNewWrestler = true;
          log.info(
              "🆕 Creating new wrestler: {} with external ID: {}",
              dto.getName(),
              dto.getExternalId());
        } else {
          log.info(
              "🔄 Updating existing wrestler: {} (ID: {}) with external ID: {}",
              dto.getName(),
              wrestler.getId(),
              dto.getExternalId());
        }

        // Set basic properties (always update these)
        wrestler.setName(dto.getName());
        wrestler.setExternalId(dto.getExternalId());

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
          try {
            wrestler.setGender(Gender.valueOf(dto.getGender().toUpperCase()));
          } catch (IllegalArgumentException e) {
            log.warn("Invalid sex value '{}' for wrestler '{}'", dto.getGender(), dto.getName());
          }
        }

        if (dto.getTier() != null && !dto.getTier().isBlank()) {
          try {
            wrestler.setTier(WrestlerTier.fromDisplayName(dto.getTier()));
          } catch (IllegalArgumentException e) {
            log.warn("Invalid tier value '{}' for wrestler '{}'", dto.getTier(), dto.getName());
          }
        }

        // Update description if provided
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
          wrestler.setDescription(dto.getDescription());
          log.debug(
              "Updated description for wrestler {}: {}",
              dto.getName(),
              dto.getDescription().substring(0, Math.min(50, dto.getDescription().length()))
                  + "...");
        }

        // Set default values for required fields if this is a new wrestler
        if (isNewWrestler) {
          wrestler.setDeckSize(dto.getDeckSize() != null ? dto.getDeckSize() : 15);
          wrestler.setStartingHealth(dto.getStartingHealth() != null ? dto.getStartingHealth() : 0);
          wrestler.setLowHealth(dto.getLowHealth() != null ? dto.getLowHealth() : 0);
          wrestler.setStartingStamina(
              dto.getStartingStamina() != null ? dto.getStartingStamina() : 0);
          wrestler.setLowStamina(dto.getLowStamina() != null ? dto.getLowStamina() : 0);
          wrestler.setFans(dto.getFans() != null ? dto.getFans() : 0L);
          wrestler.setIsPlayer(dto.getIsPlayer() != null ? dto.getIsPlayer() : false);
          wrestler.setBumps(dto.getBumps() != null ? dto.getBumps() : 0);
          wrestler.setCreationDate(java.time.Instant.now());
        } else {
          // For existing wrestlers, update game fields from DTO if they have values
          if (dto.getDeckSize() != null) wrestler.setDeckSize(dto.getDeckSize());
          if (dto.getStartingHealth() != null) wrestler.setStartingHealth(dto.getStartingHealth());
          if (dto.getLowHealth() != null) wrestler.setLowHealth(dto.getLowHealth());
          if (dto.getStartingStamina() != null)
            wrestler.setStartingStamina(dto.getStartingStamina());
          if (dto.getLowStamina() != null) wrestler.setLowStamina(dto.getLowStamina());
          if (dto.getFans() != null) wrestler.setFans(dto.getFans());
          if (dto.getIsPlayer() != null) wrestler.setIsPlayer(dto.getIsPlayer());
          if (dto.getBumps() != null) wrestler.setBumps(dto.getBumps());
        }

        // Update campaign attributes
        if (dto.getDrive() != null) wrestler.setDrive(dto.getDrive());
        if (dto.getResilience() != null) wrestler.setResilience(dto.getResilience());
        if (dto.getCharisma() != null) wrestler.setCharisma(dto.getCharisma());
        if (dto.getBrawl() != null) wrestler.setBrawl(dto.getBrawl());
        if (dto.getHeritageTag() != null) wrestler.setHeritageTag(dto.getHeritageTag());

        // Resolve relationships
        // 1. Faction
        if (dto.getFaction() != null && !dto.getFaction().isBlank()) {
          factionRepository
              .findByName(dto.getFaction())
              .ifPresentOrElse(
                  wrestler::setFaction,
                  () -> log.debug("Faction not found for wrestler: {}", dto.getFaction()));
        }

        // 2. Manager
        if (dto.getManagerExternalId() != null) {
          npcRepository
              .findByExternalId(dto.getManagerExternalId())
              .ifPresentOrElse(
                  wrestler::setManager,
                  () ->
                      log.debug("Manager not found for wrestler: {}", dto.getManagerExternalId()));
        }

        // 3. Injuries
        if (dto.getInjuryExternalIds() != null && !dto.getInjuryExternalIds().isEmpty()) {
          wrestler.getInjuries().clear();
          for (String id : dto.getInjuryExternalIds()) {
            java.util.Optional<com.github.javydreamercsw.management.domain.injury.Injury>
                injuryOpt = injuryRepository.findByExternalId(id);
            if (injuryOpt.isPresent()) {
              com.github.javydreamercsw.management.domain.injury.Injury injury = injuryOpt.get();
              injury.setWrestler(wrestler);
              wrestler.getInjuries().add(injury);
            }
          }
        }

        // 4. Title Reigns
        if (dto.getTitleReignExternalIds() != null && !dto.getTitleReignExternalIds().isEmpty()) {
          for (String id : dto.getTitleReignExternalIds()) {
            java.util.Optional<com.github.javydreamercsw.management.domain.title.TitleReign>
                reignOpt = titleReignRepository.findByExternalId(id);
            if (reignOpt.isPresent()) {
              com.github.javydreamercsw.management.domain.title.TitleReign reign = reignOpt.get();
              if (!wrestler.getReigns().contains(reign)) {
                wrestler.getReigns().add(reign);
                if (!reign.getChampions().contains(wrestler)) {
                  reign.getChampions().add(wrestler);
                }
              }
            }
          }
        }

        // Update alignment
        if (dto.getAlignment() != null && !dto.getAlignment().isBlank()) {
          try {
            AlignmentType type = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
            WrestlerAlignment alignment =
                wrestlerAlignmentRepository.findByWrestler(wrestler).orElse(null);
            if (alignment == null) {
              alignment =
                  WrestlerAlignment.builder()
                      .wrestler(wrestler)
                      .alignmentType(type)
                      .level(1) // Default level 1
                      .build();
            } else {
              alignment.setAlignmentType(type);
            }
            wrestler.setAlignment(alignment);
          } catch (IllegalArgumentException e) {
            log.warn(
                "Invalid alignment value '{}' for wrestler '{}'",
                dto.getAlignment(),
                dto.getName());
          }
        }

        tierRecalculationService.recalculateTier(wrestler);

        // Save the wrestler
        log.info(
            "💾 Saving wrestler to database: {} (ID: {}, isNew: {})",
            wrestler.getName(),
            wrestler.getId(),
            isNewWrestler);

        Wrestler savedWrestler;
        if (isNewWrestler) {
          savedWrestler = wrestlerService.save(wrestler);
        } else {
          savedWrestler = wrestlerRepository.saveAndFlush(wrestler);
        }
        savedCount++;

        log.info(
            "✅ Wrestler saved successfully: {} (Final ID: {})",
            savedWrestler.getName(),
            savedWrestler.getId());

      } catch (Exception e) {
        log.error("❌ Failed to save wrestler: {} - {}", dto.getName(), e.getMessage());
        skippedCount++;
      }
    }

    log.info(
        "Database persistence completed: {} saved/updated, {} skipped", savedCount, skippedCount);

    // Final progress update
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(
            operationId,
            4,
            String.format(
                "✅ Completed database save: %d wrestlers saved/updated, %d skipped",
                savedCount, skippedCount));

    return savedCount;
  }

  public SyncResult syncToNotion(@NonNull String operationId) {
    return wrestlerNotionSyncService.syncToNotion(operationId);
  }

  /** DTO for Wrestler data from Notion. */
  @Setter
  @Getter
  public static class WrestlerDTO {
    // Getters and setters
    private String name;
    private String description;
    private String externalId; // Notion page ID
    private String gender;
    private String tier;
    private String alignment;
    private String managerExternalId;
    private List<String> injuryExternalIds = new java.util.ArrayList<>();
    private List<String> teamExternalIds = new java.util.ArrayList<>();
    private List<String> titleReignExternalIds = new java.util.ArrayList<>();
    private Integer drive;
    private Integer resilience;
    private Integer charisma;
    private Integer brawl;
    private String heritageTag;

    // Game-specific fields (preserved from existing data)
    private Integer deckSize;
    private Integer startingHealth;
    private Integer lowHealth;
    private Integer startingStamina;
    private Integer lowStamina;
    private Long fans;
    private Boolean isPlayer;
    private Integer bumps;
    private String faction;
    private String creationDate;
  }
}
