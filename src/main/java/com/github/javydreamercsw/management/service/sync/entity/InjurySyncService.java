package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.dto.InjuryDTO;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing injuries from Notion to the database. */
@Service
@Slf4j
public class InjurySyncService extends BaseSyncService {

  @Autowired private InjuryTypeService injuryTypeService;
  @Autowired private InjuryTypeRepository injuryTypeRepository;
  @Autowired private InjuryService injuryService;
  @Autowired private InjuryRepository injuryRepository;

  public InjurySyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes injury types from Notion Injuries database to the local database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncInjuryTypes(@NonNull String operationId) {
    // Check if already synced in current session
    if (isAlreadySyncedInSession("injury-types")) {
      log.info("⏭️ Injury types already synced in current session, skipping");
      return SyncResult.success("Injuries", 0, 0);
    }

    if (!syncProperties.isEntityEnabled("injuries")) {
      log.debug("Injuries synchronization is disabled in configuration");
      return SyncResult.success("Injuries", 0, 0);
    }

    try {
      SyncResult result = performInjuryTypesSync(operationId);
      if (result.isSuccess()) {
        markAsSyncedInSession("injury-types");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync injury types", e);
      return SyncResult.failure("Injuries", e.getMessage());
    }
  }

  /**
   * Internal method to perform the actual injury types sync logic.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  private SyncResult performInjuryTypesSync(@NonNull String operationId) {
    log.info("🏥 Starting injuries synchronization from Notion with operation ID: {}", operationId);
    long startTime = System.currentTimeMillis();

    try {
      // Check if NOTION_TOKEN is available
      if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
        String errorMsg = "NOTION_TOKEN is not available for injuries sync";
        log.error(errorMsg);
        return SyncResult.failure("Injuries", errorMsg);
      }

      // Perform the actual sync
      return performInjuriesSync(operationId, startTime);

    } catch (Exception e) {
      log.error("Failed to sync injuries from Notion", e);
      return SyncResult.failure("Injuries", "Failed to sync injuries: " + e.getMessage());
    }
  }

  /**
   * Performs the actual injuries synchronization from Notion to database.
   *
   * @param operationId Operation ID for progress tracking
   * @param startTime Start time for performance tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  private SyncResult performInjuriesSync(@NonNull String operationId, long startTime) {
    try {
      // Initialize progress tracking
      progressTracker.startOperation(operationId, "Sync Injuries", 4);
      progressTracker.updateProgress(operationId, 1, "Loading injuries from Notion...");

      // Load injuries from Notion
      List<InjuryPage> injuryPages = loadInjuriesFromNotion();
      log.info("📥 Loaded {} injuries from Notion", injuryPages.size());

      if (injuryPages.isEmpty()) {
        log.info("No injuries found in Notion database");
        progressTracker.completeOperation(operationId, true, "No injuries to sync", 0);
        return SyncResult.success("Injuries", 0, 0);
      }

      // Convert to DTOs with parallel processing
      progressTracker.updateProgress(operationId, 2, "Converting injuries to DTOs...");
      List<InjuryDTO> injuryDTOs = convertInjuriesToDTOs(injuryPages);
      log.info("🔄 Converted {} injuries to DTOs", injuryDTOs.size());

      // Save to database with parallel processing and caching
      progressTracker.updateProgress(operationId, 3, "Saving injuries to database...");
      int syncedCount = saveInjuriesToDatabase(injuryDTOs);
      log.info("💾 Saved {} injuries to database", syncedCount);

      // Validate sync results
      progressTracker.updateProgress(operationId, 4, "Validating injury sync results...");
      boolean validationPassed = validateInjurySyncResults(injuryDTOs, syncedCount);

      if (!validationPassed) {
        return SyncResult.failure("Injuries", "Injury sync validation failed");
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Injuries sync completed successfully in {}ms", totalTime);

      progressTracker.completeOperation(
          operationId, true, "Injuries sync completed successfully", syncedCount);

      // Record success in health monitor
      healthMonitor.recordSuccess("Injuries", totalTime, syncedCount);

      return SyncResult.success("Injuries", syncedCount, 0);

    } catch (Exception e) {
      log.error("Failed to perform injuries sync", e);
      progressTracker.failOperation(operationId, "Failed to sync injuries: " + e.getMessage());
      healthMonitor.recordFailure("Injuries", e.getMessage());
      return SyncResult.failure("Injuries", "Failed to sync injuries: " + e.getMessage());
    }
  }

  /**
   * Loads all injuries from Notion database.
   *
   * @return List of InjuryPage objects from Notion
   */
  private List<InjuryPage> loadInjuriesFromNotion() {
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot load injuries from Notion.");
      throw new RuntimeException("NotionHandler is not available for sync operations");
    }

    try {
      return notionHandler.loadAllInjuries();
    } catch (Exception e) {
      log.error("Failed to load injuries from Notion", e);
      throw new RuntimeException("Failed to load injuries from Notion: " + e.getMessage(), e);
    }
  }

