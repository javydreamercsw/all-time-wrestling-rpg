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
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing matches from Notion to the database. */
@Service
@Slf4j
public class MatchSyncService extends BaseSyncService {

  @Autowired private MatchResultService matchResultService;
  @Autowired private MatchTypeService matchTypeService;
  @Autowired private ShowService showService;
  @Autowired private WrestlerService wrestlerService;

  public MatchSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes matches from Notion Matches database directly to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncMatches(@NonNull String operationId) {
    if (!syncProperties.isEntityEnabled("matches")) {
      log.debug("Matches synchronization is disabled in configuration");
      return SyncResult.success("Matches", 0, 0);
    }

    // Check if NOTION_TOKEN is available before starting sync
    if (!validateNotionToken("Matches")) {
      progressTracker.failOperation(
          operationId, "NOTION_TOKEN environment variable is required for Notion sync");
      healthMonitor.recordFailure("Matches", "NOTION_TOKEN not available");
      return SyncResult.failure(
          "Matches", "NOTION_TOKEN environment variable is required for Notion sync");
    }

    log.info("🚀 Starting matches synchronization from Notion to database...");
    long startTime = System.currentTimeMillis();

    // Initialize progress tracking
    progressTracker.startOperation(operationId, "Sync Matches", 8);
    progressTracker.updateProgress(operationId, 1, "Initializing matches sync operation...");

    try {
      log.debug("🔧 Executing matches sync with circuit breaker and retry logic");

      // Execute with circuit breaker and retry logic
      SyncResult result =
          circuitBreakerService.execute(
              "matches",
              () -> {
                log.debug("🔄 Circuit breaker executing matches sync");
                return retryService.executeWithRetry(
                    "matches",
                    (attemptNumber) -> {
                      log.debug("🔄 Matches sync attempt {} starting", attemptNumber);
                      progressTracker.updateProgress(operationId, 3, "Starting matches sync...");
                      SyncResult attemptResult = performMatchesSync(operationId, startTime);
                      log.debug(
                          "🔄 Matches sync attempt {} result: {}",
                          attemptNumber,
                          attemptResult != null ? attemptResult.isSuccess() : "NULL");
                      return attemptResult;
                    });
              });

      // Update final progress
      progressTracker.updateProgress(operationId, 8, "Matches sync completed");
      progressTracker.completeOperation(
          operationId,
          result.isSuccess(),
          result.isSuccess() ? "Matches sync completed successfully" : result.getErrorMessage(),
          result.getSyncedCount());

      // Record health metrics
      if (result.isSuccess()) {
        healthMonitor.recordSuccess(
            "Matches", System.currentTimeMillis() - startTime, result.getSyncedCount());
        log.info("✅ Matches sync completed successfully: {}", result.getSummary());
      } else {
        healthMonitor.recordFailure("Matches", result.getErrorMessage());
        log.error("❌ Matches sync failed: {}", result.getErrorMessage());
      }

      return result;
    } catch (Exception e) {
      String errorMessage = "Matches sync failed with exception: " + e.getMessage();
      log.error("❌ {}", errorMessage, e);

      progressTracker.failOperation(operationId, errorMessage);
      healthMonitor.recordFailure("Matches", errorMessage);

      return SyncResult.failure("Matches", errorMessage);
    }
  }

