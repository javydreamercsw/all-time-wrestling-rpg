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
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for synchronizing shows from Notion to the database. */
@Service
@Slf4j
public class ShowSyncService extends BaseSyncService {

  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;

  @Autowired @Lazy private ShowSyncService self;
  @Autowired @Lazy private ShowTypeSyncService showTypeSyncService;

  public ShowSyncService(
      final ObjectMapper objectMapper,
      final SyncServiceDependencies syncServiceDependencies,
      final ShowService showService,
      final ShowTypeService showTypeService,
      final SeasonService seasonService,
      final ShowTemplateService showTemplateService,
      final NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.showService = showService;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.self = this;
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database.
   *
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows() {
    return syncShows(UUID.randomUUID().toString());
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database with optional progress
   * tracking.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows(@NonNull final String operationId) {
    if (isNotionHandlerAvailable()) {
      // Check if already synced in current session
      if (syncServiceDependencies.getSyncSessionManager().isAlreadySyncedInSession("shows")) {
        log.info("⏭️ Shows already synced in current session, skipping");
        return SyncResult.success("shows", 0, 0, 0);
      }

      try {
        SyncResult result = performShowsSync(operationId);
        if (result.isSuccess()) {
          syncServiceDependencies.getSyncSessionManager().markAsSyncedInSession("shows");
        }
        return result;
      } catch (Exception e) {
        log.error("Failed to sync shows", e);
        return SyncResult.failure("shows", e.getMessage());
      }
    }
    return SyncResult.failure("shows", "Notion Token was not provided!");
  }

  /** Internal method to perform the actual shows sync logic. */
  private SyncResult performShowsSync(@NonNull final String operationId) {
    if (!syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("shows")) {
      log.info("Shows sync is disabled in configuration");
      return SyncResult.success("shows", 0, 0, 0);
    }

    // Check if NOTION_TOKEN is available before starting sync
    if (!validateNotionToken("shows")) {
      syncServiceDependencies
          .getProgressTracker()
          .failOperation(
              operationId, "NOTION_TOKEN environment variable is required for Notion sync");
      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure("shows", "NOTION_TOKEN not available");
      return SyncResult.failure(
          "shows", "NOTION_TOKEN environment variable is required for Notion sync");
    }

    log.info("🚀 Starting shows synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Sync Shows", 10);
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 1, "Initializing sync operation...");

    try {
      log.debug("🔧 Executing shows sync with circuit breaker and retry logic");

      // Execute with circuit breaker and retry logic
      SyncResult result =
          syncServiceDependencies
              .getCircuitBreakerService()
              .execute(
                  "shows",
                  () -> {
                    log.debug("🔄 Circuit breaker executing shows sync");
                    return syncServiceDependencies
                        .getRetryService()
                        .executeWithRetry(
                            "shows",
                            attemptNumber -> {
                              log.debug("🔄 Shows sync attempt {} starting", attemptNumber);
                              syncServiceDependencies
                                  .getProgressTracker()
                                  .updateProgress(operationId, 5, "Starting shows sync...");
                              SyncResult attemptResult =
                                  performShowsSyncInternal(operationId, startTime);
                              log.debug(
                                  "🔄 Shows sync attempt {} result: {}",
                                  attemptNumber,
                                  attemptResult != null ? attemptResult.isSuccess() : "NULL");
                              return attemptResult;
                            });
                  });

      return result != null
          ? result
          : SyncResult.failure("shows", "Sync operation returned null result");

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize shows from Notion after {}ms", totalTime, e);

      // Fail progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());
      syncServiceDependencies.getHealthMonitor().recordFailure("shows", e.getMessage());

      return SyncResult.failure("shows", e.getMessage());
    }
  }

  /** Performs the actual shows sync operation with enhanced error handling. */
  @SneakyThrows
  private SyncResult performShowsSyncInternal(
      @NonNull final String operationId, final long startTime) {
    try {
      // Step 1: Get all local external IDs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Fetching local show IDs");
      List<String> localExternalIds = showService.getAllExternalIds();
      log.info("Found {} shows in the local database.", localExternalIds.size());

      // Step 2: Load ALL Notion show pages (includes last_edited_time for staleness check)
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Fetching Notion show pages");
      List<ShowPage> allNotionPages;
      try {
        allNotionPages =
            super.executeWithRateLimit(
                () -> syncServiceDependencies.getNotionHandler().loadAllShowsForSync());
      } catch (Exception e) {
        log.error("Failed to load show pages from Notion", e);
        return SyncResult.failure("shows", "Failed to load show pages: " + e.getMessage());
      }
      log.info("Found {} shows in Notion.", allNotionPages.size());

      // Step 3: Build local lastSync lookup for existing shows
      Map<String, Instant> localLastSyncMap =
          showService.findAllByExternalId(localExternalIds).stream()
              .filter(s -> s.getExternalId() != null)
              .collect(
                  Collectors.toMap(
                      Show::getExternalId,
                      s -> s.getLastSync() != null ? s.getLastSync() : Instant.EPOCH));

      // Step 4: Separate into new and stale pages
      List<ShowPage> newPages =
          allNotionPages.stream()
              .filter(p -> !localExternalIds.contains(p.getId()))
              .collect(java.util.stream.Collectors.toList());

      List<ShowPage> stalePages =
          allNotionPages.stream()
              .filter(p -> localExternalIds.contains(p.getId()))
              .filter(
                  p -> {
                    String notionEdited = p.getLast_edited_time();
                    if (notionEdited == null) {
                      return false;
                    }
                    try {
                      Instant notionEditedInstant = OffsetDateTime.parse(notionEdited).toInstant();
                      Instant localSync = localLastSyncMap.getOrDefault(p.getId(), Instant.EPOCH);
                      return notionEditedInstant.isAfter(localSync);
                    } catch (Exception e) {
                      log.warn(
                          "Could not parse last_edited_time '{}' for show page {}",
                          notionEdited,
                          p.getId());
                      return false;
                    }
                  })
              .collect(java.util.stream.Collectors.toList());

      if (newPages.isEmpty() && stalePages.isEmpty()) {
        log.info("No new or updated shows to sync from Notion.");
        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(operationId, true, "No new or updated shows to sync.", 0);
        return SyncResult.success("shows", 0, 0, 0);
      }
      log.info(
          "Found {} new shows and {} updated shows to sync from Notion.",
          newPages.size(),
          stalePages.size());

      // Step 5: Convert to DTOs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 4, "Converting shows to DTOs");
      List<ShowDTO> newDTOs = convertShowPagesToDTO(newPages, operationId);
      List<ShowDTO> staleDTOs = convertShowPagesToDTO(stalePages, operationId);

      // Step 6: Save to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 5, "Saving shows to database");
      SaveResult newResult = saveShowsToDatabase(newDTOs);
      SaveResult staleResult = saveShowsToDatabase(staleDTOs);
      int newSaved = newResult.saved();
      int staleSaved = staleResult.saved();
      int totalSkipped = newResult.skipped() + staleResult.skipped();
      int totalProcessed = newPages.size() + stalePages.size();
      int savedCount = newSaved + staleSaved;
      int errorCount = totalProcessed - savedCount - totalSkipped;

      log.info(
          "✅ Synced {} new, {} updated shows with {} skipped, {} errors",
          newSaved,
          staleSaved,
          totalSkipped,
          errorCount);

      boolean success = errorCount == 0;
      String message =
          success
              ? "Delta-sync for shows completed successfully."
              : "Delta-sync for shows completed with errors.";
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, success, message, savedCount);

      if (success) {
        return SyncResult.success("shows", newSaved, staleSaved, errorCount);
      } else {
        return SyncResult.failure("shows", message);
      }

    } catch (Exception e) {
      throw new RuntimeException("Shows sync operation failed: " + e.getMessage(), e);
    }
  }

