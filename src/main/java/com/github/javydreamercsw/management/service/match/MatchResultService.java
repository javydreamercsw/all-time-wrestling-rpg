package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.MatchResultRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing match results in the ATW RPG system. Provides CRUD operations and business
 * logic for match results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatchResultService {

  private final MatchResultRepository matchResultRepository;

  /**
   * Creates a new match result.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param winner The winning wrestler (can be null for draws)
   * @param matchDate The date/time of the match
   * @param durationMinutes Duration of the match in minutes
   * @param matchRating Rating of the match (1-5)
   * @param narration Match narration text
   * @param isTitleMatch Whether this was a title match
   * @param isNpcGenerated Whether this match was NPC-generated
   * @return The created MatchResult
   */
  public MatchResult createMatchResult(
      Show show,
      MatchType matchType,
      Wrestler winner,
      Instant matchDate,
      Integer durationMinutes,
      Integer matchRating,
      String narration,
      Boolean isTitleMatch,
      Boolean isNpcGenerated) {

    MatchResult matchResult = new MatchResult();
    matchResult.setShow(show);
    matchResult.setMatchType(matchType);
    matchResult.setWinner(winner);
    matchResult.setMatchDate(matchDate != null ? matchDate : Instant.now());
    matchResult.setDurationMinutes(durationMinutes);
    matchResult.setMatchRating(matchRating);
    matchResult.setNarration(narration);
    matchResult.setIsTitleMatch(isTitleMatch != null ? isTitleMatch : false);
    matchResult.setIsNpcGenerated(isNpcGenerated != null ? isNpcGenerated : false);

    MatchResult saved = matchResultRepository.save(matchResult);
    log.info("Created match result with ID: {} for show: {}", saved.getId(), show.getName());
    return saved;
  }

  /**
   * Updates an existing match result.
   *
   * @param matchResult The match result to update
   * @return The updated MatchResult
   */
  public MatchResult updateMatchResult(MatchResult matchResult) {
    MatchResult updated = matchResultRepository.save(matchResult);
    log.info("Updated match result with ID: {}", updated.getId());
    return updated;
  }

  /**
   * Finds a match result by ID.
   *
   * @param id The match result ID
   * @return Optional containing the MatchResult if found
   */
  @Transactional(readOnly = true)
  public Optional<MatchResult> findById(Long id) {
    return matchResultRepository.findById(id);
  }

  /**
   * Gets all match results with pagination.
   *
   * @param pageable Pagination information
   * @return Page of MatchResult objects
   */
  @Transactional(readOnly = true)
  public Page<MatchResult> getAllMatchResults(Pageable pageable) {
    return matchResultRepository.findAllBy(pageable);
  }

  /**
   * Gets all match results for a specific show.
   *
   * @param show The show to get matches for
   * @return List of MatchResult objects for the show
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getMatchResultsByShow(Show show) {
    return matchResultRepository.findByShow(show);
  }

  /**
   * Gets all matches where a wrestler participated.
   *
   * @param wrestler The wrestler to search for
   * @return List of MatchResult objects where the wrestler participated
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getMatchResultsByWrestlerParticipation(Wrestler wrestler) {
    return matchResultRepository.findByWrestlerParticipation(wrestler);
  }

  /**
   * Gets all matches won by a specific wrestler.
   *
   * @param wrestler The wrestler to search for
   * @return List of MatchResult objects won by the wrestler
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getMatchResultsByWinner(Wrestler wrestler) {
    return matchResultRepository.findByWinner(wrestler);
  }

  /**
   * Gets matches between two specific wrestlers.
   *
   * @param wrestler1 First wrestler
   * @param wrestler2 Second wrestler
   * @return List of MatchResult objects between the two wrestlers
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getMatchesBetween(Wrestler wrestler1, Wrestler wrestler2) {
    return matchResultRepository.findMatchesBetween(wrestler1, wrestler2);
  }

  /**
   * Gets all NPC-generated matches.
   *
   * @return List of NPC-generated MatchResult objects
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getNpcGeneratedMatches() {
    return matchResultRepository.findByIsNpcGeneratedTrue();
  }

  /**
   * Gets all title matches.
   *
   * @return List of title MatchResult objects
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getTitleMatches() {
    return matchResultRepository.findByIsTitleMatchTrue();
  }

  /**
   * Gets matches after a specific date.
   *
   * @param date The date to search after
   * @return List of MatchResult objects after the specified date
   */
  @Transactional(readOnly = true)
  public List<MatchResult> getMatchesAfter(Instant date) {
    return matchResultRepository.findByMatchDateAfter(date);
  }

  /**
   * Counts wins for a wrestler.
   *
   * @param wrestler The wrestler to count wins for
   * @return Number of wins
   */
  @Transactional(readOnly = true)
  public long countWinsByWrestler(Wrestler wrestler) {
    return matchResultRepository.countWinsByWrestler(wrestler);
  }

  /**
   * Counts total matches for a wrestler.
   *
   * @param wrestler The wrestler to count matches for
   * @return Total number of matches
   */
  @Transactional(readOnly = true)
  public long countMatchesByWrestler(Wrestler wrestler) {
    return matchResultRepository.countMatchesByWrestler(wrestler);
  }

  /**
   * Deletes a match result.
   *
   * @param id The ID of the match result to delete
   */
  public void deleteMatchResult(Long id) {
    matchResultRepository.deleteById(id);
    log.info("Deleted match result with ID: {}", id);
  }

  /**
   * Checks if a match result exists by external ID.
   *
   * @param externalId The external ID to check
   * @return true if a match result with the external ID exists
   */
  @Transactional(readOnly = true)
  public boolean existsByExternalId(String externalId) {
    return matchResultRepository.existsByExternalId(externalId);
  }

  /**
   * Finds a match result by external ID.
   *
   * @param externalId The external ID to search for
   * @return Optional containing the MatchResult if found
   */
  @Transactional(readOnly = true)
  public Optional<MatchResult> findByExternalId(String externalId) {
    return matchResultRepository.findByExternalId(externalId);
  }
}
