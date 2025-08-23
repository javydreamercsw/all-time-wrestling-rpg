package com.github.javydreamercsw.management.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.DataInitializer.ShowDTO;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.util.NotionBlocksRetriever;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for synchronizing data between Notion databases and local JSON files. This
 * service handles the extraction of data from Notion and updates the corresponding JSON files used
 * by the application for data initialization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
public class NotionSyncService {

  private final ObjectMapper objectMapper;
  private final NotionHandler notionHandler;
  private final NotionSyncProperties syncProperties;
  private final SyncProgressTracker progressTracker;

  // Database services for persisting synced data
  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;

  // Thread pool for async processing - using fixed thread pool for Java 17 compatibility
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  /**
   * Synchronizes shows from Notion Shows database directly to the database. This method retrieves
   * all shows from Notion and saves them to the database only.
   *
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows() {
    return syncShows(null);
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database with optional progress
   * tracking.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows(String operationId) {
    if (!syncProperties.isEntityEnabled("shows")) {
      log.debug("Shows synchronization is disabled in configuration");
      return SyncResult.success("Shows", 0, 0);
    }

    log.info("🚀 Starting shows synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking if operation ID provided
    if (operationId != null) {
      progressTracker.startOperation(
          operationId, "Sync Shows", 4); // Updated to 4 steps (no JSON file writing)
      progressTracker.updateProgress(operationId, 1, "Initializing sync operation...");
    }

    try {
      // Create backup if enabled
      if (syncProperties.isBackupEnabled()) {
        log.info("📦 Creating backup...");
        if (operationId != null) {
          progressTracker.updateProgress(operationId, 1, "Creating backup of existing data...");
        }
        createBackup("shows.json");
      }

      // Get all shows from Notion using optimized method
      log.info("📥 Retrieving shows from Notion...");
      if (operationId != null) {
        progressTracker.updateProgress(operationId, 2, "Retrieving shows from Notion database...");
      }
      List<ShowPage> notionShows = getAllShowsFromNotion();
      log.info(
          "✅ Retrieved {} shows from Notion in {}ms",
          notionShows.size(),
          System.currentTimeMillis() - startTime);

      // Convert to DTOs using parallel processing
      log.info("🔄 Converting shows to DTOs...");
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Converting %d shows to data format...", notionShows.size()));
      }
      long conversionStart = System.currentTimeMillis();
      List<ShowDTO> showDTOs = convertShowPagesToDTO(notionShows);
      log.info(
          "✅ Converted {} shows to DTOs in {}ms",
          showDTOs.size(),
          System.currentTimeMillis() - conversionStart);

      // Save to database only (no JSON file writing)
      log.info("🗄️ Saving shows to database...");
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId, 4, String.format("Saving %d shows to database...", showDTOs.size()));
      }
      long dbStart = System.currentTimeMillis();
      int savedCount = saveShowsToDatabase(showDTOs);
      log.info(
          "✅ Saved {} shows to database in {}ms", savedCount, System.currentTimeMillis() - dbStart);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} shows to database in {}ms total",
          showDTOs.size(),
          totalTime);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d shows in %dms", showDTOs.size(), totalTime),
            showDTOs.size());
      }

      return SyncResult.success("Shows", showDTOs.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize shows from Notion after {}ms", totalTime, e);

      // Fail progress tracking
      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      return SyncResult.failure("Shows", e.getMessage());
    }
  }

  // ==================== SHOW TEMPLATES SYNC ====================

  /**
   * Synchronizes show templates from Notion to the local JSON file and database. This method should
   * be called before syncing shows to ensure templates are available.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTemplates(String operationId) {
    log.info("🎭 Starting show templates synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("templates")) {
        log.info("Show templates sync is disabled in configuration");
        return SyncResult.success("Show Templates", 0, 0);
      }

      // Initialize progress tracking
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Show Templates", 3);
        progressTracker.updateProgress(operationId, 1, "Retrieving show templates from Notion...");
      }

      // Retrieve show templates from Notion
      log.info("📥 Retrieving show templates from Notion...");
      long retrieveStart = System.currentTimeMillis();
      List<ShowTemplatePage> templatePages = notionHandler.loadAllShowTemplates();
      log.info(
          "✅ Retrieved {} show templates in {}ms",
          templatePages.size(),
          System.currentTimeMillis() - retrieveStart);

      // Convert to DTOs
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            2,
            String.format("Converting %d show templates to DTOs...", templatePages.size()));
      }
      log.info("🔄 Converting show templates to DTOs...");
      long convertStart = System.currentTimeMillis();
      List<ShowTemplateDTO> templateDTOs = convertShowTemplatePagesToDTOs(templatePages);
      log.info(
          "✅ Converted {} show templates in {}ms",
          templateDTOs.size(),
          System.currentTimeMillis() - convertStart);

      // Write to JSON file
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Writing %d show templates to JSON file...", templateDTOs.size()));
      }
      log.info("💾 Writing show templates to JSON file...");
      long writeStart = System.currentTimeMillis();
      writeShowTemplatesToJsonFile(templateDTOs);
      log.info(
          "✅ Written {} show templates to file in {}ms",
          templateDTOs.size(),
          System.currentTimeMillis() - writeStart);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} show templates in {}ms total",
          templateDTOs.size(),
          totalTime);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d show templates", templateDTOs.size()),
            templateDTOs.size());
      }

      return SyncResult.success("Show Templates", templateDTOs.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize show templates from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  // ==================== WRESTLERS SYNC ====================

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(String operationId) {
    log.info("🤼 Starting wrestlers synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("wrestlers")) {
        log.info("Wrestlers sync is disabled in configuration");
        return SyncResult.success("Wrestlers", 0, 0);
      }

      // Initialize progress tracking
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Wrestlers", 4);
        progressTracker.updateProgress(operationId, 1, "Retrieving wrestlers from Notion...");
      }

      // Retrieve wrestlers from Notion (sync mode for faster processing)
      log.info("📥 Retrieving wrestlers from Notion...");
      long retrieveStart = System.currentTimeMillis();
      List<WrestlerPage> wrestlerPages = notionHandler.loadAllWrestlers();
      log.info(
          "✅ Retrieved {} wrestlers in {}ms",
          wrestlerPages.size(),
          System.currentTimeMillis() - retrieveStart);

      // Update progress with retrieval results
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            1,
            String.format(
                "✅ Retrieved %d wrestlers from Notion in %dms",
                wrestlerPages.size(), System.currentTimeMillis() - retrieveStart));
      }

      // Convert to DTOs and merge with existing data
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            2,
            String.format(
                "Converting %d wrestlers to DTOs and merging with existing data...",
                wrestlerPages.size()));
      }
      log.info("🔄 Converting wrestlers to DTOs and merging with existing data...");
      long convertStart = System.currentTimeMillis();
      List<WrestlerDTO> wrestlerDTOs = convertAndMergeWrestlerData(wrestlerPages);
      log.info(
          "✅ Converted and merged {} wrestlers in {}ms",
          wrestlerDTOs.size(),
          System.currentTimeMillis() - convertStart);

      // Update progress with conversion results
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            2,
            String.format(
                "✅ Converted and merged %d wrestlers in %dms",
                wrestlerDTOs.size(), System.currentTimeMillis() - convertStart));
      }

      // Write to JSON file
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Writing %d wrestlers to JSON file...", wrestlerDTOs.size()));
      }
      log.info("💾 Writing wrestlers to JSON file...");
      long writeStart = System.currentTimeMillis();
      writeWrestlersToJsonFile(wrestlerDTOs);
      log.info(
          "✅ Written {} wrestlers to file in {}ms",
          wrestlerDTOs.size(),
          System.currentTimeMillis() - writeStart);

      // Update progress with file write results
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format(
                "✅ Written %d wrestlers to JSON file in %dms",
                wrestlerDTOs.size(), System.currentTimeMillis() - writeStart));
      }

      // Save wrestlers to database
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            4,
            String.format("Saving %d wrestlers to database...", wrestlerDTOs.size()));
      }
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
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d wrestlers", wrestlerDTOs.size()),
            wrestlerDTOs.size());
      }

      return SyncResult.success("Wrestlers", wrestlerDTOs.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize wrestlers from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      return SyncResult.failure("Wrestlers", e.getMessage());
    }
  }

  /**
   * Cleanup method to shutdown the executor service. Called automatically when the service is being
   * destroyed.
   */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down NotionSyncService executor...");
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for executor termination");
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Retrieves all shows from the Notion Shows database. This method uses the NotionHandler to query
   * all shows.
   *
   * @return List of ShowPage objects from Notion
   */
  private List<ShowPage> getAllShowsFromNotion() {
    log.debug("Retrieving all shows from Notion Shows database");

    // Check if NOTION_TOKEN is available
    String notionToken = System.getenv("NOTION_TOKEN");
    if (notionToken == null || notionToken.trim().isEmpty()) {
      log.warn("NOTION_TOKEN environment variable is not set. Cannot sync from Notion.");
      throw new IllegalStateException(
          "NOTION_TOKEN environment variable is required for Notion sync");
    }

    return notionHandler.loadAllShowsForSync();
  }

