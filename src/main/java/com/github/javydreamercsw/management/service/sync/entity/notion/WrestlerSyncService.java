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
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.base.util.NotionBlocksRetriever;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing wrestlers from Notion to the database. */
@Service
@Slf4j
public class WrestlerSyncService extends BaseSyncService {
  private final WrestlerRepository wrestlerRepository;
  private final NotionApiExecutor notionApiExecutor;
  private final NotionPageDataExtractor notionPageDataExtractor;

  @Autowired
  public WrestlerSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      WrestlerRepository wrestlerRepository,
      NotionApiExecutor notionApiExecutor,
      NotionPageDataExtractor notionPageDataExtractor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.wrestlerRepository = wrestlerRepository;
    this.notionApiExecutor = notionApiExecutor;
    this.notionPageDataExtractor = notionPageDataExtractor;
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
      if (!notionApiExecutor.getSyncProperties().isEntityEnabled("wrestlers")) {
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
          notionApiExecutor.executeWithRateLimit(
              notionApiExecutor.getNotionHandler()::loadAllWrestlers);
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
      List<Wrestler> wrestlers = convertAndMergeWrestlerData(wrestlerPages);
      log.info(
          "✅ Converted and merged {} wrestlers in {}ms",
          wrestlers.size(),
          System.currentTimeMillis() - convertStart);

      // Update progress with conversion results
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d wrestlers to database...", wrestlers.size()));

      // Save wrestlers to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d wrestlers to database...", wrestlers.size()));
      log.info("🗄️ Saving wrestlers to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = saveWrestlersToDatabase(wrestlers, operationId);
      log.info(
          "✅ Saved {} wrestlers to database in {}ms",
          savedCount,
          System.currentTimeMillis() - dbStart);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} wrestlers (JSON + Database) in {}ms total",
          wrestlers.size(),
          totalTime);

