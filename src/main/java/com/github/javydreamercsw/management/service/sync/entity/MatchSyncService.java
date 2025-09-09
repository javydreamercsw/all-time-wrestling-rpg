package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.MatchDTO;
import com.github.javydreamercsw.management.service.match.MatchService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
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
public class MatchSyncService extends BaseSyncService {

  @Autowired private MatchService matchService;
  @Autowired private ShowService showService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private MatchTypeService matchTypeService;
  @Autowired private ShowSyncService showSyncService;

  public MatchSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  @Transactional
  public SyncResult syncMatches(@NonNull String operationId) {
    log.info("🤼 Starting matches synchronization from Notion with operation ID: {}", operationId);
    progressTracker.startOperation(operationId, "Matches Sync", 4);

    try {
      return performMatchSyncInternal(operationId);
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize matches from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      progressTracker.failOperation(operationId, errorMessage);
      return SyncResult.failure("Matches", errorMessage);
    }
  }

  private SyncResult performMatchSyncInternal(@NonNull String operationId) throws Exception {
    // 1. Get all local external IDs
    progressTracker.updateProgress(operationId, 1, "Fetching local match IDs");
    List<String> localExternalIds = matchService.getAllExternalIds();
    log.info("Found {} matches in the local database.", localExternalIds.size());

    // 2. Get all Notion page IDs
    progressTracker.updateProgress(operationId, 2, "Fetching Notion match IDs");
    List<String> notionMatchIds =
        executeWithRateLimit(() -> notionHandler.getDatabasePageIds("Matches"));
    log.info("Found {} matches in Notion.", notionMatchIds.size());

    // 3. Calculate the difference
    List<String> newMatchIds =
        notionMatchIds.stream()
            .filter(id -> !localExternalIds.contains(id))
            .collect(java.util.stream.Collectors.toList());

    if (newMatchIds.isEmpty()) {
      log.info("No new matches to sync from Notion.");
      progressTracker.completeOperation(operationId, true, "No new matches to sync.", 0);
      return SyncResult.success("Matches", 0, 0);
    }
    log.info("Found {} new matches to sync from Notion.", newMatchIds.size());

    // 4. Load only the new MatchPage objects in parallel
    progressTracker.updateProgress(operationId, 3, "Loading new match pages from Notion");
    List<MatchPage> matchPages =
        processWithControlledParallelism(
            newMatchIds,
            (id) -> {
              try {
                return notionHandler.loadMatchById(id).orElse(null);
              } catch (Exception e) {
                log.error("Failed to load match page for id: {}", id, e);
                return null;
              }
            },
            10,
            operationId,
            3,
            "Loaded");

    List<MatchPage> validMatchPages =
        matchPages.stream()
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());

    // 5. Convert to DTOs
    progressTracker.updateProgress(operationId, 4, "Converting new matches to DTOs");
    List<MatchDTO> matchDTOs = convertMatchesWithRateLimit(validMatchPages, operationId);

    // 6. Save to database
    progressTracker.updateProgress(operationId, 5, "Saving new matches to database");
    int savedCount = saveMatchesToDatabase(matchDTOs);

    int errorCount = newMatchIds.size() - savedCount;
    log.info("✅ Synced {} new matches with {} errors", savedCount, errorCount);

    boolean success = errorCount == 0;
    String message =
        success
            ? "Delta-sync for matches completed successfully."
            : "Delta-sync for matches completed with errors.";
    progressTracker.completeOperation(operationId, success, message, savedCount);