  /**
   * Converts ShowPage objects from Notion to ShowDTO objects for JSON serialization. Uses parallel
   * processing for better performance with large datasets.
   *
   * @param showPages List of ShowPage objects from Notion
   * @return List of ShowDTO objects
   */
  private List<ShowDTO> convertShowPagesToDTO(List<ShowPage> showPages) {
    log.info("Converting {} shows to DTOs using parallel processing", showPages.size());

    // Use parallel stream for faster processing of large datasets
    List<ShowDTO> showDTOs =
        showPages.parallelStream().map(this::convertShowPageToDTO).collect(Collectors.toList());

    log.info("Successfully converted {} shows to DTOs", showDTOs.size());
    return showDTOs;
  }

  /**
   * Converts a single ShowPage to ShowDTO.
   *
   * @param showPage The ShowPage to convert
   * @return ShowDTO object
   */
  private ShowDTO convertShowPageToDTO(ShowPage showPage) {
    try {
      ShowDTO dto = new ShowDTO();
      String showName = extractName(showPage);
      dto.setName(showName);
      dto.setDescription(extractDescription(showPage));
      dto.setShowType(extractShowType(showPage));
      dto.setShowDate(extractShowDate(showPage));
      dto.setSeasonName(extractSeasonName(showPage));
      dto.setTemplateName(extractTemplateName(showPage));
      dto.setExternalId(showPage.getId()); // Use Notion page ID as external ID

      log.debug("Converted show: {} -> DTO", showName);
      return dto;

    } catch (Exception e) {
      log.warn("Failed to convert show page to DTO: {}", e.getMessage());
      // Return a minimal DTO with error information
      ShowDTO errorDto = new ShowDTO();
      errorDto.setName("ERROR: " + showPage.getId());
      errorDto.setDescription("Failed to convert: " + e.getMessage());
      return errorDto;
    }
  }

