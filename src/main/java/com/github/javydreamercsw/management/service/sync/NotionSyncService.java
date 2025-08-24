package com.github.javydreamercsw.management.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
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
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionDTO;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.dto.TeamDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
          operationId, "Sync Shows", 7); // Updated to 7 steps with validation and integrity checks
      progressTracker.updateProgress(operationId, 1, "Initializing sync operation...");
    }

    try {
      // Execute with circuit breaker and retry logic
      return circuitBreakerService.execute(
          "shows",
          () ->
              retryService.executeWithRetry(
                  "shows",
                  (attemptNumber) -> {
                    log.debug("Shows sync attempt {}", attemptNumber);
                    return performShowsSync(operationId, startTime);
                  }));

    } catch (CircuitBreakerService.CircuitBreakerOpenException e) {
      log.warn("❌ Shows sync rejected by circuit breaker: {}", e.getMessage());

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Circuit breaker open: " + e.getMessage());
      }

      healthMonitor.recordFailure("Shows", "Circuit breaker open");
      return SyncResult.failure("Shows", "Service temporarily unavailable - circuit breaker open");

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize shows from Notion after {}ms", totalTime, e);

      // Fail progress tracking
      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Shows", e.getMessage());

      return SyncResult.failure("Shows", e.getMessage());
    }
  }

  /**
   * Performs the actual shows sync operation with enhanced error handling, validation, and
   * transaction management. This method is called by the retry and circuit breaker logic.
   */
  private SyncResult performShowsSync(String operationId, long startTime) throws Exception {
    return syncTransactionManager.executeInTransaction(
        operationId,
        "shows",
        (transaction) -> {
          try {
            // Step 1: Validate sync prerequisites
            log.info("🔍 Validating sync prerequisites...");
            if (operationId != null) {
              progressTracker.updateProgress(operationId, 1, "Validating sync prerequisites...");
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

            // Step 2: Create backup if enabled
            if (syncProperties.isBackupEnabled()) {
              log.info("📦 Creating backup...");
              if (operationId != null) {
                progressTracker.updateProgress(
                    operationId, 2, "Creating backup of existing data...");
                progressTracker.addLogMessage(operationId, "📦 Creating backup...", "INFO");
              }
              createBackup("shows.json");
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
            log.info("✅ Retrieved {} shows from Notion in {}ms", notionShows.size(), retrieveTime);

            if (operationId != null) {
              progressTracker.addLogMessage(
                  operationId,
                  String.format(
                      "✅ Retrieved %d shows from Notion in %dms", notionShows.size(), retrieveTime),
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
              progressTracker.addLogMessage(operationId, "🔄 Converting shows to DTOs...", "INFO");
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
              progressTracker.addLogMessage(operationId, "💾 Saving shows to database...", "INFO");
            }

            // Create savepoint before database operations
            Object savepoint = transaction.createSavepoint("before-database-save");

            try {
              int savedCount = saveShowsToDatabase(showDTOs);

              // Step 7: Perform post-sync data integrity check
              log.info("🔍 Performing post-sync data integrity check...");
              if (operationId != null) {
                progressTracker.updateProgress(operationId, 7, "Checking data integrity...");
                progressTracker.addLogMessage(operationId, "🔍 Checking data integrity...", "INFO");
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
                      "⚠️ Integrity warnings: " + String.join(", ", integrityResult.getWarnings()),
                      "WARNING");
                }
              }

              // Release savepoint - everything is good
              transaction.releaseSavepoint(savepoint);

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

            } catch (Exception dbException) {
              // Rollback to savepoint on database error
              log.error("❌ Database operation failed, rolling back to savepoint", dbException);
              transaction.rollbackToSavepoint(savepoint);
              throw dbException;
            }

          } catch (Exception e) {
            // Re-throw to be handled by retry/circuit breaker logic and transaction manager
            throw new RuntimeException("Shows sync operation failed: " + e.getMessage(), e);
          }
        });
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

      // Record success in health monitor
      healthMonitor.recordSuccess("Show Templates", totalTime, templateDTOs.size());

      return SyncResult.success("Show Templates", templateDTOs.size(), 0);

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
  public SyncResult syncFactions(String operationId) {
    log.info("🏴 Starting factions synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("factions")) {
        log.info("Factions sync is disabled in configuration");
        return SyncResult.success("Factions", 0, 0);
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
    String operationId = null;

    try {
      // Start progress tracking
      SyncProgressTracker.SyncProgress progress =
          progressTracker.startOperation("Teams Sync", "Synchronizing teams from Notion", 0);
      operationId = progress.getOperationId();

      // Load teams from Notion
      progressTracker.addLogMessage(
          operationId, "📥 Loading teams from Notion database...", "INFO");
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
          saveOrUpdateTeam(teamDTO);
          savedCount++;
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
    return saveShowsToDatabaseWithBatching(showDTOs, 50); // Use batch size of 50
  }

  /**
   * Enhanced method to save shows to database with batch processing and error recovery. Processes
   * shows in batches to handle large datasets and provides partial success capability.
   */
  private int saveShowsToDatabaseWithBatching(List<ShowDTO> showDTOs, int batchSize) {
    log.info(
        "Starting database persistence for {} shows with batch size {}",
        showDTOs.size(),
        batchSize);

    // Cache lookups for performance (same as DataInitializer) with null safety
    Map<String, ShowType> showTypes = new HashMap<>();
    Map<String, Season> seasons = new HashMap<>();
    Map<String, ShowTemplate> templates = new HashMap<>();

    // Load reference data with error handling
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
      ShowDTO dto,
      Map<String, ShowType> showTypes,
      Map<String, Season> seasons,
      Map<String, ShowTemplate> templates) {
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

  /** Extracts a string property from any NotionPage type using raw properties. */
  private String extractStringPropertyFromNotionPage(NotionPage page, String propertyName) {
    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      return property != null ? property.toString() : null;
    }
    return null;
  }

  /** Extracts a date property from any NotionPage type using raw properties. */
  private String extractDatePropertyFromNotionPage(NotionPage page, String propertyName) {
    if (page.getRawProperties() != null) {
      Object dateProperty = page.getRawProperties().get(propertyName);
      if (dateProperty != null) {
        try {
          // Try to parse and format the date
          LocalDate date = LocalDate.parse(dateProperty.toString());
          return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse date property {}: {}", propertyName, dateProperty);
          return dateProperty.toString();
        }
      }
    }
    return null;
  }

  /** Extracts a relationship property from any NotionPage type using raw properties. */
  private String extractRelationshipPropertyFromNotionPage(NotionPage page, String propertyName) {
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

    return notionHandler.loadAllFactions();
  }

  /**
   * Converts FactionPage objects from Notion to FactionDTO objects for database operations.
   *
   * @param factionPages List of FactionPage objects from Notion
   * @return List of FactionDTO objects
   */
  private List<FactionDTO> convertFactionPagesToDTO(List<FactionPage> factionPages) {
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
  private FactionDTO convertFactionPageToDTO(FactionPage factionPage) {
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
  private int saveFactionsToDatabase(List<FactionDTO> factionDTOs) {
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
  private List<String> extractMultipleRelationshipProperty(NotionPage page, String propertyName) {
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
   * Converts a TeamPage from Notion to a TeamDTO.
   *
   * @param teamPage The TeamPage from Notion
   * @return TeamDTO or null if conversion fails
   */
  private TeamDTO convertTeamPageToDTO(TeamPage teamPage) {
    if (teamPage == null) {
      return null;
    }

    try {
      TeamDTO dto = new TeamDTO();

      // Extract basic properties using existing methods
      dto.setName(extractNameFromNotionPage(teamPage));
      dto.setExternalId(teamPage.getId());
      dto.setDescription(extractDescriptionFromNotionPage(teamPage));

      // Extract wrestler names from Members property
      String membersStr = extractStringPropertyFromNotionPage(teamPage, "Members");
      if (membersStr != null && !membersStr.trim().isEmpty()) {
        // Parse members - could be comma-separated or other format
        String[] members = membersStr.split(",");
        if (members.length >= 1) {
          dto.setWrestler1Name(members[0].trim());
        }
        if (members.length >= 2) {
          dto.setWrestler2Name(members[1].trim());
        }
      }

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
   */
  private void saveOrUpdateTeam(TeamDTO dto) {
    if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
      log.warn("Cannot save team: DTO is null or has no name");
      return;
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
            (wrestler1 == null ? dto.getWrestler1Name() : "")
                + (wrestler1 == null && wrestler2 == null ? ", " : "")
                + (wrestler2 == null ? dto.getWrestler2Name() : "");
        throw new RuntimeException(
            "Cannot save team '"
                + dto.getName()
                + "': Missing required wrestlers: "
                + missingWrestlers);
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

          teamRepository.saveAndFlush(newTeam);
          log.info("✅ Created new team: {}", dto.getName());
        } else {
          log.warn("Failed to create team '{}' - TeamService validation failed", dto.getName());
        }
      }

    } catch (Exception e) {
      log.error("Failed to save team '{}': {}", dto.getName(), e.getMessage(), e);
      throw new RuntimeException("Failed to save team: " + dto.getName(), e);
    }
  }
}