  /** Performs the actual matches synchronization from Notion to database. */
  private SyncResult performMatchesSync(@NonNull String operationId, long startTime) {
    try {
      // Step 1: Load matches from Notion
      progressTracker.updateProgress(operationId, 4, "Loading matches from Notion...");
      List<MatchPage> matchPages = loadMatchesFromNotion();
      log.info(
          "✅ Retrieved {} matches in {}ms",
          matchPages.size(),
          System.currentTimeMillis() - startTime);

      if (matchPages.isEmpty()) {
        log.info("No matches found in Notion database");
        return SyncResult.success("Matches", 0, 0);
      }

      // Step 2: Convert to DTOs
      progressTracker.updateProgress(operationId, 5, "Converting matches to DTOs...");
      List<MatchDTO> matchDTOs = convertMatchesToDTOs(matchPages);
      log.info(
          "✅ Converted {} matches in {}ms",
          matchDTOs.size(),
          System.currentTimeMillis() - startTime);

      // Step 3: Save to database
      progressTracker.updateProgress(operationId, 6, "Saving matches to database...");
      int syncedCount = saveMatchesToDatabase(matchDTOs);
      log.info(
          "✅ Saved {} matches to database in {}ms",
          syncedCount,
          System.currentTimeMillis() - startTime);

      // Step 4: Validate results
      progressTracker.updateProgress(operationId, 7, "Validating match sync results...");
      boolean validationPassed = validateMatchSyncResults(matchDTOs, syncedCount);

      if (!validationPassed) {
        return SyncResult.failure("Matches", "Match sync validation failed");
      }

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Matches sync completed successfully in {}ms", totalTime);

      return SyncResult.success("Matches", syncedCount, 0);

    } catch (Exception e) {
      log.error("Failed to perform matches sync", e);
      return SyncResult.failure("Matches", "Failed to sync matches: " + e.getMessage());
    }
  }

  /** Loads all matches from Notion database. */
  private List<MatchPage> loadMatchesFromNotion() {
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot load matches from Notion.");
      throw new RuntimeException("NotionHandler is not available for sync operations");
    }

