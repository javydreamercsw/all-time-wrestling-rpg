package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SegmentService {

  @Autowired private final SegmentRepository segmentRepository;

  @PersistenceContext private EntityManager entityManager;

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @param isTitleSegment Whether this was a title match
   * @return The created Segment
   */
  public Segment createSegment(
      @NonNull Show show,
      @NonNull SegmentType matchType,
      @NonNull Instant matchDate,
      @NonNull Boolean isTitleSegment) {

    Segment match = new Segment();
    match.setShow(show);
    match.setSegmentType(matchType);
    match.setSegmentDate(matchDate);
    match.setIsTitleSegment(isTitleSegment);

    Segment saved = segmentRepository.save(match);
    log.info("Created match with ID: {} for show: {}", saved.getId(), show.getName());
    return saved;
  }

  /**
   * Updates an existing segment.
   *
   * @param segment The segment to update
   * @return The updated Segment
   */
  public Segment updateSegment(@NonNull Segment segment) {
    try {
      return segmentRepository.save(segment);
    } catch (DataIntegrityViolationException e) {
      log.error(
          "Data integrity violation when saving segment with external ID {}: {}",
          segment.getExternalId(),
          e.getMessage());
      // Attempt to find by external ID and update if it's a unique constraint violation
      if (e.getMessage() != null && e.getMessage().contains("unique constraint")) {
        // Detach the problematic entity from the session
        entityManager.detach(segment);

        Optional<Segment> existingSegment =
            segmentRepository.findByExternalId(segment.getExternalId());
        if (existingSegment.isPresent()) {
          log.warn(
              "Segment with external ID {} already exists, attempting to merge.",
              segment.getExternalId());
          // Copy properties from the new segment to the existing one
          Segment foundSegment = existingSegment.get();
          // BeanUtils.copyProperties(segment, foundSegment, "id"); // Exclude ID
          foundSegment.setShow(segment.getShow());
          foundSegment.setSegmentType(segment.getSegmentType());
          foundSegment.setWinner(segment.getWinner());
          foundSegment.setSegmentDate(segment.getSegmentDate());
          foundSegment.setStatus(segment.getStatus());
          foundSegment.setNarration(segment.getNarration());
          foundSegment.setIsTitleSegment(segment.getIsTitleSegment());
          foundSegment.setIsNpcGenerated(segment.getIsNpcGenerated());
          // Clear and re-add participants and rules to ensure they are updated
          foundSegment.getParticipants().clear();
          segment
              .getParticipants()
              .forEach(participant -> foundSegment.addParticipant(participant.getWrestler()));
          foundSegment.getSegmentRules().clear();
          segment.getSegmentRules().forEach(foundSegment::addSegmentRule);

          return segmentRepository.save(foundSegment);
        }
      }
      throw e; // Re-throw if not a unique constraint violation or cannot be handled
    }
  }

  /**
   * Finds a match by ID.
   *
   * @param id The match ID
   * @return Optional containing the Segment if found
   */
  @Transactional(readOnly = true)
  public Optional<Segment> findById(@NonNull Long id) {
    return segmentRepository.findById(id);
  }

  /**
   * Gets all matches with pagination.
   *
   * @param pageable Pagination information
   * @return Page of Segment objects
   */
  @Transactional(readOnly = true)
  public Page<Segment> getAllSegments(@NonNull Pageable pageable) {
    return segmentRepository.findAllBy(pageable);
  }

  /**
   * Gets all matches for a specific show.
   *
   * @param show The show to get matches for
   * @return List of Segment objects for the show
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsByShow(@NonNull Show show) {
    return segmentRepository.findByShow(show);
  }

  /**
   * Gets all matches where a wrestler participated.
   *
   * @param wrestler The wrestler to search for
   * @return List of Segment objects where the wrestler participated
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsByWrestlerParticipation(@NonNull Wrestler wrestler) {
    return segmentRepository.findByWrestlerParticipation(wrestler);
  }

  /**
   * Gets all matches won by a specific wrestler.
   *
   * @param wrestler The wrestler to search for
   * @return List of Segment objects won by the wrestler
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsByWinner(@NonNull Wrestler wrestler) {
    return segmentRepository.findByWinner(wrestler);
  }

  /**
   * Gets matches between two specific wrestlers.
   *
   * @param wrestler1 First wrestler
   * @param wrestler2 Second wrestler
   * @return List of Segment objects between the two wrestlers
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsBetween(
      @NonNull Wrestler wrestler1, @NonNull Wrestler wrestler2) {
    return segmentRepository.findSegmentsBetween(wrestler1, wrestler2);
  }

  /**
   * Gets all NPC-generated matches.
   *
   * @return List of NPC-generated Segment objects
   */
  @Transactional(readOnly = true)
  public List<Segment> getNpcGeneratedSegments() {
    return segmentRepository.findByIsNpcGeneratedTrue();
  }

  /**
   * Gets all title matches.
   *
   * @return List of title Segment objects
   */
  @Transactional(readOnly = true)
  public List<Segment> getTitleSegments() {
    return segmentRepository.findByIsTitleSegmentTrue();
  }

  /**
   * Gets matches after a specific date.
   *
   * @param date The date to search after
   * @return List of Segment objects after the specified date
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsAfter(@NonNull Instant date) {
    return segmentRepository.findBySegmentDateAfter(date);
  }

  /**
   * Counts wins for a wrestler.
   *
   * @param wrestler The wrestler to count wins for
   * @return Number of wins
   */
  @Transactional(readOnly = true)
  public long countWinsByWrestler(@NonNull Wrestler wrestler) {
    return segmentRepository.countWinsByWrestler(wrestler);
  }

  /**
   * Counts total matches for a wrestler.
   *
   * @param wrestler The wrestler to count matches for
   * @return Total number of matches
   */
  @Transactional(readOnly = true)
  public long countSegmentsByWrestler(@NonNull Wrestler wrestler) {
    return segmentRepository.countSegmentsByWrestler(wrestler);
  }

  /**
   * Deletes a match.
   *
   * @param id The ID of the match to delete
   */
  public void deleteSegment(@NonNull Long id) {
    segmentRepository.deleteById(id);
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
    return segmentRepository.existsByExternalId(externalId);
  }

  /**
   * Finds a match by external ID.
   *
   * @param externalId The external ID to search for
   * @return Optional containing the Segment if found
   */
  @Transactional(readOnly = true)
  public Optional<Segment> findByExternalId(@NonNull String externalId) {
    return segmentRepository.findByExternalId(externalId);
  }

  /**
   * Gets all external IDs of all matches.
   *
   * @return List of all external IDs.
   */
  @Transactional(readOnly = true)
  public List<String> getAllExternalIds() {
    return segmentRepository.findAllExternalIds();
  }
}