    if (success) {
      return SyncResult.success("Matches", savedCount, errorCount);
    } else {
      return SyncResult.failure("Matches", "Some new matches failed to sync.");
    }
  }

  private List<MatchDTO> convertMatchesWithRateLimit(
      List<MatchPage> notionMatches, String operationId) {
    return processWithControlledParallelism(
        notionMatches,
        this::convertNotionPageToDTO,
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d matches");
  }

  private MatchDTO convertNotionPageToDTO(@NonNull MatchPage matchPage) {
    try {
      MatchDTO matchDTO = new MatchDTO();
      matchDTO.setExternalId(matchPage.getId());
      matchDTO.setName(matchPage.getRawProperties().get("Name").toString());
      matchDTO.setCreatedTime(java.time.Instant.parse(matchPage.getCreated_time()));
      matchDTO.setLastEditedTime(java.time.Instant.parse(matchPage.getLast_edited_time()));

      if (matchPage.getProperties().getShows() != null
          && !matchPage.getProperties().getShows().getRelation().isEmpty()) {
        matchDTO.setShowExternalId(
            matchPage.getProperties().getShows().getRelation().get(0).getId());
      }

      Object participantsProperty = matchPage.getRawProperties().get("Participants");
      if (participantsProperty instanceof String && !((String) participantsProperty).isEmpty()) {
        matchDTO.setParticipantNames(
            java.util.stream.Stream.of(((String) participantsProperty).split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList()));
      } else {
        matchDTO.setParticipantNames(new java.util.ArrayList<>());
      }

      Object winnersProperty = matchPage.getRawProperties().get("Winners");
      if (winnersProperty instanceof String && !((String) winnersProperty).isEmpty()) {
        matchDTO.setWinnerNames(
            java.util.stream.Stream.of(((String) winnersProperty).split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList()));
      } else {
        matchDTO.setWinnerNames(new java.util.ArrayList<>());
      }

      Object matchTypeProperty = matchPage.getRawProperties().get("Match Type");
      if (matchTypeProperty instanceof String && !((String) matchTypeProperty).isEmpty()) {
        matchDTO.setMatchTypeName((String) matchTypeProperty);
      }

      Object dateProperty = matchPage.getRawProperties().get("Date");
      if (dateProperty instanceof String dateString && !((String) dateProperty).isEmpty()) {
        if (dateString.startsWith("@")) {
          dateString = dateString.substring(1);
        }
        try {
          // The date from Notion is in the format "MMMM d, yyyy"
          java.time.format.DateTimeFormatter formatter =
              java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy");
          java.time.LocalDate localDate = java.time.LocalDate.parse(dateString, formatter);
          matchDTO.setMatchDate(localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
          log.warn(
              "Could not parse date '{}' for match {}: {}",
              dateProperty,
              matchPage.getId(),
              e.getMessage());
          matchDTO.setMatchDate(null);
        }
      }

      return matchDTO;
    } catch (Exception e) {
      log.error(
          "Error converting Notion MatchPage to DTO for page {}: {}",
          matchPage.getId(),
          e.getMessage(),
          e);
      return null;
    }
  }

  private int saveMatchesToDatabase(@NonNull List<MatchDTO> matchDTOs) {
    int savedCount = 0;
    for (MatchDTO matchDTO : matchDTOs) {
      try {
        if (processSingleMatch(matchDTO)) {
          savedCount++;
        }
      } catch (Exception e) {
        log.error("Failed to process match DTO {}: {}", matchDTO.getName(), e.getMessage(), e);
      }
    }
    return savedCount;
  }

  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public boolean processSingleMatch(@NonNull MatchDTO matchDTO) {
    try {
      Optional<Match> existingMatchOpt =
          Optional.ofNullable(matchDTO.getExternalId()).flatMap(matchService::findByExternalId);

      Match match = existingMatchOpt.orElseGet(Match::new);

      if (match.getId() == null) {
        log.debug(
            "Creating new match: {} (External ID: {})",
            matchDTO.getName(),
            matchDTO.getExternalId());
        match.setExternalId(matchDTO.getExternalId());
      } else {
        log.debug("Updating existing match: {} (ID: {})", matchDTO.getName(), match.getId());
      }

      Optional<Show> showOpt = showService.findByExternalId(matchDTO.getShowExternalId());
      if (showOpt.isEmpty()) {
        log.warn(
            "Show '{}' for match {} was not found locally. Attempting to sync it.",
            matchDTO.getShowName(),
            matchDTO.getName());
        // Attempt to sync the missing show
        SyncResult showSyncResult = showSyncService.syncShow(matchDTO.getShowExternalId());
        if (showSyncResult.isSuccess()) {
          log.info("Successfully synced show '{}'. Retrying lookup.", matchDTO.getShowName());
          showOpt = showService.findByExternalId(matchDTO.getShowExternalId());
        } else {
          log.error(
              "Failed to sync show '{}' for match {}: {}",
              matchDTO.getShowName(),
              matchDTO.getName(),
              showSyncResult.getErrorMessage());
        }

        if (showOpt.isEmpty()) {
          log.warn(
              "Skipping match {} as show '{}' could not be found or synced.",
              matchDTO.getName(),
              matchDTO.getShowName());
          return false;
        }
      }
      match.setShow(showOpt.get());

      Optional<MatchType> matchTypeOpt = matchTypeService.findByName(matchDTO.getMatchTypeName());
      if (matchTypeOpt.isEmpty()) {
        log.warn(
            "Skipping match {} as match type '{}' was not found.",
            matchDTO.getName(),
            matchDTO.getMatchTypeName());
        return false;
      }
      match.setMatchType(matchTypeOpt.get());

      List<Wrestler> participants = new java.util.ArrayList<>();
      for (String participantName : matchDTO.getParticipantNames()) {
        wrestlerService.findByName(participantName).ifPresent(participants::add);
      }

      List<Wrestler> winners = new java.util.ArrayList<>();
      for (String winnerName : matchDTO.getWinnerNames()) {
        wrestlerService.findByName(winnerName).ifPresent(winners::add);
      }

      match.getParticipants().clear();
      for (Wrestler participant : participants) {
        match.addParticipant(participant);
      }

      if (!winners.isEmpty()) {
        match.setWinner(winners.get(0));
      }

      match.setMatchDate(matchDTO.getMatchDate());

      matchService.updateMatch(match);
      return true;
    } catch (Exception e) {
      log.error("Failed to process match DTO {}: {}", matchDTO.getName(), e.getMessage(), e);
      return false;
    }
  }

  public List<String> getMatchIds() {
    return notionHandler.getDatabasePageIds("Matches");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SyncResult syncMatch(@NonNull String matchId) {
    log.info("🤼 Starting match synchronization from Notion for ID: {}", matchId);
    String operationId = "match-sync-" + matchId;
    progressTracker.startOperation(operationId, "Match Sync", 4);

    try {
      Optional<MatchPage> matchPageOpt = notionHandler.loadMatchById(matchId);
      if (matchPageOpt.isEmpty()) {
        String errorMessage = "Match with ID " + matchId + " not found in Notion.";
        log.error(errorMessage);
        progressTracker.failOperation(operationId, errorMessage);
        return SyncResult.failure("Match", errorMessage);
      }

      MatchPage matchPage = matchPageOpt.get();
      MatchDTO matchDTO = convertNotionPageToDTO(matchPage);

      if (processSingleMatch(matchDTO)) {
        String message = "Match sync completed successfully. Synced 1 match.";
        log.info(message);
        progressTracker.completeOperation(operationId, true, message, 1);
        return SyncResult.success("Match", 1, 0);
      } else {
        String errorMessage = "Failed to process match with ID " + matchId;
        log.error(errorMessage);
        progressTracker.failOperation(operationId, errorMessage);
        return SyncResult.failure("Match", errorMessage);
      }
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize match from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      progressTracker.failOperation(operationId, errorMessage);
      return SyncResult.failure("Match", errorMessage);
    }
  }
}
