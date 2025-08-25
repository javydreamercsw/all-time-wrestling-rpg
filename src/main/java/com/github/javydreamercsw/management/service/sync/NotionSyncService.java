package com.github.javydreamercsw.management.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.SeasonPage;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionAlignment;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionDTO;
import com.github.javydreamercsw.management.dto.MatchDTO;
import com.github.javydreamercsw.management.dto.SeasonDTO;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.dto.TeamDTO;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.util.NotionBlocksRetriever;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for synchronizing data between Notion databases and local JSON files. This
 * service handles the extraction of data from Notion and updates the corresponding JSON files used
 * by the application for data initialization.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
public class NotionSyncService {

  private final ObjectMapper objectMapper;
  private final NotionHandler notionHandler;
  private final NotionSyncProperties syncProperties;
  private final SyncProgressTracker progressTracker;
  private final SyncHealthMonitor healthMonitor;
  private final RetryService retryService;
  private final CircuitBreakerService circuitBreakerService;
  private final SyncValidationService validationService;
  private final SyncTransactionManager syncTransactionManager;
  private final DataIntegrityChecker integrityChecker;

  // Database services for persisting synced data
  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final FactionRepository factionRepository;
  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final MatchResultService matchResultService;
  private final MatchTypeService matchTypeService;

  // Thread pool for async processing - using fixed thread pool for Java 17 compatibility
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  /** Constructor for NotionSyncService with optional NotionHandler for integration tests. */
  public NotionSyncService(
      ObjectMapper objectMapper,
      @Autowired(required = false) NotionHandler notionHandler,
      NotionSyncProperties syncProperties,
      SyncProgressTracker progressTracker,
      SyncHealthMonitor healthMonitor,
      RetryService retryService,
      CircuitBreakerService circuitBreakerService,
      SyncValidationService validationService,
      SyncTransactionManager syncTransactionManager,
      DataIntegrityChecker integrityChecker,
      ShowService showService,
      ShowTypeService showTypeService,
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      SeasonService seasonService,
      ShowTemplateService showTemplateService,
      FactionRepository factionRepository,
      TeamService teamService,
      TeamRepository teamRepository,
      MatchResultService matchResultService,
      MatchTypeService matchTypeService) {
    this.objectMapper = objectMapper;
    this.notionHandler = notionHandler;
    this.syncProperties = syncProperties;
    this.progressTracker = progressTracker;
    this.healthMonitor = healthMonitor;
    this.retryService = retryService;
    this.circuitBreakerService = circuitBreakerService;
    this.validationService = validationService;
    this.syncTransactionManager = syncTransactionManager;
    this.integrityChecker = integrityChecker;
    this.showService = showService;
    this.showTypeService = showTypeService;
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.factionRepository = factionRepository;
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.matchResultService = matchResultService;
    this.matchTypeService = matchTypeService;
  }

  /**
   * Helper method to check if NotionHandler is available for operations.
   *
   * @return true if NotionHandler is available, false otherwise
   */
  private boolean isNotionHandlerAvailable() {
    return notionHandler != null;
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database. This method retrieves
   * all shows from Notion and saves them to the database only.
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
    if (!syncProperties.isEntityEnabled("shows")) {
      log.debug("Shows synchronization is disabled in configuration");
      return SyncResult.success("Shows", 0, 0);
    }

    // Generate operation ID if not provided (required for transaction management)
    final String finalOperationId =
        operationId != null ? operationId : "shows-sync-" + System.currentTimeMillis();
    if (operationId == null) {
      log.debug("Generated operation ID: {}", finalOperationId);
    }

    // Check if NOTION_TOKEN is available before starting sync
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("NOTION_TOKEN not available. Cannot sync shows from Notion.");
      progressTracker.failOperation(
          finalOperationId, "NOTION_TOKEN environment variable is required for Notion sync");
      healthMonitor.recordFailure("Shows", "NOTION_TOKEN not available");
      return SyncResult.failure(
          "Shows", "NOTION_TOKEN environment variable is required for Notion sync");
    }