  private List<ShowDTO> convertShowPagesToDTO(
      @NonNull final List<ShowPage> showPages, final String operationId) {
    return processWithControlledParallelism(
        showPages,
        this::convertShowPageToDTO,
        10, // Batch size
        operationId,
        5, // Progress step
        "Converted %d/%d shows");
  }

  /** Converts a single ShowPage to ShowDTO. */
  private ShowDTO convertShowPageToDTO(@NonNull final ShowPage showPage) {
    try {
      ShowDTO dto = new ShowDTO();
      String showName =
          syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(showPage);
      dto.setName(showName);
      dto.setDescription(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(showPage));
      dto.setShowType(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractPropertyAsString(showPage.getRawProperties(), "Show Type"));
      dto.setShowDate(extractShowDate(showPage));
      dto.setSeasonName(extractSeasonName(showPage));
      dto.setTemplateName(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractPropertyAsString(showPage.getRawProperties(), "Template"));
      dto.setExternalId(showPage.getId());

      log.debug("Converted show: {} -> DTO", showName);
      return dto;

    } catch (Exception e) {
      log.warn("Failed to convert show page to DTO: {}", e.getMessage());
      ShowDTO errorDto = new ShowDTO();
      errorDto.setName("ERROR: " + showPage.getId());
      errorDto.setDescription("Failed to convert: " + e.getMessage());
      return errorDto;
    }
  }

  private record SaveResult(int saved, int skipped) {}