  // ==================== DATABASE PERSISTENCE METHODS ====================

  /**
   * Saves the list of ShowDTO objects to the database. Uses the same logic as DataInitializer to
   * handle duplicates intelligently.
   *
   * @param showDTOs List of ShowDTO objects to save
   * @return Number of shows successfully saved/updated
   */
  private int saveShowsToDatabase(List<ShowDTO> showDTOs) {
    log.info("Starting database persistence for {} shows", showDTOs.size());

    // Cache lookups for performance (same as DataInitializer) with null safety
    Map<String, ShowType> showTypes = new HashMap<>();
    Map<String, Season> seasons = new HashMap<>();
    Map<String, ShowTemplate> templates = new HashMap<>();

    try {
      List<ShowType> showTypeList = showTypeService.findAll();
      if (showTypeList != null) {
        showTypes = showTypeList.stream().collect(Collectors.toMap(ShowType::getName, s -> s));
      }
    } catch (Exception e) {
      log.warn("Failed to load show types: {}", e.getMessage());
    }

    try {
      var seasonsPage = seasonService.getAllSeasons(Pageable.unpaged());
      if (seasonsPage != null) {
        seasons = seasonsPage.stream().collect(Collectors.toMap(Season::getName, s -> s));
      }
    } catch (Exception e) {
      log.warn("Failed to load seasons: {}", e.getMessage());
    }

    try {
      List<ShowTemplate> templateList = showTemplateService.findAll();
      if (templateList != null) {
        templates = templateList.stream().collect(Collectors.toMap(ShowTemplate::getName, t -> t));
      }
    } catch (Exception e) {
      log.warn("Failed to load show templates: {}", e.getMessage());
    }

    int savedCount = 0;
    int skippedCount = 0;

    for (ShowDTO dto : showDTOs) {
      try {
        // Find show type (required) - handle "N/A" case with smart mapping
        String showTypeName = dto.getShowType();
        if ("N/A".equals(showTypeName) || showTypeName == null || showTypeName.trim().isEmpty()) {
          // Smart mapping based on show name patterns
          String showName = dto.getName().toLowerCase();
          if (showName.contains("continuum") || showName.contains("timeless")) {
            showTypeName = "Weekly"; // These are weekly shows
            log.debug("Mapped show '{}' to Weekly show type", dto.getName());
          } else {
            // For other shows, try to find a default show type
            if (showTypes.containsKey("PLE")) {
              showTypeName = "PLE"; // Assume premium live events for others
              log.debug("Mapped show '{}' to PLE show type", dto.getName());
            } else if (!showTypes.isEmpty()) {
              showTypeName = showTypes.keySet().iterator().next();
              log.debug("Using default show type '{}' for show: {}", showTypeName, dto.getName());
            }
          }
        }

        ShowType type = showTypes.get(showTypeName);
        if (type == null) {
          log.warn(
              "Show type not found: {} for show: {} - skipping (available types: {})",
              showTypeName,
              dto.getName(),
              showTypes.keySet());
          skippedCount++;
          continue;
        }

        // Smart duplicate handling using external ID (allows multiple shows with same name)
        Show show = null;
        boolean isNewShow = true;

        // First, try to find by external ID (most reliable for sync operations)
        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          show = showService.findByExternalId(dto.getExternalId()).orElse(null);
          if (show != null) {
            isNewShow = false;
            log.debug(
                "Found existing show by external ID: {} for show: {}",
                dto.getExternalId(),
                dto.getName());
          }
        }

        // If not found by external ID, create new show
        if (show == null) {
          show = new Show();
          isNewShow = true;
          log.debug(
              "Creating new show: {} with external ID: {}", dto.getName(), dto.getExternalId());
        }

        // Set basic properties
        show.setName(dto.getName());
        show.setDescription(dto.getDescription());
        show.setType(type);
        show.setExternalId(dto.getExternalId()); // Set external ID for future sync operations

        // Set show date if provided
        if (dto.getShowDate() != null && !dto.getShowDate().trim().isEmpty()) {
          try {
            show.setShowDate(LocalDate.parse(dto.getShowDate()));
          } catch (Exception e) {
            log.warn("Invalid date format for show {}: {}", dto.getName(), dto.getShowDate());
          }
        }

        // Set season if provided - handle UUID case
        if (dto.getSeasonName() != null && !dto.getSeasonName().trim().isEmpty()) {
          String seasonIdentifier = dto.getSeasonName();
          Season season = seasons.get(seasonIdentifier);

          // If not found by name and looks like UUID, skip with debug message
          if (season == null) {
            if (seasonIdentifier.matches("[0-9a-fA-F-]{36}")) {
              log.debug(
                  "Season UUID found but not resolved: {} for show: {} - skipping season"
                      + " assignment",
                  seasonIdentifier,
                  dto.getName());
            } else {
              log.warn(
                  "Season not found: {} for show: {} (available: {})",
                  seasonIdentifier,
                  dto.getName(),
                  seasons.keySet());
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
        savedCount++;

        log.debug(
            "{} show: {} (Date: {}, Season: {}, Template: {})",
            isNewShow ? "Saved new" : "Updated existing",
            show.getName(),
            show.getShowDate(),
            show.getSeason() != null ? show.getSeason().getName() : "None",
            show.getTemplate() != null ? show.getTemplate().getName() : "None");

      } catch (Exception e) {
        log.error("Failed to save show: {} - {}", dto.getName(), e.getMessage());
        skippedCount++;
      }
    }

    log.info(
        "Database persistence completed: {} saved/updated, {} skipped", savedCount, skippedCount);
    return savedCount;
  }

  /**
   * Saves the list of WrestlerDTO objects to the database. Uses smart matching logic: 1. Try to
   * find by external ID (Notion page ID) first 2. Fallback to name matching if external ID not
   * present 3. Create new wrestler if no match found
   *
   * @param wrestlerDTOs List of WrestlerDTO objects to save
   * @param operationId Operation ID for progress tracking (can be null)
   * @return Number of wrestlers successfully saved/updated
   */
  private int saveWrestlersToDatabase(List<WrestlerDTO> wrestlerDTOs, String operationId) {
    log.info("Starting database persistence for {} wrestlers", wrestlerDTOs.size());

    int savedCount = 0;
    int skippedCount = 0;
    int processedCount = 0;

    for (WrestlerDTO dto : wrestlerDTOs) {
      processedCount++;

      // Update progress every 5 wrestlers
      if (operationId != null && processedCount % 5 == 0) {
        progressTracker.updateProgress(
            operationId,
            4,
            String.format(
                "Saving wrestlers to database... (%d/%d processed)",
                processedCount, wrestlerDTOs.size()));
      }

      try {
        // Smart duplicate handling - prefer external ID, fallback to name
        com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler = null;
        boolean isNewWrestler = false;

        // 1. Try to find by external ID first (most reliable)
        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          wrestler = wrestlerService.findByExternalId(dto.getExternalId()).orElse(null);
          if (wrestler != null) {
            log.debug(
                "Found existing wrestler by external ID: {} for wrestler: {}",
                dto.getExternalId(),
                dto.getName());
          }
        }

        // 2. Fallback to name matching if external ID didn't work
        if (wrestler == null && dto.getName() != null && !dto.getName().trim().isEmpty()) {
          wrestler = wrestlerService.findByName(dto.getName()).orElse(null);
          if (wrestler != null) {
            log.debug("Found existing wrestler by name: {}", dto.getName());
          }
        }

        // 3. Create new wrestler if no match found
        if (wrestler == null) {
          wrestler = new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
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
        wrestler.setExternalId(dto.getExternalId()); // Set external ID for future sync operations

        // Update description if provided
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
          wrestler.setDescription(dto.getDescription());
          log.debug(
              "Updated description for wrestler {}: {}",
              dto.getName(),
              dto.getDescription().substring(0, Math.min(50, dto.getDescription().length()))
                  + "...");
        }

        // Note: Faction is a Faction entity, not a String, so we skip database faction updates
        // The faction will be preserved in the JSON file and handled by DataInitializer
        log.debug("Processing wrestler: {} (isNew: {})", dto.getName(), isNewWrestler);

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
          if (wrestler.getWrestlingStyle() == null) {
            wrestler.setWrestlingStyle(
                dto.getWrestlingStyle() != null ? dto.getWrestlingStyle() : "TODO");
          }
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
          if (dto.getWrestlingStyle() != null && !dto.getWrestlingStyle().trim().isEmpty()) {
            wrestler.setWrestlingStyle(dto.getWrestlingStyle());
          }
        }

        // Save the wrestler (use service for new, repository for existing to avoid creation date
        // override)
        log.info(
            "💾 Saving wrestler to database: {} (ID: {}, isNew: {})",
            wrestler.getName(),
            wrestler.getId(),
            isNewWrestler);
        com.github.javydreamercsw.management.domain.wrestler.Wrestler savedWrestler;
        if (isNewWrestler) {
          // Use service for new wrestlers (sets creation date)
          log.info("🆕 Using WrestlerService.save() for new wrestler: {}", wrestler.getName());
          savedWrestler = wrestlerService.save(wrestler);
        } else {
          // Use repository directly for existing wrestlers (preserves creation date)
          log.info(
              "🔄 Using WrestlerRepository.saveAndFlush() for existing wrestler: {}",
              wrestler.getName());
          savedWrestler = wrestlerRepository.saveAndFlush(wrestler);
        }
        savedCount++;
        log.info(
            "✅ Wrestler saved successfully: {} (Final ID: {})",
            savedWrestler.getName(),
            savedWrestler.getId());

        if (isNewWrestler) {
          log.info(
              "✅ Created new wrestler in database: {} (ID: {})",
              savedWrestler.getName(),
              savedWrestler.getId());
        } else {
          log.info(
              "✅ Updated existing wrestler in database: {} (ID: {})",
              savedWrestler.getName(),
              savedWrestler.getId());
        }

      } catch (Exception e) {
        log.error("❌ Failed to save wrestler: {} - {}", dto.getName(), e.getMessage());
        skippedCount++;
      }
    }

    log.info(
        "Database persistence completed: {} saved/updated, {} skipped", savedCount, skippedCount);

    // Final progress update
    if (operationId != null) {
      progressTracker.updateProgress(
          operationId,
          4,
          String.format(
              "✅ Completed database save: %d wrestlers saved/updated, %d skipped",
              savedCount, skippedCount));
    }

    return savedCount;
  }