      // Complete progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synced %d wrestlers", wrestlers.size()),
              wrestlers.size());

      // Record success in health monitor
      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess("Wrestlers", totalTime, wrestlers.size());

      return SyncResult.success("Wrestlers", wrestlers.size(), 0, 0);

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

  /** Converts WrestlerPage objects to Wrestler objects and merges with existing JSON data. */
  private List<Wrestler> convertAndMergeWrestlerData(@NonNull List<WrestlerPage> wrestlerPages) {
    Map<String, Wrestler> existingWrestlers = new HashMap<>();
    if (notionApiExecutor.getSyncProperties().isLoadFromJson()) {
      existingWrestlers = loadExistingWrestlersFromJson();
    }

    // Convert Notion pages to DTOs and merge with existing data
    List<Wrestler> mergedWrestlers = new ArrayList<>();

    for (WrestlerPage wrestlerPage : wrestlerPages) {
      Wrestler notionDTO = convertWrestlerPageToDTO(wrestlerPage);

      // Try to find existing wrestler by external ID first, then by name
      Wrestler existingDTO = null;
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
      Wrestler mergedDTO = mergeWrestlerData(existingDTO, notionDTO);
      mergedWrestlers.add(mergedDTO);
    }

    // Add any existing wrestlers that weren't found in Notion (preserve local-only wrestlers)
    for (Wrestler existing : existingWrestlers.values()) {
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

  /** Converts a single WrestlerPage to Wrestler. */
  private Wrestler convertWrestlerPageToDTO(@NonNull WrestlerPage wrestlerPage) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(notionPageDataExtractor.extractNameFromNotionPage(wrestlerPage));

    // Extract and truncate description to fit database constraint (1000 chars)
    String description = extractDescriptionFromPageBody(wrestlerPage);
    if (description.length() > 1000) {
      description = description.substring(0, 997) + "...";
      log.debug(
          "Truncated description for wrestler '{}' from {} to 1000 characters",
          wrestler.getName(),
          description.length() + 3);
    }
    wrestler.setDescription(description);

    wrestler.setExternalId(wrestlerPage.getId());

    Map<String, Object> rawProperties = wrestlerPage.getRawProperties();
    if (rawProperties != null) {
      // Extract Fans
      Object fansObj = rawProperties.get("Fans");
      if (fansObj instanceof Number) {
        try {
          wrestler.setFans(((Number) fansObj).longValue());
        } catch (NumberFormatException e) {
          log.warn("Invalid 'Fans' value for {}: {}", wrestler.getName(), fansObj);
        }
      }

      // Extract Gender
      Object genderObj = rawProperties.get("Gender");
      if (genderObj instanceof String) {
        try {
          wrestler.setGender(Gender.valueOf((String) genderObj));
        } catch (IllegalArgumentException e) {
          log.warn("Invalid 'Gender' value for {}: {}", wrestler.getName(), genderObj);
        }
      }

      // Extract Bumps
      Object bumpsObj = rawProperties.get("Bumps");
      if (bumpsObj instanceof Number) {
        try {
          wrestler.setBumps(((Number) bumpsObj).intValue());
        } catch (NumberFormatException e) {
          log.warn("Invalid 'Bumps' value for {}: {}", wrestler.getName(), bumpsObj);
        }
      }

      // Extract IsPlayer
      Object isPlayerObj = rawProperties.get("Player");
      wrestler.setIsPlayer(Boolean.TRUE.equals(isPlayerObj));

      // Extract Tier
      Object tierObj = rawProperties.get("Tier");
      if (tierObj instanceof String) {
        try {
          wrestler.setTier(WrestlerTier.fromDisplayName((String) tierObj));
        } catch (IllegalArgumentException e) {
          log.warn("Invalid 'Tier' value for {}: {}", wrestler.getName(), tierObj);
        }
      }

      // Extract Starting Health
      Object startingHealthObj = rawProperties.get("Starting Health");
      if (startingHealthObj instanceof Number) {
        wrestler.setStartingHealth(((Number) startingHealthObj).intValue());
      }
      // Extract Low Health
      Object lowHealthObj = rawProperties.get("Low Health");
      if (lowHealthObj instanceof Number) {
        wrestler.setLowHealth(((Number) lowHealthObj).intValue());
      }
      // Extract Starting Stamina
      Object startingStaminaObj = rawProperties.get("Starting Stamina");
      if (startingStaminaObj instanceof Number) {
        wrestler.setStartingStamina(((Number) startingStaminaObj).intValue());
      }
      // Extract Low Stamina
      Object lowStaminaObj = rawProperties.get("Low Stamina");
      if (lowStaminaObj instanceof Number) {
        wrestler.setLowStamina(((Number) lowStaminaObj).intValue());
      }
      // Extract Deck Size
      Object deckSizeObj = rawProperties.get("Deck Size");
      if (deckSizeObj instanceof Number) {
        wrestler.setDeckSize(((Number) deckSizeObj).intValue());
      }
    }
    return wrestler;
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

  /** Loads existing wrestlers from the wrestlers.json file. */
  private Map<String, Wrestler> loadExistingWrestlersFromJson() {
    Map<String, Wrestler> existingWrestlers = new HashMap<>();
    Path wrestlersFile = Paths.get("src/main/resources/wrestlers.json");

    if (!Files.exists(wrestlersFile)) {
      log.debug("No existing wrestlers.json file found");
      return existingWrestlers;
    }

    try {
      List<Wrestler> wrestlers =
          objectMapper.readValue(
              wrestlersFile.toFile(),
              objectMapper.getTypeFactory().constructCollectionType(List.class, Wrestler.class));

      for (Wrestler wrestler : wrestlers) {
        if (wrestler.getName() != null) {
          existingWrestlers.put(wrestler.getName(), wrestler);
          log.debug("Loaded existing wrestler: {}", wrestler.getName());
        }
      }

      log.debug("Loaded {} existing wrestlers from JSON file", existingWrestlers.size());
    } catch (Exception e) {
      log.warn("Failed to load existing wrestlers from JSON file: {}", e.getMessage());
    }

    return existingWrestlers;
  }

  /** Merges Notion data with existing wrestler data, preserving game-specific fields. */
  private Wrestler mergeWrestlerData(Wrestler existing, @NonNull Wrestler notion) {
    Wrestler merged = new Wrestler();

    log.debug("Merging wrestler data for: {}", notion.getName());
    if (existing != null) {
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

    // Smart gender handling: prefer Notion if available, otherwise preserve existing
    if (notion.getGender() != null) {
      merged.setGender(notion.getGender());
    } else if (existing != null && existing.getGender() != null) {
      merged.setGender(existing.getGender());
    } else {
      merged.setGender(null);
    }

    // Smart tier handling: prefer Notion if available, otherwise preserve existing
    if (notion.getTier() != null) {
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

    merged.setExternalId(notion.getExternalId());
    return merged;
  }

  /** Saves wrestler DTOs to the database. */
  private int saveWrestlersToDatabase(
      @NonNull List<Wrestler> wrestlers, @NonNull String operationId) {
    log.info("Starting database persistence for {} wrestlers", wrestlers.size());

    int savedCount = 0;
    int skippedCount = 0;
    int processedCount = 0;

    for (Wrestler dto : wrestlers) {
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
                    processedCount, wrestlers.size()));
      }

      try {
        wrestlerRepository.saveAndFlush(dto);
        savedCount++;

        log.info("✅ Wrestler saved successfully: {} (Final ID: {})", dto.getName(), dto.getId());

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
}
