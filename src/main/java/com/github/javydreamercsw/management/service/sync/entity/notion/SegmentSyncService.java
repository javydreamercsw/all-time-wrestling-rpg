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
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for synchronizing segments from Notion to the database. */
@Service
@Slf4j
public class SegmentSyncService extends BaseSyncService {

  private final SegmentRepository segmentRepository;
  private final ShowRepository showRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final WrestlerService wrestlerService;
  private final TitleRepository titleRepository;

  @Autowired @Lazy protected SegmentSyncService self;

  protected SegmentSyncService getSelf() {
    return self != null ? self : this;
  }

  public SegmentSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor,
      SegmentRepository segmentRepository,
      ShowRepository showRepository,
      SegmentTypeRepository segmentTypeRepository,
      SegmentRuleRepository segmentRuleRepository,
      WrestlerService wrestlerService,
      TitleRepository titleRepository) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.segmentRepository = segmentRepository;
    this.showRepository = showRepository;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.wrestlerService = wrestlerService;
    this.titleRepository = titleRepository;
    this.self = this;
  }

  public SyncResult syncSegments(@NonNull String operationId) {
    log.info(
        "🎞️ Starting segments synchronization from Notion with operation ID: {}", operationId);
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Segments Sync", 4);

    if (!syncServiceDependencies.getNotionSyncProperties().isEnabled()
        || !syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("segments")) {
      log.debug("Segments synchronization is disabled, skipping.");
      return SyncResult.success("Segments", 0, 0, 0);
    }

    try {
      // Step 1: Load all segments from Notion
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Loading segments from Notion...");
      log.info("📥 Loading segments from Notion...");
      long notionStart = System.currentTimeMillis();
      List<SegmentPage> segmentPages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllSegments());
      log.info(
          "✅ Retrieved {} segments from Notion in {}ms",
          segmentPages.size(),
          System.currentTimeMillis() - notionStart);

      // Step 2: Convert to DTOs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Processing Notion data...");
      log.info("⚙️ Processing Notion data...");
      List<SegmentSyncDTO> segmentDTOs = new ArrayList<>();
      for (SegmentPage page : segmentPages) {
        segmentDTOs.add(convertSegmentPageToDTO(page));
      }

      // Step 3: Save to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d segments to database...", segmentDTOs.size()));
      log.info("🗄️ Saving segments to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = 0;
      int processedItems = 0;
      for (SegmentSyncDTO dto : segmentDTOs) {
        processedItems++;
        if (processedItems % 5 == 0) {
          syncServiceDependencies
              .getProgressTracker()
              .updateProgress(
                  operationId,
                  4,
                  String.format(
                      "Saving segments to database... (%d/%d processed)",
                      processedItems, segmentDTOs.size()));
        }
        if (getSelf().processSingleSegment(dto)) {
          savedCount++;
        }
      }
      log.info(
          "✅ Saved {} segments to database in {}ms",
          savedCount,
          System.currentTimeMillis() - dbStart);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synchronized %d segments", savedCount),
              savedCount);

      return SyncResult.success("Segments", savedCount, 0, 0);

    } catch (Exception e) {
      String errorMessage = "Failed to synchronize segments from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Segments", errorMessage);
    }
  }

  private SegmentSyncDTO convertSegmentPageToDTO(@NonNull SegmentPage segmentPage) {
    SegmentSyncDTO dto = new SegmentSyncDTO();
    dto.setExternalId(segmentPage.getId());

    Map<String, Object> rawProperties = segmentPage.getRawProperties();

    // Extract Basic Fields
    dto.setName(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractNameFromNotionPage(segmentPage));
    dto.setNarration(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDescriptionFromNotionPage(segmentPage));

    Object summaryObj = rawProperties.get("Summary");
    if (summaryObj instanceof String) dto.setSummary((String) summaryObj);

    Object orderObj = rawProperties.get("Order");
    if (orderObj instanceof Number) dto.setSegmentOrder(((Number) orderObj).intValue());

    Object mainEventObj = rawProperties.get("Main Event");
    dto.setMainEvent(Boolean.TRUE.equals(mainEventObj));

    Object isTitleSegmentObj = rawProperties.get("Is Title Segment");
    dto.setIsTitleSegment(Boolean.TRUE.equals(isTitleSegmentObj));

    Object statusObj = rawProperties.get("Status");
    if (statusObj instanceof String) dto.setStatus((String) statusObj);

    Object adjudicationStatusObj = rawProperties.get("Adjudication Status");
    if (adjudicationStatusObj instanceof String)
      dto.setAdjudicationStatus((String) adjudicationStatusObj);

    // Extract Relations
    dto.setShowExternalId(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationId(segmentPage, "Shows"));

    dto.setSegmentTypeExternalId(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationId(segmentPage, "Segment Type"));

    dto.setRuleExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Rules"));

    dto.setParticipantExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Participants"));

    dto.setWinnerExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Winners"));

    dto.setTitleExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Titles"));

    return dto;
  }

  /** Saves a single segment DTO to the database. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleSegment(@NonNull SegmentSyncDTO dto) {
    try {
      // Smart duplicate handling - prefer external ID
      Segment segment = null;
      boolean isNewSegment = false;

      // 1. Try to find by external ID first (most reliable)
      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        segment = segmentRepository.findByExternalId(dto.getExternalId()).orElse(null);
      }

      // 2. Fallback to name/show matching if external ID didn't work (hard for segments as names
      // repeat)
      // For segments, we really rely on external ID or manual creation.

      // 3. Create new segment if no segment found
      if (segment == null) {
        segment = new Segment();
        isNewSegment = true;
        log.info("🆕 Creating new segment with external ID: {}", dto.getExternalId());
      } else {
        log.info(
            "🔄 Updating existing segment: {} (ID: {}) with external ID: {}",
            segment.getId(),
            segment.getId(),
            dto.getExternalId());
      }

      // Set properties
      boolean changed = false;
      if (!Objects.equals(segment.getExternalId(), dto.getExternalId())) {
        segment.setExternalId(dto.getExternalId());
        changed = true;
      }
      if (!Objects.equals(segment.getNarration(), dto.getNarration())) {
        segment.setNarration(dto.getNarration());
        changed = true;
      }
      if (!Objects.equals(segment.getSummary(), dto.getSummary())) {
        segment.setSummary(dto.getSummary());
        changed = true;
      }
      if (!Objects.equals(segment.getSegmentOrder(), dto.getSegmentOrder())) {
        segment.setSegmentOrder(dto.getSegmentOrder());
        changed = true;
      }
      if (!Objects.equals(segment.isMainEvent(), dto.getMainEvent())) {
        segment.setMainEvent(dto.getMainEvent());
        changed = true;
      }
      if (!Objects.equals(segment.getIsTitleSegment(), dto.getIsTitleSegment())) {
        segment.setIsTitleSegment(dto.getIsTitleSegment());
        changed = true;
      }

      // Handle Status
      if (dto.getStatus() != null) {
        try {
          SegmentStatus newStatus = SegmentStatus.valueOf(dto.getStatus().toUpperCase());
          if (segment.getStatus() != newStatus) {
            segment.setStatus(newStatus);
            changed = true;
          }
        } catch (Exception e) {
          log.warn("Invalid status value: {}", dto.getStatus());
        }
      }

      // Handle Adjudication Status
      if (dto.getAdjudicationStatus() != null) {
        try {
          AdjudicationStatus newStatus =
              AdjudicationStatus.valueOf(dto.getAdjudicationStatus().toUpperCase());
          if (segment.getAdjudicationStatus() != newStatus) {
            segment.setAdjudicationStatus(newStatus);
            changed = true;
          }
        } catch (Exception e) {
          log.warn("Invalid adjudication status value: {}", dto.getAdjudicationStatus());
        }
      }

      // Resolve Relations
      // 1. Show
      if (dto.getShowExternalId() != null) {
        Optional<Show> show = showRepository.findByExternalId(dto.getShowExternalId());
        if (show.isPresent() && !show.get().equals(segment.getShow())) {
          segment.setShow(show.get());
          changed = true;
        }
      }

      // 2. Segment Type
      if (dto.getSegmentTypeExternalId() != null) {
        Optional<SegmentType> type =
            segmentTypeRepository.findByExternalId(dto.getSegmentTypeExternalId());
        if (type.isPresent() && !type.get().equals(segment.getSegmentType())) {
          segment.setSegmentType(type.get());
          changed = true;
        }
      }

      // 3. Rules
      if (dto.getRuleExternalIds() != null) {
        Set<SegmentRule> rules = new HashSet<>();
        for (String extId : dto.getRuleExternalIds()) {
          segmentRuleRepository.findByExternalId(extId).ifPresent(rules::add);
        }
        if (!Objects.equals(segment.getSegmentRules(), rules)) {
          segment.getSegmentRules().clear();
          segment.getSegmentRules().addAll(rules);
          changed = true;
        }
      }

      // 4. Participants and Winners
      if (dto.getParticipantExternalIds() != null) {
        List<Wrestler> participants = new ArrayList<>();
        for (String extId : dto.getParticipantExternalIds()) {
          wrestlerService.findByExternalId(extId).ifPresent(participants::add);
        }

        List<Wrestler> winners = new ArrayList<>();
        if (dto.getWinnerExternalIds() != null) {
          for (String extId : dto.getWinnerExternalIds()) {
            wrestlerService.findByExternalId(extId).ifPresent(winners::add);
          }
        }

        // Check if participants list has changed
        List<Wrestler> currentWrestlers = segment.getWrestlers();
        if (!new HashSet<>(currentWrestlers).equals(new HashSet<>(participants))
            || !new HashSet<>(segment.getWinners()).equals(new HashSet<>(winners))) {

          segment.getParticipants().clear();
          for (Wrestler w : participants) {
            SegmentParticipant p = new SegmentParticipant();
            p.setWrestler(w);
            p.setSegment(segment);
            p.setIsWinner(winners.contains(w));
            segment.getParticipants().add(p);
          }
          changed = true;
        }
      }

      // 5. Titles
      if (dto.getTitleExternalIds() != null) {
        Set<Title> titles = new HashSet<>();
        for (String extId : dto.getTitleExternalIds()) {
          titleRepository.findByExternalId(extId).ifPresent(titles::add);
        }
        if (!new HashSet<>(segment.getTitles()).equals(titles)) {
          segment.getTitles().clear();
          segment.getTitles().addAll(titles);
          changed = true;
        }
      }

      if (changed) {
        if (isNewSegment) {
          segmentRepository.save(segment);
        } else {
          segmentRepository.saveAndFlush(segment);
        }
        return true;
      }

      return false;
    } catch (Exception e) {
      log.error("❌ Failed to save segment: {} - {}", dto.getExternalId(), e.getMessage());
      return false;
    }
  }

  /** DTO for Segment data from Notion. */
  @Setter
  @Getter
  public static class SegmentSyncDTO {
    private String name;
    private String narration;
    private String summary;
    private String externalId;
    private Integer segmentOrder;
    private Boolean mainEvent;
    private Boolean isTitleSegment;
    private String status;
    private String adjudicationStatus;
    private String showExternalId;
    private String segmentTypeExternalId;
    private List<String> ruleExternalIds = new ArrayList<>();
    private List<String> participantExternalIds = new ArrayList<>();
    private List<String> winnerExternalIds = new ArrayList<>();
    private List<String> titleExternalIds = new ArrayList<>();
  }
}
