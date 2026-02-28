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
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TitleSyncService extends BaseSyncService {

  private final TitleService titleService;
  private final TitleNotionSyncService titleNotionSyncService;

  @Autowired
  public TitleSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      TitleService titleService,
      TitleNotionSyncService titleNotionSyncService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.titleService = titleService;
    this.titleNotionSyncService = titleNotionSyncService;
  }

  @Transactional
  public SyncResult syncTitles(@NonNull String operationId) {
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession(SyncEntityType.TITLES.getKey())) {
      return SyncResult.success(SyncEntityType.TITLES.getKey(), 0, 0, 0);
    }

    log.info("🏆 Starting titles synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performTitlesSync(operationId, startTime);
      if (result.isSuccess()) {
        syncServiceDependencies
            .getSyncSessionManager()
            .markAsSyncedInSession(SyncEntityType.TITLES.getKey());
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync titles", e);
      return SyncResult.failure(SyncEntityType.TITLES.getKey(), e.getMessage());
    }
  }

  private SyncResult performTitlesSync(@NonNull String operationId, long startTime) {
    if (!syncServiceDependencies
        .getNotionSyncProperties()
        .isEntityEnabled(SyncEntityType.TITLES.getKey())) {
      log.info("Titles sync is disabled in configuration");
      return SyncResult.success(SyncEntityType.TITLES.getKey(), 0, 0, 0);
    }

    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync titles from Notion.");
      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "NotionHandler is not available for sync operations");
      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure(
              SyncEntityType.TITLES.getKey(), "NotionHandler is not available for sync operations");
      return SyncResult.failure(
          SyncEntityType.TITLES.getKey(), "NotionHandler is not available for sync operations");
    }

    try {
      List<TitlePage> titlePages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllTitles());
      log.info("✅ Retrieved {} titles from Notion", titlePages.size());

      for (TitlePage titlePage : titlePages) {
        String titleName =
            syncServiceDependencies
                .getNotionPageDataExtractor()
                .extractNameFromNotionPage(titlePage);
        Title title =
            titleService
                .findByExternalId(titlePage.getId())
                .orElseGet(
                    () ->
                        titleService
                            .findByName(titleName)
                            .orElseGet(
                                () -> {
                                  log.info(
                                      "Title '{}' not found locally, creating new one.", titleName);
                                  Title t = new Title();
                                  t.setName(titleName);
                                  return t;
                                }));

        title.setExternalId(titlePage.getId());
        title.setDescription(titlePage.getDescription());

        // Tier mapping
        String tierString = titlePage.getTier();
        if (tierString != null) {
          try {
            WrestlerTier mappedTier =
                switch (tierString) {
                  case "Main Event", "MAIN_EVENTER" -> WrestlerTier.MAIN_EVENTER;
                  case "Midcard", "MIDCARDER" -> WrestlerTier.MIDCARDER;
                  case "Lower Midcard", "CONTENDER" -> WrestlerTier.CONTENDER;
                  case "Rookie", "ROOKIE" -> WrestlerTier.ROOKIE;
                  case "Riser", "RISER" -> WrestlerTier.RISER;
                  case "Icon", "ICON" -> WrestlerTier.ICON;
                  default -> WrestlerTier.valueOf(tierString.toUpperCase().replace(" ", "_"));
                };
            title.setTier(mappedTier);
          } catch (IllegalArgumentException e) {
            log.warn("Invalid tier '{}' for title '{}'", tierString, titleName);
          }
        }

        // Championship Type resolution
        String typeString = titlePage.getChampionshipType();
        if (typeString != null) {
          try {
            title.setChampionshipType(ChampionshipType.valueOf(typeString.toUpperCase()));
          } catch (IllegalArgumentException e) {
            // Heuristic fallback
            if (titleName.toLowerCase().contains("tag")) {
              title.setChampionshipType(ChampionshipType.TEAM);
            } else {
              title.setChampionshipType(ChampionshipType.SINGLE);
            }
          }
        } else if (title.getChampionshipType() == null) {
          if (titleName.toLowerCase().contains("tag")) {
            title.setChampionshipType(ChampionshipType.TEAM);
          } else {
            title.setChampionshipType(ChampionshipType.SINGLE);
          }
        }
        // Gender mapping
        String genderString = titlePage.getGender();
        if (genderString != null) {
          try {
            title.setGender(Gender.valueOf(genderString.toUpperCase()));
          } catch (IllegalArgumentException e) {
            log.warn("Invalid gender '{}' for title '{}'", genderString, titleName);
          }
        }

        if (titlePage.getIncludeInRankings() != null) {
          title.setIncludeInRankings(titlePage.getIncludeInRankings());
        }
        if (titlePage.getIsActive() != null) {
          title.setIsActive(titlePage.getIsActive());
        }
        if (titlePage.getDefenseFrequency() != null) {
          title.setDefenseFrequency(titlePage.getDefenseFrequency());
        }

        syncServiceDependencies.getTitleRepository().save(title);
        updateTitleFromNotion(title, titlePage);
      }

      long totalTime = System.currentTimeMillis() - startTime;
      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess(SyncEntityType.TITLES.getKey(), totalTime, titlePages.size());
      return SyncResult.success(SyncEntityType.TITLES.getKey(), titlePages.size(), 0, 0);

    } catch (Exception e) {
      log.error("❌ Failed to synchronize titles from Notion", e);
      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure(SyncEntityType.TITLES.getKey(), e.getMessage());
      return SyncResult.failure(SyncEntityType.TITLES.getKey(), e.getMessage());
    }
  }

  void updateTitleFromNotion(Title title, TitlePage titlePage) {
    log.debug("Updating title relationships: {}", title.getName());

    // Sync Champions
    List<String> championIds = titlePage.getChampionRelationIds();
    if (championIds != null && !championIds.isEmpty()) {
      List<Wrestler> newChampions =
          championIds.stream()
              .map(
                  id ->
                      syncServiceDependencies
                          .getWrestlerRepository()
                          .findByExternalId(id.trim())
                          .or(
                              () ->
                                  syncServiceDependencies
                                      .getWrestlerRepository()
                                      .findByName(id.trim()))
                          .orElse(null))
              .filter(Objects::nonNull)
              .toList();

      if (!newChampions.isEmpty()) {
        title.awardTitleTo(newChampions, java.time.Instant.now());
        syncServiceDependencies.getTitleRepository().saveAndFlush(title);
        title
            .getCurrentReign()
            .ifPresent(
                reign -> {
                  if (reign.getStartDate() == null) reign.setStartDate(java.time.Instant.now());
                  syncServiceDependencies.getTitleReignRepository().saveAndFlush(reign);
                });
      }
    } else if (!title.isVacant()) {
      title.vacateTitle(java.time.Instant.now());
      syncServiceDependencies.getTitleRepository().save(title);
    }

    // Sync Contender
    List<String> contenderIds = titlePage.getContenderRelationIds();
    if (contenderIds != null && !contenderIds.isEmpty()) {
      List<Wrestler> newContenders =
          contenderIds.stream()
              .map(
                  id ->
                      syncServiceDependencies
                          .getWrestlerRepository()
                          .findByExternalId(id.trim())
                          .or(
                              () ->
                                  syncServiceDependencies
                                      .getWrestlerRepository()
                                      .findByName(id.trim()))
                          .orElse(null))
              .filter(Objects::nonNull)
              .toList();

      if (!newContenders.isEmpty() && !newContenders.equals(title.getChallengers())) {
        title.getChallengers().clear();
        newContenders.forEach(title::addChallenger);
        syncServiceDependencies.getTitleRepository().saveAndFlush(title);
      }
    } else if (title.getChallengers() != null && !title.getChallengers().isEmpty()) {
      title.getChallengers().clear();
      syncServiceDependencies.getTitleRepository().saveAndFlush(title);
    }
  }

  public SyncResult syncToNotion(@NonNull String operationId) {
    return titleNotionSyncService.syncToNotion(operationId);
  }
}
