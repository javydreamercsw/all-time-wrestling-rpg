package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentSyncService extends BaseSyncService {

  @Autowired private SegmentService segmentService;
  @Autowired private ShowService showService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private ShowSyncService showSyncService;

  public SegmentSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  @Transactional
  public SyncResult syncSegments(@NonNull String operationId) {
    log.info("🤼 Starting segments synchronization from Notion with operation ID: {}", operationId);
    progressTracker.startOperation(operationId, "Segments Sync", 4);

    try {
      return performSegmentSyncInternal(operationId);
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize segments from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      progressTracker.failOperation(operationId, errorMessage);
      return SyncResult.failure("Segments", errorMessage);
    }
  }

  private SyncResult performSegmentSyncInternal(@NonNull String operationId) throws Exception {
    // 1. Get all local external IDs
    progressTracker.updateProgress(operationId, 1, "Fetching local segment IDs");
    List<String> localExternalIds = segmentService.getAllExternalIds();
    log.info("Found {} segments in the local database.", localExternalIds.size());

    // 2. Get all Notion page IDs
    progressTracker.updateProgress(operationId, 2, "Fetching Notion segment IDs");
    List<String> notionSegmentIds =
        executeWithRateLimit(() -> notionHandler.getDatabasePageIds("Segments"));
    log.info("Found {} segments in Notion.", notionSegmentIds.size());

    // 3. Calculate the difference
    List<String> newSegmentIds =
        notionSegmentIds.stream()
            .filter(id -> !localExternalIds.contains(id))
            .collect(java.util.stream.Collectors.toList());

    if (newSegmentIds.isEmpty()) {
      log.info("No new segments to sync from Notion.");
      progressTracker.completeOperation(operationId, true, "No new segments to sync.", 0);
      return SyncResult.success("Segments", 0, 0);
    }
    log.info("Found {} new segments to sync from Notion.", newSegmentIds.size());

    // 4. Load only the new SegmentPage objects in parallel
    progressTracker.updateProgress(operationId, 3, "Loading new segment pages from Notion");
    List<SegmentPage> segmentPages =
        processWithControlledParallelism(
            newSegmentIds,
            (id) -> {
              try {
                return notionHandler.loadSegmentById(id).orElse(null);
              } catch (Exception e) {
                log.error("Failed to load segment page for id: {}", id, e);
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
    progressTracker.updateProgress(operationId, 4, "Converting new segments to DTOs");
    List<SegmentDTO> segmentDTOs = convertSegmentsWithRateLimit(validSegmentPages, operationId);

    // 6. Save to database
    progressTracker.updateProgress(operationId, 5, "Saving new segments to database");
    int savedCount = saveSegmentsToDatabase(segmentDTOs);

    int errorCount = newSegmentIds.size() - savedCount;
    log.info("✅ Synced {} new segments with {} errors", savedCount, errorCount);

    boolean success = errorCount == 0;
    String message =
        success
            ? "Delta-sync for segments completed successfully."
            : "Delta-sync for segments completed with errors.";
    progressTracker.completeOperation(operationId, success, message, savedCount);

    if (success) {
      return SyncResult.success("Segments", savedCount, errorCount);
    } else {
      return SyncResult.failure("Segments", "Some new segments failed to sync.");
    }
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

  private SegmentDTO convertNotionPageToDTO(@NonNull SegmentPage segmentPage) {
    try {
      SegmentDTO segmentDTO = new SegmentDTO();
      segmentDTO.setExternalId(segmentPage.getId());
      segmentDTO.setName(segmentPage.getRawProperties().get("Name").toString());
      segmentDTO.setCreatedTime(java.time.Instant.parse(segmentPage.getCreated_time()));
      segmentDTO.setLastEditedTime(java.time.Instant.parse(segmentPage.getLast_edited_time()));

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
        log.warn(
            "Segment type property for segment {} is not a string or is empty. Actual type: {},"
                + " Value: {}",
            segmentPage.getId(),
            segmentTypeProperty != null ? segmentTypeProperty.getClass().getName() : "null",
            segmentTypeProperty);
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
          log.warn(
              "Could not parse date '{}' for segment {}: {}",
              dateProperty,
              segmentPage.getId(),
              e.getMessage());
          segmentDTO.setSegmentDate(null);
        }
      }

      return segmentDTO;
    } catch (Exception e) {
      log.error(
          "Error converting Notion SegmentPage to DTO for page {}: {}",
          segmentPage.getId(),
          e.getMessage(),
          e);
      return null;
    }
  }

  private int saveSegmentsToDatabase(@NonNull List<SegmentDTO> segmentDTOs) {
    int savedCount = 0;
    for (SegmentDTO segmentDTO : segmentDTOs) {
      try {
        if (processSingleSegment(segmentDTO)) {
          savedCount++;
        }
      } catch (Exception e) {
        log.error("Failed to process segment DTO {}: {}", segmentDTO.getName(), e.getMessage(), e);
      }
    }
    return savedCount;
  }

  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public boolean processSingleSegment(@NonNull SegmentDTO segmentDTO) {
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
        log.warn(
            "Show '{}' for segment {} was not found locally. Attempting to sync it.",
            segmentDTO.getShowName(),
            segmentDTO.getName());
        // Attempt to sync the missing show
        SyncResult showSyncResult = showSyncService.syncShow(segmentDTO.getShowExternalId());
        if (showSyncResult.isSuccess()) {
          log.info("Successfully synced show '{}'. Retrying lookup.", segmentDTO.getShowName());
          showOpt = showService.findByExternalId(segmentDTO.getShowExternalId());
        } else {
          log.error(
              "Failed to sync show '{}' for segment {}: {}",
              segmentDTO.getShowName(),
              segmentDTO.getName(),
              showSyncResult.getErrorMessage());
        }

        if (showOpt.isEmpty()) {
          log.warn(
              "Skipping segment {} as show '{}' could not be found or synced.",
              segmentDTO.getName(),
              segmentDTO.getShowName());
          return false;
        }
      }
      segment.setShow(showOpt.get());

      String segmentTypeName = segmentDTO.getSegmentTypeName();
      if (segmentTypeName == null || segmentTypeName.trim().isEmpty()) {
        log.warn(
            "Skipping segment {} as segment type name is null or empty in Notion data.",
            segmentDTO.getName());
        return false;
      }

      Optional<SegmentType> segmentTypeOpt = segmentTypeService.findByName(segmentTypeName);
      if (segmentTypeOpt.isEmpty()) {
        log.warn(
            "Skipping segment {} as segment type '{}' was not found in local database.",
            segmentDTO.getName(),
            segmentTypeName);
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

      segment.getParticipants().clear();
      for (Wrestler participant : participants) {
        segment.addParticipant(participant);
      }

      if (!winners.isEmpty()) {
        segment.setWinner(winners.get(0));
      }

      segment.setSegmentDate(segmentDTO.getSegmentDate());

      segmentService.updateSegment(segment);
      return true;
    } catch (Exception e) {
      log.error("Failed to process segment DTO {}: {}", segmentDTO.getName(), e.getMessage(), e);
      return false;
    }
  }

  public List<String> getSegmentIds() {
    return notionHandler.getDatabasePageIds("Segments");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SyncResult syncSegment(@NonNull String segmentId) {
    log.info("🤼 Starting segment synchronization from Notion for ID: {}", segmentId);
    String operationId = "segment-sync-" + segmentId;
    progressTracker.startOperation(operationId, "Segment Sync", 4);

    try {
      Optional<SegmentPage> segmentPageOpt = notionHandler.loadSegmentById(segmentId);
      if (segmentPageOpt.isEmpty()) {
        String errorMessage = "Segment with ID " + segmentId + " not found in Notion.";
        log.error(errorMessage);
        progressTracker.failOperation(operationId, errorMessage);
        return SyncResult.failure("Segment", errorMessage);
      }

      SegmentPage segmentPage = segmentPageOpt.get();
      SegmentDTO segmentDTO = convertNotionPageToDTO(segmentPage);

      if (processSingleSegment(segmentDTO)) {
        String message = "Segment sync completed successfully. Synced 1 segment.";
        log.info(message);
        progressTracker.completeOperation(operationId, true, message, 1);
        return SyncResult.success("Segment", 1, 0);
      } else {
        String errorMessage = "Failed to process segment with ID " + segmentId;
        log.error(errorMessage);
        progressTracker.failOperation(operationId, errorMessage);
        return SyncResult.failure("Segment", errorMessage);
      }
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize segment from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      progressTracker.failOperation(operationId, errorMessage);
      return SyncResult.failure("Segment", errorMessage);
    }
  }
}
