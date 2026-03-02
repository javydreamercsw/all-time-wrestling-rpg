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
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for synchronizing individual wrestler injuries from Notion to the database.
 */
@Service
@Slf4j
public class InjurySyncService extends BaseSyncService {

  private final InjuryRepository injuryRepository;
  private final WrestlerRepository wrestlerRepository;

  @Autowired
  public InjurySyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      InjuryRepository injuryRepository,
      WrestlerRepository wrestlerRepository,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.injuryRepository = injuryRepository;
    this.wrestlerRepository = wrestlerRepository;
  }

  /**
   * Synchronizes individual injuries from Notion Injuries database to the local database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncInjuries(@NonNull String operationId) {
    // Check if already synced in current session
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession(SyncEntityType.INJURIES.getKey())) {
      log.info("⏭️ Injuries already synced in current session, skipping");
      return SyncResult.success(SyncEntityType.INJURIES.getKey(), 0, 0, 0);
    }

    if (!syncServiceDependencies
        .getNotionSyncProperties()
        .isEntityEnabled(SyncEntityType.INJURIES.getKey())) {
      log.debug("Injuries synchronization is disabled in configuration");
      return SyncResult.success(SyncEntityType.INJURIES.getKey(), 0, 0, 0);
    }

    try {
      SyncResult result = performInjuriesSync(operationId);
      if (result.isSuccess()) {
        syncServiceDependencies
            .getSyncSessionManager()
            .markAsSyncedInSession(SyncEntityType.INJURIES.getKey());
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync injuries", e);
      return SyncResult.failure(SyncEntityType.INJURIES.getKey(), e.getMessage());
    }
  }

  private SyncResult performInjuriesSync(@NonNull String operationId) {
    log.info("🏥 Starting injuries synchronization from Notion with operation ID: {}", operationId);
    long startTime = System.currentTimeMillis();

    try {
      if (!isNotionHandlerAvailable()) {
        String errorMsg = "NotionHandler is not available for injuries sync";
        log.error(errorMsg);
        return SyncResult.failure(SyncEntityType.INJURIES.getKey(), errorMsg);
      }

      syncServiceDependencies.getProgressTracker().startOperation(operationId, "Sync Injuries", 4);
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Loading injuries from Notion...");

      List<InjuryPage> injuryPages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllInjuries());
      log.info("📥 Loaded {} injuries from Notion", injuryPages.size());

      if (injuryPages.isEmpty()) {
        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(operationId, true, "No injuries to sync", 0);
        return SyncResult.success(SyncEntityType.INJURIES.getKey(), 0, 0, 0);
      }

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Converting injuries to DTOs...");
      List<InjurySyncDTO> injuryDTOs =
          injuryPages.stream().map(this::convertInjuryPageToDTO).filter(Objects::nonNull).toList();

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 3, "Saving injuries to database...");
      int savedCount = 0;
      for (InjurySyncDTO dto : injuryDTOs) {
        if (processSingleInjury(dto)) {
          savedCount++;
        }
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Injuries sync completed successfully in {}ms", totalTime);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, true, "Injuries sync completed successfully", savedCount);

      return SyncResult.success(SyncEntityType.INJURIES.getKey(), savedCount, 0, 0);

    } catch (Exception e) {
      log.error("Failed to perform injuries sync", e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, e.getMessage());
      return SyncResult.failure(SyncEntityType.INJURIES.getKey(), e.getMessage());
    }
  }

  private InjurySyncDTO convertInjuryPageToDTO(InjuryPage page) {
    try {
      InjurySyncDTO dto = new InjurySyncDTO();
      dto.setExternalId(page.getId());
      dto.setName(
          syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(page));

      Map<String, Object> props = page.getRawProperties();
      dto.setWrestlerExternalId(
          syncServiceDependencies.getNotionPageDataExtractor().extractRelationId(page, "Wrestler"));
      dto.setSeverity((String) props.get("Severity"));

      Object activeObj = props.get("Active");
      dto.setIsActive(activeObj instanceof Boolean ? (Boolean) activeObj : true);

      dto.setInjuryDate((String) props.get("Injury Date"));
      dto.setHealedDate((String) props.get("Healed Date"));

      Object costObj = props.get("Healing Cost");
      if (costObj instanceof Number n) dto.setHealingCost(n.longValue());

      Object hPenalty = props.get("Health Penalty");
      if (hPenalty instanceof Number n) dto.setHealthPenalty(n.intValue());

      Object sPenalty = props.get("Stamina Penalty");
      if (sPenalty instanceof Number n) dto.setStaminaPenalty(n.intValue());

      Object handPenalty = props.get("Hand Size Penalty");
      if (handPenalty instanceof Number n) dto.setHandSizePenalty(n.intValue());

      dto.setInjuryNotes((String) props.get("Injury Notes"));
      dto.setDescription(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(page));

      return dto;
    } catch (Exception e) {
      log.warn("Failed to convert injury page {}: {}", page.getId(), e.getMessage());
      return null;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleInjury(InjurySyncDTO dto) {
    try {
      Injury injury = injuryRepository.findByExternalId(dto.getExternalId()).orElse(null);
      boolean isNew = false;

      if (injury == null) {
        injury = new Injury();
        injury.setExternalId(dto.getExternalId());
        isNew = true;
      }

      boolean changed = isNew;

      if (!Objects.equals(injury.getName(), dto.getName())) {
        injury.setName(dto.getName());
        changed = true;
      }

      if (dto.getWrestlerExternalId() != null) {
        Optional<Wrestler> wrestlerOpt =
            wrestlerRepository.findByExternalId(dto.getWrestlerExternalId());
        if (wrestlerOpt.isPresent() && !Objects.equals(injury.getWrestler(), wrestlerOpt.get())) {
          injury.setWrestler(wrestlerOpt.get());
          changed = true;
        }
      }

      if (dto.getSeverity() != null) {
        try {
          InjurySeverity severity = InjurySeverity.valueOf(dto.getSeverity().toUpperCase());
          if (!Objects.equals(injury.getSeverity(), severity)) {
            injury.setSeverity(severity);
            changed = true;
          }
        } catch (IllegalArgumentException e) {
          log.warn("Invalid severity '{}' for injury '{}'", dto.getSeverity(), dto.getName());
        }
      }

      if (!Objects.equals(injury.getIsActive(), dto.getIsActive())) {
        injury.setIsActive(dto.getIsActive());
        changed = true;
      }

      if (dto.getHealthPenalty() != null
          && !Objects.equals(injury.getHealthPenalty(), dto.getHealthPenalty())) {
        injury.setHealthPenalty(dto.getHealthPenalty());
        changed = true;
      }

      if (dto.getStaminaPenalty() != null
          && !Objects.equals(injury.getStaminaPenalty(), dto.getStaminaPenalty())) {
        injury.setStaminaPenalty(dto.getStaminaPenalty());
        changed = true;
      }

      if (dto.getHandSizePenalty() != null
          && !Objects.equals(injury.getHandSizePenalty(), dto.getHandSizePenalty())) {
        injury.setHandSizePenalty(dto.getHandSizePenalty());
        changed = true;
      }

      if (dto.getHealingCost() != null
          && !Objects.equals(injury.getHealingCost(), dto.getHealingCost())) {
        injury.setHealingCost(dto.getHealingCost());
        changed = true;
      }

      if (dto.getInjuryDate() != null) {
        Instant date = Instant.parse(dto.getInjuryDate());
        if (!Objects.equals(injury.getInjuryDate(), date)) {
          injury.setInjuryDate(date);
          changed = true;
        }
      }

      if (dto.getHealedDate() != null) {
        Instant date = Instant.parse(dto.getHealedDate());
        if (!Objects.equals(injury.getHealedDate(), date)) {
          injury.setHealedDate(date);
          changed = true;
        }
      }

      if (changed) {
        injuryRepository.saveAndFlush(injury);
        return true;
      }
      return false;
    } catch (Exception e) {
      log.error("Failed to process injury {}: {}", dto.getName(), e.getMessage());
      return false;
    }
  }

  @Getter
  @Setter
  public static class InjurySyncDTO {
    private String name;
    private String externalId;
    private String wrestlerExternalId;
    private String severity;
    private Boolean isActive;
    private String injuryDate;
    private String healedDate;
    private Long healingCost;
    private Integer healthPenalty;
    private Integer staminaPenalty;
    private Integer handSizePenalty;
    private String injuryNotes;
    private String description;
  }
}
