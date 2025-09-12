package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TitleSyncService extends BaseSyncService {

  @Autowired private TitleService titleService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TitleReignRepository titleReignRepository;

  public TitleSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

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
                      newTitle.setIsActive(true);
                      newTitle.setIsVacant(true);
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
    log.info("Updating title: {}", title.getName());

    String sex = titlePage.getGender();
    if (sex != null && !sex.isBlank()) {
      try {
        title.setGender(Gender.valueOf(sex.toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid sex value '{}' for title '{}'", sex, title.getName());
      }
    }

    String championId = titlePage.getChampionRelationId();
    if (championId != null && !championId.isBlank()) {
      Optional<Wrestler> championOpt = wrestlerRepository.findByExternalId(championId);
      if (championOpt.isPresent()) {
        Wrestler champion = championOpt.get();
        log.info("Setting champion for title '{}' to '{}'", title.getName(), champion.getName());
        title.setCurrentChampion(champion);
        titleRepository.save(title);
      } else {
        log.warn(
            "Champion with external ID '{}' not found for title '{}'", championId, title.getName());
      }
    } else {
      // If the champion relation is empty in Notion, we might want to vacate it locally.
      if (title.getCurrentChampion() != null) {
        log.info("Vacating title: {}", title.getName());
        title.vacateTitle();
        titleRepository.save(title);
      }
    }

    String contenderId = titlePage.getContenderRelationId();
    if (contenderId != null && !contenderId.isBlank()) {
      Optional<Wrestler> contenderOpt = wrestlerRepository.findByExternalId(contenderId);
      if (contenderOpt.isPresent()) {
        Wrestler contender = contenderOpt.get();
        log.info(
            "Setting #1 contender for title '{}' to '{}'", title.getName(), contender.getName());
        title.setNumberOneContender(contender);
        titleRepository.save(title);
      } else {
        log.warn(
            "Contender with external ID '{}' not found for title '{}'",
            contenderId,
            title.getName());
      }
    } else {
      if (title.getNumberOneContender() != null) {
        log.info("Removing #1 contender from title: {}", title.getName());
        title.setNumberOneContender(null);
        titleRepository.save(title);
      }
    }

    List<TitleReign> reigns = titleReignRepository.findByTitle(title);
    title.getTitleReigns().clear();
    title.getTitleReigns().addAll(reigns);
    title.getTitleHistory().clear();
    title.getTitleHistory().addAll(reigns);
    titleRepository.save(title);
  }
}