  // ==================== BACKUP METHODS ====================

  /**
   * Creates a backup of the specified JSON file before sync operation.
   *
   * @param fileName The name of the JSON file to backup
   * @throws IOException if backup creation fails
   */
  private void createBackup(String fileName) throws IOException {
    Path originalFile = Paths.get("src/main/resources/" + fileName);

    if (!Files.exists(originalFile)) {
      log.debug("Original file {} does not exist, skipping backup", fileName);
      return;
    }

    // Create backup directory
    Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
    Files.createDirectories(backupDir);

    // Create backup file with timestamp
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String backupFileName = fileName.replace(".json", "_" + timestamp + ".json");
    Path backupFile = backupDir.resolve(backupFileName);

    // Copy original file to backup location
    Files.copy(originalFile, backupFile);
    log.info("Created backup: {}", backupFile);

    // Clean up old backups
    cleanupOldBackups(fileName);
  }

  /**
   * Removes old backup files, keeping only the configured maximum number.
   *
   * @param fileName The base file name to clean up backups for
   */
  private void cleanupOldBackups(String fileName) {
    try {
      Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
      if (!Files.exists(backupDir)) {
        return;
      }

      String baseFileName = fileName.replace(".json", "");
      List<Path> backupFiles =
          Files.list(backupDir)
              .filter(path -> path.getFileName().toString().startsWith(baseFileName + "_"))
              .sorted(
                  (p1, p2) ->
                      p2.getFileName()
                          .toString()
                          .compareTo(p1.getFileName().toString())) // Newest first
              .toList();

      int maxFiles = syncProperties.getBackup().getMaxFiles();
      if (backupFiles.size() > maxFiles) {
        List<Path> filesToDelete = backupFiles.subList(maxFiles, backupFiles.size());
        for (Path fileToDelete : filesToDelete) {
          Files.delete(fileToDelete);
          log.debug("Deleted old backup: {}", fileToDelete);
        }
        log.info("Cleaned up {} old backup files for {}", filesToDelete.size(), fileName);
      }

    } catch (IOException e) {
      log.warn("Failed to cleanup old backups for {}: {}", fileName, e.getMessage());
    }
  }