  /** Saves the list of ShowDTO objects to the database. */
  private SaveResult saveShowsToDatabase(@NonNull final List<ShowDTO> showDTOs) {
    return saveShowsToDatabaseWithBatching(showDTOs, 50);
  }

  /** Enhanced method to save shows to database with batch processing. */
  private SaveResult saveShowsToDatabaseWithBatching(
      @NonNull final List<ShowDTO> showDTOs, final int batchSize) {
    log.info(
        "Starting database persistence for {} shows with batch size {}",
        showDTOs.size(),
        batchSize);

    // Ensure ShowTypes are synced from Notion before building the local cache,
    // so shows whose ShowType relation exists in Notion but not yet locally can be saved.
    try {
      showTypeSyncService.syncShowTypes("pre-show-sync-show-types");
    } catch (Exception e) {
      log.warn(
          "Pre-sync of show types failed, proceeding with local data only: {}", e.getMessage());
    }

    // Cache lookups for performance
    Map<String, ShowType> showTypes = new HashMap<>();
    Map<String, Season> seasons = new HashMap<>();
    Map<String, ShowTemplate> templates = new HashMap<>();

    // Load reference data
    try {
      List<ShowType> showTypeList = showTypeService.findAll();
      if (showTypeList != null) {
        showTypes = showTypeList.stream().collect(Collectors.toMap(ShowType::getName, s -> s));
        log.info("Loaded {} show types: {}", showTypes.size(), showTypes.keySet());
      }
    } catch (Exception e) {
      log.warn("Failed to load show types: {}", e.getMessage());
    }

    try {
      var seasonsPage = seasonService.getAllSeasons(Pageable.unpaged());
      if (seasonsPage != null) {
        seasons = seasonsPage.stream().collect(Collectors.toMap(Season::getName, s -> s));
        log.info("Loaded {} seasons: {}", seasons.size(), seasons.keySet());
      }
    } catch (Exception e) {
      log.warn("Failed to load seasons: {}", e.getMessage());
    }

    try {
      List<ShowTemplate> templateList = showTemplateService.findAll();
      if (templateList != null) {
        templates = templateList.stream().collect(Collectors.toMap(ShowTemplate::getName, t -> t));
        log.info("Loaded {} show templates: {}", templates.size(), templates.keySet());
      }
    } catch (Exception e) {
      log.warn("Failed to load show templates: {}", e.getMessage());
    }

    int savedCount = 0;
    int skippedCount = 0;
    int totalBatches = (int) Math.ceil((double) showDTOs.size() / batchSize);

    // Process shows in batches
    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
      int startIndex = batchIndex * batchSize;
      int endIndex = Math.min(startIndex + batchSize, showDTOs.size());
      List<ShowDTO> batch = showDTOs.subList(startIndex, endIndex);

      log.debug("Processing batch {}/{} ({} shows)", batchIndex + 1, totalBatches, batch.size());

      try {
        for (ShowDTO dto : batch) {
          try {
            if (self.processSingleShow(dto, showTypes, seasons, templates)) {
              savedCount++;
            } else {
              skippedCount++;
            }
          } catch (Exception e) {
            log.warn(
                "Failed to process show '{}' in batch {}: {}",
                dto.getName(),
                batchIndex + 1,
                e.getMessage());
            skippedCount++;
          }
        }
      } catch (Exception e) {
        log.error(
            "Failed to process entire batch {}/{}: {}",
            batchIndex + 1,
            totalBatches,
            e.getMessage());
        skippedCount += batch.size();
      }
    }