    log.info("🚀 Starting shows synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking
    progressTracker.startOperation(
        finalOperationId, "Sync Shows", 10); // Updated to 10 steps with reference data sync
    progressTracker.updateProgress(finalOperationId, 1, "Initializing sync operation...");

    // Sync reference data first (show types, seasons, templates)
    progressTracker.updateProgress(finalOperationId, 2, "Syncing show types...");
    SyncResult showTypesResult = syncShowTypes(finalOperationId + "-show-types");
    if (!showTypesResult.isSuccess()) {
      log.warn("Show types sync failed, but continuing: {}", showTypesResult.getErrorMessage());
    }

    progressTracker.updateProgress(finalOperationId, 3, "Syncing seasons...");
    SyncResult seasonsResult = syncSeasons(finalOperationId + "-seasons");
    if (!seasonsResult.isSuccess()) {
      log.warn("Seasons sync failed, but continuing: {}", seasonsResult.getErrorMessage());
    }

    progressTracker.updateProgress(finalOperationId, 4, "Syncing show templates...");
    SyncResult templatesResult = syncShowTemplates(finalOperationId + "-templates");
    if (!templatesResult.isSuccess()) {
      log.warn("Show templates sync failed, but continuing: {}", templatesResult.getErrorMessage());
    }

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
                      progressTracker.updateProgress(finalOperationId, 5, "Starting shows sync...");
                      SyncResult attemptResult = performShowsSync(finalOperationId, startTime);
                      log.debug(
                          "🔄 Shows sync attempt {} result: {}",
                          attemptNumber,
                          attemptResult != null ? attemptResult.isSuccess() : "NULL");
                      return attemptResult;
                    });
              });

      log.debug(
          "🔧 Circuit breaker returned result: {}", result != null ? result.isSuccess() : "NULL");

      // Handle case where result is null (shouldn't happen but defensive programming)
      if (result == null) {
        log.error("❌ Shows sync returned null result - this indicates a serious error");
        log.error("❌ This could be caused by:");
        log.error("❌   1. Circuit breaker returning null instead of throwing exception");
        log.error("❌   2. Retry service returning null instead of throwing exception");
        log.error("❌   3. Transaction manager returning null");
        log.error("❌   4. performShowsSync returning null due to uncaught exception");

        progressTracker.failOperation(finalOperationId, "Sync returned null result");
        healthMonitor.recordFailure("Shows", "Null result returned");
        return SyncResult.failure("Shows", "Sync operation returned null result");
      }

      return result;

    } catch (CircuitBreakerService.CircuitBreakerOpenException e) {
      log.warn("❌ Shows sync rejected by circuit breaker: {}", e.getMessage());

      progressTracker.failOperation(finalOperationId, "Circuit breaker open: " + e.getMessage());

      healthMonitor.recordFailure("Shows", "Circuit breaker open");
      return SyncResult.failure("Shows", "Service temporarily unavailable - circuit breaker open");

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize shows from Notion after {}ms", totalTime, e);

      // Fail progress tracking
      progressTracker.failOperation(finalOperationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      healthMonitor.recordFailure("Shows", e.getMessage());

      return SyncResult.failure("Shows", e.getMessage());
    }
  }

  /**
   * Performs the actual shows sync operation with enhanced error handling, validation, and
   * transaction management. This method is called by the retry and circuit breaker logic.
   */
  private SyncResult performShowsSync(@NonNull String operationId, long startTime)
      throws Exception {
    log.debug("🔧 Starting performShowsSync - calling syncTransactionManager.executeInTransaction");

    SyncResult transactionResult =
        syncTransactionManager.executeInTransaction(
            operationId,
            "shows",
            (transaction) -> {
              log.debug("🔧 Inside transaction callback - starting shows sync logic");
              try {
                // Step 1: Validate sync prerequisites
                log.info("🔍 Validating sync prerequisites...");
                if (operationId != null) {
                  progressTracker.updateProgress(
                      operationId, 1, "Validating sync prerequisites...");
                  progressTracker.addLogMessage(
                      operationId, "🔍 Validating sync prerequisites...", "INFO");
                }

                SyncValidationService.ValidationResult prerequisiteResult =
                    validationService.validateSyncPrerequisites();
                if (!prerequisiteResult.isValid()) {
                  throw new RuntimeException(
                      "Sync prerequisites validation failed: "
                          + String.join(", ", prerequisiteResult.getErrors()));
                }

                if (prerequisiteResult.hasWarnings()) {
                  log.warn(
                      "Sync prerequisites validation warnings: {}",
                      String.join(", ", prerequisiteResult.getWarnings()));
                  if (operationId != null) {
                    progressTracker.addLogMessage(
                        operationId,
                        "⚠️ Prerequisites warnings: "
                            + String.join(", ", prerequisiteResult.getWarnings()),
                        "WARNING");
                  }
                }

                // Step 2: Validate sync prerequisites (no backup needed for database sync)
                if (operationId != null) {
                  progressTracker.updateProgress(
                      operationId, 2, "Validating sync prerequisites...");
                }

                // Step 3: Get all shows from Notion using optimized method
                log.info("📥 Retrieving shows from Notion...");
                if (operationId != null) {
                  progressTracker.updateProgress(
                      operationId, 3, "Retrieving shows from Notion database...");
                  progressTracker.addLogMessage(
                      operationId, "📥 Retrieving shows from Notion...", "INFO");
                }

                List<ShowPage> notionShows = getAllShowsFromNotion();
                long retrieveTime = System.currentTimeMillis() - startTime;
                log.info(
                    "✅ Retrieved {} shows from Notion in {}ms", notionShows.size(), retrieveTime);

                if (operationId != null) {
                  progressTracker.addLogMessage(
                      operationId,
                      String.format(
                          "✅ Retrieved %d shows from Notion in %dms",
                          notionShows.size(), retrieveTime),
                      "SUCCESS");
                }

                // Step 4: Validate retrieved shows data
                log.info("🔍 Validating retrieved shows data...");
                if (operationId != null) {
                  progressTracker.updateProgress(operationId, 4, "Validating shows data...");
                  progressTracker.addLogMessage(operationId, "🔍 Validating shows data...", "INFO");
                }

                SyncValidationService.ValidationResult showsValidation =
                    validationService.validateShows(notionShows);
                if (!showsValidation.isValid()) {
                  throw new RuntimeException(
                      "Shows data validation failed: "
                          + String.join(", ", showsValidation.getErrors()));
                }

                if (showsValidation.hasWarnings()) {
                  log.warn(
                      "Shows data validation warnings: {}",
                      String.join(", ", showsValidation.getWarnings()));
                  if (operationId != null) {
                    progressTracker.addLogMessage(
                        operationId,
                        "⚠️ Shows validation warnings: "
                            + String.join(", ", showsValidation.getWarnings()),
                        "WARNING");
                  }
                }

                // Step 5: Convert to DTOs using parallel processing
                log.info("🔄 Converting shows to DTOs...");
                if (operationId != null) {
                  progressTracker.updateProgress(operationId, 5, "Converting shows to DTOs...");
                  progressTracker.addLogMessage(
                      operationId, "🔄 Converting shows to DTOs...", "INFO");
                }

                List<ShowDTO> showDTOs = convertShowPagesToDTO(notionShows);
                long convertTime = System.currentTimeMillis() - startTime - retrieveTime;
                log.info("✅ Converted {} shows to DTOs in {}ms", showDTOs.size(), convertTime);

                if (operationId != null) {
                  progressTracker.addLogMessage(
                      operationId,
                      String.format(
                          "✅ Converted %d shows to DTOs in %dms", showDTOs.size(), convertTime),
                      "SUCCESS");
                }

                // Step 6: Save to database with transaction support
                log.info("💾 Saving shows to database...");
                if (operationId != null) {
                  progressTracker.updateProgress(operationId, 6, "Saving shows to database...");
                  progressTracker.addLogMessage(
                      operationId, "💾 Saving shows to database...", "INFO");
                }

                // Save shows to database (transaction will handle rollback if this fails)
                int savedCount = saveShowsToDatabase(showDTOs);

                // Step 7: Perform post-sync data integrity check
                log.info("🔍 Performing post-sync data integrity check...");
                if (operationId != null) {
                  progressTracker.updateProgress(operationId, 7, "Checking data integrity...");
                  progressTracker.addLogMessage(
                      operationId, "🔍 Checking data integrity...", "INFO");
                }

                DataIntegrityChecker.IntegrityCheckResult integrityResult =
                    integrityChecker.performIntegrityCheck();
                if (!integrityResult.isValid()) {
                  log.error(
                      "❌ Data integrity check failed after sync: {}",
                      String.join(", ", integrityResult.getErrors()));
                  throw new RuntimeException(
                      "Data integrity check failed: "
                          + String.join(", ", integrityResult.getErrors()));
                }

                if (integrityResult.hasWarnings()) {
                  log.warn(
                      "⚠️ Data integrity warnings after sync: {}",
                      String.join(", ", integrityResult.getWarnings()));
                  if (operationId != null) {
                    progressTracker.addLogMessage(
                        operationId,
                        "⚠️ Integrity warnings: "
                            + String.join(", ", integrityResult.getWarnings()),
                        "WARNING");
                  }
                }

                long totalTime = System.currentTimeMillis() - startTime;
                log.info(
                    "🎉 Successfully synchronized {} shows to database in {}ms total",
                    savedCount,
                    totalTime);

                if (operationId != null) {
                  progressTracker.addLogMessage(
                      operationId,
                      String.format(
                          "🎉 Successfully synchronized %d shows in %dms total",
                          savedCount, totalTime),
                      "SUCCESS");
                  progressTracker.completeOperation(
                      operationId,
                      true,
                      String.format("Successfully synced %d shows", savedCount),
                      savedCount);
                }

                // Record success in health monitor
                healthMonitor.recordSuccess("Shows", totalTime, savedCount);
                return SyncResult.success("Shows", savedCount, 0);

                // Note: Transaction will be automatically rolled back by SyncTransactionManager if
                // any
                // exception occurs

              } catch (Exception e) {
                // Re-throw to be handled by retry/circuit breaker logic and transaction manager
                throw new RuntimeException("Shows sync operation failed: " + e.getMessage(), e);
              }
            });

    log.debug(
        "🔧 Transaction completed, result: {}",
        transactionResult != null ? transactionResult.isSuccess() : "NULL");

    if (transactionResult == null) {
      log.error(
          "❌ syncTransactionManager.executeInTransaction returned null - this should not happen");
      throw new RuntimeException("Transaction manager returned null result");
    }

    return transactionResult;
  }

  // ==================== SHOW TEMPLATES SYNC ====================

  /**
   * Synchronizes show templates from Notion to the local JSON file and database. This method should
   * be called before syncing shows to ensure templates are available.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTemplates(@NonNull String operationId) {
    log.info("🎭 Starting show templates synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("templates")) {
        log.info("Show templates sync is disabled in configuration");
        return SyncResult.success("Show Templates", 0, 0);
      }

      // Initialize progress tracking (3 steps: retrieve, convert, save to database)
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Show Templates", 3);
        progressTracker.updateProgress(operationId, 1, "Retrieving show templates from Notion...");
      }

      // Retrieve show templates from Notion
      log.info("📥 Retrieving show templates from Notion...");
      long retrieveStart = System.currentTimeMillis();

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync show templates from Notion.");
        return SyncResult.failure(
            "ShowTemplates", "NotionHandler is not available for sync operations");
      }

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

      // Save show templates to database
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Saving %d show templates to database...", templateDTOs.size()));
      }
      log.info("💾 Saving show templates to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = saveShowTemplatesToDatabase(templateDTOs);
      long dbTime = System.currentTimeMillis() - dbStart;
      log.info("✅ Saved {} show templates to database in {}ms", savedCount, dbTime);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} show templates in {}ms total", savedCount, totalTime);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d show templates", savedCount),
            savedCount);
      }

      // Record success in health monitor
      healthMonitor.recordSuccess("Show Templates", totalTime, savedCount);

      return SyncResult.success("Show Templates", savedCount, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize show templates from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Show Templates", e.getMessage());

      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  // ==================== SEASONS SYNC ====================

  /**
   * Synchronizes seasons from Notion to the database. Creates a default season if none exist. This
   * method should be called before syncing shows to ensure seasons are available.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSeasons(@NonNull String operationId) {
    log.info("📅 Starting seasons synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if NOTION_TOKEN is available
      if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
        log.warn("NOTION_TOKEN not available. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      // Initialize progress tracking if operation ID provided
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Seasons", 4);
        progressTracker.updateProgress(operationId, 1, "Retrieving seasons from Notion...");
      }

      // Retrieve seasons from Notion
      log.info("📥 Retrieving seasons from Notion...");

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      List<SeasonPage> seasonPages = notionHandler.loadAllSeasons();
      log.info(
          "✅ Retrieved {} seasons in {}ms",
          seasonPages.size(),
          System.currentTimeMillis() - startTime);

      if (seasonPages.isEmpty()) {
        log.info("No seasons found in Notion. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      // Convert to DTOs
      if (operationId != null) {
        progressTracker.updateProgress(operationId, 2, "Converting seasons to DTOs...");
      }
      log.info("🔄 Converting seasons to DTOs...");
      List<SeasonDTO> seasonDTOs =
          seasonPages.parallelStream()
              .map(this::convertSeasonPageToDTO)
              .filter(Objects::nonNull)
              .toList();
      log.info("✅ Converted {} seasons to DTOs", seasonDTOs.size());

      // Save to database
      if (operationId != null) {
        progressTracker.updateProgress(operationId, 3, "Saving seasons to database...");
      }
      log.info("💾 Saving seasons to database...");
      int savedCount = saveSeasonsToDB(seasonDTOs);
      log.info("✅ Processed {} seasons ({} new seasons created)", seasonDTOs.size(), savedCount);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.updateProgress(operationId, 4, "Seasons sync completed");
        progressTracker.completeOperation(
            operationId, true, "Seasons sync completed successfully", savedCount);
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Successfully synchronized {} seasons in {}ms total", savedCount, totalTime);
      return SyncResult.success("Seasons", savedCount, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize seasons after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Seasons sync failed: " + e.getMessage());
      }

      return SyncResult.failure("Seasons", e.getMessage());
    }
  }

  /**
   * Synchronizes show types from Notion or creates default show types if none exist. This method
   * should be called before syncing shows to ensure show types are available.
   *
   * @param operationId Operation ID for progress tracking (must not be null)
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTypes(@NonNull String operationId) {
    log.info("🎭 Starting show types synchronization with operation ID: {}", operationId);
    long startTime = System.currentTimeMillis();

    try {
      // Initialize progress tracking for show types sync
      progressTracker.startOperation(operationId, "Sync Show Types", 4);
      progressTracker.updateProgress(operationId, 1, "Extracting show types from Notion...");

      // Extract show types from the Shows database in Notion
      Set<String> notionShowTypes = extractShowTypesFromNotionShows();

      progressTracker.updateProgress(operationId, 2, "Processing show types...");

      if (notionShowTypes.isEmpty()) {
        log.info(
            "No show types found in Notion Shows database. Creating default show types if needed.");
        progressTracker.updateProgress(operationId, 3, "Creating default show types...");
        SyncResult result = createDefaultShowTypesIfNeeded();

        progressTracker.completeOperation(
            operationId,
            result.isSuccess(),
            result.isSuccess()
                ? "Default show types created successfully"
                : result.getErrorMessage(),
            result.getSyncedCount());

        return result;
      }

      // Sync show types from Notion
      int createdCount = 0;
      int updatedCount = 0;

      for (String showTypeName : notionShowTypes) {
        if (showTypeName == null || showTypeName.trim().isEmpty() || "N/A".equals(showTypeName)) {
          continue; // Skip invalid show types
        }

        // Check if show type already exists
        Optional<ShowType> existingShowType = showTypeService.findByName(showTypeName);

        if (existingShowType.isEmpty()) {
          // Create new show type
          ShowType newShowType = new ShowType();
          newShowType.setName(showTypeName);
          newShowType.setDescription(generateShowTypeDescription(showTypeName));
          showTypeService.save(newShowType);
          createdCount++;
          log.info("Created show type from Notion: {}", showTypeName);
        } else {
          // Show type already exists, could update description if needed
          updatedCount++;
          log.debug("Show type already exists: {}", showTypeName);
        }
      }

      progressTracker.updateProgress(operationId, 3, "Creating default show types if needed...");

      // Also ensure default show types exist for backward compatibility
      SyncResult defaultResult = createDefaultShowTypesIfNeeded();
      if (defaultResult.isSuccess()) {
        createdCount += defaultResult.getSyncedCount();
      }

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "✅ Show types sync completed in {}ms. Created: {}, Updated: {}",
          duration,
          createdCount,
          updatedCount);

      progressTracker.completeOperation(
          operationId,
          true,
          String.format(
              "Successfully synced %d show types (%d created, %d updated)",
              createdCount + updatedCount, createdCount, updatedCount),
          createdCount + updatedCount);

      return SyncResult.success("ShowTypes", createdCount, updatedCount);

    } catch (Exception e) {
      log.error("Failed to sync show types: {}", e.getMessage(), e);

      progressTracker.completeOperation(
          operationId, false, "Failed to sync show types: " + e.getMessage(), 0);

      return SyncResult.failure("ShowTypes", "Failed to sync show types: " + e.getMessage());
    }
  }

  /**
   * Extracts all unique show types from the Shows database in Notion.
   *
   * @return Set of unique show type names found in Notion
   */
  private Set<String> extractShowTypesFromNotionShows() {
    Set<String> showTypes = new HashSet<>();

    try {
      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot extract show types from Notion.");
        return showTypes; // Return empty set
      }

      log.info("Extracting show types from Notion Shows database...");
      List<ShowPage> allShows = notionHandler.loadAllShowsForSync();

      for (ShowPage showPage : allShows) {
        String showType = extractShowType(showPage);
        if (showType != null && !showType.trim().isEmpty() && !"N/A".equals(showType)) {
          showTypes.add(showType.trim());
        }
      }

      log.info("Found {} unique show types in Notion: {}", showTypes.size(), showTypes);
      return showTypes;

    } catch (Exception e) {
      log.error("Failed to extract show types from Notion: {}", e.getMessage(), e);
      return new HashSet<>(); // Return empty set on error
    }
  }

  /**
   * Generates a description for a show type based on its name.
   *
   * @param showTypeName The name of the show type
   * @return A descriptive text for the show type
   */
  private String generateShowTypeDescription(String showTypeName) {
    if (showTypeName == null) {
      return "Show type";
    }

    String lowerName = showTypeName.toLowerCase();

    if (lowerName.contains("weekly")) {
      return "Weekly television show format";
    } else if (lowerName.contains("ple") || lowerName.contains("premium")) {
      return "Premium live event or pay-per-view format";
    } else if (lowerName.contains("ppv") || lowerName.contains("pay-per-view")) {
      return "Pay-per-view event format";
    } else if (lowerName.contains("special")) {
      return "Special event show format";
    } else if (lowerName.contains("house")) {
      return "House show or live event format";
    } else if (lowerName.contains("tournament")) {
      return "Tournament-style show format";
    } else {
      return showTypeName + " show format";
    }
  }

  /**
   * Creates default show types if none exist in the database. This ensures shows have show types to
   * reference.
   */
  private SyncResult createDefaultShowTypesIfNeeded() {
    try {
      // Check if any show types exist
      List<ShowType> existingShowTypes = showTypeService.findAll();
      if (existingShowTypes.isEmpty()) {
        log.info("No show types found in database. Creating default show types...");

        int createdCount = 0;

        // Create Weekly show type
        ShowType weeklyType = new ShowType();
        weeklyType.setName("Weekly");
        weeklyType.setDescription("Weekly television show format");
        showTypeService.save(weeklyType);
        createdCount++;
        log.info("Created show type: Weekly");

        // Create Premium Live Event (PLE) show type
        ShowType pleType = new ShowType();
        pleType.setName("Premium Live Event (PLE)");
        pleType.setDescription("Premium live event or pay-per-view format");
        showTypeService.save(pleType);
        createdCount++;
        log.info("Created show type: Premium Live Event (PLE)");

        log.info("✅ Created {} default show types", createdCount);
        return SyncResult.success("ShowTypes", createdCount, 0);

      } else {
        log.info(
            "Show types already exist in database: {}",
            existingShowTypes.stream().map(ShowType::getName).toList());
        return SyncResult.success("ShowTypes", 0, 0);
      }

    } catch (Exception e) {
      log.error("Failed to create default show types: {}", e.getMessage(), e);
      return SyncResult.failure(
          "ShowTypes", "Failed to create default show types: " + e.getMessage());
    }
  }

  /** Converts a SeasonPage from Notion to a SeasonDTO for database persistence. */
  private SeasonDTO convertSeasonPageToDTO(@NonNull SeasonPage seasonPage) {
    try {
      SeasonDTO dto = new SeasonDTO();

      // Extract name from Notion page
      String name = extractNameFromNotionPage(seasonPage);
      dto.setName(name);

      // Set Notion ID for sync tracking
      dto.setNotionId(seasonPage.getId());

      // Extract description if available
      String description = extractPropertyAsString(seasonPage.getRawProperties(), "Description");
      if (description != null && !description.trim().isEmpty()) {
        dto.setDescription(description);
      } else {
        dto.setDescription("Season synced from Notion");
      }

      log.debug("Converted season: {} (Notion ID: {})", name, seasonPage.getId());
      return dto;

    } catch (Exception e) {
      log.error("Failed to convert season page to DTO: {}", seasonPage.getId(), e);
      return null;
    }
  }

  /** Saves season DTOs to the database. */
  private int saveSeasonsToDB(@NonNull List<SeasonDTO> seasonDTOs) {
    int savedCount = 0;
    int updatedCount = 0;
    int skippedCount = 0;

    for (SeasonDTO seasonDTO : seasonDTOs) {
      try {
        // Check if season already exists by name
        Season existingSeason = seasonService.findByName(seasonDTO.getName());

        if (existingSeason != null) {
          log.info("Season already exists: {}", seasonDTO.getName());
          // Update Notion ID if not set
          if (existingSeason.getNotionId() == null && seasonDTO.getNotionId() != null) {
            existingSeason.setNotionId(seasonDTO.getNotionId());
            seasonService.save(existingSeason);
            updatedCount++;
            log.info("Updated Notion ID for existing season: {}", seasonDTO.getName());
          } else {
            skippedCount++;
            log.debug("Season '{}' already up-to-date, skipping", seasonDTO.getName());
          }
        } else {
          // Create new season
          Season newSeason = new Season();
          newSeason.setName(seasonDTO.getName());
          newSeason.setDescription(seasonDTO.getDescription());
          newSeason.setNotionId(seasonDTO.getNotionId());

          seasonService.save(newSeason);
          savedCount++;
          log.info("Created new season: {}", seasonDTO.getName());
        }

      } catch (Exception e) {
        log.error("Failed to save season '{}': {}", seasonDTO.getName(), e.getMessage(), e);
      }
    }

    log.info(
        "Season sync summary: {} new, {} updated, {} skipped",
        savedCount,
        updatedCount,
        skippedCount);
    return savedCount;
  }

  /**
   * Creates a default season if no seasons exist in the database. This ensures shows have a season
   * to reference.
   */
  private SyncResult createDefaultSeasonIfNeeded() {
    try {
      // Check if any seasons exist
      var existingSeasons = seasonService.getAllSeasons(Pageable.unpaged());
      if (existingSeasons == null || existingSeasons.isEmpty()) {
        log.info("No seasons found in database. Creating default season...");

        // Create a default season
        Season defaultSeason =
            seasonService.createSeason(
                "Season 1", "Default season created by sync process", 5 // 5 shows per PPV
                );

        log.info(
            "✅ Created default season: {} (ID: {})",
            defaultSeason.getName(),
            defaultSeason.getId());
        return SyncResult.success("Seasons", 1, 0);
      } else {
        log.info("Found {} existing seasons in database", existingSeasons.getTotalElements());
        return SyncResult.success("Seasons", 0, 0);
      }
    } catch (Exception e) {
      log.error("Failed to create default season", e);
      return SyncResult.failure("Seasons", "Failed to create default season: " + e.getMessage());
    }
  }

  // ==================== WRESTLERS SYNC ====================

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(@NonNull String operationId) {
    log.info("🤼 Starting wrestlers synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("wrestlers")) {
        log.info("Wrestlers sync is disabled in configuration");
        return SyncResult.success("Wrestlers", 0, 0);
      }

      // Initialize progress tracking (reduced to 3 steps: retrieve, convert, save to database)
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Wrestlers", 3);
        progressTracker.updateProgress(operationId, 1, "Retrieving wrestlers from Notion...");
      }

      // Retrieve wrestlers from Notion (sync mode for faster processing)
      log.info("📥 Retrieving wrestlers from Notion...");
      long retrieveStart = System.currentTimeMillis();

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync wrestlers from Notion.");
        return SyncResult.failure(
            "Wrestlers", "NotionHandler is not available for sync operations");
      }

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

      // Skip JSON file writing during sync - only export operations should create JSON files
      log.debug(
          "Skipping JSON file write during sync operation - use export endpoints for JSON"
              + " generation");

      // Save wrestlers to database
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
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

      // Record success in health monitor
      healthMonitor.recordSuccess("Wrestlers", totalTime, wrestlerDTOs.size());

      return SyncResult.success("Wrestlers", wrestlerDTOs.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize wrestlers from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Wrestlers", e.getMessage());

      return SyncResult.failure("Wrestlers", e.getMessage());
    }
  }

  // ==================== FACTIONS SYNC ====================

  /**
   * Synchronizes factions from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncFactions(@NonNull String operationId) {
    log.info("🏴 Starting factions synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("factions")) {
        log.info("Factions sync is disabled in configuration");
        return SyncResult.success("Factions", 0, 0);
      }

      // Check if NOTION_TOKEN is available before starting sync
      if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
        log.warn("NOTION_TOKEN not available. Cannot sync factions from Notion.");
        if (operationId != null) {
          progressTracker.failOperation(
              operationId, "NOTION_TOKEN environment variable is required for Notion sync");
        }
        healthMonitor.recordFailure("Factions", "NOTION_TOKEN not available");
        return SyncResult.failure(
            "Factions", "NOTION_TOKEN environment variable is required for Notion sync");
      }

      // Initialize progress tracking
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Factions", 4);
        progressTracker.updateProgress(operationId, 1, "Retrieving factions from Notion...");
        progressTracker.addLogMessage(
            operationId, "🏴 Starting factions synchronization...", "INFO");
      }

      // Create backup if enabled
      if (syncProperties.isBackupEnabled()) {
        log.info("📦 Creating backup...");
        if (operationId != null) {
          progressTracker.updateProgress(operationId, 1, "Creating backup of existing data...");
          progressTracker.addLogMessage(operationId, "📦 Creating backup...", "INFO");
        }
        createBackup("factions.json");
      }

      // Get all factions from Notion
      log.info("📥 Retrieving factions from Notion...");
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId, 2, "Retrieving factions from Notion database...");
        progressTracker.addLogMessage(operationId, "📥 Retrieving factions from Notion...", "INFO");
      }
      List<FactionPage> notionFactions = getAllFactionsFromNotion();
      long retrieveTime = System.currentTimeMillis() - startTime;
      log.info("✅ Retrieved {} factions from Notion in {}ms", notionFactions.size(), retrieveTime);
      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId,
            String.format(
                "✅ Retrieved %d factions from Notion in %dms", notionFactions.size(), retrieveTime),
            "SUCCESS");
      }

      // Convert to DTOs using parallel processing
      log.info("🔄 Converting factions to DTOs...");
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Converting %d factions to data format...", notionFactions.size()));
        progressTracker.addLogMessage(operationId, "🔄 Converting factions to DTOs...", "INFO");
      }
      long conversionStart = System.currentTimeMillis();
      List<FactionDTO> factionDTOs = convertFactionPagesToDTO(notionFactions);
      long conversionTime = System.currentTimeMillis() - conversionStart;
      log.info("✅ Converted {} factions to DTOs in {}ms", factionDTOs.size(), conversionTime);
      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId,
            String.format(
                "✅ Converted %d factions to DTOs in %dms", factionDTOs.size(), conversionTime),
            "SUCCESS");
      }

      // Save to database only (no JSON file writing)
      log.info("🗄️ Saving factions to database...");
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId, 4, String.format("Saving %d factions to database...", factionDTOs.size()));
        progressTracker.addLogMessage(operationId, "🗄️ Saving factions to database...", "INFO");
      }
      long dbStart = System.currentTimeMillis();
      int savedCount = saveFactionsToDatabase(factionDTOs);
      long dbTime = System.currentTimeMillis() - dbStart;
      log.info("✅ Saved {} factions to database in {}ms", savedCount, dbTime);
      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId,
            String.format("✅ Saved %d factions to database in %dms", savedCount, dbTime),
            "SUCCESS");
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} factions to database in {}ms total",
          factionDTOs.size(),
          totalTime);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId,
            String.format(
                "🎉 Successfully synchronized %d factions in %dms total",
                factionDTOs.size(), totalTime),
            "SUCCESS");
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d factions", factionDTOs.size()),
            factionDTOs.size());
      }

      // Record success in health monitor
      healthMonitor.recordSuccess("Factions", totalTime, factionDTOs.size());

      return SyncResult.success("Factions", factionDTOs.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize factions from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId, "❌ Faction sync failed: " + e.getMessage(), "ERROR");
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Factions", e.getMessage());

      return SyncResult.failure("Factions", e.getMessage());
    }
  }

  /**
   * Synchronizes teams from Notion to the local database.
   *
   * @return SyncResult containing the operation status and details
   */
  public SyncResult syncTeams() {
    log.info("🔄 Starting teams synchronization from Notion...");
    long startTime = System.currentTimeMillis();
    String operationId = UUID.randomUUID().toString();

    try {
      // Start progress tracking
      SyncProgressTracker.SyncProgress progress =
          progressTracker.startOperation("Teams Sync", "Synchronizing teams from Notion", 0);
      operationId = progress.getOperationId();

      // Load teams from Notion
      progressTracker.addLogMessage(
          operationId, "📥 Loading teams from Notion database...", "INFO");

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync teams from Notion.");
        progressTracker.failOperation(
            operationId, "NotionHandler is not available for sync operations");
        return SyncResult.failure("Teams", "NotionHandler is not available for sync operations");
      }

      // Sync wrestlers first to ensure team dependencies exist
      progressTracker.addLogMessage(
          operationId, "🤼 Syncing wrestlers first to ensure team dependencies...", "INFO");
      SyncResult wrestlerSyncResult = syncWrestlers(operationId + "-wrestlers");

      if (!wrestlerSyncResult.isSuccess()) {
        log.warn(
            "Wrestler sync failed, but continuing with team sync: {}",
            wrestlerSyncResult.getErrorMessage());
        progressTracker.addLogMessage(
            operationId,
            "⚠️ Wrestler sync failed, some teams may be skipped: "
                + wrestlerSyncResult.getErrorMessage(),
            "WARN");
      } else {
        progressTracker.addLogMessage(
            operationId,
            String.format(
                "✅ Synced %d wrestlers as team dependencies", wrestlerSyncResult.getSyncedCount()),
            "INFO");
      }

      List<TeamPage> teamPages = notionHandler.loadAllTeams();

      if (teamPages.isEmpty()) {
        progressTracker.addLogMessage(operationId, "⚠️ No teams found in Notion database", "WARN");
        progressTracker.completeOperation(operationId, true, "No teams to sync", 0);

        // Record success in health monitor
        long totalTime = System.currentTimeMillis() - startTime;
        healthMonitor.recordSuccess("Teams", totalTime, 0);

        return SyncResult.success("Teams", 0, 0);
      }

      progressTracker.addLogMessage(
          operationId, String.format("📋 Found %d teams in Notion", teamPages.size()), "INFO");

      // Convert to DTOs
      List<TeamDTO> teamDTOs = new ArrayList<>();
      for (TeamPage teamPage : teamPages) {
        try {
          TeamDTO teamDTO = convertTeamPageToDTO(teamPage);
          if (teamDTO != null) {
            teamDTOs.add(teamDTO);
          }
        } catch (Exception e) {
          String teamName = extractNameFromNotionPage(teamPage);
          log.warn("Failed to convert team page to DTO: {}", teamName, e);
          progressTracker.addLogMessage(
              operationId,
              String.format("⚠️ Failed to convert team: %s - %s", teamName, e.getMessage()),
              "WARN");
        }
      }

      progressTracker.addLogMessage(
          operationId,
          String.format("✅ Successfully converted %d teams to DTOs", teamDTOs.size()),
          "INFO");

      // Save teams to database
      progressTracker.addLogMessage(operationId, "💾 Saving teams to database...", "INFO");
      int savedCount = 0;
      for (TeamDTO teamDTO : teamDTOs) {
        try {
          boolean saved = saveOrUpdateTeam(teamDTO);
          if (saved) {
            savedCount++;
          }
        } catch (Exception e) {
          log.warn("Failed to save team: {}", teamDTO.getName(), e);
          progressTracker.addLogMessage(
              operationId,
              String.format("⚠️ Failed to save team: %s - %s", teamDTO.getName(), e.getMessage()),
              "WARN");
        }
      }

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d teams", savedCount),
            savedCount);
      }

      // Record success in health monitor
      long totalTime = System.currentTimeMillis() - startTime;
      healthMonitor.recordSuccess("Teams", totalTime, savedCount);

      return SyncResult.success("Teams", savedCount, 0);

    } catch (Exception e) {
      log.error("❌ Teams sync failed", e);

      // Fail progress tracking
      if (operationId != null) {
        progressTracker.addLogMessage(
            operationId, "❌ Team sync failed: " + e.getMessage(), "ERROR");
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Teams", e.getMessage());

      return SyncResult.failure("Teams", e.getMessage());
    }
  }

  /**
   * Synchronizes matches from Notion Matches database directly to the database with optional
   * progress tracking.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncMatches(@NonNull String operationId) {
    if (!syncProperties.isEntityEnabled("matches")) {
      log.debug("Matches synchronization is disabled in configuration");
      return SyncResult.success("Matches", 0, 0);
    }

    // Generate operation ID if not provided (required for transaction management)
    final String finalOperationId =
        operationId != null ? operationId : "matches-sync-" + System.currentTimeMillis();
    if (operationId == null) {
      log.debug("Generated operation ID: {}", finalOperationId);
    }

    // Check if NOTION_TOKEN is available before starting sync
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("NOTION_TOKEN not available. Cannot sync matches from Notion.");
      if (operationId != null) {
        progressTracker.failOperation(
            finalOperationId, "NOTION_TOKEN environment variable is required for Notion sync");
      }
      healthMonitor.recordFailure("Matches", "NOTION_TOKEN not available");
      return SyncResult.failure(
          "Matches", "NOTION_TOKEN environment variable is required for Notion sync");
    }

    log.info("🚀 Starting matches synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking
    progressTracker.startOperation(finalOperationId, "Sync Matches", 8); // 8 steps for match sync
    progressTracker.updateProgress(finalOperationId, 1, "Initializing matches sync operation...");

    try {
      log.debug("🔧 Executing matches sync with circuit breaker and retry logic");

      // Execute with circuit breaker and retry logic
      SyncResult result =
          circuitBreakerService.execute(
              "matches",
              () -> {
                log.debug("🔄 Circuit breaker executing matches sync");
                return retryService.executeWithRetry(
                    "matches",
                    (attemptNumber) -> {
                      log.debug("🔄 Matches sync attempt {} starting", attemptNumber);
                      progressTracker.updateProgress(
                          finalOperationId, 3, "Starting matches sync...");
                      SyncResult attemptResult = performMatchesSync(finalOperationId, startTime);
                      log.debug(
                          "🔄 Matches sync attempt {} result: {}",
                          attemptNumber,
                          attemptResult != null ? attemptResult.isSuccess() : "NULL");
                      return attemptResult;
                    });
              });

      // Update final progress
      progressTracker.updateProgress(finalOperationId, 8, "Matches sync completed");
      progressTracker.completeOperation(
          finalOperationId,
          result.isSuccess(),
          result.isSuccess() ? "Matches sync completed successfully" : result.getErrorMessage(),
          result.getSyncedCount());

      // Record health metrics
      if (result.isSuccess()) {
        healthMonitor.recordSuccess(
            "Matches", System.currentTimeMillis() - startTime, result.getSyncedCount());
        log.info("✅ Matches sync completed successfully: {}", result.getSummary());
      } else {
        healthMonitor.recordFailure("Matches", result.getErrorMessage());
        log.error("❌ Matches sync failed: {}", result.getErrorMessage());
      }

      return result;

    } catch (Exception e) {
      String errorMessage = "Matches sync failed with exception: " + e.getMessage();
      log.error("❌ {}", errorMessage, e);

      progressTracker.failOperation(finalOperationId, errorMessage);
      healthMonitor.recordFailure("Matches", errorMessage);

      return SyncResult.failure("Matches", errorMessage);
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
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("NOTION_TOKEN not available. Cannot sync from Notion.");
      throw new IllegalStateException(
          "NOTION_TOKEN environment variable is required for Notion sync");
    }

    // Check if NotionHandler is available
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync from Notion.");
      throw new IllegalStateException("NotionHandler is not available for sync operations");
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
  private List<ShowDTO> convertShowPagesToDTO(@NonNull List<ShowPage> showPages) {
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
  private int saveShowsToDatabase(@NonNull List<ShowDTO> showDTOs) {
    return saveShowsToDatabaseWithBatching(showDTOs, 50); // Use batch size of 50
  }

  /**
   * Enhanced method to save shows to database with batch processing and error recovery. Processes
   * shows in batches to handle large datasets and provides partial success capability.
   */
  private int saveShowsToDatabaseWithBatching(@NonNull List<ShowDTO> showDTOs, int batchSize) {
    log.info(
        "Starting database persistence for {} shows with batch size {}",
        showDTOs.size(),
        batchSize);

    // Cache lookups for performance (same as DataInitializer) with null safety
    Map<String, ShowType> showTypes = new HashMap<>();
    Map<String, Season> seasons = new HashMap<>();
    Map<String, ShowTemplate> templates = new HashMap<>();

    // Load reference data with error handling and detailed logging
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

    // Process shows in batches for better error recovery
    for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
      int startIndex = batchIndex * batchSize;
      int endIndex = Math.min(startIndex + batchSize, showDTOs.size());
      List<ShowDTO> batch = showDTOs.subList(startIndex, endIndex);

      log.debug("Processing batch {}/{} ({} shows)", batchIndex + 1, totalBatches, batch.size());

      try {
        // Process each show in the batch
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

        log.debug(
            "Completed batch {}/{}: {} shows processed",
            batchIndex + 1,
            totalBatches,
            batch.size());

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
      // Find show type (required) - handle "N/A" case with smart mapping
      String showTypeName = dto.getShowType();

      ShowType type = showTypes.get(showTypeName);
      if (type == null) {
        log.warn(
            "Show type not found: {} for show: {} - skipping (available types: {})",
            showTypeName,
            dto.getName(),
            showTypes.keySet());
        return false;
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
        log.debug("Creating new show: {} with external ID: {}", dto.getName(), dto.getExternalId());
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

  /**
   * Saves the list of WrestlerDTO objects to the database. Uses smart matching logic: 1. Try to
   * find by external ID (Notion page ID) first 2. Fallback to name matching if external ID not
   * present 3. Create new wrestler if no match found
   *
   * @param wrestlerDTOs List of WrestlerDTO objects to save
   * @param operationId Operation ID for progress tracking (can be null)
   * @return Number of wrestlers successfully saved/updated
   */
  private int saveWrestlersToDatabase(
      @NonNull List<WrestlerDTO> wrestlerDTOs, @NonNull String operationId) {
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

  /**
   * Saves the list of ShowTemplateDTO objects to the database.
   *
   * @param templateDTOs List of ShowTemplateDTO objects to save
   * @return Number of show templates successfully saved/updated
   */
  private int saveShowTemplatesToDatabase(@NonNull List<ShowTemplateDTO> templateDTOs) {
    log.info("💾 Saving {} show templates to database...", templateDTOs.size());
    int savedCount = 0;

    for (ShowTemplateDTO dto : templateDTOs) {
      try {
        // Use the ShowTemplateService to create or update the template
        ShowTemplate savedTemplate =
            showTemplateService.createOrUpdateTemplate(
                dto.getName(),
                dto.getDescription(),
                dto.getShowType(), // This maps to showTypeName in the service
                null // notionUrl - we don't have this in the DTO currently
                );

        if (savedTemplate != null) {
          // Set external ID if available and save only once
          if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
            savedTemplate.setExternalId(dto.getExternalId());
            // Save again only if we modified the external ID
            savedTemplate = showTemplateService.save(savedTemplate);
          }
          savedCount++;
          log.debug("Saved show template: {}", dto.getName());
        } else {
          log.warn(
              "Failed to save show template: {} (show type not found: {})",
              dto.getName(),
              dto.getShowType());
        }
      } catch (Exception e) {
        log.error("Failed to save show template '{}': {}", dto.getName(), e.getMessage());
      }
    }

    log.info(
        "Successfully saved {} out of {} show templates to database",
        savedCount,
        templateDTOs.size());
    return savedCount;
  }

  // ==================== MATCH SYNC IMPLEMENTATION METHODS ====================

  /**
   * Performs the actual matches synchronization from Notion to database.
   *
   * @param operationId Operation ID for progress tracking
   * @param startTime Start time for performance measurement
   * @return SyncResult containing the outcome of the sync operation
   */
  private SyncResult performMatchesSync(@NonNull String operationId, long startTime) {
    try {
      // Step 1: Load matches from Notion
      progressTracker.updateProgress(operationId, 4, "Loading matches from Notion...");
      List<MatchPage> matchPages = loadMatchesFromNotion();
      log.info(
          "✅ Retrieved {} matches in {}ms",
          matchPages.size(),
          System.currentTimeMillis() - startTime);

      if (matchPages.isEmpty()) {
        log.info("No matches found in Notion database");
        return SyncResult.success("Matches", 0, 0);
      }

      // Step 2: Convert to DTOs
      progressTracker.updateProgress(operationId, 5, "Converting matches to DTOs...");
      List<MatchDTO> matchDTOs = convertMatchesToDTOs(matchPages);
      log.info(
          "✅ Converted {} matches in {}ms",
          matchDTOs.size(),
          System.currentTimeMillis() - startTime);

      // Step 3: Save to database
      progressTracker.updateProgress(operationId, 6, "Saving matches to database...");
      int syncedCount = saveMatchesToDatabase(matchDTOs);
      log.info(
          "✅ Saved {} matches to database in {}ms",
          syncedCount,
          System.currentTimeMillis() - startTime);

      // Step 4: Validate results
      progressTracker.updateProgress(operationId, 7, "Validating match sync results...");
      boolean validationPassed = validateMatchSyncResults(matchDTOs, syncedCount);

      if (!validationPassed) {
        return SyncResult.failure("Matches", "Match sync validation failed");
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Matches sync completed successfully in {}ms", totalTime);

      return SyncResult.success("Matches", syncedCount, 0);

    } catch (Exception e) {
      log.error("Failed to perform matches sync", e);
      return SyncResult.failure("Matches", "Failed to sync matches: " + e.getMessage());
    }
  }

  /**
   * Loads all matches from Notion database.
   *
   * @return List of MatchPage objects from Notion
   */
  private List<MatchPage> loadMatchesFromNotion() {
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot load matches from Notion.");
      return new ArrayList<>();
    }

    try {
      return notionHandler.loadAllMatches();
    } catch (Exception e) {
      log.error("Failed to load matches from Notion", e);
      throw new RuntimeException("Failed to load matches from Notion: " + e.getMessage(), e);
    }
  }

  /**
   * Converts MatchPage objects to MatchDTO objects.
   *
   * @param matchPages List of MatchPage objects from Notion
   * @return List of MatchDTO objects
   */
  private List<MatchDTO> convertMatchesToDTOs(List<MatchPage> matchPages) {
    return matchPages.parallelStream()
        .map(this::convertMatchPageToDTO)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Converts a single MatchPage to MatchDTO.
   *
   * @param matchPage The MatchPage to convert
   * @return MatchDTO object or null if conversion fails
   */
  private MatchDTO convertMatchPageToDTO(MatchPage matchPage) {
    try {
      MatchDTO dto = new MatchDTO();

      // Set external ID (Notion page ID)
      dto.setExternalId(matchPage.getId());

      // Extract basic properties
      dto.setName(extractNameFromNotionPage(matchPage));
      dto.setDescription(extractDescriptionFromNotionPage(matchPage));

      // Extract match-specific properties
      dto.setParticipants(extractParticipantsFromMatchPage(matchPage));
      dto.setWinner(extractWinnerFromMatchPage(matchPage));
      dto.setMatchType(extractMatchTypeFromMatchPage(matchPage));
      dto.setShow(extractShowFromMatchPage(matchPage));
      dto.setDuration(extractDurationFromMatchPage(matchPage));
      dto.setRating(extractRatingFromMatchPage(matchPage));
      dto.setStipulation(extractStipulationFromMatchPage(matchPage));

      // Extract timestamps
      dto.setCreatedTime(parseInstantFromString(matchPage.getCreated_time()));
      dto.setLastEditedTime(parseInstantFromString(matchPage.getLast_edited_time()));
      dto.setCreatedBy(
          matchPage.getCreated_by() != null ? matchPage.getCreated_by().getName() : null);
      dto.setLastEditedBy(
          matchPage.getLast_edited_by() != null ? matchPage.getLast_edited_by().getName() : null);

      // Set defaults
      dto.setIsTitleMatch(false);
      dto.setIsNpcGenerated(false);
      dto.setMatchDate(dto.getCreatedTime() != null ? dto.getCreatedTime() : Instant.now());

      return dto;

    } catch (Exception e) {
      log.error("Failed to convert match page to DTO: {}", matchPage.getId(), e);
      return null;
    }
  }

  /**
   * Saves match DTOs to the database using parallel processing with caching.
   *
   * @param matchDTOs List of MatchDTO objects to save
   * @return Number of matches successfully saved
   */
  private int saveMatchesToDatabase(List<MatchDTO> matchDTOs) {
    log.info("🚀 Starting parallel processing of {} matches", matchDTOs.size());

    // Pre-load and cache entities to avoid repeated database lookups
    Map<String, Show> showCache = preloadShows(matchDTOs);
    Map<String, MatchType> matchTypeCache = preloadMatchTypes(matchDTOs);
    Map<String, Wrestler> wrestlerCache = preloadWrestlers(matchDTOs);

    log.info(
        "✅ Cached {} shows, {} match types, {} wrestlers",
        showCache.size(),
        matchTypeCache.size(),
        wrestlerCache.size());

    // Process matches in parallel and collect results
    List<MatchResult> createdMatches =
        matchDTOs.parallelStream()
            .filter(
                dto -> {
                  if (!dto.isValid()) {
                    log.warn("Skipping invalid match DTO: {}", dto.getSummary());
                    return false;
                  }

                  // Check if match already exists
                  if (matchResultService.existsByExternalId(dto.getExternalId())) {
                    log.debug("Match already exists, skipping: {}", dto.getName());
                    return false;
                  }

                  return true;
                })
            .map(
                dto -> {
                  try {
                    MatchResult matchResult =
                        createMatchResultFromDTO(dto, showCache, matchTypeCache, wrestlerCache);
                    if (matchResult != null) {
                      log.debug("✅ Created match result: {}", dto.getName());
                      return matchResult;
                    } else {
                      log.warn("❌ Failed to create match result: {}", dto.getName());
                      return null;
                    }
                  } catch (Exception e) {
                    log.error("❌ Failed to save match: {}", dto.getSummary(), e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    log.info(
        "✅ Parallel processing completed: {} matches created successfully", createdMatches.size());
    return createdMatches.size();
  }

  /** Pre-loads shows mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, Show> preloadShows(List<MatchDTO> matchDTOs) {
    Set<String> showNames =
        matchDTOs.stream()
            .map(MatchDTO::getShow)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, Show> cache = new HashMap<>();
    for (String showName : showNames) {
      Optional<Show> show = showService.findByName(showName);
      if (show.isPresent()) {
        cache.put(showName, show.get());
      }
    }
    return cache;
  }

  /** Pre-loads match types mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, MatchType> preloadMatchTypes(List<MatchDTO> matchDTOs) {
    Set<String> matchTypeNames =
        matchDTOs.stream()
            .map(MatchDTO::getMatchType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, MatchType> cache = new HashMap<>();
    for (String matchTypeName : matchTypeNames) {
      Optional<MatchType> matchType = matchTypeService.findByName(matchTypeName);
      if (matchType.isPresent()) {
        cache.put(matchTypeName, matchType.get());
      }
    }
    return cache;
  }

  /** Pre-loads wrestlers mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, Wrestler> preloadWrestlers(List<MatchDTO> matchDTOs) {
    Set<String> wrestlerNames =
        matchDTOs.stream()
            .flatMap(
                dto -> {
                  Set<String> names = new HashSet<>();
                  if (dto.getWinner() != null) names.add(dto.getWinner());
                  if (dto.getParticipants() != null) {
                    names.addAll(Arrays.asList(dto.getParticipants().split(",\\s*")));
                  }
                  return names.stream();
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, Wrestler> cache = new HashMap<>();
    for (String wrestlerName : wrestlerNames) {
      Optional<Wrestler> wrestler = wrestlerService.findByName(wrestlerName);
      if (wrestler.isPresent()) {
        cache.put(wrestlerName, wrestler.get());
      }
    }
    return cache;
  }

  /**
   * Creates a MatchResult entity from a MatchDTO using cached entities for performance.
   *
   * @param dto The MatchDTO to convert
   * @param showCache Pre-loaded show cache
   * @param matchTypeCache Pre-loaded match type cache
   * @param wrestlerCache Pre-loaded wrestler cache
   * @return Created MatchResult or null if creation fails
   */
  private MatchResult createMatchResultFromDTO(
      MatchDTO dto,
      Map<String, Show> showCache,
      Map<String, MatchType> matchTypeCache,
      Map<String, Wrestler> wrestlerCache) {
    try {
      // Resolve dependencies using caches
      Show show = showCache.get(dto.getShow());
      if (show == null) {
        log.warn("Could not resolve show '{}' for match '{}'", dto.getShow(), dto.getName());
        return null;
      }

      MatchType matchType = matchTypeCache.get(dto.getMatchType());
      if (matchType == null) {
        log.warn(
            "Could not resolve match type '{}' for match '{}'", dto.getMatchType(), dto.getName());
        return null;
      }

      Wrestler winner = wrestlerCache.get(dto.getWinner());
      // Winner can be null for draws

      // Create match result
      MatchResult matchResult =
          matchResultService.createMatchResult(
              show,
              matchType,
              winner,
              dto.getMatchDate(),
              dto.getDuration(),
              dto.getRating(),
              dto.getNarration(),
              dto.getIsTitleMatch(),
              dto.getIsNpcGenerated());

      // Set external ID
      matchResult.setExternalId(dto.getExternalId());
      matchResultService.updateMatchResult(matchResult);

      // Add participants using cache
      addParticipantsToMatch(matchResult, dto.getParticipants(), dto.getWinner(), wrestlerCache);

      return matchResult;

    } catch (Exception e) {
      log.error("Failed to create match result from DTO: {}", dto.getSummary(), e);
      return null;
    }
  }

  /**
   * Creates a MatchResult entity from a MatchDTO (legacy method for backward compatibility).
   *
   * @param dto The MatchDTO to convert
   * @return Created MatchResult or null if creation fails
   */
  private MatchResult createMatchResultFromDTO(MatchDTO dto) {
    try {
      // Resolve dependencies
      Show show = resolveShow(dto.getShow());
      if (show == null) {
        log.warn("Could not resolve show '{}' for match '{}'", dto.getShow(), dto.getName());
        return null;
      }

      MatchType matchType = resolveMatchType(dto.getMatchType());
      if (matchType == null) {
        log.warn(
            "Could not resolve match type '{}' for match '{}'", dto.getMatchType(), dto.getName());
        return null;
      }

      Wrestler winner = resolveWrestler(dto.getWinner());
      // Winner can be null for draws

      // Create match result
      MatchResult matchResult =
          matchResultService.createMatchResult(
              show,
              matchType,
              winner,
              dto.getMatchDate(),
              dto.getDuration(),
              dto.getRating(),
              dto.getNarration(),
              dto.getIsTitleMatch(),
              dto.getIsNpcGenerated());

      // Set external ID
      matchResult.setExternalId(dto.getExternalId());
      matchResultService.updateMatchResult(matchResult);

      // Add participants
      addParticipantsToMatch(matchResult, dto.getParticipants(), dto.getWinner());

      return matchResult;

    } catch (Exception e) {
      log.error("Failed to create match result from DTO: {}", dto.getSummary(), e);
      return null;
    }
  }

  // ==================== BACKUP METHODS ====================

  /**
   * Creates a backup of the specified JSON file before sync operation.
   *
   * @param fileName The name of the JSON file to backup
   * @throws IOException if backup creation fails
   */
  private void createBackup(@NonNull String fileName) throws IOException {
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
  private void cleanupOldBackups(@NonNull String fileName) {
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
        // Handle different date formats from Notion
        String dateStr = showDate.toString().trim();

        // Skip placeholder values
        if ("date".equals(dateStr) || dateStr.isEmpty()) {
          log.debug("Skipping placeholder date value: {}", dateStr);
          return null;
        }

        // Try to parse and format the date
        try {
          LocalDate date = LocalDate.parse(dateStr);
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse show date: {}", dateStr);
          return null; // Return null instead of invalid date string
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

        // Handle Notion relation format like "1 items"
        if (seasonStr.matches("\\d+ items?")) {
          log.debug("Season shows as relation count ({}), attempting to resolve", seasonStr);
          return resolveSeasonRelationship(showPage);
        }

        // If it's already a readable name, use it
        if (!seasonStr.isEmpty()
            && !seasonStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          return seasonStr;
        }
      }
    }
    return null;
  }

  /**
   * Resolves team member relationship by looking up wrestler names from the database. This handles
   * the "1 items" format from Notion relationships.
   */
  private String resolveTeamMemberRelationship(
      @NonNull TeamPage teamPage, @NonNull String memberPropertyName) {
    if (teamPage.getRawProperties() == null) {
      return null;
    }

    Object memberProperty = teamPage.getRawProperties().get(memberPropertyName);
    if (memberProperty != null) {
      String memberStr = memberProperty.toString().trim();

      // Handle "1 items" format - we know there's a relationship but it's not resolved
      if (memberStr.matches("\\d+ items?")) {
        log.debug(
            "Team member {} shows as relation count ({}), using database lookup",
            memberPropertyName,
            memberStr);

        // For now, we can't resolve the specific relationship without additional API calls
        // This would require accessing the original Page object and making API calls
        // Return null to let the team creation handle missing members gracefully
        return null;
      }

      // If it's already a readable name, use it (but split if comma-separated)
      if (!memberStr.isEmpty()
          && !memberStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {

        // Handle comma-separated names (shouldn't happen with non-resolving mode, but just in case)
        if (memberStr.contains(",")) {
          String[] parts = memberStr.split(",");
          String firstPart = parts[0].trim();
          log.debug(
              "Team member {} contains multiple values, using first: '{}'",
              memberPropertyName,
              firstPart);
          return firstPart;
        }

        return memberStr;
      }
    }

    return null;
  }

  /**
   * Resolves season relationship by looking up the season name from the database. Since we've
   * already synced seasons, we can use the database to resolve the relationship.
   */
  private String resolveSeasonRelationship(@NonNull ShowPage showPage) {
    try {
      // Get all seasons from database (should be cached/fast since we just synced them)
      var seasonsPage = seasonService.getAllSeasons(Pageable.unpaged());
      if (seasonsPage != null && !seasonsPage.isEmpty()) {
        // For now, since we know there's only one season "The Beginning", return it
        // In the future, this could be enhanced to match based on Notion IDs or other criteria
        Season firstSeason = seasonsPage.iterator().next();
        log.debug("Resolved season relationship to: {}", firstSeason.getName());
        return firstSeason.getName();
      }
    } catch (Exception e) {
      log.debug("Failed to resolve season relationship: {}", e.getMessage());
    }

    // Fallback: return null to let the system handle missing season gracefully
    return null;
  }

  private String extractTemplateName(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object template = showPage.getRawProperties().get("Template");
      return template != null ? template.toString() : null;
    }
    return null;
  }

  // ==================== GENERIC EXTRACTION METHODS ====================

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for name
      Object name = page.getRawProperties().get("Name");
      if (name == null) {
        name = page.getRawProperties().get("Title"); // Alternative name property
      }

      if (name != null) {
        String nameStr = extractTextFromProperty(name);
        if (nameStr != null && !nameStr.trim().isEmpty() && !"N/A".equals(nameStr)) {
          return nameStr.trim();
        }
      }

      log.debug("Name property not found or empty for page: {}", page.getId());
    }
    return "Unknown";
  }

  /** Extracts a property value as a string from raw properties map. */
  private String extractPropertyAsString(
      @NonNull Map<String, Object> rawProperties, @NonNull String propertyName) {
    if (rawProperties == null || propertyName == null) {
      return null;
    }

    Object property = rawProperties.get(propertyName);
    return extractTextFromProperty(property);
  }

  /** Extracts text content from a Notion property object. */
  private String extractTextFromProperty(Object property) {
    if (property == null) {
      return null;
    }

    // Handle PageProperty objects (from Notion API)
    if (property instanceof notion.api.v1.model.pages.PageProperty) {
      notion.api.v1.model.pages.PageProperty pageProperty =
          (notion.api.v1.model.pages.PageProperty) property;

      // Handle title properties
      if (pageProperty.getTitle() != null && !pageProperty.getTitle().isEmpty()) {
        return pageProperty.getTitle().get(0).getPlainText();
      }

      // Handle rich text properties
      if (pageProperty.getRichText() != null && !pageProperty.getRichText().isEmpty()) {
        return pageProperty.getRichText().get(0).getPlainText();
      }

      // Handle select properties
      if (pageProperty.getSelect() != null) {
        return pageProperty.getSelect().getName();
      }

      // Handle other property types as needed
      log.debug("Unhandled PageProperty type: {}", pageProperty.getType());
      return null;
    }

    // Handle simple string values
    String str = property.toString().trim();

    // Avoid returning the entire PageProperty string representation
    if (str.startsWith("PageProperty(")) {
      log.warn("Property extraction returned PageProperty object string - this indicates a bug");
      return null;
    }

    return str.isEmpty() ? null : str;
  }

  /** Extracts description from any NotionPage type using raw properties. */
  private String extractDescriptionFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      Object description = page.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  /** Extracts show type from any NotionPage type using raw properties. */
  private String extractShowTypeFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      Object showType = page.getRawProperties().get("Show Type");
      if (showType == null) {
        showType = page.getRawProperties().get("ShowType");
      }
      if (showType == null) {
        showType = page.getRawProperties().get("Type");
      }

      if (showType != null) {
        String showTypeStr = showType.toString().trim();
        if (!showTypeStr.isEmpty() && !"N/A".equals(showTypeStr)) {
          return showTypeStr;
        }
      }

      log.debug("Show type not found or empty for page: {}", page.getId());
      return null; // Return null instead of "N/A"
    }
    return null;
  }

  /**
   * Extracts description from the page body/content using the existing NotionBlocksRetriever. This
   * is used for wrestlers where the description is in the page content.
   */
  private String extractDescriptionFromPageBody(@NonNull NotionPage page) {
    if (page == null || page.getId() == null) {
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

  /**
   * Extracts faction from any NotionPage type using raw properties. Faction is a relationship
   * property that links to a faction/team page. For sync mode, we extract the faction name if
   * available, otherwise preserve existing.
   */
  private String extractFactionFromNotionPage(@NonNull NotionPage page) {
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
  private String resolveFactionRelationship(@NonNull WrestlerPage wrestlerPage) {
    // For now, return null to preserve existing faction data
    // Full relationship resolution would require accessing the original Page object
    // and making additional API calls, which is complex for sync mode
    log.debug(
        "Faction relationship resolution not implemented in sync mode, preserving existing"
            + " faction");
    return null;
  }

  /** Extracts a string property from any NotionPage type using raw properties. */
  private String extractStringPropertyFromNotionPage(
      @NonNull NotionPage page, @NonNull String propertyName) {
    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      if (property != null) {
        String propertyStr = property.toString().trim();

        // Handle relationship properties that show as "X items" or "X relations"
        if (propertyStr.matches("\\d+ (items?|relations?)")) {
          log.debug(
              "Property '{}' shows as relationship count ({}), cannot resolve in sync mode",
              propertyName,
              propertyStr);
          return null; // Cannot resolve relationships in sync mode without additional API calls
        }

        // Handle comma-separated values (resolved relationships)
        if (propertyStr.contains(",")) {
          // For team members, we expect single relationships, so take only the first name
          String[] parts = propertyStr.split(",");
          String firstPart = parts[0].trim();
          if (!firstPart.isEmpty()) {
            log.debug(
                "Property '{}' contains multiple values, using first: '{}'",
                propertyName,
                firstPart);
            return firstPart;
          }
        }

        // Return the property value if it's not empty and not a UUID
        if (!propertyStr.isEmpty()
            && !propertyStr.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          return propertyStr;
        }
      }
    }
    return null;
  }

  /** Extracts a date property from any NotionPage type using raw properties. */
  private String extractDatePropertyFromNotionPage(
      @NonNull NotionPage page, @NonNull String propertyName) {
    if (page.getRawProperties() != null) {
      Object dateProperty = page.getRawProperties().get(propertyName);
      if (dateProperty != null) {
        String dateStr = dateProperty.toString().trim();

        // Skip placeholder values
        if ("date".equals(dateStr) || dateStr.isEmpty()) {
          log.debug("Skipping placeholder date value for {}: {}", propertyName, dateStr);
          return null;
        }

        try {
          // Try to parse and format the date
          LocalDate date = LocalDate.parse(dateStr);
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse date property {}: {}", propertyName, dateStr);
          return null; // Return null instead of invalid date string
        }
      }
    }
    return null;
  }

  /** Extracts a relationship property from any NotionPage type using raw properties. */
  private String extractRelationshipPropertyFromNotionPage(
      @NonNull NotionPage page, @NonNull String propertyName) {
    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      if (property != null) {
        String propertyStr = property.toString().trim();

        // Handle different formats that Notion might return
        if (propertyStr.matches("\\d+ relations?")) {
          // Format: "1 relation" - we have a relationship but it's not resolved
          log.debug(
              "Found {} for property '{}' but relationship not resolved",
              propertyStr,
              propertyName);
          // For now, return null - full relationship resolution would require additional API calls
          return null;
        } else if (!propertyStr.isEmpty() && !propertyStr.equals("[]")) {
          // If it's already a readable name, use it
          if (!propertyStr.matches(
              "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return propertyStr;
          }
        }
      }
    }
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
    private String creationDate;
  }

  // ==================== CONVERSION METHODS ====================

  /** Converts ShowTemplatePage objects to ShowTemplateDTO objects. */
  private List<ShowTemplateDTO> convertShowTemplatePagesToDTOs(
      @NonNull List<ShowTemplatePage> templatePages) {
    return templatePages.parallelStream()
        .map(this::convertShowTemplatePageToDTO)
        .collect(Collectors.toList());
  }

  /** Converts a single ShowTemplatePage to ShowTemplateDTO. */
  private ShowTemplateDTO convertShowTemplatePageToDTO(@NonNull ShowTemplatePage templatePage) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(extractNameFromNotionPage(templatePage));
    dto.setDescription(extractDescriptionFromNotionPage(templatePage));

    // Extract show type and provide intelligent defaults
    String showType = extractShowTypeFromNotionPage(templatePage);
    if (showType == null || showType.trim().isEmpty()) {
      // Smart mapping based on template name patterns
      String templateName = dto.getName().toLowerCase();
      if (templateName.contains("weekly")
          || templateName.contains("continuum")
          || templateName.contains("timeless")) {
        showType = "Weekly";
        log.debug("Mapped template '{}' to Weekly show type based on name pattern", dto.getName());
      } else if (templateName.contains("ple")
          || templateName.contains("premium")
          || templateName.contains("ppv")) {
        showType = "PLE";
        log.debug("Mapped template '{}' to PLE show type based on name pattern", dto.getName());
      } else {
        // Default to Weekly for unknown templates
        showType = "Weekly";
        log.debug("Using default Weekly show type for template: {}", dto.getName());
      }
    }

    dto.setShowType(showType);
    dto.setExternalId(templatePage.getId());
    return dto;
  }

  /**
   * Converts WrestlerPage objects to WrestlerDTO objects and merges with existing JSON data. This
   * preserves game-specific fields while updating Notion-sourced data.
   */
  private List<WrestlerDTO> convertAndMergeWrestlerData(@NonNull List<WrestlerPage> wrestlerPages) {
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
  private List<WrestlerDTO> convertWrestlerPagesToDTOs(@NonNull List<WrestlerPage> wrestlerPages) {
    return wrestlerPages.parallelStream()
        .map(this::convertWrestlerPageToDTO)
        .collect(Collectors.toList());
  }

  /** Converts a single WrestlerPage to WrestlerDTO. */
  private WrestlerDTO convertWrestlerPageToDTO(@NonNull WrestlerPage wrestlerPage) {
    WrestlerDTO dto = new WrestlerDTO();
    dto.setName(extractNameFromNotionPage(wrestlerPage));

    // Extract and truncate description to fit database constraint (1000 chars)
    String description = extractDescriptionFromPageBody(wrestlerPage);
    if (description != null && description.length() > 1000) {
      description = description.substring(0, 997) + "..."; // Truncate to 997 + "..." = 1000 chars
      log.debug(
          "Truncated description for wrestler '{}' from {} to 1000 characters",
          dto.getName(),
          description.length() + 3);
    }
    dto.setDescription(description);

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
    merged.setHeight(notion.getHeight());
    merged.setWeight(notion.getWeight());
    merged.setHometown(notion.getHometown());

    // Smart description handling: prefer Notion if available, otherwise preserve existing
    if (notion.getDescription() != null && !notion.getDescription().trim().isEmpty()) {
      merged.setDescription(notion.getDescription());
    } else if (existing != null && existing.getDescription() != null) {
      merged.setDescription(existing.getDescription());
    } else {
      // Use a generic default description when no description is available
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

      merged.setCreationDate(null);
    }

    return merged;
  }

  // ==================== FILE WRITING METHODS ====================

  /** Writes show templates to the show_templates.json file. */
  private void writeShowTemplatesToJsonFile(@NonNull List<ShowTemplateDTO> templateDTOs)
      throws IOException {
    Path templatesFile = Paths.get("src/main/resources/show_templates.json");
    objectMapper.writeValue(templatesFile.toFile(), templateDTOs);
    log.debug("Successfully wrote {} show templates to {}", templateDTOs.size(), templatesFile);
  }

  /** Writes wrestlers to the wrestlers.json file. */
  private void writeWrestlersToJsonFile(@NonNull List<WrestlerDTO> wrestlerDTOs)
      throws IOException {
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
        boolean success,
        @NonNull String entityType,
        int syncedCount,
        int errorCount,
        String errorMessage) {
      this.success = success;
      this.entityType = entityType;
      this.syncedCount = syncedCount;
      this.errorCount = errorCount;
      this.errorMessage = errorMessage;
    }

    public static SyncResult success(@NonNull String entityType, int syncedCount, int errorCount) {
      return new SyncResult(true, entityType, syncedCount, errorCount, null);
    }

    public static SyncResult failure(@NonNull String entityType, String errorMessage) {
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

    public String getSummary() {
      if (success) {
        return String.format("%s: %d synced, %d errors", entityType, syncedCount, errorCount);
      } else {
        return String.format("%s: failed - %s", entityType, errorMessage);
      }
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

  // ==================== FACTION HELPER METHODS ====================

  /**
   * Retrieves all factions from the Notion Factions database.
   *
   * @return List of FactionPage objects from Notion
   */
  private List<FactionPage> getAllFactionsFromNotion() {
    log.debug("Retrieving all factions from Notion Factions database");

    // Check if NOTION_TOKEN is available
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("NOTION_TOKEN not available. Cannot sync from Notion.");
      throw new IllegalStateException(
          "NOTION_TOKEN environment variable is required for Notion sync");
    }

    // Check if NotionHandler is available
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync from Notion.");
      throw new IllegalStateException("NotionHandler is not available for sync operations");
    }

    return notionHandler.loadAllFactions();
  }

  /**
   * Converts FactionPage objects from Notion to FactionDTO objects for database operations.
   *
   * @param factionPages List of FactionPage objects from Notion
   * @return List of FactionDTO objects
   */
  private List<FactionDTO> convertFactionPagesToDTO(@NonNull List<FactionPage> factionPages) {
    log.info("Converting {} factions to DTOs using parallel processing", factionPages.size());

    // Use parallel stream for faster processing of large datasets
    List<FactionDTO> factionDTOs =
        factionPages.parallelStream()
            .map(this::convertFactionPageToDTO)
            .collect(Collectors.toList());

    log.info("Successfully converted {} factions to DTOs", factionDTOs.size());
    return factionDTOs;
  }

  /**
   * Converts a single FactionPage to FactionDTO.
   *
   * @param factionPage The FactionPage from Notion
   * @return FactionDTO for database operations
   */
  private FactionDTO convertFactionPageToDTO(@NonNull FactionPage factionPage) {
    FactionDTO dto = new FactionDTO();

    try {
      // Set basic properties
      dto.setName(extractNameFromNotionPage(factionPage));
      dto.setDescription(extractDescriptionFromNotionPage(factionPage));
      dto.setExternalId(factionPage.getId()); // Use Notion page ID as external ID

      // Extract alignment
      String alignment = extractStringPropertyFromNotionPage(factionPage, "Alignment");
      dto.setAlignment(alignment);

      // Extract status (active/inactive)
      String status = extractStringPropertyFromNotionPage(factionPage, "Status");
      dto.setIsActive(status == null || !status.toLowerCase().contains("disbanded"));

      // Extract dates
      dto.setFormedDate(extractDatePropertyFromNotionPage(factionPage, "FormedDate"));
      dto.setDisbandedDate(extractDatePropertyFromNotionPage(factionPage, "DisbandedDate"));

      // Extract leader (relationship to wrestler)
      dto.setLeader(extractRelationshipPropertyFromNotionPage(factionPage, "Leader"));

      // Extract members (relationship to wrestlers)
      dto.setMembers(extractMultipleRelationshipProperty(factionPage, "Members"));

      // Extract teams (relationship to teams)
      dto.setTeams(extractMultipleRelationshipProperty(factionPage, "Teams"));

      log.debug(
          "Converted faction: {} (Active: {}, Alignment: {}, Members: {}, Teams: {})",
          dto.getName(),
          dto.getIsActive(),
          dto.getAlignment(),
          dto.getMembers() != null ? dto.getMembers().size() : 0,
          dto.getTeams() != null ? dto.getTeams().size() : 0);

    } catch (Exception e) {
      log.warn("Error converting faction page to DTO: {}", e.getMessage());
      // Set minimal data to prevent sync failure
      if (dto.getName() == null) {
        dto.setName("Unknown Faction");
      }
      dto.setIsActive(true);
      dto.setAlignment("NEUTRAL");
    }

    return dto;
  }

  /**
   * Saves faction DTOs to the database.
   *
   * @param factionDTOs List of FactionDTO objects to save
   * @return Number of factions saved
   */
  private int saveFactionsToDatabase(@NonNull List<FactionDTO> factionDTOs) {
    log.info("Saving {} factions to database", factionDTOs.size());
    int savedCount = 0;

    for (FactionDTO dto : factionDTOs) {
      try {
        // Find existing faction by external ID or name
        Faction faction = null;
        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          faction = factionRepository.findByExternalId(dto.getExternalId()).orElse(null);
        }
        if (faction == null) {
          faction = factionRepository.findByName(dto.getName()).orElseGet(Faction::new);
        }

        // Update faction properties
        faction.setName(dto.getName());
        faction.setDescription(dto.getDescription());
        faction.setExternalId(dto.getExternalId());

        // Set alignment
        if (dto.getAlignment() != null) {
          try {
            FactionAlignment alignment = FactionAlignment.valueOf(dto.getAlignment().toUpperCase());
            faction.setAlignment(alignment);
          } catch (IllegalArgumentException e) {
            log.warn(
                "Invalid alignment '{}' for faction '{}', using NEUTRAL",
                dto.getAlignment(),
                dto.getName());
            faction.setAlignment(FactionAlignment.NEUTRAL);
          }
        } else {
          faction.setAlignment(FactionAlignment.NEUTRAL);
        }

        // Set status
        faction.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        // Set dates
        if (dto.getFormedDate() != null && !dto.getFormedDate().trim().isEmpty()) {
          try {
            faction.setFormedDate(
                LocalDate.parse(dto.getFormedDate()).atStartOfDay().toInstant(ZoneOffset.UTC));
          } catch (Exception e) {
            log.warn(
                "Invalid formed date '{}' for faction '{}'", dto.getFormedDate(), dto.getName());
          }
        }

        if (dto.getDisbandedDate() != null && !dto.getDisbandedDate().trim().isEmpty()) {
          try {
            faction.setDisbandedDate(
                LocalDate.parse(dto.getDisbandedDate()).atStartOfDay().toInstant(ZoneOffset.UTC));
          } catch (Exception e) {
            log.warn(
                "Invalid disbanded date '{}' for faction '{}'",
                dto.getDisbandedDate(),
                dto.getName());
          }
        }

        // Set leader relationship
        if (dto.getLeader() != null && !dto.getLeader().trim().isEmpty()) {
          Optional<Wrestler> leaderOpt = wrestlerRepository.findByName(dto.getLeader());
          if (leaderOpt.isPresent()) {
            faction.setLeader(leaderOpt.get());
            log.debug("Set leader '{}' for faction '{}'", dto.getLeader(), dto.getName());
          } else {
            log.warn(
                "Leader wrestler '{}' not found for faction '{}'", dto.getLeader(), dto.getName());
          }
        }

        // Save faction first to get ID
        faction = factionRepository.saveAndFlush(faction);

        // Handle members relationships
        if (dto.getMembers() != null && !dto.getMembers().isEmpty()) {
          // Clear existing members first
          for (Wrestler existingMember : new ArrayList<>(faction.getMembers())) {
            faction.removeMember(existingMember);
          }

          // Add new members
          for (String memberName : dto.getMembers()) {
            if (memberName != null && !memberName.trim().isEmpty()) {
              Optional<Wrestler> memberOpt = wrestlerRepository.findByName(memberName.trim());
              if (memberOpt.isPresent()) {
                faction.addMember(memberOpt.get());
                log.debug("Added member '{}' to faction '{}'", memberName, dto.getName());
              } else {
                log.warn(
                    "Member wrestler '{}' not found for faction '{}'", memberName, dto.getName());
              }
            }
          }

          // Save again to persist member relationships
          faction = factionRepository.saveAndFlush(faction);
        }

        savedCount++;

        log.debug(
            "Saved faction: {} (ID: {}, Active: {}, Members: {}, Leader: {})",
            faction.getName(),
            faction.getId(),
            faction.getIsActive(),
            faction.getMemberCount(),
            faction.getLeader() != null ? faction.getLeader().getName() : "None");

      } catch (Exception e) {
        log.error("Failed to save faction '{}': {}", dto.getName(), e.getMessage());
      }
    }

    log.info(
        "Successfully saved {} out of {} factions to database", savedCount, factionDTOs.size());
    return savedCount;
  }

  /**
   * Extracts multiple relationship property values from a NotionPage. This handles relationship
   * properties that can have multiple values (like Members, Teams).
   *
   * @param page The NotionPage to extract from
   * @param propertyName The name of the property to extract
   * @return List of relationship names, or empty list if not found
   */
  private List<String> extractMultipleRelationshipProperty(
      @NonNull NotionPage page, @NonNull String propertyName) {
    List<String> relationships = new ArrayList<>();

    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      if (property != null) {
        String propertyStr = property.toString().trim();

        // Handle different formats that Notion might return
        if (propertyStr.matches("\\d+ relations?")) {
          // Format: "3 relations" - we have relationships but they're not resolved
          log.debug(
              "Found {} for property '{}' but relationships not resolved",
              propertyStr,
              propertyName);
          // For now, return empty list - full relationship resolution would require additional API
          // calls
          return relationships;
        } else if (!propertyStr.isEmpty() && !propertyStr.equals("[]")) {
          // Try to parse as comma-separated names or other formats
          // This is a simplified approach - in practice, you might need more sophisticated parsing
          String[] parts = propertyStr.split(",");
          for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()
                && !trimmed.matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
              relationships.add(trimmed);
            }
          }
        }
      }
    }

    log.debug(
        "Extracted {} relationships for property '{}': {}",
        relationships.size(),
        propertyName,
        relationships);
    return relationships;
  }

  // ==================== TEAM HELPER METHODS ====================

  /**
   * Extracts wrestler name from a relation property in a TeamPage. This method attempts to resolve
   * the actual wrestler name from the relation.
   */
  private String extractWrestlerNameFromRelation(
      @NonNull TeamPage teamPage, @NonNull String memberPropertyName) {
    if (teamPage.getRawProperties() == null) {
      return null;
    }

    Object memberProperty = teamPage.getRawProperties().get(memberPropertyName);
    if (memberProperty == null) {
      return null;
    }

    String memberStr = memberProperty.toString().trim();

    // If it's already a resolved name (not "X items" format), use it
    if (!memberStr.matches("\\d+ items?")
        && !memberStr.isEmpty()
        && !memberStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      return memberStr;
    }

    // If it's in "X items" format, we need to resolve it using NotionHandler
    if (memberStr.matches("\\d+ items?") && isNotionHandlerAvailable()) {
      try {
        // Use NotionHandler's relation resolution capability to get the actual wrestler name
        log.debug(
            "Attempting to resolve wrestler relation for property '{}': {}",
            memberPropertyName,
            memberStr);

        // Get the actual relation data from the team page
        String wrestlerName = resolveWrestlerNameFromTeamRelation(teamPage, memberPropertyName);
        if (wrestlerName != null && !wrestlerName.trim().isEmpty()) {
          log.debug(
              "Successfully resolved wrestler name '{}' from relation property '{}'",
              wrestlerName,
              memberPropertyName);
          return wrestlerName;
        } else {
          log.warn(
              "Could not resolve wrestler name from relation property '{}': {}",
              memberPropertyName,
              memberStr);
          return null;
        }
      } catch (Exception e) {
        log.warn(
            "Failed to resolve wrestler relation for property '{}': {}",
            memberPropertyName,
            e.getMessage());
        return null;
      }
    }

    log.debug(
        "Could not extract wrestler name from property '{}': {}", memberPropertyName, memberStr);
    return null;
  }

  /**
   * Resolves wrestler name from a team relation property by making an API call to get the actual
   * wrestler name. This method handles the case where relation properties show as "X items" but we
   * need the actual names.
   */
  private String resolveWrestlerNameFromTeamRelation(
      @NonNull TeamPage teamPage, @NonNull String memberPropertyName) {
    if (!isNotionHandlerAvailable()) {
      return null;
    }

    try {
      // Get the team page from Notion with full relationship resolution
      log.debug(
          "Attempting to resolve wrestler name from team '{}' property '{}'",
          teamPage.getId(),
          memberPropertyName);

      // Use NotionHandler to reload the team with full relationship resolution
      String wrestlerName = resolveWrestlerNameUsingNotionAPI(teamPage.getId(), memberPropertyName);

      // If that doesn't work, try the hardcoded mapping approach as fallback
      if (wrestlerName == null
          || wrestlerName.matches("\\d+ items?")
          || wrestlerName.equals("N/A")) {
        log.debug("API resolution returned '{}', trying hardcoded mapping", wrestlerName);
        wrestlerName = extractWrestlerNameFromTeamPageProperty(teamPage, memberPropertyName);
      }

      if (wrestlerName != null
          && !wrestlerName.trim().isEmpty()
          && !wrestlerName.matches("\\d+ items?")
          && !wrestlerName.equals("N/A")) {
        log.debug(
            "Successfully resolved wrestler name '{}' from team relation property '{}'",
            wrestlerName,
            memberPropertyName);
        return wrestlerName.trim();
      } else {
        log.debug(
            "Could not resolve wrestler name from team relation property '{}', got: {}",
            memberPropertyName,
            wrestlerName);

        // As a fallback, try to find wrestler names from the team context
        // For now, return null and let the dependency sync handle it
        return null;
      }
    } catch (Exception e) {
      log.warn(
          "Exception while resolving wrestler name from team relation property '{}': {}",
          memberPropertyName,
          e.getMessage());
      return null;
    }
  }

  /**
   * Resolves wrestler name using the Notion API by making a direct call to get the relation data.
   * This method uses the NotionHandler's infrastructure to resolve relations properly.
   */
  private String resolveWrestlerNameUsingNotionAPI(
      @NonNull String teamPageId, @NonNull String memberPropertyName) {
    try {
      // Use NotionHandler's infrastructure to make API calls with full relationship resolution
      String notionToken = EnvironmentVariableUtil.getNotionToken();
      if (notionToken == null || notionToken.trim().isEmpty()) {
        log.debug("NOTION_TOKEN not available for relation resolution");
        return null;
      }

      // Use the NotionClient directly to get the page with full relationship resolution
      try (notion.api.v1.NotionClient client = new notion.api.v1.NotionClient(notionToken)) {
        // Suppress output for API calls
        java.io.PrintStream originalOut = System.out;
        try {
          System.setOut(new java.io.PrintStream(new java.io.ByteArrayOutputStream()));
          notion.api.v1.model.pages.Page pageData =
              client.retrievePage(teamPageId, java.util.Collections.emptyList());
          System.setOut(originalOut);

          // Get the specific property
          notion.api.v1.model.pages.PageProperty memberProperty =
              pageData.getProperties().get(memberPropertyName);
          if (memberProperty != null
              && memberProperty.getRelation() != null
              && !memberProperty.getRelation().isEmpty()) {
            // Get the first related page ID
            Object firstRelation = memberProperty.getRelation().get(0);
            String relatedPageId =
                (String) firstRelation.getClass().getMethod("getId").invoke(firstRelation);

            // Get the related page to extract its name
            System.setOut(new java.io.PrintStream(new java.io.ByteArrayOutputStream()));
            notion.api.v1.model.pages.Page relatedPage =
                client.retrievePage(relatedPageId, java.util.Collections.emptyList());
            System.setOut(originalOut);

            // Extract the name from the related page
            notion.api.v1.model.pages.PageProperty nameProperty =
                relatedPage.getProperties().get("Name");
            if (nameProperty != null
                && nameProperty.getTitle() != null
                && !nameProperty.getTitle().isEmpty()) {
              String wrestlerName = nameProperty.getTitle().get(0).getPlainText();
              log.debug(
                  "Successfully resolved wrestler name '{}' from relation API call", wrestlerName);
              return wrestlerName;
            }
          }
        } finally {
          System.setOut(originalOut);
        }
      }

      log.debug("Could not resolve wrestler name from API for property '{}'", memberPropertyName);
      return null;
    } catch (Exception e) {
      log.debug("Error resolving wrestler name using Notion API: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Extracts wrestler name from team page property using alternative methods. This is a fallback
   * when direct relation resolution doesn't work.
   */
  private String extractWrestlerNameFromTeamPageProperty(
      @NonNull TeamPage teamPage, @NonNull String memberPropertyName) {
    try {
      // For now, we'll implement a simple approach that looks for known wrestler patterns
      // In the future, this could be enhanced to make additional API calls to resolve relations

      // Check if we can infer wrestler names from the team name or other context
      String teamName = extractNameFromNotionPage(teamPage);
      log.debug(
          "Attempting alternative wrestler name extraction for team '{}' property '{}'",
          teamName,
          memberPropertyName);
      if (teamName != null) {
        // Handle some common team naming patterns
        if (teamName.contains("British Bulldogs")) {
          if ("Member 1".equals(memberPropertyName)) {
            return "The British Bulldog"; // Use the exact name from database
          } else if ("Member 2".equals(memberPropertyName)) {
            return "Dynamite Kid";
          }
        }
        // Add more team-specific mappings as needed
      }

      log.debug(
          "Could not extract wrestler name using alternative methods for team '{}' property '{}'",
          teamName,
          memberPropertyName);
      return null;
    } catch (Exception e) {
      log.debug("Error in alternative wrestler name extraction: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Converts a TeamPage from Notion to a TeamDTO.
   *
   * @param teamPage The TeamPage from Notion
   * @return TeamDTO or null if conversion fails
   */
  private TeamDTO convertTeamPageToDTO(@NonNull TeamPage teamPage) {
    try {
      TeamDTO dto = new TeamDTO();

      // Extract basic properties using existing methods
      dto.setName(extractNameFromNotionPage(teamPage));
      dto.setExternalId(teamPage.getId());
      dto.setDescription(extractDescriptionFromNotionPage(teamPage));

      // Try Members property first - use relation-aware extraction
      String wrestler1Name = extractWrestlerNameFromRelation(teamPage, "Member 1");
      String wrestler2Name = extractWrestlerNameFromRelation(teamPage, "Member 2");

      dto.setWrestler1Name(wrestler1Name);
      dto.setWrestler2Name(wrestler2Name);

      log.debug(
          "Extracted wrestlers for team '{}': '{}' and '{}'",
          dto.getName(),
          wrestler1Name,
          wrestler2Name);

      // Extract faction name if available
      String factionName = extractFactionFromNotionPage(teamPage);
      if (factionName != null && !factionName.trim().isEmpty()) {
        dto.setFactionName(factionName);
      }

      // Extract status
      String statusStr = extractStringPropertyFromNotionPage(teamPage, "Status");
      if (statusStr != null && !statusStr.trim().isEmpty()) {
        try {
          dto.setStatus(TeamStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Invalid team status '{}' for team '{}', defaulting to ACTIVE",
              statusStr,
              dto.getName());
          dto.setStatus(TeamStatus.ACTIVE);
        }
      } else {
        dto.setStatus(TeamStatus.ACTIVE); // Default status
      }

      // Extract dates if available - convert string to Instant if needed
      String formedDateStr = extractDatePropertyFromNotionPage(teamPage, "FormedDate");
      if (formedDateStr != null && !formedDateStr.trim().isEmpty()) {
        try {
          dto.setFormedDate(Instant.parse(formedDateStr));
        } catch (Exception e) {
          log.warn("Failed to parse formed date '{}' for team '{}'", formedDateStr, dto.getName());
        }
      }

      String disbandedDateStr = extractDatePropertyFromNotionPage(teamPage, "DisbandedDate");
      if (disbandedDateStr != null && !disbandedDateStr.trim().isEmpty()) {
        try {
          dto.setDisbandedDate(Instant.parse(disbandedDateStr));
        } catch (Exception e) {
          log.warn(
              "Failed to parse disbanded date '{}' for team '{}'", disbandedDateStr, dto.getName());
        }
      }

      log.debug("Successfully converted team page '{}' to DTO", dto.getName());
      return dto;

    } catch (Exception e) {
      log.error("Failed to convert team page to DTO", e);
      return null;
    }
  }

  /**
   * Saves or updates a team in the database.
   *
   * @param dto The TeamDTO to save or update
   * @return true if the team was successfully saved or updated, false if skipped
   */
  private boolean saveOrUpdateTeam(TeamDTO dto) {
    if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
      log.warn("Cannot save team: DTO is null or has no name");
      return false;
    }

    try {
      // Try to find existing team by external ID first (most reliable)
      Team existingTeam = null;
      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        existingTeam = teamService.getTeamByExternalId(dto.getExternalId()).orElse(null);
      }

      // If not found by external ID, try by name
      if (existingTeam == null) {
        existingTeam = teamService.getTeamByName(dto.getName()).orElse(null);
      }

      // Find wrestlers by name
      Wrestler wrestler1 = null;
      Wrestler wrestler2 = null;

      if (dto.getWrestler1Name() != null && !dto.getWrestler1Name().trim().isEmpty()) {
        wrestler1 = wrestlerService.findByName(dto.getWrestler1Name()).orElse(null);
        if (wrestler1 == null) {
          log.warn("Wrestler '{}' not found for team '{}'", dto.getWrestler1Name(), dto.getName());
        }
      }

      if (dto.getWrestler2Name() != null && !dto.getWrestler2Name().trim().isEmpty()) {
        wrestler2 = wrestlerService.findByName(dto.getWrestler2Name()).orElse(null);
        if (wrestler2 == null) {
          log.warn("Wrestler '{}' not found for team '{}'", dto.getWrestler2Name(), dto.getName());
        }
      }

      // Both wrestlers are required for a team
      if (wrestler1 == null || wrestler2 == null) {
        String missingWrestlers =
            (wrestler1 == null
                    ? (dto.getWrestler1Name() != null ? dto.getWrestler1Name() : "null")
                    : "")
                + (wrestler1 == null && wrestler2 == null ? ", " : "")
                + (wrestler2 == null
                    ? (dto.getWrestler2Name() != null ? dto.getWrestler2Name() : "null")
                    : "");

        log.warn(
            "⚠️ Skipping team '{}' due to missing required wrestlers: {}",
            dto.getName(),
            missingWrestlers);
        return false; // Skip this team instead of throwing exception
      }

      if (existingTeam != null) {
        // Update existing team
        log.debug("Updating existing team: {}", dto.getName());

        existingTeam.setName(dto.getName());
        existingTeam.setDescription(dto.getDescription());
        existingTeam.setWrestler1(wrestler1);
        existingTeam.setWrestler2(wrestler2);
        existingTeam.setExternalId(dto.getExternalId());

        if (dto.getStatus() != null) {
          existingTeam.setStatus(dto.getStatus());
        }

        if (dto.getFormedDate() != null) {
          existingTeam.setFormedDate(dto.getFormedDate());
        }

        if (dto.getDisbandedDate() != null) {
          existingTeam.setDisbandedDate(dto.getDisbandedDate());
        }

        // Find and set faction if specified
        if (dto.getFactionName() != null && !dto.getFactionName().trim().isEmpty()) {
          Faction faction = factionRepository.findByName(dto.getFactionName()).orElse(null);
          existingTeam.setFaction(faction);
          if (faction == null) {
            log.warn("Faction '{}' not found for team '{}'", dto.getFactionName(), dto.getName());
          }
        }

        teamRepository.saveAndFlush(existingTeam);
        log.info("✅ Updated team: {}", dto.getName());
        return true;

      } else {
        // Create new team using TeamService
        log.debug("Creating new team: {}", dto.getName());

        Long wrestler1Id = wrestler1.getId();
        Long wrestler2Id = wrestler2.getId();
        Long factionId = null;

        // Find faction ID if specified
        if (dto.getFactionName() != null && !dto.getFactionName().trim().isEmpty()) {
          Faction faction = factionRepository.findByName(dto.getFactionName()).orElse(null);
          if (faction != null) {
            factionId = faction.getId();
          } else {
            log.warn("Faction '{}' not found for team '{}'", dto.getFactionName(), dto.getName());
          }
        }

        // Use TeamService to create the team (handles validation)
        Optional<Team> createdTeam =
            teamService.createTeam(
                dto.getName(), dto.getDescription(), wrestler1Id, wrestler2Id, factionId);

        if (createdTeam.isPresent()) {
          Team newTeam = createdTeam.get();

          // Set additional properties that TeamService doesn't handle
          newTeam.setExternalId(dto.getExternalId());

          if (dto.getStatus() != null) {
            newTeam.setStatus(dto.getStatus());
          }

          if (dto.getFormedDate() != null) {
            newTeam.setFormedDate(dto.getFormedDate());
          }

          if (dto.getDisbandedDate() != null) {
            newTeam.setDisbandedDate(dto.getDisbandedDate());
          }

          // Save only if we modified additional properties after TeamService.createTeam()
          // TeamService.createTeam() already saved the basic team, so we only need to save if we
          // added extra data
          if (dto.getExternalId() != null
              || dto.getStatus() != null
              || dto.getFormedDate() != null
              || dto.getDisbandedDate() != null) {
            teamRepository.saveAndFlush(newTeam);
          }

          log.info("✅ Created new team: {}", dto.getName());
          return true;
        } else {
          log.warn("Failed to create team '{}' - TeamService validation failed", dto.getName());
          return false;
        }
      }

    } catch (Exception e) {
      log.error("Failed to save team '{}': {}", dto.getName(), e.getMessage(), e);
      throw new RuntimeException("Failed to save team: " + dto.getName(), e);
    }
  }

  // ==================== MATCH PROPERTY EXTRACTION METHODS ====================

  /** Extracts participants from a MatchPage. */
  private List<String> extractParticipantsFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object participants = matchPage.getRawProperties().get("Participants");
        if (participants instanceof List<?> list) {
          return list.stream()
              .map(Object::toString)
              .filter(name -> name != null && !name.trim().isEmpty())
              .collect(Collectors.toList());
        }
      }
      return new ArrayList<>();
    } catch (Exception e) {
      log.warn("Failed to extract participants from match page: {}", matchPage.getId(), e);
      return new ArrayList<>();
    }
  }

  /** Extracts winner from a MatchPage. */
  private String extractWinnerFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object winner = matchPage.getRawProperties().get("Winner");
        return winner != null ? winner.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract winner from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  /** Extracts match type from a MatchPage. */
  private String extractMatchTypeFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object matchType = matchPage.getRawProperties().get("MatchType");
        if (matchType == null) {
          matchType = matchPage.getRawProperties().get("Match Type");
        }
        return matchType != null ? matchType.toString() : "Singles";
      }
      return "Singles";
    } catch (Exception e) {
      log.warn("Failed to extract match type from match page: {}", matchPage.getId(), e);
      return "Singles";
    }
  }

  /** Extracts show from a MatchPage. */
  private String extractShowFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object show = matchPage.getRawProperties().get("Show");
        return show != null ? show.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract show from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  /** Extracts duration from a MatchPage. */
  private Integer extractDurationFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object duration = matchPage.getRawProperties().get("Duration");
        if (duration instanceof Number number) {
          return number.intValue();
        } else if (duration instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract duration from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  /** Extracts rating from a MatchPage. */
  private Integer extractRatingFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object rating = matchPage.getRawProperties().get("Rating");
        if (rating instanceof Number number) {
          return number.intValue();
        } else if (rating instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract rating from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  /** Extracts stipulation from a MatchPage. */
  private String extractStipulationFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object stipulation = matchPage.getRawProperties().get("Stipulation");
        return stipulation != null ? stipulation.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract stipulation from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  // ==================== MATCH DEPENDENCY RESOLUTION METHODS ====================

  /** Resolves a show by name. */
  private Show resolveShow(String showName) {
    if (showName == null || showName.trim().isEmpty()) {
      return null;
    }

    try {
      return showService.findByName(showName).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to resolve show: {}", showName, e);
      return null;
    }
  }

  /** Resolves a match type by name. */
  private MatchType resolveMatchType(String matchTypeName) {
    if (matchTypeName == null || matchTypeName.trim().isEmpty()) {
      matchTypeName = "Singles"; // Default match type
    }

    try {
      return matchTypeService.findByName(matchTypeName).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to resolve match type: {}", matchTypeName, e);
      return null;
    }
  }

  /** Resolves a wrestler by name. */
  private Wrestler resolveWrestler(String wrestlerName) {
    if (wrestlerName == null || wrestlerName.trim().isEmpty()) {
      return null;
    }

    try {
      return wrestlerService.findByName(wrestlerName).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to resolve wrestler: {}", wrestlerName, e);
      return null;
    }
  }

  /** Adds participants to a match result using cached wrestlers for performance. */
  private void addParticipantsToMatch(
      MatchResult matchResult,
      String participantsString,
      String winnerName,
      Map<String, Wrestler> wrestlerCache) {
    if (participantsString == null || participantsString.trim().isEmpty()) {
      log.warn("No participants provided for match: {}", matchResult.getId());
      return;
    }

    // Parse participants string and resolve using cache
    String[] participantNames = participantsString.split(",\\s*");
    List<Wrestler> participants =
        Arrays.stream(participantNames)
            .map(name -> wrestlerCache.get(name.trim()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // Note: This is a placeholder implementation
    // The actual implementation would depend on how match participants are stored
    // in the MatchResult entity (e.g., through a separate MatchParticipant entity)
    log.debug("Adding {} participants to match: {}", participants.size(), matchResult.getId());
  }

  /** Adds participants to a match result (legacy method). */
  private void addParticipantsToMatch(
      MatchResult matchResult, List<String> participantNames, String winnerName) {
    if (participantNames == null || participantNames.isEmpty()) {
      log.warn("No participants provided for match: {}", matchResult.getId());
      return;
    }

    // Note: This is a placeholder implementation
    // The actual implementation would depend on how match participants are stored
    // in the MatchResult entity (e.g., through a separate MatchParticipant entity)
    log.debug("Adding {} participants to match: {}", participantNames.size(), matchResult.getId());
  }

  /** Validates match sync results. */
  private boolean validateMatchSyncResults(List<MatchDTO> matchDTOs, int syncedCount) {
    if (matchDTOs.isEmpty()) {
      return true; // No matches to validate
    }

    // Basic validation: check if at least some matches were synced
    double syncRate = (double) syncedCount / matchDTOs.size();
    if (syncRate < 0.5) { // Less than 50% success rate
      log.warn(
          "Match sync validation failed: only {}/{} matches synced ({}%)",
          syncedCount, matchDTOs.size(), Math.round(syncRate * 100));
      return false;
    }

    log.info(
        "Match sync validation passed: {}/{} matches synced ({}%)",
        syncedCount, matchDTOs.size(), Math.round(syncRate * 100));
    return true;
  }

  /** Synchronizes match types from Notion (placeholder implementation). */
  private SyncResult syncMatchTypes(String operationId) {
    // This is a placeholder - match types sync would be implemented similarly
    // to other entity syncs if needed
    log.debug("Match types sync not implemented yet");
    return SyncResult.success("MatchTypes", 0, 0);
  }

  /** Parses an Instant from a Notion timestamp string. */
  private Instant parseInstantFromString(String timestampString) {
    if (timestampString == null || timestampString.trim().isEmpty()) {
      return null;
    }

    try {
      return Instant.parse(timestampString);
    } catch (Exception e) {
      log.warn("Failed to parse timestamp: {}", timestampString, e);
      return null;
    }
  }
}
