package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionDTO;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing factions from Notion to the database. */
@Service
@Slf4j
public class FactionSyncService extends BaseSyncService {

  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  public FactionSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes factions from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  @Transactional
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
      if (!validateNotionToken("Factions")) {
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
          "Converted faction: {} (Active: {}, Members: {}, Teams: {})",
          dto.getName(),
          dto.getIsActive(),
          dto.getMembers() != null ? dto.getMembers().size() : 0,
          dto.getTeams() != null ? dto.getTeams().size() : 0);

    } catch (Exception e) {
      log.warn("Error converting faction page to DTO: {}", e.getMessage());
      // Set minimal data to prevent sync failure
      if (dto.getName() == null) {
        dto.setName("Unknown Faction");
      }
      dto.setIsActive(true);
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
        faction = factionRepository.save(faction);

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
          faction = factionRepository.save(faction);
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
          return null;
        }

        // Handle comma-separated values (resolved relationships)
        if (propertyStr.contains(",")) {
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
          return date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
          log.warn("Failed to parse date property {}: {}", propertyName, dateStr);
          return null;
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
          log.debug(
              "Found {} for property '{}' but relationship not resolved",
              propertyStr,
              propertyName);
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

  /** Extracts multiple relationship property values from a NotionPage. */
  private List<String> extractMultipleRelationshipProperty(
      @NonNull NotionPage page, @NonNull String propertyName) {
    log.info("Extracting multiple relationship property: {}", propertyName);
    List<String> relationships = new ArrayList<>();

    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      log.info("Raw property for '{}': {}", propertyName, property);
      if (property != null) {
        String propertyStr = property.toString().trim();

        // Handle different formats that Notion might return
        if (propertyStr.matches("\\d+ relations?")) {
          log.debug(
              "Found {} for property '{}' but relationships not resolved",
              propertyStr,
              propertyName);
          return relationships;
        } else if (!propertyStr.isEmpty() && !propertyStr.equals("[]")) {
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
}
