package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.util.NotionBlocksRetriever;
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

  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;

  public WrestlerSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(@NonNull String operationId) {
    // Check if already synced in current session
    if (isAlreadySyncedInSession("wrestlers")) {
      log.info("⏭️ Wrestlers already synced in current session, skipping");
      return SyncResult.success("Wrestlers", 0, 0);
    }

    log.info("🤼 Starting wrestlers synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performWrestlersSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("wrestlers");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync wrestlers", e);
      return SyncResult.failure("Wrestlers", e.getMessage());
    }
  }

  private SyncResult performWrestlersSync(@NonNull String operationId, long startTime) {
    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("wrestlers")) {
        log.info("Wrestlers sync is disabled in configuration");
        return SyncResult.success("Wrestlers", 0, 0);
      }

      // Initialize progress tracking
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Wrestlers", 3);
        progressTracker.updateProgress(operationId, 1, "Retrieving wrestlers from Notion...");
      }

      // Retrieve wrestlers from Notion
      log.info("📥 Retrieving wrestlers from Notion...");
      long retrieveStart = System.currentTimeMillis();

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

  /** Converts WrestlerPage objects to WrestlerDTO objects and merges with existing JSON data. */
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

  /** Converts a single WrestlerPage to WrestlerDTO. */
  private WrestlerDTO convertWrestlerPageToDTO(@NonNull WrestlerPage wrestlerPage) {
    WrestlerDTO dto = new WrestlerDTO();
    dto.setName(extractNameFromNotionPage(wrestlerPage));

    // Extract and truncate description to fit database constraint (1000 chars)
    String description = extractDescriptionFromPageBody(wrestlerPage);
    if (description != null && description.length() > 1000) {
      description = description.substring(0, 997) + "...";
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

  /** Extracts description from the page body/content using NotionBlocksRetriever. */
  private String extractDescriptionFromPageBody(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page) {
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
    merged.setHeight(notion.getHeight());
    merged.setWeight(notion.getWeight());
    merged.setHometown(notion.getHometown());

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
        Wrestler wrestler = null;
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
          wrestler = new Wrestler();
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

  /** DTO for Wrestler data from Notion. */
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

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getHeight() {
      return height;
    }

    public void setHeight(String height) {
      this.height = height;
    }

    public String getWeight() {
      return weight;
    }

    public void setWeight(String weight) {
      this.weight = weight;
    }

    public String getHometown() {
      return hometown;
    }

    public void setHometown(String hometown) {
      this.hometown = hometown;
    }

    public String getExternalId() {
      return externalId;
    }

    public void setExternalId(String externalId) {
      this.externalId = externalId;
    }

    public Integer getDeckSize() {
      return deckSize;
    }

    public void setDeckSize(Integer deckSize) {
      this.deckSize = deckSize;
    }

    public Integer getStartingHealth() {
      return startingHealth;
    }

    public void setStartingHealth(Integer startingHealth) {
      this.startingHealth = startingHealth;
    }

    public Integer getLowHealth() {
      return lowHealth;
    }

    public void setLowHealth(Integer lowHealth) {
      this.lowHealth = lowHealth;
    }

    public Integer getStartingStamina() {
      return startingStamina;
    }

    public void setStartingStamina(Integer startingStamina) {
      this.startingStamina = startingStamina;
    }

    public Integer getLowStamina() {
      return lowStamina;
    }

    public void setLowStamina(Integer lowStamina) {
      this.lowStamina = lowStamina;
    }

    public Long getFans() {
      return fans;
    }

    public void setFans(Long fans) {
      this.fans = fans;
    }

    public Boolean getIsPlayer() {
      return isPlayer;
    }

    public void setIsPlayer(Boolean isPlayer) {
      this.isPlayer = isPlayer;
    }

    public Integer getBumps() {
      return bumps;
    }

    public void setBumps(Integer bumps) {
      this.bumps = bumps;
    }

    public String getFaction() {
      return faction;
    }

    public void setFaction(String faction) {
      this.faction = faction;
    }

    public String getCreationDate() {
      return creationDate;
    }

    public void setCreationDate(String creationDate) {
      this.creationDate = creationDate;
    }
  }
}
