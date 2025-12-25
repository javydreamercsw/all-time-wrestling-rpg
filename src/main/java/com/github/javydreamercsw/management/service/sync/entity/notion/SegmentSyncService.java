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
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentSyncService extends BaseSyncService {

  private final SegmentService segmentService;
  private final ShowService showService;
  private final WrestlerService wrestlerService;
  private final SegmentTypeService segmentTypeService;
  private final ShowSyncService showSyncService;

  public SegmentSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      SegmentService segmentService,
      ShowService showService,
      WrestlerService wrestlerService,
      SegmentTypeService segmentTypeService,
      ShowSyncService showSyncService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.segmentService = segmentService;
    this.showService = showService;
    this.wrestlerService = wrestlerService;
    this.segmentTypeService = segmentTypeService;
    this.showSyncService = showSyncService;
  }

  @Transactional
  public SyncResult syncSegments(@NonNull String operationId) {
    log.info("🤼 Starting segments synchronization from Notion with operation ID: {}", operationId);
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Segments Sync", 4);

    try {
      return performSegmentSyncInternal(operationId);
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize segments from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Segments", errorMessage);
    }
  }

  private SyncResult performSegmentSyncInternal(@NonNull String operationId) throws Exception {
    final List<String> messages = new java.util.concurrent.CopyOnWriteArrayList<>();
    // 1. Get all local external IDs
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 1, "Fetching local segment IDs");
    List<String> localExternalIds = segmentService.getAllExternalIds();
    log.info("Found {} segments in the local database.", localExternalIds.size());

    // 2. Get all Notion page IDs
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 2, "Fetching Notion segment IDs");
    List<String> notionSegmentIds =
        executeWithRateLimit(
            () -> syncServiceDependencies.getNotionHandler().getDatabasePageIds("Segments"));
    log.info("Found {} segments in Notion.", notionSegmentIds.size());

    // 3. Calculate the difference
    List<String> newSegmentIds =
        notionSegmentIds.stream()
            .filter(id -> !localExternalIds.contains(id))
            .collect(java.util.stream.Collectors.toList());

    if (newSegmentIds.isEmpty()) {
      log.info("No new segments to sync from Notion.");
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, true, "No new segments to sync.", 0);
      return SyncResult.success("Segments", 0, 0, 0);
    }
    log.info("Found {} new segments to sync from Notion.", newSegmentIds.size());

    // 4. Load only the new SegmentPage objects in parallel
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 3, "Loading new segment pages from Notion");
    List<SegmentPage> segmentPages =
        processWithControlledParallelism(
            newSegmentIds,
            (id) -> {
              try {
                return syncServiceDependencies.getNotionHandler().loadSegmentById(id).orElse(null);
              } catch (Exception e) {
                String msg = "Failed to load segment page for id: " + id;
                log.error(msg, e);
                messages.add(msg);
                return null;
              }
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
      List<SegmentPage> notionSegments, String operationId) {
    return processWithControlledParallelism(
        notionSegments,
        this::convertNotionPageToDTO,
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d segments");
  }

  private List<SegmentDTO> convertSegmentsWithRateLimit(
      List<SegmentPage> notionSegments, String operationId, Consumer<String> messageConsumer) {
    return processWithControlledParallelism(
        notionSegments,
        (segmentPage) -> convertNotionPageToDTO(segmentPage, messageConsumer),
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d segments",
        messageConsumer);
  }

  private SegmentDTO convertNotionPageToDTO(@NonNull SegmentPage segmentPage) {
    return convertNotionPageToDTO(segmentPage, (msg) -> {});
  }

  private SegmentDTO convertNotionPageToDTO(
      @NonNull SegmentPage segmentPage, Consumer<String> messageConsumer) {
    try {
      SegmentDTO segmentDTO = new SegmentDTO();
      segmentDTO.setExternalId(segmentPage.getId());
      segmentDTO.setName(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractNameFromNotionPage(segmentPage));

      if (segmentPage.getProperties().getShows() != null
          && !segmentPage.getProperties().getShows().getRelation().isEmpty()) {
        segmentDTO.setShowExternalId(
            segmentPage.getProperties().getShows().getRelation().get(0).getId());
      }

      Object participantsProperty = segmentPage.getRawProperties().get("Participants");
      if (participantsProperty instanceof String && !((String) participantsProperty).isEmpty()) {
        segmentDTO.setParticipantNames(
            java.util.stream.Stream.of(((String) participantsProperty).split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList()));
      } else {
        segmentDTO.setParticipantNames(new java.util.ArrayList<>());
      }

      Object winnersProperty = segmentPage.getRawProperties().get("Winners");
      if (winnersProperty instanceof String && !((String) winnersProperty).isEmpty()) {
        segmentDTO.setWinnerNames(
            java.util.stream.Stream.of(((String) winnersProperty).split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList()));
      } else {
        segmentDTO.setWinnerNames(new java.util.ArrayList<>());
      }

      Object segmentTypeProperty = segmentPage.getRawProperties().get("Segment Type");
      if (segmentTypeProperty instanceof String && !((String) segmentTypeProperty).isEmpty()) {
        segmentDTO.setSegmentTypeName((String) segmentTypeProperty);
      } else {
        String msg =
            String.format(
                "Segment type property for segment %s is not a string or is empty. Actual type: %s,"
                    + " Value: %s",
                segmentPage.getId(),
                segmentTypeProperty != null ? segmentTypeProperty.getClass().getName() : "null",
                segmentTypeProperty);
        log.warn(msg);
        messageConsumer.accept(msg);
      }

      Object dateProperty = segmentPage.getRawProperties().get("Date");
      if (dateProperty instanceof String dateString && !((String) dateProperty).isEmpty()) {
        if (dateString.startsWith("@")) {
          dateString = dateString.substring(1);
        }
        try {
          // The date from Notion is in the format "MMMM d, yyyy"
          java.time.format.DateTimeFormatter formatter =
              java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy");
          java.time.LocalDate localDate = java.time.LocalDate.parse(dateString, formatter);
          segmentDTO.setSegmentDate(localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
          String msg =
              String.format(
                  "Could not parse date '%s' for segment %s: %s",
                  dateProperty, segmentPage.getId(), e.getMessage());
          log.warn(msg);
          messageConsumer.accept(msg);
          segmentDTO.setSegmentDate(null);
        }
      }

      segmentDTO.setNarration(
          syncServiceDependencies.getNotionHandler().getPageContentPlainText(segmentPage.getId()));

      return segmentDTO;
    } catch (Exception e) {
      String msg =
          String.format(
              "Error converting Notion SegmentPage to DTO for page %s: %s",
              segmentPage.getId(), e.getMessage());
      log.error(msg, e);
      messageConsumer.accept(msg);
      return null;
    }
  }

  private int saveSegmentsToDatabase(
      @NonNull List<SegmentDTO> segmentDTOs, Consumer<String> messageConsumer) {
    int savedCount = 0;
    for (SegmentDTO segmentDTO : segmentDTOs) {
      try {
        if (processSingleSegment(segmentDTO, messageConsumer)) {
          savedCount++;
        }
      } catch (Exception e) {
        String msg =
            String.format(
                "Failed to process segment DTO %s: %s", segmentDTO.getName(), e.getMessage());
        log.error(msg, e);
        messageConsumer.accept(msg);
      }
    }
    return savedCount;
  }

  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public boolean processSingleSegment(@NonNull SegmentDTO segmentDTO) {
    return processSingleSegment(segmentDTO, (msg) -> {});
  }

  public boolean processSingleSegment(
      @NonNull SegmentDTO segmentDTO, Consumer<String> messageConsumer) {
    try {
      Optional<Segment> existingSegmentOpt =
          Optional.ofNullable(segmentDTO.getExternalId()).flatMap(segmentService::findByExternalId);

      Segment segment = existingSegmentOpt.orElseGet(Segment::new);

      if (segment.getId() == null) {
        log.debug(
            "Creating new segment: {} (External ID: {})",
            segmentDTO.getName(),
            segmentDTO.getExternalId());
        segment.setExternalId(segmentDTO.getExternalId());
      } else {
        log.debug("Updating existing segment: {} (ID: {})", segmentDTO.getName(), segment.getId());
      }

      Optional<Show> showOpt = showService.findByExternalId(segmentDTO.getShowExternalId());
      if (showOpt.isEmpty()) {
        String msg =
            String.format(
                "Show '%s' for segment %s was not found locally. Attempting to sync it.",
                segmentDTO.getShowName(), segmentDTO.getName());
        log.warn(msg);
        messageConsumer.accept(msg);
        // Attempt to sync the missing show
        SyncResult showSyncResult = showSyncService.syncShow(segmentDTO.getShowExternalId());
        if (showSyncResult.isSuccess()) {
          log.info("Successfully synced show '{}'. Retrying lookup.", segmentDTO.getShowName());
          showOpt = showService.findByExternalId(segmentDTO.getShowExternalId());
        } else {
          String errorMsg =
              String.format(
                  "Failed to sync show '%s' for segment %s: %s",
                  segmentDTO.getShowName(), segmentDTO.getName(), showSyncResult.getErrorMessage());
          log.error(errorMsg);
          messageConsumer.accept(errorMsg);
        }

        if (showOpt.isEmpty()) {
          String errorMsg =
              String.format(
                  "Skipping segment %s as show '%s' could not be found or synced.",
                  segmentDTO.getName(), segmentDTO.getShowName());
          log.warn(errorMsg);
          messageConsumer.accept(errorMsg);
          return false;
        }
      }
      segment.setShow(showOpt.get());

      String segmentTypeName = segmentDTO.getSegmentTypeName();
      if (segmentTypeName == null || segmentTypeName.trim().isEmpty()) {
        String msg =
            String.format(
                "Skipping segment %s as segment type name is null or empty in Notion data.",
                segmentDTO.getName());
        log.warn(msg);
        messageConsumer.accept(msg);
        return false;
      }

      Optional<SegmentType> segmentTypeOpt = segmentTypeService.findByName(segmentTypeName);
      if (segmentTypeOpt.isEmpty()) {
        String msg =
            String.format(
                "Skipping segment %s as segment type '%s' was not found in local database.",
                segmentDTO.getName(), segmentTypeName);
        log.warn(msg);
        messageConsumer.accept(msg);
        return false;
      }
      segment.setSegmentType(segmentTypeOpt.get());

      List<Wrestler> participants = new java.util.ArrayList<>();
      for (String participantName : segmentDTO.getParticipantNames()) {
        wrestlerService.findByName(participantName).ifPresent(participants::add);
      }

      List<Wrestler> winners = new java.util.ArrayList<>();
      for (String winnerName : segmentDTO.getWinnerNames()) {
        wrestlerService.findByName(winnerName).ifPresent(winners::add);
      }

      segment.syncParticipants(participants);

      if (!winners.isEmpty()) {
        segment.setWinners(winners);
      }

      segment.setSegmentDate(segmentDTO.getSegmentDate());

      segment.setNarration(segmentDTO.getNarration());

      segmentService.updateSegment(segment);
      return true;
    } catch (Exception e) {
      String msg =
          String.format(
              "Failed to process segment DTO %s: %s", segmentDTO.getName(), e.getMessage());
      log.error(msg, e);
      messageConsumer.accept(msg);
      return false;
    }
  }

  public List<String> getSegmentIds() {
    return syncServiceDependencies.getNotionHandler().getDatabasePageIds("Segments");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SyncResult syncSegment(@NonNull String segmentId) {
    log.info("🤼 Starting segment synchronization from Notion for ID: {}", segmentId);
    String operationId = "segment-sync-" + segmentId;
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Segment Sync", 4);
    final List<String> messages = new CopyOnWriteArrayList<>();

    try {
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
        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(operationId, true, message, 1);
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
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize segment from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      SyncResult result = SyncResult.failure("Segment", errorMessage);
      result.getMessages().add(e.getMessage());
      return result;
    }
  }
}
