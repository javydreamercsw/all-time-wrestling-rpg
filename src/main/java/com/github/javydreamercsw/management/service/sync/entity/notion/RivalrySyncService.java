package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.RivalryPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RivalrySyncService extends BaseSyncService {
  private final RivalryService rivalryService;
  private final WrestlerRepository wrestlerRepository;

  public RivalrySyncService(
      ObjectMapper objectMapper,
      NotionSyncProperties syncProperties,
      RivalryService rivalryService,
      WrestlerRepository wrestlerRepository) {
    super(objectMapper, syncProperties);
    this.rivalryService = rivalryService;
    this.wrestlerRepository = wrestlerRepository;
  }

  public SyncResult syncRivalries(@NonNull String operationId) {
    if (isAlreadySyncedInSession("rivalries")) {
      log.info("⏭️ Rivalries already synced in current session, skipping");
      return SyncResult.success("Rivalries", 0, 0, 0);
    }

    log.info("🔥 Starting rivalries synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performRivalriesSync(operationId);
      if (result.isSuccess()) {
        markAsSyncedInSession("rivalries");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync rivalries", e);
      healthMonitor.recordFailure("Rivalries", e.getMessage());
      return SyncResult.failure("Rivalries", e.getMessage());
    }
  }

  @SneakyThrows
  private SyncResult performRivalriesSync(@NonNull String operationId) {
    if (!isNotionHandlerAvailable()) {
      return SyncResult.failure("Rivalries", "NotionHandler is not available.");
    }

    progressTracker.startOperation(operationId, "Sync Rivalries", 3);

    // 1. Load all rivalry pages from Notion
    progressTracker.updateProgress(operationId, 1, "Retrieving rivalries from Notion...");
    List<RivalryPage> rivalryPages = executeWithRateLimit(notionHandler::loadAllRivalries);
    log.info("Found {} rivalries in Notion.", rivalryPages.size());
    progressTracker.updateProgress(
        operationId, 1, "Retrieved " + rivalryPages.size() + " rivalries.");

    // 2. Convert pages to DTOs
    progressTracker.updateProgress(operationId, 2, "Processing rivalry data...");
    List<RivalryDTO> dtos = rivalryPages.stream().map(this::toDto).collect(Collectors.toList());
    progressTracker.updateProgress(operationId, 2, "Processed " + dtos.size() + " rivalries.");

    // 3. Save to database
    progressTracker.updateProgress(operationId, 3, "Saving rivalries to database...");
    AtomicInteger createdCount = new AtomicInteger(0);
    AtomicInteger updatedCount = new AtomicInteger(0);
    saveRivalriesToDatabase(dtos, createdCount, updatedCount);
    progressTracker.updateProgress(
        operationId,
        3,
        "Saved to database. Created: " + createdCount.get() + ", Updated: " + updatedCount.get());

    progressTracker.completeOperation(
        operationId, true, "Sync complete.", createdCount.get() + updatedCount.get());
    healthMonitor.recordSuccess(
        "Rivalries",
        System.currentTimeMillis() - System.currentTimeMillis(),
        createdCount.get() + updatedCount.get());

    return SyncResult.success("Rivalries", createdCount.get(), updatedCount.get(), 0);
  }

  private RivalryDTO toDto(RivalryPage page) {
    RivalryDTO dto = new RivalryDTO();
    Map<String, Object> props = page.getRawProperties();
    dto.setExternalId(page.getId());
    dto.setWrestler1Name((String) props.get("Wrestler 1"));
    dto.setWrestler2Name((String) props.get("Wrestler 2"));
    try {
      dto.setHeat(Integer.parseInt((String) props.get("Heat")));
    } catch (NumberFormatException e) {
      log.warn("Invalid heat value for rivalry page {}: {}", page.getId(), props.get("Heat"));
      dto.setHeat(0);
    }
    return dto;
  }

  private void saveRivalriesToDatabase(
      List<RivalryDTO> dtos, AtomicInteger createdCount, AtomicInteger updatedCount) {
    for (RivalryDTO dto : dtos) {
      if (dto.getWrestler1Name() == null || dto.getWrestler2Name() == null) {
        log.warn("Skipping rivalry with missing wrestler names (ID: {})", dto.getExternalId());
        continue;
      }

      Optional<Wrestler> wrestler1Opt = wrestlerRepository.findByName(dto.getWrestler1Name());
      Optional<Wrestler> wrestler2Opt = wrestlerRepository.findByName(dto.getWrestler2Name());

      if (wrestler1Opt.isEmpty() || wrestler2Opt.isEmpty()) {
        log.warn(
            "Skipping rivalry for '{}' and '{}' because one or both wrestlers were not found.",
            dto.getWrestler1Name(),
            dto.getWrestler2Name());
        continue;
      }

      Wrestler w1 = wrestler1Opt.get();
      Wrestler w2 = wrestler2Opt.get();

      Optional<Rivalry> existingRivalryOpt =
          rivalryService.getRivalryBetweenWrestlers(w1.getId(), w2.getId());

      if (existingRivalryOpt.isPresent()) {
        Rivalry rivalry = existingRivalryOpt.get();
        if (rivalry.getHeat() != dto.getHeat()) {
          int heatChange = dto.getHeat() - rivalry.getHeat();
          rivalryService.addHeat(rivalry.getId(), heatChange, "Notion Sync");
          updatedCount.incrementAndGet();
          log.info("Updated heat for rivalry: {} vs {}", w1.getName(), w2.getName());
        }
      } else {
        Optional<Rivalry> newRivalryOpt =
            rivalryService.createRivalry(w1.getId(), w2.getId(), "Created from Notion Sync");
        if (newRivalryOpt.isPresent()) {
          Rivalry newRivalry = newRivalryOpt.get();
          if (dto.getHeat() > 0) {
            rivalryService.addHeat(
                newRivalry.getId(), dto.getHeat(), "Initial heat from Notion Sync");
          }
          createdCount.incrementAndGet();
          log.info("Created new rivalry: {} vs {}", w1.getName(), w2.getName());
        }
      }
    }
  }

  @Data
  private static class RivalryDTO {
    private String externalId;
    private String wrestler1Name;
    private String wrestler2Name;
    private int heat;
  }
}
