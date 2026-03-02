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
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

  @Autowired @Lazy protected WrestlerSyncService self;

  protected WrestlerSyncService getSelf() {
    return self != null ? self : this;
  }

  public void setSelf(WrestlerSyncService self) {
    this.self = self;
  }

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
      InjuryRepository injuryRepository) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerNotionSyncService = wrestlerNotionSyncService;
    this.tierRecalculationService = tierRecalculationService;
    this.wrestlerAlignmentRepository = wrestlerAlignmentRepository;
    this.factionRepository = factionRepository;
    this.npcRepository = npcRepository;
    this.injuryRepository = injuryRepository;
    this.self = this;
  }

  public SyncResult syncWrestlers(@NonNull String operationId) {
    log.info(
        "🤼 Starting wrestlers synchronization from Notion with operation ID: {}", operationId);
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Wrestlers Sync", 4);

    if (!syncServiceDependencies.getNotionSyncProperties().isEnabled()
        || !syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("wrestlers")) {
      log.debug("Wrestlers synchronization is disabled, skipping.");
      return SyncResult.success("Wrestlers", 0, 0, 0);
    }

    try {
      // Step 1: Load all wrestlers from Notion
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Loading wrestlers from Notion...");
      log.info("📥 Loading wrestlers from Notion...");
      long notionStart = System.currentTimeMillis();
      List<WrestlerPage> wrestlerPages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllWrestlers());
      log.info(
          "✅ Retrieved {} wrestlers from Notion in {}ms",
          wrestlerPages.size(),
          System.currentTimeMillis() - notionStart);

      // Step 2: Convert to DTOs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Processing Notion data...");
      log.info("⚙️ Processing Notion data...");
      List<WrestlerDTO> wrestlerDTOs = new ArrayList<>();
      for (WrestlerPage page : wrestlerPages) {
        wrestlerDTOs.add(convertWrestlerPageToDTO(page));
      }

      // Step 3: Save to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d wrestlers to database...", wrestlerDTOs.size()));
      log.info("🗄️ Saving wrestlers to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = 0;
      int processedItems = 0;
      for (WrestlerDTO dto : wrestlerDTOs) {
        processedItems++;
        if (processedItems % 5 == 0) {
          syncServiceDependencies
              .getProgressTracker()
              .updateProgress(
                  operationId,
                  4,
                  String.format(
                      "Saving wrestlers to database... (%d/%d processed)",
                      processedItems, wrestlerDTOs.size()));
        }
        if (getSelf().processSingleWrestler(dto)) {
          savedCount++;
        }
      }
      log.info(
          "✅ Saved {} wrestlers to database in {}ms",
          savedCount,
          System.currentTimeMillis() - dbStart);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synchronized %d wrestlers", savedCount),
              savedCount);

      return SyncResult.success("Wrestlers", savedCount, 0, 0);

    } catch (Exception e) {
      String errorMessage = "Failed to synchronize wrestlers from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Wrestlers", errorMessage);
    }
  }

  private WrestlerDTO convertWrestlerPageToDTO(@NonNull WrestlerPage wrestlerPage) {
    WrestlerDTO dto = new WrestlerDTO();
    dto.setExternalId(wrestlerPage.getId());

    Map<String, Object> rawProperties = wrestlerPage.getRawProperties();

    // Extract Name
    dto.setName(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractNameFromNotionPage(wrestlerPage));

    // Extract Basic Fields
    dto.setGender((String) rawProperties.get("Sex"));
    dto.setTier((String) rawProperties.get("Tier"));
    dto.setAlignment((String) rawProperties.get("Alignment"));

    // Extract Numbers
    Object deckSizeObj = rawProperties.get("Deck Size");
    if (deckSizeObj instanceof Double) dto.setDeckSize(((Double) deckSizeObj).intValue());

    Object startingHealthObj = rawProperties.get("Starting Health");
    if (startingHealthObj instanceof Double)
      dto.setStartingHealth(((Double) startingHealthObj).intValue());

    Object lowHealthObj = rawProperties.get("Low Health");
    if (lowHealthObj instanceof Double) dto.setLowHealth(((Double) lowHealthObj).intValue());

    Object startingStaminaObj = rawProperties.get("Starting Stamina");
    if (startingStaminaObj instanceof Double)
      dto.setStartingStamina(((Double) startingStaminaObj).intValue());

    Object lowStaminaObj = rawProperties.get("Low Stamina");
    if (lowStaminaObj instanceof Double) dto.setLowStamina(((Double) lowStaminaObj).intValue());

    Object driveObj = rawProperties.get("Drive");
    if (driveObj instanceof Double) dto.setDrive(((Double) driveObj).intValue());

    Object resilienceObj = rawProperties.get("Resilience");
    if (resilienceObj instanceof Double) dto.setResilience(((Double) resilienceObj).intValue());

    Object charismaObj = rawProperties.get("Charisma");
    if (charismaObj instanceof Double) dto.setCharisma(((Double) charismaObj).intValue());

    Object brawlObj = rawProperties.get("Brawl");
    if (brawlObj instanceof Double) dto.setBrawl(((Double) brawlObj).intValue());

    Object fansObj = rawProperties.get("Fans");
    if (fansObj instanceof Double) dto.setFans(((Double) fansObj).longValue());

    Object bumpsObj = rawProperties.get("Bumps");
    if (bumpsObj instanceof Double) dto.setBumps(((Double) bumpsObj).intValue());

    Object isPlayerObj = rawProperties.get("Is Player");
    dto.setIsPlayer(Boolean.TRUE.equals(isPlayerObj));

    // Extract CreationDate
    Object creationDateObj = rawProperties.get("Created");
    if (creationDateObj instanceof String) {
      dto.setCreationDate((String) creationDateObj);
    }

    // Extract Relations
    dto.setManagerExternalId(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationId(wrestlerPage, "Manager"));

    dto.setInjuryExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(wrestlerPage, "Injuries"));

    dto.setTeamExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(wrestlerPage, "Teams"));

    dto.setTitleReignExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(wrestlerPage, "Title Reigns"));

    dto.setFaction((String) rawProperties.get("Faction"));
    dto.setHeritageTag((String) rawProperties.get("Heritage Tag"));

    // Description/Narration (from page content)
    dto.setDescription(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDescriptionFromNotionPage(wrestlerPage));

    return dto;
  }

  /**
   * Merges DTO data into an existing entity.
   *
   * @param existing Existing WrestlerDTO
   * @param notion DTO from Notion
   * @return Merged DTO
   */
  private WrestlerDTO mergeWrestlerData(WrestlerDTO existing, @NonNull WrestlerDTO notion) {
    WrestlerDTO merged = new WrestlerDTO();

    // Use notion values if present, otherwise existing
    merged.setName(
        notion.getName() != null && !notion.getName().isBlank()
            ? notion.getName()
            : existing.getName());

    merged.setGender(
        notion.getGender() != null
            ? notion.getGender()
            : (existing != null ? existing.getGender() : null));

    merged.setTier(
        notion.getTier() != null
            ? notion.getTier()
            : (existing != null ? existing.getTier() : null));

    merged.setAlignment(
        notion.getAlignment() != null
            ? notion.getAlignment()
            : (existing != null ? existing.getAlignment() : null));

    merged.setDescription(
        notion.getDescription() != null && !notion.getDescription().isBlank()
            ? notion.getDescription()
            : (existing != null ? existing.getDescription() : null));

    // Numerical game fields
    if (notion.getDeckSize() != null) {
      merged.setDeckSize(notion.getDeckSize());
    } else if (existing != null && existing.getDeckSize() != null) {
      merged.setDeckSize(existing.getDeckSize());
    } else {
      merged.setDeckSize(15);
    }

    if (notion.getStartingHealth() != null) {
      merged.setStartingHealth(notion.getStartingHealth());
    } else if (existing != null && existing.getStartingHealth() != null) {
      merged.setStartingHealth(existing.getStartingHealth());
    } else {
      merged.setStartingHealth(0);
    }

    if (notion.getLowHealth() != null) {
      merged.setLowHealth(notion.getLowHealth());
    } else if (existing != null && existing.getLowHealth() != null) {
      merged.setLowHealth(existing.getLowHealth());
    } else {
      merged.setLowHealth(0);
    }

    if (notion.getStartingStamina() != null) {
      merged.setStartingStamina(notion.getStartingStamina());
    } else if (existing != null && existing.getStartingStamina() != null) {
      merged.setStartingStamina(existing.getStartingStamina());
    } else {
      merged.setStartingStamina(0);
    }

    if (notion.getLowStamina() != null) {
      merged.setLowStamina(notion.getLowStamina());
    } else if (existing != null && existing.getLowStamina() != null) {
      merged.setLowStamina(existing.getLowStamina());
    } else {
      merged.setLowStamina(0);
    }

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

    if (notion.getBumps() != null) {
      merged.setBumps(notion.getBumps());
    } else if (existing != null && existing.getBumps() != null) {
      merged.setBumps(existing.getBumps());
    } else {
      merged.setBumps(0);
    }

    // Campaign fields
    if (notion.getDrive() != null) {
      merged.setDrive(notion.getDrive());
    } else if (existing != null && existing.getDrive() != null) {
      merged.setDrive(existing.getDrive());
    } else {
      merged.setDrive(1);
    }

    if (notion.getResilience() != null) {
      merged.setResilience(notion.getResilience());
    } else if (existing != null && existing.getResilience() != null) {
      merged.setResilience(existing.getResilience());
    } else {
      merged.setResilience(1);
    }

    if (notion.getCharisma() != null) {
      merged.setCharisma(notion.getCharisma());
    } else if (existing != null && existing.getCharisma() != null) {
      merged.setCharisma(existing.getCharisma());
    } else {
      merged.setCharisma(1);
    }

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

  /** Saves a single wrestler DTO to the database. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleWrestler(@NonNull WrestlerDTO dto) {
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

      // Set basic properties (only update if changed)
      boolean changed = false;
      if (!Objects.equals(wrestler.getName(), dto.getName())) {
        wrestler.setName(dto.getName());
        changed = true;
      }
      if (!Objects.equals(wrestler.getExternalId(), dto.getExternalId())) {
        wrestler.setExternalId(dto.getExternalId());
        changed = true;
      }

      if (dto.getGender() != null && !dto.getGender().isBlank()) {
        try {
          Gender newGender = Gender.valueOf(dto.getGender().toUpperCase());
          if (!Objects.equals(wrestler.getGender(), newGender)) {
            wrestler.setGender(newGender);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn("Invalid sex value '{}' for wrestler '{}'", dto.getGender(), dto.getName());
        }
      }

      if (dto.getTier() != null && !dto.getTier().isBlank()) {
        try {
          WrestlerTier newTier = WrestlerTier.fromDisplayName(dto.getTier());
          if (!Objects.equals(wrestler.getTier(), newTier)) {
            wrestler.setTier(newTier);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn("Invalid tier value '{}' for wrestler '{}'", dto.getTier(), dto.getName());
        }
      }

      // Update description if provided
      if (dto.getDescription() != null
          && !dto.getDescription().trim().isEmpty()
          && !Objects.equals(wrestler.getDescription(), dto.getDescription())) {
        wrestler.setDescription(dto.getDescription());
        changed = true;
        log.debug(
            "Updated description for wrestler {}: {}",
            dto.getName(),
            dto.getDescription().substring(0, Math.min(50, dto.getDescription().length())) + "...");
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
        changed = true;
      } else {
        // For existing wrestlers, update game fields from DTO if they have values and are different
        if (dto.getDeckSize() != null
            && !Objects.equals(wrestler.getDeckSize(), dto.getDeckSize())) {
          wrestler.setDeckSize(dto.getDeckSize());
          changed = true;
        }
        if (dto.getStartingHealth() != null
            && !Objects.equals(wrestler.getStartingHealth(), dto.getStartingHealth())) {
          wrestler.setStartingHealth(dto.getStartingHealth());
          changed = true;
        }
        if (dto.getLowHealth() != null
            && !Objects.equals(wrestler.getLowHealth(), dto.getLowHealth())) {
          wrestler.setLowHealth(dto.getLowHealth());
          changed = true;
        }
        if (dto.getStartingStamina() != null
            && !Objects.equals(wrestler.getStartingStamina(), dto.getStartingStamina())) {
          wrestler.setStartingStamina(dto.getStartingStamina());
          changed = true;
        }
        if (dto.getLowStamina() != null
            && !Objects.equals(wrestler.getLowStamina(), dto.getLowStamina())) {
          wrestler.setLowStamina(dto.getLowStamina());
          changed = true;
        }
        if (dto.getFans() != null && !Objects.equals(wrestler.getFans(), dto.getFans())) {
          wrestler.setFans(dto.getFans());
          changed = true;
        }
        if (dto.getIsPlayer() != null
            && !Objects.equals(wrestler.getIsPlayer(), dto.getIsPlayer())) {
          wrestler.setIsPlayer(dto.getIsPlayer());
          changed = true;
        }
        if (dto.getBumps() != null && !Objects.equals(wrestler.getBumps(), dto.getBumps())) {
          wrestler.setBumps(dto.getBumps());
          changed = true;
        }
      }

      // Update campaign attributes
      if (dto.getDrive() != null && !Objects.equals(wrestler.getDrive(), dto.getDrive())) {
        wrestler.setDrive(dto.getDrive());
        changed = true;
      }
      if (dto.getResilience() != null
          && !Objects.equals(wrestler.getResilience(), dto.getResilience())) {
        wrestler.setResilience(dto.getResilience());
        changed = true;
      }
      if (dto.getCharisma() != null && !Objects.equals(wrestler.getCharisma(), dto.getCharisma())) {
        wrestler.setCharisma(dto.getCharisma());
        changed = true;
      }
      if (dto.getBrawl() != null && !Objects.equals(wrestler.getBrawl(), dto.getBrawl())) {
        wrestler.setBrawl(dto.getBrawl());
        changed = true;
      }
      if (dto.getHeritageTag() != null
          && !Objects.equals(wrestler.getHeritageTag(), dto.getHeritageTag())) {
        wrestler.setHeritageTag(dto.getHeritageTag());
        changed = true;
      }

      // Resolve relationships
      // 1. Faction
      if (dto.getFaction() != null && !dto.getFaction().isBlank()) {
        java.util.Optional<com.github.javydreamercsw.management.domain.faction.Faction> factionOpt =
            factionRepository.findByName(dto.getFaction());
        if (factionOpt.isPresent() && !Objects.equals(wrestler.getFaction(), factionOpt.get())) {
          wrestler.setFaction(factionOpt.get());
          changed = true;
        }
      }

      // 2. Manager
      if (dto.getManagerExternalId() != null) {
        java.util.Optional<com.github.javydreamercsw.management.domain.npc.Npc> managerOpt =
            npcRepository.findByExternalId(dto.getManagerExternalId());
        if (managerOpt.isPresent() && !Objects.equals(wrestler.getManager(), managerOpt.get())) {
          wrestler.setManager(managerOpt.get());
          changed = true;
        }
      }

      // 3. Injuries
      if (dto.getInjuryExternalIds() != null && !dto.getInjuryExternalIds().isEmpty()) {
        // Simplified check: if sizes differ, it definitely changed.
        // More robust check would compare actual IDs.
        if (wrestler.getInjuries().size() != dto.getInjuryExternalIds().size()) {
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
          changed = true;
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
                WrestlerAlignment.builder().wrestler(wrestler).alignmentType(type).level(1).build();
            wrestler.setAlignment(alignment);
            changed = true;
          } else if (!Objects.equals(alignment.getAlignmentType(), type)) {
            alignment.setAlignmentType(type);
            wrestler.setAlignment(alignment);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn(
              "Invalid alignment value '{}' for wrestler '{}'", dto.getAlignment(), dto.getName());
        }
      }

      if (changed) {
        tierRecalculationService.recalculateTier(wrestler);
        log.info(
            "💾 Saving wrestler to database: {} (ID: {}, isNew: {})",
            wrestler.getName(),
            wrestler.getId(),
            isNewWrestler);

        if (isNewWrestler) {
          wrestlerService.save(wrestler);
        } else {
          wrestlerRepository.saveAndFlush(wrestler);
        }
        log.info("✅ Wrestler saved successfully: {}", wrestler.getName());
        return true;
      }

      log.debug("⏭️ No changes detected for wrestler: {}", wrestler.getName());
      return false;
    } catch (Exception e) {
      log.error("❌ Failed to save wrestler: {} - {}", dto.getName(), e.getMessage());
      return false;
    }
  }

  public SyncResult syncToNotion(@NonNull String operationId) {
    return wrestlerNotionSyncService.syncToNotion(operationId);
  }

  /** DTO for Wrestler data from Notion. */
  @Setter
  @Getter
  public static class WrestlerDTO {
    private String name;
    private String description;
    private String externalId; // Notion page ID
    private String tier;
    private String alignment;
    private String gender;
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
    private Integer drive;
    private Integer resilience;
    private Integer charisma;
    private Integer brawl;
    private String heritageTag;
    private String managerExternalId;
    private java.util.List<String> injuryExternalIds = new java.util.ArrayList<>();
    private java.util.List<String> teamExternalIds = new java.util.ArrayList<>();
    private java.util.List<String> titleReignExternalIds = new java.util.ArrayList<>();
  }
}