  /**
   * Converts injury pages to DTOs using parallel processing.
   *
   * @param injuryPages List of InjuryPage objects from Notion
   * @return List of InjuryDTO objects
   */
  private List<InjuryDTO> convertInjuriesToDTOs(List<InjuryPage> injuryPages) {
    log.info("🔄 Converting {} injury pages to DTOs using parallel processing", injuryPages.size());

    return injuryPages.parallelStream()
        .map(this::convertInjuryPageToDTO)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Converts a single InjuryPage to InjuryDTO.
   *
   * @param injuryPage The InjuryPage to convert
   * @return InjuryDTO object or null if conversion fails
   */
  private InjuryDTO convertInjuryPageToDTO(InjuryPage injuryPage) {
    try {
      // Extract basic properties first
      String externalId = injuryPage.getId();
      String injuryName = extractNameFromNotionPage(injuryPage);

      // Create DTO with required constructor parameters
      InjuryDTO dto = new InjuryDTO(externalId, injuryName);

      // Extract injury-specific properties
      dto.setHealthEffect(extractHealthEffectFromInjuryPage(injuryPage));
      dto.setStaminaEffect(extractStaminaEffectFromInjuryPage(injuryPage));
      dto.setCardEffect(extractCardEffectFromInjuryPage(injuryPage));
      dto.setSpecialEffects(extractSpecialEffectsFromInjuryPage(injuryPage));

      // Extract timestamps
      dto.setCreatedTime(parseInstantFromString(injuryPage.getCreated_time()));
      dto.setLastEditedTime(parseInstantFromString(injuryPage.getLast_edited_time()));
      dto.setCreatedBy(
          injuryPage.getCreated_by() != null ? injuryPage.getCreated_by().getName() : null);
      dto.setLastEditedBy(
          injuryPage.getLast_edited_by() != null ? injuryPage.getLast_edited_by().getName() : null);

      return dto;

    } catch (Exception e) {
      log.error("Failed to convert injury page to DTO: {}", injuryPage.getId(), e);
      return null;
    }
  }

  /**
   * Saves injury DTOs to the database.
   *
   * @param injuryDTOs List of InjuryDTO objects to save
   * @return Number of injuries successfully saved
   */
  private int saveInjuriesToDatabase(List<InjuryDTO> injuryDTOs) {
    log.info("🚀 Starting processing of {} injuries", injuryDTOs.size());

    // Log all injury names for debugging
    log.debug(
        "📋 Injuries to process: {}",
        injuryDTOs.stream().map(InjuryDTO::getInjuryName).collect(Collectors.toList()));

    // Filter and process valid injuries
    List<InjuryType> createdInjuryTypes =
        injuryDTOs.parallelStream()
            .filter(
                dto -> {
                  log.debug("🔍 Processing injury DTO: {}", dto.getSummary());

                  if (!dto.isValid()) {
                    log.warn("❌ Skipping invalid injury DTO: {}", dto.getSummary());
                    return false;
                  }

                  // Check if injury type already exists
                  boolean exists = injuryTypeRepository.existsByInjuryName(dto.getInjuryName());
                  if (exists) {
                    log.warn("⚠️ Injury type already exists, skipping: {}", dto.getInjuryName());
                    return false;
                  }

                  log.debug("✅ Injury type '{}' does not exist, will create", dto.getInjuryName());
                  return true;
                })
            .map(
                dto -> {
                  try {
                    log.debug("🏗️ Creating injury type from DTO: {}", dto.getInjuryName());
                    InjuryType injuryType = createInjuryTypeFromDTO(dto);
                    if (injuryType != null) {
                      log.debug(
                          "✅ Created injury type: {} with ID: {}",
                          dto.getInjuryName(),
                          injuryType.getId());
                      return injuryType;
                    } else {
                      log.warn(
                          "❌ Failed to create injury type (returned null): {}",
                          dto.getInjuryName());
                      return null;
                    }
                  } catch (Exception e) {
                    log.error(
                        "❌ Failed to save injury '{}': {}", dto.getInjuryName(), e.getMessage(), e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    log.info(
        "✅ Processing completed: {} injury types created successfully", createdInjuryTypes.size());
    return createdInjuryTypes.size();
  }

  /**
   * Creates an InjuryType entity from an InjuryDTO.
   *
   * @param dto The InjuryDTO to convert
   * @return Created InjuryType or null if creation fails
   */
  private InjuryType createInjuryTypeFromDTO(InjuryDTO dto) {
    try {
      log.debug(
          "Attempting to create injury type: {} with effects H:{}, S:{}, C:{}",
          dto.getInjuryName(),
          dto.getHealthEffect(),
          dto.getStaminaEffect(),
          dto.getCardEffect());

      // Double-check if injury type already exists to avoid race conditions
      if (injuryTypeRepository.existsByInjuryName(dto.getInjuryName())) {
        log.warn("Injury type '{}' already exists during creation, skipping", dto.getInjuryName());
        return null;
      }

      // Create injury type using the service's create method
      InjuryType injuryType =
          injuryTypeService.createInjuryType(
              dto.getInjuryName(),
              dto.getHealthEffect(),
              dto.getStaminaEffect(),
              dto.getCardEffect(),
              dto.getSpecialEffects());

      log.debug("Successfully created injury type with ID: {}", injuryType.getId());

      // Set external ID for tracking
      injuryType.setExternalId(dto.getExternalId());

      // Update the entity to save the external ID
      InjuryType updated = injuryTypeService.updateInjuryType(injuryType);
      log.debug("Updated injury type with external ID: {}", dto.getExternalId());

      return updated;

    } catch (IllegalArgumentException e) {
      // Handle duplicate name exception gracefully
      log.info("Injury type '{}' already exists: {}", dto.getInjuryName(), e.getMessage());
      return null;
    } catch (Exception e) {
      log.error("Failed to create injury type '{}': {}", dto.getInjuryName(), e.getMessage(), e);
      return null;
    }
  }

  /** Validates injury sync results. */
  private boolean validateInjurySyncResults(List<InjuryDTO> injuryDTOs, int syncedCount) {
    if (injuryDTOs.isEmpty()) {
      return true; // No injuries to validate
    }

    // Basic validation: check if at least some injuries were synced
    double syncRate = (double) syncedCount / injuryDTOs.size();
    if (syncRate < 0.5) { // Less than 50% success rate
      log.warn(
          "Injury sync validation failed: only {}/{} injuries synced ({}%)",
          syncedCount, injuryDTOs.size(), Math.round(syncRate * 100));
      return false;
    }

    log.info(
        "Injury sync validation passed: {}/{} injuries synced ({}%)",
        syncedCount, injuryDTOs.size(), Math.round(syncRate * 100));
    return true;
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

  // Injury property extraction methods
  private String extractSeverityFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object severity = injuryPage.getRawProperties().get("Severity");
        return severity != null ? severity.toString() : "MINOR";
      }
      return "MINOR";
    } catch (Exception e) {
      log.warn("Failed to extract severity from injury page: {}", injuryPage.getId(), e);
      return "MINOR";
    }
  }

  private Integer extractRecoveryTimeFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object recoveryTime = injuryPage.getRawProperties().get("Recovery Time");
        if (recoveryTime instanceof Number number) {
          return number.intValue();
        } else if (recoveryTime instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return 1;
    } catch (Exception e) {
      log.warn("Failed to extract recovery time from injury page: {}", injuryPage.getId(), e);
      return 1;
    }
  }

  private String extractBodyPartFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object bodyPart = injuryPage.getRawProperties().get("Body Part");
        return bodyPart != null ? bodyPart.toString() : "General";
      }
      return "General";
    } catch (Exception e) {
      log.warn("Failed to extract body part from injury page: {}", injuryPage.getId(), e);
      return "General";
    }
  }

  private Integer extractHealthEffectFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object healthEffect = injuryPage.getRawProperties().get("Health Effect");
        if (healthEffect instanceof Number number) {
          return number.intValue();
        } else if (healthEffect instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return 0;
    } catch (Exception e) {
      log.warn("Failed to extract health effect from injury page: {}", injuryPage.getId(), e);
      return 0;
    }
  }

  private Integer extractStaminaEffectFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object staminaEffect = injuryPage.getRawProperties().get("Stamina Effect");
        if (staminaEffect instanceof Number number) {
          return number.intValue();
        } else if (staminaEffect instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return 0;
    } catch (Exception e) {
      log.warn("Failed to extract stamina effect from injury page: {}", injuryPage.getId(), e);
      return 0;
    }
  }

  private Integer extractCardEffectFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object cardEffect = injuryPage.getRawProperties().get("Card Effect");
        if (cardEffect instanceof Number number) {
          return number.intValue();
        } else if (cardEffect instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return 0;
    } catch (Exception e) {
      log.warn("Failed to extract card effect from injury page: {}", injuryPage.getId(), e);
      return 0;
    }
  }

  private String extractSpecialEffectsFromInjuryPage(InjuryPage injuryPage) {
    try {
      if (injuryPage.getRawProperties() != null) {
        Object specialEffects = injuryPage.getRawProperties().get("Special Effects");
        return specialEffects != null ? specialEffects.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract special effects from injury page: {}", injuryPage.getId(), e);
      return null;
    }
  }
}
