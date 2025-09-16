package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TitleSyncService extends BaseSyncService {

  @Autowired protected TitleService titleService;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected WrestlerRepository wrestlerRepository;
  @Autowired protected TitleReignRepository titleReignRepository;

  public TitleSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  @Transactional
  public SyncResult syncTitles(@NonNull String operationId) {
    if (isAlreadySyncedInSession("titles")) {
      log.info("⏭️ Titles already synced in current session, skipping");
      return SyncResult.success("Titles", 0, 0);
    }

    log.info("🏆 Starting titles synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performTitlesSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("titles");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync titles", e);
      return SyncResult.failure("Titles", e.getMessage());
    }
  }

  private SyncResult performTitlesSync(@NonNull String operationId, long startTime) {
    if (!syncProperties.isEntityEnabled("titles")) {
      log.info("Titles sync is disabled in configuration");
      return SyncResult.success("Titles", 0, 0);
    }

    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync titles from Notion.");
      return SyncResult.failure("Titles", "NotionHandler is not available for sync operations");
    }

    try {
      List<TitlePage> titlePages = executeWithRateLimit(notionHandler::loadAllTitles);
      log.info("✅ Retrieved {} titles from Notion", titlePages.size());

      for (TitlePage titlePage : titlePages) {
        String titleName = extractNameFromNotionPage(titlePage);
        Title title =
            titleService
                .findByName(titleName)
                .orElseGet(
                    () -> {
                      log.info(
                          "Title '{}' not found locally, creating new one from Notion data.",
                          titleName);
                      Title newTitle = new Title();
                      newTitle.setName(titleName);
                      newTitle.setExternalId(titlePage.getId());
                      // Set defaults or extract from page
                      String tierString = titlePage.getTier();
                      if (tierString != null) {
                        try {
                          // Attempt to map Notion tier string to WrestlerTier enum
                          WrestlerTier mappedTier =
                              switch (tierString) {
                                case "Main Event" -> WrestlerTier.MAIN_EVENTER;
                                case "Midcard" -> WrestlerTier.MIDCARDER;
                                case "Lower Midcard" -> WrestlerTier.CONTENDER;
                                case "Rookie" -> WrestlerTier.ROOKIE;
                                case "Riser" -> WrestlerTier.RISER;
                                case "Icon" -> WrestlerTier.ICON;
                                default -> {
                                  log.warn(
                                      "Unknown tier string '{}' for title '{}'",
                                      tierString,
                                      titleName);
                                  // Fallback to direct valueOf if no specific mapping
                                  yield WrestlerTier.valueOf(
                                      tierString.toUpperCase().replace(" ", "_"));
                                }
                              };
                          newTitle.setTier(mappedTier);
                        } catch (IllegalArgumentException e) {
                          log.warn("Invalid tier '{}' for title '{}'", tierString, titleName);
                        }
                      }
                      newTitle.setGender(Gender.valueOf(titlePage.getGender().toUpperCase()));
                      newTitle.setIsActive(true);
                      newTitle.setIsVacant(false);
                      return titleRepository.save(newTitle);
                    });

        updateTitleFromNotion(title, titlePage);
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Successfully synchronized titles in {}ms total", totalTime);
      healthMonitor.recordSuccess("Titles", totalTime, titlePages.size());
      return SyncResult.success("Titles", titlePages.size(), 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize titles from Notion after {}ms", totalTime, e);
      healthMonitor.recordFailure("Titles", e.getMessage());
      return SyncResult.failure("Titles", e.getMessage());
    }
  }

  private void updateTitleFromNotion(Title title, TitlePage titlePage) {
    log.debug("Updating title: {}", title.getName());

    String gender = titlePage.getGender();
    if (gender != null && !gender.isBlank()) {
      try {
        title.setGender(Gender.valueOf(gender.toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid gender value '{}' for title '{}'", gender, title.getName());
      }
    }

    // Sync Champions
    List<String> championIds = titlePage.getChampionRelationIds();
    log.info("Champion IDs from Notion for '{}': {}", title.getName(), championIds);
    if (championIds != null && !championIds.isEmpty()) {
      List<Wrestler> newChampions = new java.util.ArrayList<>();
      for (String championId : championIds) {
        var wrestlerOpt = wrestlerRepository.findByExternalId(championId.trim());
        if (wrestlerOpt.isPresent()) {
          Wrestler wrestler = wrestlerOpt.get();
          log.info(
              "Resolved wrestler by externalId for championId '{}': {} (externalId={})",
              championId,
              wrestler.getName(),
              wrestler.getExternalId());
          newChampions.add(wrestler);
        } else {
          // Try by name as fallback
          var wrestlerByNameOpt = wrestlerRepository.findByName(championId.trim());
          if (wrestlerByNameOpt.isPresent()) {
            Wrestler wrestler = wrestlerByNameOpt.get();
            log.info(
                "Resolved wrestler by name for championId '{}': {} (externalId={})",
                championId,
                wrestler.getName(),
                wrestler.getExternalId());
            newChampions.add(wrestler);
          } else {
            log.warn(
                "No wrestler found for championId '{}' (tried externalId and name) when syncing"
                    + " title '{}'.",
                championId,
                title.getName());
          }
        }
      }
      log.info(
          "Final resolved champions for '{}': {}",
          title.getName(),
          newChampions.stream().map(w -> w.getName() + " (" + w.getExternalId() + ")").toList());
      if (!newChampions.isEmpty()) {
        // Always award the title if new champions are present
        log.info(
            "Setting champions for title '{}' to '{}'",
            title.getName(),
            newChampions.stream()
                .map(Wrestler::getName)
                .collect(java.util.stream.Collectors.joining(", ")));
        title.awardTitleTo(newChampions, java.time.Instant.now());
        titleRepository.saveAndFlush(title);
        // Ensure the reign is persisted
        title
            .getCurrentReign()
            .ifPresent(
                reign -> {
                  if (reign.getStartDate() == null) {
                    reign.setStartDate(java.time.Instant.now());
                  }
                  titleReignRepository.saveAndFlush(reign);
                });
      } else {
        log.warn(
            "No champions resolved for title '{}'. Title will remain vacant.", title.getName());
      }
    } else if (!title.getIsVacant()) {
      log.info("Vacating title from Notion: {}", title.getName());
      title.vacateTitle();
      titleRepository.save(title);
    }

    // Sync Contender
    List<String> contenderIds = titlePage.getContenderRelationIds();
    if (contenderIds != null && !contenderIds.isEmpty()) {
      List<Wrestler> newContenders = new java.util.ArrayList<>();
      for (String contenderId : contenderIds) {
        var wrestlerOpt = wrestlerRepository.findByExternalId(contenderId.trim());
        if (wrestlerOpt.isPresent()) {
          Wrestler wrestler = wrestlerOpt.get();
          log.info(
              "Resolved contender by externalId for contenderId '{}': {} (externalId={})",
              contenderId,
              wrestler.getName(),
              wrestler.getExternalId());
          newContenders.add(wrestler);
        } else {
          var wrestlerByNameOpt = wrestlerRepository.findByName(contenderId.trim());
          if (wrestlerByNameOpt.isPresent()) {
            Wrestler wrestler = wrestlerByNameOpt.get();
            log.info(
                "Resolved contender by name for contenderId '{}': {} (externalId={})",
                contenderId,
                wrestler.getName(),
                wrestler.getExternalId());
            newContenders.add(wrestler);
          } else {
            log.warn(
                "No wrestler found for contenderId '{}' (tried externalId and name) when syncing"
                    + " title '{}'.",
                contenderId,
                title.getName());
          }
        }
      }
      log.info(
          "Final resolved contenders for '{}': {}",
          title.getName(),
          newContenders.stream().map(w -> w.getName() + " (" + w.getExternalId() + ")").toList());
      if (!newContenders.isEmpty()) {
        // Only update if contenders have changed
        if (!newContenders.equals(title.getContender())) {
          log.info(
              "Setting contenders for title '{}' to '{}'",
              title.getName(),
              newContenders.stream()
                  .map(Wrestler::getName)
                  .collect(java.util.stream.Collectors.joining(", ")));
          title.setContender(newContenders);
          titleRepository.save(title);
        }
      }
    } else if (title.getContender() != null && !title.getContender().isEmpty()) {
      log.info("Removing contenders from title: {}", title.getName());
      title.setContender(new java.util.ArrayList<>());
      titleRepository.save(title);
    }
  }
}
