package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing matches in the ATW RPG system. Provides CRUD operations and business logic
 * for matches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatchService {

  @Autowired private MatchRepository matchRepository;

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @param isTitleMatch Whether this was a title match
   * @return The created Match
   */
  public Match createMatch(
      @NonNull Show show,
      @NonNull MatchType matchType,
      @NonNull Instant matchDate,
      @NonNull Boolean isTitleMatch) {

    Match match = new Match();
    match.setShow(show);
    match.setMatchType(matchType);
    match.setMatchDate(matchDate);
    match.setIsTitleMatch(isTitleMatch);

    Match saved = matchRepository.save(match);
    log.info("Created match with ID: {} for show: {}", saved.getId(), show.getName());
    return saved;
  }

  /**
   * Updates an existing match.
   *
   * @param match The match to update
   * @return The updated Match
   */
  public Match updateMatch(@NonNull Match match) {
    Match updated = matchRepository.save(match);
    log.info("Updated match with ID: {}", updated.getId());
    return updated;
  }

  /**
   * Finds a match by ID.
   *
   * @param id The match ID
   * @return Optional containing the Match if found
   */
  @Transactional(readOnly = true)
  public Optional<Match> findById(@NonNull Long id) {
    return matchRepository.findById(id);
  }

  /**
   * Gets all matches with pagination.
   *
   * @param pageable Pagination information
   * @return Page of Match objects
   */
  @Transactional(readOnly = true)
  public Page<Match> getAllMatches(@NonNull Pageable pageable) {
    return matchRepository.findAllBy(pageable);
  }

  /**
   * Gets all matches for a specific show.
   *
   * @param show The show to get matches for
   * @return List of Match objects for the show
   */
  @Transactional(readOnly = true)
  public List<Match> getMatchesByShow(@NonNull Show show) {
    return matchRepository.findByShow(show);
  }

  /**
   * Gets all matches where a wrestler participated.
   *
   * @param wrestler The wrestler to search for
   * @return List of Match objects where the wrestler participated
   */
  @Transactional(readOnly = true)
  public List<Match> getMatchesByWrestlerParticipation(@NonNull Wrestler wrestler) {
    return matchRepository.findByWrestlerParticipation(wrestler);
  }

  /**
   * Gets all matches won by a specific wrestler.
   *
   * @param wrestler The wrestler to search for
   * @return List of Match objects won by the wrestler
   */
  @Transactional(readOnly = true)
  public List<Match> getMatchesByWinner(@NonNull Wrestler wrestler) {
    return matchRepository.findByWinner(wrestler);
  }

  /**
   * Gets matches between two specific wrestlers.
   *
   * @param wrestler1 First wrestler
   * @param wrestler2 Second wrestler
   * @return List of Match objects between the two wrestlers
   */
  @Transactional(readOnly = true)
  public List<Match> getMatchesBetween(@NonNull Wrestler wrestler1, @NonNull Wrestler wrestler2) {
    return matchRepository.findMatchesBetween(wrestler1, wrestler2);
  }

  /**
   * Gets all NPC-generated matches.
   *
   * @return List of NPC-generated Match objects
   */
  @Transactional(readOnly = true)
  public List<Match> getNpcGeneratedMatches() {
    return matchRepository.findByIsNpcGeneratedTrue();
  }

  /**
   * Gets all title matches.
   *
   * @return List of title Match objects
   */
  @Transactional(readOnly = true)
  public List<Match> getTitleMatches() {
    return matchRepository.findByIsTitleMatchTrue();
  }

  /**
   * Gets matches after a specific date.
   *
   * @param date The date to search after
   * @return List of Match objects after the specified date
   */
  @Transactional(readOnly = true)
  public List<Match> getMatchesAfter(@NonNull Instant date) {
    return matchRepository.findByMatchDateAfter(date);
  }

  /**
   * Counts wins for a wrestler.
   *
   * @param wrestler The wrestler to count wins for
   * @return Number of wins
   */
  @Transactional(readOnly = true)
  public long countWinsByWrestler(@NonNull Wrestler wrestler) {
    return matchRepository.countWinsByWrestler(wrestler);
  }

  /**
   * Counts total matches for a wrestler.
   *
   * @param wrestler The wrestler to count matches for
   * @return Total number of matches
   */
  @Transactional(readOnly = true)
  public long countMatchesByWrestler(@NonNull Wrestler wrestler) {
    return matchRepository.countMatchesByWrestler(wrestler);
  }

  /**
   * Deletes a match.
   *
   * @param id The ID of the match to delete
   */
  public void deleteMatch(@NonNull Long id) {
    matchRepository.deleteById(id);
    log.info("Deleted match with ID: {}", id);
  }

  /**
   * Checks if a match exists by external ID.
   *
   * @param externalId The external ID to check
   * @return true if a match with the external ID exists
   */
  @Transactional(readOnly = true)
  public boolean existsByExternalId(@NonNull String externalId) {
    return matchRepository.existsByExternalId(externalId);
  }

  /**
   * Finds a match by external ID.
   *
   * @param externalId The external ID to search for
   * @return Optional containing the Match if found
   */
  @Transactional(readOnly = true)
  public Optional<Match> findByExternalId(@NonNull String externalId) {
    return matchRepository.findByExternalId(externalId);
  }
}
