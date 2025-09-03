package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing shows from Notion to the database. */
@Service
@Slf4j
public class ShowSyncService extends BaseSyncService {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private SeasonService seasonService;
  @Autowired private ShowTemplateService showTemplateService;

  public ShowSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
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
  public SyncResult syncShows(@NonNull String operationId) {
    if (EnvironmentVariableUtil.isNotionTokenAvailable()) {
      // Check if already synced in current session
      if (isAlreadySyncedInSession("shows")) {
        log.info("⏭️ Shows already synced in current session, skipping");
        return SyncResult.success("Shows", 0, 0);
      }

      try {
        SyncResult result = performShowsSync(operationId);
        if (result.isSuccess()) {
          markAsSyncedInSession("shows");
        }
        return result;
      } catch (Exception e) {
        log.error("Failed to sync shows", e);
        return SyncResult.failure("Shows", e.getMessage());
      }
    }
    return SyncResult.failure("Shows", "Notion Token was not provided!");
  }

  /** Internal method to perform the actual shows sync logic. */
  private SyncResult performShowsSync(@NonNull String operationId) {
    if (!syncProperties.isEntityEnabled("shows")) {
      log.debug("Shows synchronization is disabled in configuration");
      return SyncResult.success("Shows", 0, 0);
    }

    // Check if NOTION_TOKEN is available before starting sync
    if (!validateNotionToken("Shows")) {
      progressTracker.failOperation(
          operationId, "NOTION_TOKEN environment variable is required for Notion sync");
      healthMonitor.recordFailure("Shows", "NOTION_TOKEN not available");
      return SyncResult.failure(
          "Shows", "NOTION_TOKEN environment variable is required for Notion sync");
    }

    log.info("🚀 Starting shows synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking
    progressTracker.startOperation(operationId, "Sync Shows", 10);
    progressTracker.updateProgress(operationId, 1, "Initializing sync operation...");

    try {
      log.debug("🔧 Executing shows sync with circuit breaker and retry logic");

      // Execute with circuit breaker and retry logic
      SyncResult result =
          circuitBreakerService.execute(
              "shows",
              () -> {
                log.debug("🔄 Circuit breaker executing shows sync");
                return retryService.executeWithRetry(
                    "shows",
                    (attemptNumber) -> {
                      log.debug("🔄 Shows sync attempt {} starting", attemptNumber);
                      progressTracker.updateProgress(operationId, 5, "Starting shows sync...");
                      SyncResult attemptResult = performShowsSyncInternal(operationId, startTime);
                      log.debug(
                          "🔄 Shows sync attempt {} result: {}",
                          attemptNumber,
                          attemptResult != null ? attemptResult.isSuccess() : "NULL");
                      return attemptResult;
                    });
              });

      return result != null
          ? result
          : SyncResult.failure("Shows", "Sync operation returned null result");

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize shows from Notion after {}ms", totalTime, e);

      // Fail progress tracking
      progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      healthMonitor.recordFailure("Shows", e.getMessage());

      return SyncResult.failure("Shows", e.getMessage());
    }
  }

  /** Performs the actual shows sync operation with enhanced error handling. */
  @SneakyThrows
  private SyncResult performShowsSyncInternal(@NonNull String operationId, long startTime) {
    return syncTransactionManager.executeInTransaction(
        operationId,
        "shows",
        (transaction) -> {
          try {
            // Step 1: Validate sync prerequisites
            log.info("🔍 Validating sync prerequisites...");
            progressTracker.updateProgress(operationId, 1, "Validating sync prerequisites...");

            // Step 2: Get all shows from Notion
            log.info("📥 Retrieving shows from Notion...");
            progressTracker.updateProgress(
                operationId, 3, "Retrieving shows from Notion database...");

            List<ShowPage> notionShows = executeWithRateLimit(notionHandler::loadAllShowsForSync);
            long retrieveTime = System.currentTimeMillis() - startTime;
            log.info("✅ Retrieved {} shows from Notion in {}ms", notionShows.size(), retrieveTime);

            // Step 3: Convert to DTOs
            log.info("🔄 Converting shows to DTOs...");
            progressTracker.updateProgress(operationId, 5, "Converting shows to DTOs...");

            List<ShowDTO> showDTOs = convertShowPagesToDTO(notionShows, operationId);
            long convertTime = System.currentTimeMillis() - startTime - retrieveTime;
            log.info("✅ Converted {} shows to DTOs in {}ms", showDTOs.size(), convertTime);

            // Step 4: Save to database
            log.info("💾 Saving shows to database...");
            progressTracker.updateProgress(operationId, 6, "Saving shows to database...");

            int savedCount = saveShowsToDatabase(showDTOs);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info(
                "🎉 Successfully synchronized {} shows to database in {}ms total",
                savedCount,
                totalTime);

            progressTracker.completeOperation(
                operationId,
                true,
                String.format("Successfully synced %d shows", savedCount),
                savedCount);

            // Record success in health monitor
            healthMonitor.recordSuccess("Shows", totalTime, savedCount);
            return SyncResult.success("Shows", savedCount, 0);

          } catch (Exception e) {
            throw new RuntimeException("Shows sync operation failed: " + e.getMessage(), e);
          }
        });
  }

  private List<ShowDTO> convertShowPagesToDTO(
      @NonNull List<ShowPage> showPages, String operationId) {
    return processWithControlledParallelism(
        showPages,
        this::convertShowPageToDTO,
        10, // Batch size
        operationId,
        5, // Progress step
        "Converted %d/%d shows");
  }

  /** Converts a single ShowPage to ShowDTO. */
  private ShowDTO convertShowPageToDTO(@NonNull ShowPage showPage) {
    try {
      ShowDTO dto = new ShowDTO();
      String showName = extractName(showPage);
      dto.setName(showName);
      dto.setDescription(extractDescription(showPage));
      dto.setShowType(extractShowType(showPage));
      dto.setShowDate(extractShowDate(showPage));
      dto.setSeasonName(extractSeasonName(showPage));
      dto.setTemplateName(extractTemplateName(showPage));
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

  /** Saves the list of ShowDTO objects to the database. */
  private int saveShowsToDatabase(@NonNull List<ShowDTO> showDTOs) {
    return saveShowsToDatabaseWithBatching(showDTOs, 50);
  }

  /** Enhanced method to save shows to database with batch processing. */
  private int saveShowsToDatabaseWithBatching(@NonNull List<ShowDTO> showDTOs, int batchSize) {
    log.info(
        "Starting database persistence for {} shows with batch size {}",
        showDTOs.size(),
        batchSize);

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
            if (processSingleShow(dto, showTypes, seasons, templates)) {
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
    return savedCount;
  }

  /** Process a single show with error handling. */
  private boolean processSingleShow(
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
  private String extractName(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object name = showPage.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown Show";
    }
    return "Unknown Show";
  }

  private String extractDescription(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object description = showPage.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  private String extractShowType(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object showType = showPage.getRawProperties().get("Show Type");
      return showType != null ? showType.toString() : null;
    }
    return null;
  }

  private String extractShowDate(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object showDate = showPage.getRawProperties().get("Date");
      if (showDate != null) {
        String dateStr = showDate.toString().trim();

        if ("date".equals(dateStr) || dateStr.isEmpty()) {
          log.debug("Skipping placeholder date value: {}", dateStr);
          return null;
        }

        try {
          LocalDate date = LocalDate.parse(dateStr);
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse show date: {}", dateStr);
          return null;
        }
      }
    }
    return null;
  }

  private String extractSeasonName(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object season = showPage.getRawProperties().get("Season");
      if (season != null) {
        String seasonStr = season.toString().trim();

        if (seasonStr.matches("\\d+ items?")) {
          log.debug("Season shows as relation count ({}), attempting to resolve", seasonStr);
          return resolveSeasonRelationship(showPage);
        }

        if (!seasonStr.isEmpty()
            && !seasonStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[^w-z]{4}-[0-9a-f]{12}")) {
          return seasonStr;
        }
      }
    }
    return null;
  }

  private String extractTemplateName(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object template = showPage.getRawProperties().get("Template");
      return template != null ? template.toString() : null;
    }
    return null;
  }

  /** Resolves season relationship by looking up the season name from the database. */
  private String resolveSeasonRelationship(@NonNull ShowPage showPage) {
    try {
      var seasonsPage = seasonService.getAllSeasons(Pageable.unpaged());
      if (seasonsPage != null && !seasonsPage.isEmpty()) {
        Season firstSeason = seasonsPage.iterator().next();
        log.debug("Resolved season relationship to: {}", firstSeason.getName());
        return firstSeason.getName();
      }
    } catch (Exception e) {
      log.debug("Failed to resolve season relationship: {}", e.getMessage());
    }

    return null;
  }
}