    try {
      return notionHandler.loadAllMatches();
    } catch (Exception e) {
      log.error("Failed to load matches from Notion", e);
      throw new RuntimeException("Failed to load matches from Notion: " + e.getMessage(), e);
    }
  }

  /** Converts MatchPage objects to MatchDTO objects. */
  private List<MatchDTO> convertMatchesToDTOs(List<MatchPage> matchPages) {
    return matchPages.parallelStream()
        .map(this::convertMatchPageToDTO)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /** Converts a single MatchPage to MatchDTO. */
  private MatchDTO convertMatchPageToDTO(MatchPage matchPage) {
    try {
      MatchDTO dto = new MatchDTO();

      // Set external ID (Notion page ID)
      dto.setExternalId(matchPage.getId());

      // Extract basic properties
      dto.setName(extractNameFromNotionPage(matchPage));
      dto.setDescription(extractDescriptionFromNotionPage(matchPage));

      // Extract match-specific properties
      dto.setParticipants(extractParticipantsFromMatchPage(matchPage));
      dto.setWinner(extractWinnerFromMatchPage(matchPage));
      dto.setMatchType(extractMatchTypeFromMatchPage(matchPage));
      dto.setShow(extractShowFromMatchPage(matchPage));
      dto.setDuration(extractDurationFromMatchPage(matchPage));
      dto.setRating(extractRatingFromMatchPage(matchPage));
      dto.setStipulation(extractStipulationFromMatchPage(matchPage));

      // Extract timestamps
      dto.setCreatedTime(parseInstantFromString(matchPage.getCreated_time()));
      dto.setLastEditedTime(parseInstantFromString(matchPage.getLast_edited_time()));
      dto.setCreatedBy(
          matchPage.getCreated_by() != null ? matchPage.getCreated_by().getName() : null);
      dto.setLastEditedBy(
          matchPage.getLast_edited_by() != null ? matchPage.getLast_edited_by().getName() : null);

      // Set defaults
      dto.setIsTitleMatch(false);
      dto.setIsNpcGenerated(false);
      dto.setMatchDate(dto.getCreatedTime() != null ? dto.getCreatedTime() : Instant.now());

      return dto;

    } catch (Exception e) {
      log.error("Failed to convert match page to DTO: {}", matchPage.getId(), e);
      return null;
    }
  }

  /** Saves match DTOs to the database using parallel processing with caching. */
  private int saveMatchesToDatabase(List<MatchDTO> matchDTOs) {
    log.info("🚀 Starting parallel processing of {} matches", matchDTOs.size());

    // Pre-load and cache entities to avoid repeated database lookups
    Map<String, Show> showCache = preloadShows(matchDTOs);
    Map<String, MatchType> matchTypeCache = preloadMatchTypes(matchDTOs);
    Map<String, Wrestler> wrestlerCache = preloadWrestlers(matchDTOs);

    log.info(
        "✅ Cached {} shows, {} match types, {} wrestlers",
        showCache.size(),
        matchTypeCache.size(),
        wrestlerCache.size());

    // Process matches in parallel and collect results
    List<MatchResult> createdMatches =
        matchDTOs.parallelStream()
            .filter(
                dto -> {
                  if (!dto.isValid()) {
                    log.warn("Skipping invalid match DTO: {}", dto.getSummary());
                    return false;
                  }

                  // Check if match already exists
                  if (matchResultService.existsByExternalId(dto.getExternalId())) {
                    log.debug("Match already exists, skipping: {}", dto.getName());
                    return false;
                  }

                  return true;
                })
            .map(
                dto -> {
                  try {
                    MatchResult matchResult =
                        createMatchResultFromDTO(dto, showCache, matchTypeCache, wrestlerCache);
                    if (matchResult != null) {
                      log.debug("✅ Created match result: {}", dto.getName());
                      return matchResult;
                    } else {
                      log.warn("❌ Failed to create match result: {}", dto.getName());
                      return null;
                    }
                  } catch (Exception e) {
                    log.error("❌ Failed to save match: {}", dto.getSummary(), e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    log.info(
        "✅ Parallel processing completed: {} matches created successfully", createdMatches.size());
    return createdMatches.size();
  }

  /** Pre-loads shows mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, Show> preloadShows(List<MatchDTO> matchDTOs) {
    Set<String> showNames =
        matchDTOs.stream()
            .map(MatchDTO::getShow)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, Show> cache = new HashMap<>();
    for (String showName : showNames) {
      Optional<Show> show = showService.findByName(showName);
      if (show.isPresent()) {
        cache.put(showName, show.get());
      }
    }
    return cache;
  }

  /** Pre-loads match types mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, MatchType> preloadMatchTypes(List<MatchDTO> matchDTOs) {
    Set<String> matchTypeNames =
        matchDTOs.stream()
            .map(MatchDTO::getMatchType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, MatchType> cache = new HashMap<>();
    for (String matchTypeName : matchTypeNames) {
      Optional<MatchType> matchType = matchTypeService.findByName(matchTypeName);
      if (matchType.isPresent()) {
        cache.put(matchTypeName, matchType.get());
      }
    }
    return cache;
  }

  /** Pre-loads wrestlers mentioned in match DTOs to avoid repeated database lookups. */
  private Map<String, Wrestler> preloadWrestlers(List<MatchDTO> matchDTOs) {
    Set<String> wrestlerNames =
        matchDTOs.stream()
            .flatMap(
                dto -> {
                  Set<String> names = new HashSet<>();
                  if (dto.getWinner() != null) names.add(dto.getWinner());
                  if (dto.getParticipants() != null) {
                    names.addAll(dto.getParticipants());
                  }
                  return names.stream();
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<String, Wrestler> cache = new HashMap<>();
    for (String wrestlerName : wrestlerNames) {
      Optional<Wrestler> wrestler = wrestlerService.findByName(wrestlerName);
      if (wrestler.isPresent()) {
        cache.put(wrestlerName, wrestler.get());
      }
    }
    return cache;
  }

  /** Creates a MatchResult entity from a MatchDTO using cached entities for performance. */
  private MatchResult createMatchResultFromDTO(
      MatchDTO dto,
      Map<String, Show> showCache,
      Map<String, MatchType> matchTypeCache,
      Map<String, Wrestler> wrestlerCache) {
    try {
      // Resolve dependencies using caches
      Show show = showCache.get(dto.getShow());
      if (show == null) {
        log.warn("Could not resolve show '{}' for match '{}'", dto.getShow(), dto.getName());
        return null;
      }

      MatchType matchType = matchTypeCache.get(dto.getMatchType());
      if (matchType == null) {
        log.warn(
            "Could not resolve match type '{}' for match '{}'", dto.getMatchType(), dto.getName());
        return null;
      }

      Wrestler winner = wrestlerCache.get(dto.getWinner());
      // Winner can be null for draws

      // Create match result
      MatchResult matchResult =
          matchResultService.createMatchResult(
              show,
              matchType,
              winner,
              dto.getMatchDate(),
              dto.getDuration(),
              dto.getRating(),
              dto.getNarration(),
              dto.getIsTitleMatch(),
              dto.getIsNpcGenerated());

      // Set external ID
      matchResult.setExternalId(dto.getExternalId());
      matchResultService.updateMatchResult(matchResult);

      return matchResult;

    } catch (Exception e) {
      log.error("Failed to create match result from DTO: {}", dto.getSummary(), e);
      return null;
    }
  }

  /** Validates match sync results. */
  private boolean validateMatchSyncResults(List<MatchDTO> matchDTOs, int syncedCount) {
    if (matchDTOs.isEmpty()) {
      return true; // No matches to validate
    }

    // Basic validation: check if at least some matches were synced
    double syncRate = (double) syncedCount / matchDTOs.size();
    if (syncRate < 0.5) { // Less than 50% success rate
      log.warn(
          "Match sync validation failed: only {}/{} matches synced ({}%)",
          syncedCount, matchDTOs.size(), Math.round(syncRate * 100));
      return false;
    }

    log.info(
        "Match sync validation passed: {}/{} matches synced ({}%)",
        syncedCount, matchDTOs.size(), Math.round(syncRate * 100));
    return true;
  }

  /** Parses an Instant from a Notion timestamp string. */
  private Instant parseInstantFromString(String timestampString) {
    if (timestampString == null || timestampString.trim().isEmpty()) {
      return null;
    }

    try {
      return Instant.parse(timestampString);
    } catch (Exception e) {
      log.warn("Failed to parse timestamp: {}", timestampString, e);
      return null;
    }
  }

  // Match property extraction methods
  private List<String> extractParticipantsFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object participants = matchPage.getRawProperties().get("Participants");
        if (participants instanceof List<?> list) {
          return list.stream()
              .map(Object::toString)
              .filter(name -> name != null && !name.trim().isEmpty())
              .collect(Collectors.toList());
        }
      }
      return List.of();
    } catch (Exception e) {
      log.warn("Failed to extract participants from match page: {}", matchPage.getId(), e);
      return List.of();
    }
  }

  private String extractWinnerFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object winner = matchPage.getRawProperties().get("Winner");
        return winner != null ? winner.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract winner from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  private String extractMatchTypeFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object matchType = matchPage.getRawProperties().get("MatchType");
        if (matchType == null) {
          matchType = matchPage.getRawProperties().get("Match Type");
        }
        return matchType != null ? matchType.toString() : "Singles";
      }
      return "Singles";
    } catch (Exception e) {
      log.warn("Failed to extract match type from match page: {}", matchPage.getId(), e);
      return "Singles";
    }
  }

  private String extractShowFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object show = matchPage.getRawProperties().get("Show");
        return show != null ? show.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract show from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  private Integer extractDurationFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object duration = matchPage.getRawProperties().get("Duration");
        if (duration instanceof Number number) {
          return number.intValue();
        } else if (duration instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract duration from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  private Integer extractRatingFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object rating = matchPage.getRawProperties().get("Rating");
        if (rating instanceof Number number) {
          return number.intValue();
        } else if (rating instanceof String str) {
          return Integer.parseInt(str);
        }
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract rating from match page: {}", matchPage.getId(), e);
      return null;
    }
  }

  private String extractStipulationFromMatchPage(MatchPage matchPage) {
    try {
      if (matchPage.getRawProperties() != null) {
        Object stipulation = matchPage.getRawProperties().get("Stipulation");
        return stipulation != null ? stipulation.toString() : null;
      }
      return null;
    } catch (Exception e) {
      log.warn("Failed to extract stipulation from match page: {}", matchPage.getId(), e);
      return null;
    }
  }
}