  // ==================== PROPERTY EXTRACTION METHODS ====================

  private String extractName(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object name = showPage.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown Show";
    }
    return "Unknown Show";
  }

  private String extractDescription(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object description = showPage.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  private String extractShowType(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object showType = showPage.getRawProperties().get("Show Type");
      return showType != null ? showType.toString() : null;
    }
    return null;
  }

  private String extractShowDate(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object showDate = showPage.getRawProperties().get("Date");
      if (showDate != null) {
        // Try to parse and format the date
        try {
          LocalDate date = LocalDate.parse(showDate.toString());
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse show date: {}", showDate);
          return showDate.toString();
        }
      }
    }
    return null;
  }

  private String extractSeasonName(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object season = showPage.getRawProperties().get("Season");
      return season != null ? season.toString() : null;
    }
    return null;
  }

  private String extractTemplateName(ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object template = showPage.getRawProperties().get("Template");
      return template != null ? template.toString() : null;
    }
    return null;
  }

  // ==================== GENERIC EXTRACTION METHODS ====================

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(NotionPage page) {
    if (page.getRawProperties() != null) {
      Object name = page.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown";
    }
    return "Unknown";
  }

  /** Extracts description from any NotionPage type using raw properties. */
  private String extractDescriptionFromNotionPage(NotionPage page) {
    if (page.getRawProperties() != null) {
      Object description = page.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  /** Extracts show type from any NotionPage type using raw properties. */
  private String extractShowTypeFromNotionPage(NotionPage page) {
    if (page.getRawProperties() != null) {
      Object showType = page.getRawProperties().get("Show Type");
      if (showType == null) {
        showType = page.getRawProperties().get("ShowType");
      }
      return showType != null ? showType.toString() : "N/A";
    }
    return "N/A";
  }

  /**
   * Extracts description from the page body/content using the existing NotionBlocksRetriever. This
   * is used for wrestlers where the description is in the page content.
   */
  private String extractDescriptionFromPageBody(NotionPage page) {
    if (page == null || page.getId() == null) {
      return "";
    }

    try {
      NotionBlocksRetriever blocksRetriever =
          new NotionBlocksRetriever(System.getenv("NOTION_TOKEN"));
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

  /**
   * Extracts faction from any NotionPage type using raw properties. Faction is a relationship
   * property that links to a faction/team page. For sync mode, we extract the faction name if
   * available, otherwise preserve existing.
   */
  private String extractFactionFromNotionPage(NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for faction
      Object faction = page.getRawProperties().get("Faction");
      if (faction == null) {
        faction = page.getRawProperties().get("Team");
      }
      if (faction == null) {
        faction = page.getRawProperties().get("faction");
      }

      if (faction != null && !faction.toString().trim().isEmpty()) {
        String factionStr = faction.toString().trim();

        // If it shows as "X relations", it means we have relationship IDs but they weren't resolved
        // For now, we'll return null to preserve existing faction data
        if (factionStr.matches("\\d+ relations?")) {
          log.debug(
              "Faction shows as relationship count ({}), preserving existing faction", factionStr);
          return null; // Let merge logic preserve existing faction
        }

        // If it looks like a relationship ID (UUID format), don't use it
        if (factionStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          log.debug(
              "Faction appears to be a relationship ID, preserving existing faction: {}",
              factionStr);
          return null; // Let merge logic preserve existing faction
        }

        // If it's a readable name, use it
        log.debug("Found faction name: {}", factionStr);
        return factionStr;
      }
    }
    return null; // Let merge logic preserve existing faction
  }

  /**
   * Extracts faction from a WrestlerPage by resolving the relationship to get the actual faction
   * name. This method makes API calls to resolve faction relationships properly.
   */
  private String extractFactionFromWrestlerPage(WrestlerPage wrestlerPage) {
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
        log.debug("Faction shows as relationship count ({}), attempting to resolve", factionStr);
        return resolveFactionRelationship(wrestlerPage);
      }

      // If it's already a readable name, use it
      if (!factionStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        log.debug("Found faction name: {}", factionStr);
        return factionStr;
      }
    }

    return null; // Let merge logic preserve existing faction
  }

  /**
   * Resolves faction relationship by making API calls to get the actual faction name. This is a
   * simplified version of the relationship resolution logic from NotionHandler.
   */
  private String resolveFactionRelationship(WrestlerPage wrestlerPage) {
    // For now, return null to preserve existing faction data
    // Full relationship resolution would require accessing the original Page object
    // and making additional API calls, which is complex for sync mode
    log.debug(
        "Faction relationship resolution not implemented in sync mode, preserving existing"
            + " faction");
    return null;
  }

  // ==================== DTO CLASSES ====================

  /** DTO for Show Template data from Notion. */
  @Data
  public static class ShowTemplateDTO {
    private String name;
    private String description;
    private String showType;
    private String externalId; // Notion page ID
  }

  /** DTO for Wrestler data from Notion. */
  @Data
  public static class WrestlerDTO {
    private String name;
    private String description;
    private String height;
    private String weight;
    private String hometown;
    private String externalId; // Notion page ID

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
    private String wrestlingStyle;
    private String creationDate;
  }

  // ==================== CONVERSION METHODS ====================

  /** Converts ShowTemplatePage objects to ShowTemplateDTO objects. */
  private List<ShowTemplateDTO> convertShowTemplatePagesToDTOs(
      List<ShowTemplatePage> templatePages) {
    return templatePages.parallelStream()
        .map(this::convertShowTemplatePageToDTO)
        .collect(Collectors.toList());
  }

  /** Converts a single ShowTemplatePage to ShowTemplateDTO. */
  private ShowTemplateDTO convertShowTemplatePageToDTO(ShowTemplatePage templatePage) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(extractNameFromNotionPage(templatePage));
    dto.setDescription(extractDescriptionFromNotionPage(templatePage));
    dto.setShowType(extractShowTypeFromNotionPage(templatePage));
    dto.setExternalId(templatePage.getId());
    return dto;
  }

  /**
   * Converts WrestlerPage objects to WrestlerDTO objects and merges with existing JSON data. This
   * preserves game-specific fields while updating Notion-sourced data.
   */
  private List<WrestlerDTO> convertAndMergeWrestlerData(List<WrestlerPage> wrestlerPages) {
    // Load existing wrestlers from JSON file
    Map<String, WrestlerDTO> existingWrestlers = loadExistingWrestlersFromJson();

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

  /** Converts WrestlerPage objects to WrestlerDTO objects. */
  private List<WrestlerDTO> convertWrestlerPagesToDTOs(List<WrestlerPage> wrestlerPages) {
    return wrestlerPages.parallelStream()
        .map(this::convertWrestlerPageToDTO)
        .collect(Collectors.toList());
  }

  /** Converts a single WrestlerPage to WrestlerDTO. */
  private WrestlerDTO convertWrestlerPageToDTO(WrestlerPage wrestlerPage) {
    WrestlerDTO dto = new WrestlerDTO();
    dto.setName(extractNameFromNotionPage(wrestlerPage));
    dto.setDescription(extractDescriptionFromPageBody(wrestlerPage));
    dto.setFaction(extractFactionFromWrestlerPage(wrestlerPage));
    dto.setExternalId(wrestlerPage.getId());
    return dto;
  }

  // ==================== WRESTLER MERGE METHODS ====================

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
  private WrestlerDTO mergeWrestlerData(WrestlerDTO existing, WrestlerDTO notion) {
    WrestlerDTO merged = new WrestlerDTO();

    log.debug("Merging wrestler data for: {}", notion.getName());
    if (existing != null) {
      log.debug("  Existing faction: {}", existing.getFaction());
      log.debug("  Existing description: {}", existing.getDescription());
      log.debug("  Existing wrestlingStyle: {}", existing.getWrestlingStyle());
    } else {
      log.debug("  No existing data found for wrestler: {}", notion.getName());
    }

    // Always use Notion data for these fields (they're the source of truth)
    merged.setName(notion.getName());
    merged.setExternalId(notion.getExternalId());
    merged.setHeight(notion.getHeight());
    merged.setWeight(notion.getWeight());
    merged.setHometown(notion.getHometown());

    // Smart description handling: prefer Notion if available, otherwise preserve existing
    if (notion.getDescription() != null && !notion.getDescription().trim().isEmpty()) {
      merged.setDescription(notion.getDescription());
    } else if (existing != null && existing.getDescription() != null) {
      merged.setDescription(existing.getDescription());
    } else {
      merged.setDescription("TODO");
    }

    // Smart faction handling: prefer Notion if available, otherwise preserve existing
    if (notion.getFaction() != null && !notion.getFaction().trim().isEmpty()) {
      merged.setFaction(notion.getFaction());
    } else if (existing != null && existing.getFaction() != null) {
      merged.setFaction(existing.getFaction());
    } else {
      merged.setFaction(null);
    }

    // Preserve existing game data if available, otherwise use defaults
    if (existing != null) {
      merged.setDeckSize(existing.getDeckSize());
      merged.setStartingHealth(existing.getStartingHealth());
      merged.setLowHealth(existing.getLowHealth());
      merged.setStartingStamina(existing.getStartingStamina());
      merged.setLowStamina(existing.getLowStamina());
      merged.setFans(existing.getFans());
      merged.setIsPlayer(existing.getIsPlayer());
      merged.setBumps(existing.getBumps());
      merged.setWrestlingStyle(existing.getWrestlingStyle());
      merged.setCreationDate(existing.getCreationDate());
    } else {
      // Set defaults for new wrestlers
      merged.setDeckSize(15);
      merged.setStartingHealth(0);
      merged.setLowHealth(0);
      merged.setStartingStamina(0);
      merged.setLowStamina(0);
      merged.setFans(0L);
      merged.setIsPlayer(false);
      merged.setBumps(0);
      merged.setWrestlingStyle("TODO");
      merged.setCreationDate(null);
    }

    return merged;
  }

  // ==================== FILE WRITING METHODS ====================

  /** Writes show templates to the show_templates.json file. */
  private void writeShowTemplatesToJsonFile(List<ShowTemplateDTO> templateDTOs) throws IOException {
    Path templatesFile = Paths.get("src/main/resources/show_templates.json");
    objectMapper.writeValue(templatesFile.toFile(), templateDTOs);
    log.debug("Successfully wrote {} show templates to {}", templateDTOs.size(), templatesFile);
  }

  /** Writes wrestlers to the wrestlers.json file. */
  private void writeWrestlersToJsonFile(List<WrestlerDTO> wrestlerDTOs) throws IOException {
    Path wrestlersFile = Paths.get("src/main/resources/wrestlers.json");
    objectMapper.writeValue(wrestlersFile.toFile(), wrestlerDTOs);
    log.debug("Successfully wrote {} wrestlers to {}", wrestlerDTOs.size(), wrestlersFile);
  }

  // ==================== SYNC RESULT CLASS ====================

  /** Represents the result of a synchronization operation. */
  public static class SyncResult {
    private final boolean success;
    private final String entityType;
    private final int syncedCount;
    private final int errorCount;
    private final String errorMessage;

    private SyncResult(
        boolean success, String entityType, int syncedCount, int errorCount, String errorMessage) {
      this.success = success;
      this.entityType = entityType;
      this.syncedCount = syncedCount;
      this.errorCount = errorCount;
      this.errorMessage = errorMessage;
    }

    public static SyncResult success(String entityType, int syncedCount, int errorCount) {
      return new SyncResult(true, entityType, syncedCount, errorCount, null);
    }

    public static SyncResult failure(String entityType, String errorMessage) {
      return new SyncResult(false, entityType, 0, 0, errorMessage);
    }

    // Getters
    public boolean isSuccess() {
      return success;
    }

    public String getEntityType() {
      return entityType;
    }

    public int getSyncedCount() {
      return syncedCount;
    }

    public int getErrorCount() {
      return errorCount;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    @Override
    public String toString() {
      if (success) {
        return String.format(
            "SyncResult{success=true, entityType='%s', syncedCount=%d, errorCount=%d}",
            entityType, syncedCount, errorCount);
      } else {
        return String.format(
            "SyncResult{success=false, entityType='%s', errorMessage='%s'}",
            entityType, errorMessage);
      }
    }
  }
}
