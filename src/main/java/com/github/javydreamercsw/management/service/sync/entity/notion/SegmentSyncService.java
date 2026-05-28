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
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
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
  private final SegmentService segmentService;
  private final ShowService showService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleRepository segmentRuleRepository;
  private final WrestlerService wrestlerService;
  private final TitleRepository titleRepository;

  @Autowired @Lazy protected SegmentSyncService self;
  @Autowired @Lazy protected ShowSyncService showSyncService;

  protected SegmentSyncService getSelf() {
    return self != null ? self : this;
  }

  public SegmentSyncService(
      final ObjectMapper objectMapper,
      final SyncServiceDependencies syncServiceDependencies,
      final NotionApiExecutor notionApiExecutor,
      final SegmentRepository segmentRepository,
      final SegmentService segmentService,
      final ShowService showService,
      final SegmentTypeService segmentTypeService,
      final SegmentRuleRepository segmentRuleRepository,
      final WrestlerService wrestlerService,
      final TitleRepository titleRepository) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.segmentRepository = segmentRepository;
    this.segmentService = segmentService;
    this.showService = showService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleRepository = segmentRuleRepository;
    this.wrestlerService = wrestlerService;
    this.titleRepository = titleRepository;
    this.self = this;
  }

  public SyncResult syncSegments(@NonNull final String operationId) {
    log.info(
        "🎞️ Starting segments synchronization from Notion with operation ID: {}", operationId);
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Segments Sync", 4);

    if (!syncServiceDependencies.getNotionSyncProperties().isEnabled()
        || !syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("segments")) {
      log.debug("Segments synchronization is disabled, skipping.");
      return SyncResult.success("Segments", 0, 0, 0);
    }

    return performSegmentSyncInternal(operationId);
  }

  private SyncResult performSegmentSyncInternal(@NonNull final String operationId) {
    final List<String> messages = new java.util.concurrent.CopyOnWriteArrayList<>();
    // 1. Get all local external IDs
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 1, "Fetching local segment IDs");
    List<String> localExternalIds = segmentService.getAllExternalIds();
    log.info("Found {} segments in the local database.", localExternalIds.size());

    // 2. Get all segment IDs from Notion
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 2, "Fetching segment IDs from Notion");
    List<String> notionSegmentIds = getSegmentIds();
    log.info("Found {} segments in Notion.", notionSegmentIds.size());

    // 3. Identify new segments (delta sync)
    List<String> newSegmentIds =
        notionSegmentIds.stream()
            .filter(id -> !localExternalIds.contains(id))
            .collect(Collectors.toList());

    if (newSegmentIds.isEmpty()) {
      log.info("No new segments to sync from Notion.");
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, true, "No new segments to sync.", 0);
      return SyncResult.success(SyncEntityType.SEGMENTS.getKey(), 0, 0, 0);
    }
    log.info("Found {} new segments to sync from Notion.", newSegmentIds.size());

    // 4. Load only the new SegmentPage objects in parallel
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 3, "Loading new segment pages from Notion");
    List<SegmentPage> segmentPages =
        processWithControlledParallelism(
            newSegmentIds,
            id -> {
              return syncServiceDependencies.getNotionHandler().loadSegmentById(id).orElse(null);
            },
            10,
            operationId,
            3,
            "Loaded");

    List<SegmentPage> validSegmentPages =
        segmentPages.stream()
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());

    // 5. Convert to DTOs
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 4, "Converting new segments to DTOs");
    List<SegmentDTO> segmentDTOs =
        convertSegmentsWithRateLimit(validSegmentPages, operationId, messages::add);

    // 6. Save to database
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 5, "Saving new segments to database");
    int savedCount = saveSegmentsToDatabase(segmentDTOs, messages::add);

    int errorCount = newSegmentIds.size() - savedCount;
    log.info("✅ Synced {} new segments with {} errors", savedCount, errorCount);

    boolean success = errorCount == 0;
    String message =
        success
            ? "Delta-sync for segments completed successfully."
            : "Delta-sync for segments completed with errors.";
    syncServiceDependencies
        .getProgressTracker()
        .completeOperation(operationId, success, message, savedCount);

    SyncResult result;
    if (success) {
      result = SyncResult.success("Segments", savedCount, 0, errorCount);
    } else {
      result = SyncResult.failure("Segments", "Some new segments failed to sync.");
    }
    result.getMessages().addAll(messages);
    return result;
  }

  private List<SegmentDTO> convertSegmentsWithRateLimit(
      final List<SegmentPage> notionSegments,
      final String operationId,
      final Consumer<String> messageConsumer) {
    return processWithControlledParallelism(
        notionSegments,
        segmentPage -> convertNotionPageToDTO(segmentPage, messageConsumer),
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d segments",
        messageConsumer);
  }

  private SegmentDTO convertNotionPageToDTO(
      @NonNull final SegmentPage segmentPage, final Consumer<String> messageConsumer) {
    SegmentDTO segmentDTO = new SegmentDTO();
    segmentDTO.setExternalId(segmentPage.getId());
    segmentDTO.setName(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractNameFromNotionPage(segmentPage));

    Map<String, Object> rawProperties = segmentPage.getRawProperties();

    String showExternalId =
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationId(segmentPage, "Shows");
    if (showExternalId != null && !showExternalId.isEmpty()) {
      segmentDTO.setShowExternalId(showExternalId);
    }

    Object participantsProperty = rawProperties.get("Participants");
    if (participantsProperty instanceof String && !((String) participantsProperty).isEmpty()) {
      segmentDTO.setParticipantNames(
          Stream.of(((String) participantsProperty).split(","))
              .map(String::trim)
              .collect(Collectors.toList()));
    } else {
      segmentDTO.setParticipantNames(new ArrayList<>());
    }

    Object winnersProperty = rawProperties.get("Winners");
    if (winnersProperty instanceof String && !((String) winnersProperty).isEmpty()) {
      segmentDTO.setWinnerNames(
          Stream.of(((String) winnersProperty).split(","))
              .map(String::trim)
              .collect(Collectors.toList()));
    } else {
      segmentDTO.setWinnerNames(new ArrayList<>());
    }

    String segmentTypeExternalId =
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationId(segmentPage, "Segment Type");
    if (segmentTypeExternalId != null && !segmentTypeExternalId.isEmpty()) {
      segmentDTO.setSegmentTypeExternalId(segmentTypeExternalId);
    } else {
      String msg = "Segment type relation not found for segment %s.".formatted(segmentPage.getId());
      log.warn(msg);
      messageConsumer.accept(msg);
    }

    Object dateProperty = rawProperties.get("Date");
    if (dateProperty instanceof String dateString && !((String) dateProperty).isEmpty()) {
      if (dateString.startsWith("@")) {
        dateString = dateString.substring(1);
      }
      // Strip trailing time portion (e.g. " 12:00 AM") — keep only "MMMM d, yyyy"
      dateString = dateString.replaceAll("(\\w+ \\d+, \\d{4}).*", "$1");
      try {
        // The date from Notion is in the format "MMMM d, yyyy"
        java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy");
        java.time.LocalDate localDate = java.time.LocalDate.parse(dateString, formatter);
        segmentDTO.setSegmentDate(localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
      } catch (java.time.format.DateTimeParseException e) {
        String msg =
            "Could not parse date '%s' for segment %s: %s"
                .formatted(dateProperty, segmentPage.getId(), e.getMessage());
        log.warn(msg);
        messageConsumer.accept(msg);
        segmentDTO.setSegmentDate(null);
      }
    }

    // Get full narration content from page blocks
    try {
      segmentDTO.setNarration(
          syncServiceDependencies.getNotionHandler().getPageContentPlainText(segmentPage.getId()));
    } catch (Exception e) {
      log.warn(
          "Could not load narration content for segment {}: {}",
          segmentPage.getId(),
          e.getMessage());
      segmentDTO.setNarration(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(segmentPage));
    }

    Object summaryObj = rawProperties.get("Summary");
    if (summaryObj instanceof String) {
      segmentDTO.setSummary((String) summaryObj);
    }

    Object notesProperty = rawProperties.get("Notes");
    if (notesProperty instanceof String && !((String) notesProperty).isEmpty()) {
      segmentDTO.setNotes((String) notesProperty);
    }

    Object orderObj = rawProperties.get("Order");
    if (orderObj instanceof Number) {
      segmentDTO.setSegmentOrder(((Number) orderObj).intValue());
    }

    Object mainEventObj = rawProperties.get("Main Event");
    segmentDTO.setMainEvent(Boolean.TRUE.equals(mainEventObj));

    Object isTitleSegmentObj = rawProperties.get("Is Title Segment");
    segmentDTO.setTitleSegment(Boolean.TRUE.equals(isTitleSegmentObj));

    Object statusObj = rawProperties.get("Status");
    if (statusObj instanceof String) {
      segmentDTO.setStatus((String) statusObj);
    }

    Object adjudicationStatusObj = rawProperties.get("Adjudication Status");
    if (adjudicationStatusObj instanceof String) {
      segmentDTO.setAdjudicationStatus((String) adjudicationStatusObj);
    }

    // Extract Relation IDs for advanced processing
    segmentDTO.setRuleExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Rules"));

    segmentDTO.setTitleExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(segmentPage, "Titles"));

    return segmentDTO;
  }

  private int saveSegmentsToDatabase(
      @NonNull final List<SegmentDTO> segmentDTOs, final Consumer<String> messageConsumer) {
    int savedCount = 0;
    for (SegmentDTO segmentDTO : segmentDTOs) {
      if (getSelf().processSingleSegment(segmentDTO, messageConsumer)) {
        savedCount++;
      }
    }
    return savedCount;
  }

  public boolean processSingleSegment(@NonNull final SegmentDTO segmentDTO) {
    return self.processSingleSegment(segmentDTO, msg -> {});
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleSegment(
      @NonNull final SegmentDTO segmentDTO, final Consumer<String> messageConsumer) {
    Optional<Segment> existingSegmentOpt =
        Optional.ofNullable(segmentDTO.getExternalId()).flatMap(segmentService::findByExternalId);

    Segment segment = existingSegmentOpt.orElseGet(Segment::new);
    boolean isNew = segment.getId() == null;

    if (isNew) {
      log.debug(
          "Creating new segment: {} (External ID: {})",
          segmentDTO.getName(),
          segmentDTO.getExternalId());
      segment.setExternalId(segmentDTO.getExternalId());
    } else {
      log.debug("Updating existing segment: {} (ID: {})", segmentDTO.getName(), segment.getId());
    }

    // Resolve Show
    Optional<Show> showOpt = showService.findByExternalId(segmentDTO.getShowExternalId());
    if (showOpt.isEmpty()) {
      String msg =
          "Show for segment %s was not found locally. Attempting to sync it."
              .formatted(segmentDTO.getName());
      log.warn(msg);
      messageConsumer.accept(msg);
      // Attempt to sync the missing show
      SyncResult showSyncResult = showSyncService.syncShow(segmentDTO.getShowExternalId());
      if (showSyncResult.isSuccess()) {
        log.info("Successfully synced show. Retrying lookup.");
        showOpt = showService.findByExternalId(segmentDTO.getShowExternalId());
      }

      if (showOpt.isEmpty()) {
        String errorMsg =
            "Skipping segment %s as show could not be found or synced."
                .formatted(segmentDTO.getName());
        log.warn(errorMsg);
        messageConsumer.accept(errorMsg);
        return false;
      }
    }
    segment.setShow(showOpt.get());

    // Resolve Segment Type by external ID
    String segmentTypeExternalId = segmentDTO.getSegmentTypeExternalId();
    if (segmentTypeExternalId != null && !segmentTypeExternalId.trim().isEmpty()) {
      Optional<SegmentType> segmentTypeOpt =
          segmentTypeService.findByExternalId(segmentTypeExternalId);
      if (segmentTypeOpt.isEmpty()) {
        log.warn(
            "Segment type with external ID '{}' not found in local database.",
            segmentTypeExternalId);
      } else {
        segment.setSegmentType(segmentTypeOpt.get());
      }
    }

    // Resolve Participants and Winners
    List<Wrestler> participants = new ArrayList<>();
    for (String participantName : segmentDTO.getParticipantNames()) {
      wrestlerService.findByName(participantName).ifPresent(participants::add);
    }

    List<Wrestler> winners = new ArrayList<>();
    for (String winnerName : segmentDTO.getWinnerNames()) {
      wrestlerService.findByName(winnerName).ifPresent(winners::add);
    }

    segment.syncParticipants(participants);

    if (!winners.isEmpty()) {
      segment.setWinners(winners);
    }

    // Set other properties
    if (segmentDTO.getSegmentDate() != null) {
      segment.setSegmentDate(segmentDTO.getSegmentDate());
    }

    if (segmentDTO.getNarration() != null) {
      segment.setNarration(segmentDTO.getNarration());
    }

    if (segmentDTO.getSummary() != null) {
      segment.setSummary(segmentDTO.getSummary());
    }

    if (segmentDTO.getNotes() != null) {
      segment.setNotes(segmentDTO.getNotes());
    }

    segment.setSegmentOrder(segmentDTO.getSegmentOrder());
    segment.setMainEvent(segmentDTO.isMainEvent());
    segment.setIsTitleSegment(segmentDTO.isTitleSegment());

    // Handle Status
    if (segmentDTO.getStatus() != null) {
      try {
        segment.setStatus(SegmentStatus.valueOf(segmentDTO.getStatus().toUpperCase()));
      } catch (Exception e) {
        log.warn("Invalid status value: {}", segmentDTO.getStatus());
      }
    }

    // Handle Adjudication Status
    if (segmentDTO.getAdjudicationStatus() != null) {
      try {
        segment.setAdjudicationStatus(
            AdjudicationStatus.valueOf(segmentDTO.getAdjudicationStatus().toUpperCase()));
      } catch (Exception e) {
        log.warn("Invalid adjudication status value: {}", segmentDTO.getAdjudicationStatus());
      }
    }

    // Resolve Rules
    if (segmentDTO.getRuleExternalIds() != null) {
      Set<SegmentRule> rules = new HashSet<>();
      for (String extId : segmentDTO.getRuleExternalIds()) {
        segmentRuleRepository.findByExternalId(extId).ifPresent(rules::add);
      }
      segment.getSegmentRules().clear();
      segment.getSegmentRules().addAll(rules);
    }

    // Resolve Titles
    if (segmentDTO.getTitleExternalIds() != null) {
      Set<Title> titles = new HashSet<>();
      for (String extId : segmentDTO.getTitleExternalIds()) {
        titleRepository.findByExternalId(extId).ifPresent(titles::add);
      }
      segment.getTitles().clear();
      segment.getTitles().addAll(titles);
    }

    segmentService.updateSegment(segment);

    return true;
  }

  public List<String> getSegmentIds() {
    return syncServiceDependencies.getNotionHandler().getDatabasePageIds("Segments");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SyncResult syncSegment(@NonNull final String segmentId) {
    log.info("🤼 Starting segment synchronization from Notion for ID: {}", segmentId);
    String operationId = "segment-sync-" + segmentId;
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Segment Sync", 4);
    final List<String> messages = new CopyOnWriteArrayList<>();
    Optional<SegmentPage> segmentPageOpt =
        syncServiceDependencies.getNotionHandler().loadSegmentById(segmentId);
    if (segmentPageOpt.isEmpty()) {
      String errorMessage = "Segment with ID " + segmentId + " not found in Notion.";
      log.error(errorMessage);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Segment", errorMessage);
    }

    SegmentPage segmentPage = segmentPageOpt.get();
    SegmentDTO segmentDTO = convertNotionPageToDTO(segmentPage, messages::add);

    if (processSingleSegment(segmentDTO, messages::add)) {
      String message = "Segment sync completed successfully. Synced 1 segment.";
      log.info(message);
      syncServiceDependencies.getProgressTracker().completeOperation(operationId, true, message, 1);
      SyncResult result = SyncResult.success("Segment", 1, 0, 0);
      result.getMessages().addAll(messages);
      return result;
    } else {
      String errorMessage = "Failed to process segment with ID " + segmentId;
      log.error(errorMessage);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      SyncResult result = SyncResult.failure("Segment", errorMessage);
      result.getMessages().addAll(messages);
      return result;
    }
  }
}
