package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.MatchPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.MatchDTO;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MatchSyncService extends BaseSyncService {

  @Autowired private MatchResultService matchResultService;
  @Autowired private ShowService showService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private MatchTypeService matchTypeService;

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
    // 1. Load all match pages from Notion with rate limiting
    progressTracker.updateProgress(operationId, 1, "Loading matches from Notion");
    List<MatchPage> notionMatches = executeWithRateLimit(notionHandler::loadAllMatches);

    if (notionMatches.isEmpty()) {
      log.info("No matches found in Notion database");
      progressTracker.completeOperation(operationId, true, "No matches found in Notion", 0);
      return SyncResult.success("Matches", 0, 0);
    }
    log.info("📥 Loaded {} matches from Notion", notionMatches.size());

    // 2. Convert Notion match pages to DTOs with controlled parallel processing
    progressTracker.updateProgress(operationId, 2, "Converting Notion pages to DTOs");
    List<MatchDTO> matchDTOs = convertMatchesWithRateLimit(notionMatches, operationId);
    log.info("🔄 Converted {} matches to DTOs", matchDTOs.size());

    // 3. Save/update matches in the local database
    progressTracker.updateProgress(operationId, 3, "Saving matches to database");
    int savedCount = saveMatchesToDatabase(matchDTOs);
    log.info("✅ Saved {} matches to database", savedCount);

    // 4. Validate sync process
    progressTracker.updateProgress(operationId, 4, "Validating sync process");
    boolean success = true; // Placeholder for validation

    if (success) {
      String message =
          String.format("Matches sync completed successfully. Synced %d matches.", savedCount);
      log.info(message);
      progressTracker.completeOperation(operationId, true, message, savedCount);
      return SyncResult.success("Matches", savedCount, 0);
    } else {
      String message = "Matches sync completed with validation errors";
      log.warn(message);
      progressTracker.completeOperation(operationId, false, message, savedCount);
      return SyncResult.failure("Matches", message);
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
      matchDTO.setCreatedTime(Instant.parse(matchPage.getCreated_time()));
      matchDTO.setLastEditedTime(Instant.parse(matchPage.getLast_edited_time()));

      if (matchPage.getProperties().getShows() != null
          && !matchPage.getProperties().getShows().getRelation().isEmpty()) {
        matchDTO.setShowExternalId(
            matchPage.getProperties().getShows().getRelation().get(0).getId());
      }

      Object participantsProperty = matchPage.getRawProperties().get("Participants");
      if (participantsProperty instanceof String && !((String) participantsProperty).isEmpty()) {
        matchDTO.setParticipantNames(
            Stream.of(((String) participantsProperty).split(","))
                .map(String::trim)
                .collect(Collectors.toList()));
      } else {
        matchDTO.setParticipantNames(new ArrayList<>());
      }

      Object winnersProperty = matchPage.getRawProperties().get("Winners");
      if (winnersProperty instanceof String && !((String) winnersProperty).isEmpty()) {
        matchDTO.setWinnerNames(
            Stream.of(((String) winnersProperty).split(","))
                .map(String::trim)
                .collect(Collectors.toList()));
      } else {
        matchDTO.setWinnerNames(new ArrayList<>());
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
          LocalDate localDate = LocalDate.parse(dateString);
          matchDTO.setMatchDate(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException e) {
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
        Optional<MatchResult> existingMatchResultOpt =
            matchResultService.findByExternalId(matchDTO.getExternalId());

        MatchResult matchResult = existingMatchResultOpt.orElseGet(MatchResult::new);

        if (matchResult.getId() == null) {
          log.debug("Creating new match: {}", matchDTO.getName());
          matchResult.setExternalId(matchDTO.getExternalId());
        } else {
          log.debug("Updating existing match: {}", matchResult.getId());
        }

        Optional<Show> showOpt = showService.findByExternalId(matchDTO.getShowExternalId());
        if (showOpt.isEmpty()) {
          log.warn(
              "Skipping match {} as show '{}' was not found.",
              matchDTO.getName(),
              matchDTO.getShowName());
          continue;
        }
        matchResult.setShow(showOpt.get());

        Optional<MatchType> matchTypeOpt = matchTypeService.findByName(matchDTO.getMatchTypeName());
        if (matchTypeOpt.isEmpty()) {
          log.warn(
              "Skipping match {} as match type '{}' was not found.",
              matchDTO.getName(),
              matchDTO.getMatchTypeName());
          continue;
        }
        matchResult.setMatchType(matchTypeOpt.get());

        List<Wrestler> participants = new ArrayList<>();
        for (String participantName : matchDTO.getParticipantNames()) {
          wrestlerService.findByName(participantName).ifPresent(participants::add);
        }

        List<Wrestler> winners = new ArrayList<>();
        for (String winnerName : matchDTO.getWinnerNames()) {
          wrestlerService.findByName(winnerName).ifPresent(winners::add);
        }

        matchResult.getParticipants().clear();
        for (Wrestler participant : participants) {
          matchResult.addParticipant(participant, winners.contains(participant));
        }

        matchResult.setMatchDate(matchDTO.getMatchDate());

        matchResultService.updateMatchResult(matchResult);
        savedCount++;
      } catch (Exception e) {
        log.error("Failed to process match DTO {}: {}", matchDTO.getName(), e.getMessage(), e);
      }
    }
    return savedCount;
  }

  public List<String> getMatchIds() {
    return notionHandler.getDatabasePageIds("Matches");
  }

  @Transactional
  public SyncResult syncMatch(@NonNull String matchId) {
    log.info("🤼 Starting match synchronization from Notion for ID: {}", matchId);
    String operationId = "match-sync-" + matchId;
    progressTracker.startOperation(operationId, "Match Sync", 4);

    try {
      Optional<MatchPage> matchPage = notionHandler.loadMatchById(matchId);
      if (matchPage.isEmpty()) {
        String errorMessage = "Match with ID " + matchId + " not found in Notion.";
        log.error(errorMessage);
        progressTracker.failOperation(operationId, errorMessage);
        return SyncResult.failure("Match", errorMessage);
      }

      List<MatchDTO> matchDTOs = convertMatchesWithRateLimit(List.of(matchPage.get()), operationId);
      int savedCount = saveMatchesToDatabase(matchDTOs);
      String message = "Match sync completed successfully. Synced " + savedCount + " match.";
      log.info(message);
      progressTracker.completeOperation(operationId, true, message, savedCount);
      return SyncResult.success("Match", savedCount, 0);
    } catch (Exception e) {
      String errorMessage = "Failed to synchronize match from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      progressTracker.failOperation(operationId, errorMessage);
      return SyncResult.failure("Match", errorMessage);
    }
  }
}