    log.info(
        "Database persistence completed: {} saved, {} skipped out of {} total",
        savedCount,
        skippedCount,
        showDTOs.size());
    return new SaveResult(savedCount, skippedCount);
  }

  /** Process a single show with error handling. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleShow(
      @NonNull ShowDTO dto,
      @NonNull Map<String, ShowType> showTypes,
      @NonNull Map<String, Season> seasons,
      @NonNull Map<String, ShowTemplate> templates) {
    try {
      // Find show type (required)
      String showTypeName = dto.getShowType();
      ShowType type = showTypes.get(showTypeName);
      if (type == null) {
        log.warn("Show type not found: {} for show: {} - skipping", showTypeName, dto.getName());
        return false;
      }

      // Smart duplicate handling using external ID
      Show show = null;
      boolean isNewShow = true;

      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        log.debug("Searching for existing show with external ID: {}", dto.getExternalId());
        show = showService.findByExternalId(dto.getExternalId()).orElse(null);
        if (show != null) {
          isNewShow = false;
          log.debug(
              "Found existing show by external ID: {} for show: {}",
              dto.getExternalId(),
              dto.getName());
        } else {
          log.debug("No existing show found with external ID: {}", dto.getExternalId());
        }
      }

      if (show == null) {
        show = new Show();
        log.debug("Creating new show: {} with external ID: {}", dto.getName(), dto.getExternalId());
      }

      // Set basic properties
      show.setName(dto.getName());
      show.setDescription(dto.getDescription());
      show.setType(type);
      show.setExternalId(dto.getExternalId());

      // Set show date if provided
      if (dto.getShowDate() != null && !dto.getShowDate().trim().isEmpty()) {
        try {
          show.setShowDate(LocalDate.parse(dto.getShowDate()));
        } catch (Exception e) {
          log.warn("Invalid date format for show {}: {}", dto.getName(), dto.getShowDate());
        }
      }

      // Set season if provided
      if (dto.getSeasonName() != null && !dto.getSeasonName().trim().isEmpty()) {
        String seasonIdentifier = dto.getSeasonName();
        Season season = seasons.get(seasonIdentifier);

        if (season == null) {
          if (seasonIdentifier.matches("[0-9a-fA-F-]{36}")) {
            log.debug(
                "Season UUID found but not resolved: {} for show: {} - skipping season assignment",
                seasonIdentifier,
                dto.getName());
          } else {
            log.warn("Season not found: {} for show: {}", seasonIdentifier, dto.getName());
          }
        } else {
          show.setSeason(season);
        }
      }

      // Set template if provided
      if (dto.getTemplateName() != null && !dto.getTemplateName().trim().isEmpty()) {
        ShowTemplate template = templates.get(dto.getTemplateName());
        if (template != null) {
          show.setTemplate(template);
        } else {
          log.warn("Template not found: {} for show: {}", dto.getTemplateName(), dto.getName());
        }
      }

      // Save to database
      show.setLastSync(java.time.Instant.now());
      showService.save(show);

      log.debug(
          "{} show: {} (Date: {}, Season: {}, Template: {})",
          isNewShow ? "Saved new" : "Updated existing",
          show.getName(),
          show.getShowDate(),
          show.getSeason() != null ? show.getSeason().getName() : "None",
          show.getTemplate() != null ? show.getTemplate().getName() : "None");

      return true;

    } catch (Exception e) {
      log.error("Failed to save show: {} - {}", dto.getName(), e.getMessage());
      return false;
    }
  }

  // Property extraction methods

  private String extractShowDate(@NonNull final ShowPage showPage) {
    String dateStr =
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractPropertyAsString(showPage.getRawProperties(), "Date");
    if (dateStr != null) {
      if ("date".equals(dateStr) || dateStr.isEmpty()) {
        log.debug("Skipping placeholder date value: {}", dateStr);
        return null;
      }

      try {
        // Notion sends ISO 8601 with timezone (e.g. 2026-01-12T00:00:00.000+00:00)
        LocalDate date = OffsetDateTime.parse(dateStr).toLocalDate();
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (Exception e1) {
        try {
          LocalDate date = LocalDate.parse(dateStr);
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e2) {
          log.warn("Failed to parse show date: {}", dateStr);
          return null;
        }
      }
    }
    return null;
  }

  private String extractSeasonName(@NonNull final ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object season = showPage.getRawProperties().get("Season");
      if (season != null) {
        String seasonStr = season.toString().trim();

        if (seasonStr.matches("\\d+ items?")) {
          log.debug("Season shows as relation count ({}), cannot resolve directly here", seasonStr);
          return null; // Cannot resolve season relationship directly within this method anymore
        }

        if (!seasonStr.isEmpty()
            && !seasonStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[^w-z]{4}-[0-9a-f]{12}")) {
          return seasonStr;
        }
      }
    }
    return null;
  }

  public List<String> getShowIds() {
    return syncServiceDependencies.getNotionHandler().getDatabasePageIds("Shows");
  }

  public SyncResult syncShow(@NonNull final String showId) {
    log.info("Starting show synchronization from Notion for ID: {}", showId);
    String operationId = "show-sync-" + showId;
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Show Sync", 4);

    try {
      Optional<ShowPage> showPage = syncServiceDependencies.getNotionHandler().loadShowById(showId);
      if (showPage.isEmpty()) {
        String errorMessage = "Show with ID " + showId + " not found in Notion.";
        log.error(errorMessage);
        syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
        return SyncResult.failure("Show", errorMessage);
      }

      List<ShowDTO> showDTOs = convertShowPagesToDTO(List.of(showPage.get()), operationId);
      int savedCount = saveShowsToDatabase(showDTOs).saved();
      String message = "Show sync completed successfully. Synced " + savedCount + " show.";
      log.info(message);
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, true, message, savedCount);
      return SyncResult.success("Show", savedCount, 0, 0);
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize show from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Show", errorMessage);
    }
  }
}
