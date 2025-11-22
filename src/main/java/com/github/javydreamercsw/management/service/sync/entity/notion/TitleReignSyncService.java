package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TitleReignSyncService extends BaseSyncService {

  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  public TitleReignSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  public SyncResult syncTitleReigns(@NonNull String operationId) {
    if (isAlreadySyncedInSession("titlereigns")) {
      return SyncResult.success("TitleReigns", 0, 0, 0);
    }

    log.info("👑 Starting title reigns synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performTitleReignsSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("titlereigns");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync title reigns", e);
      return SyncResult.failure("TitleReigns", e.getMessage());
    }
  }

  private SyncResult performTitleReignsSync(@NonNull String operationId, long startTime) {
    if (!syncProperties.isEntityEnabled("titlereigns")) {
      log.info("Title Reigns sync is disabled in configuration");
      return SyncResult.success("TitleReigns", 0, 0, 0);
    }

    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync title reigns from Notion.");
      return SyncResult.failure(
          "TitleReigns", "NotionHandler is not available for sync operations");
    }

    try {
      List<TitleReignPage> titleReignPages =
          executeWithRateLimit(notionHandler::loadAllTitleReigns);
      log.info("✅ Retrieved {} title reigns from Notion", titleReignPages.size());
      // Log each retrieved page
      titleReignPages.forEach(page -> log.info("Retrieved Title Reign Page: {}", page));

      for (TitleReignPage page : titleReignPages) {
        String titleId = page.getTitleRelationId();
        String championIds = page.getChampionRelationId();

        if (titleId == null || championIds == null) {
          log.warn("Skipping title reign with missing title or champion ID: {}", page.getId());
          continue;
        }

        Optional<Title> titleOpt = titleRepository.findByExternalId(titleId);

        if (titleOpt.isEmpty()) {
          log.warn("Skipping title reign: Title with ID '{}' not found locally.", titleId);
          continue;
        }

        Title title = titleOpt.get();
        String[] championExtIds = championIds.split(",");
        List<Wrestler> champions = new java.util.ArrayList<>();
        for (String championExtId : championExtIds) {
          wrestlerRepository.findByExternalId(championExtId.trim()).ifPresent(champions::add);
        }

        if (champions.isEmpty()) {
          log.warn("Skipping title reign: No champions found for IDs '{}'.", championIds);
          continue;
        }

        // Attempt to find existing reign by title, and reign number
        Optional<TitleReign> existingReignOpt = Optional.empty();
        if (page.getReignNumber() != null) {
          existingReignOpt =
              titleReignRepository.findByTitleAndReignNumber(title, page.getReignNumber());
        }

        TitleReign reign = existingReignOpt.orElse(new TitleReign());
        reign.setTitle(title);
        reign.getChampions().clear();
        reign.getChampions().addAll(champions);
        reign.setReignNumber(page.getReignNumber() != null ? page.getReignNumber() : 0);
        reign.setNotes(page.getNotes());

        // Parse dates
        try {
          if (page.getStartDate() != null) {
            reign.setStartDate(
                LocalDate.parse(page.getStartDate()).atStartOfDay(ZoneOffset.UTC).toInstant());
          }
          if (page.getEndDate() != null) {
            reign.setEndDate(
                LocalDate.parse(page.getEndDate()).atStartOfDay(ZoneOffset.UTC).toInstant());
          }
        } catch (DateTimeParseException e) {
          log.warn("Failed to parse date for title reign {}: {}", page.getId(), e.getMessage());
        }

        titleReignRepository.save(reign);
        log.info(
            "Saved title reign for {} - {} (Reign #{}): {} to {}",
            title.getName(),
            reign.getChampions().stream()
                .map(Wrestler::getName)
                .collect(java.util.stream.Collectors.joining(" & ")),
            reign.getReignNumber(),
            reign.getStartDate(),
            reign.getEndDate());
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} title reigns in {}ms total",
          titleReignPages.size(),
          totalTime);
      healthMonitor.recordSuccess("TitleReigns", totalTime, titleReignPages.size());
      return SyncResult.success("TitleReigns", titleReignPages.size(), 0, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize title reigns from Notion after {}ms", totalTime, e);
      healthMonitor.recordFailure("TitleReigns", e.getMessage());
      return SyncResult.failure("TitleReigns", e.getMessage());
    }
  }
}
