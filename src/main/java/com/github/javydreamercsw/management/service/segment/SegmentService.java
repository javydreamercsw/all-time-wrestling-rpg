package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class SegmentService {

  private final SegmentRepository segmentRepository;
  private final TitleRepository titleRepository;
  private final WrestlerService wrestlerService;

  @PersistenceContext private EntityManager entityManager;

  @Autowired
  public SegmentService(
      SegmentRepository segmentRepository,
      TitleRepository titleRepository,
      @Lazy WrestlerService wrestlerService) {
    this.segmentRepository = segmentRepository;
    this.titleRepository = titleRepository;
    this.wrestlerService = wrestlerService;
  }

  /**
   * Converts a SegmentDTO to a Segment entity.
   *
   * @param dto The SegmentDTO to convert.
   * @return The corresponding Segment entity.
   */
  public Segment toEntity(@NonNull SegmentDTO dto) {
    Segment segment = new Segment();
    segment.setExternalId(dto.getExternalId());
    segment.setNarration(dto.getNarration());
    // Assuming show, segmentType, participants, and winners are handled elsewhere or are not
    // directly mapped from DTO
    segment.setSegmentDate(dto.getSegmentDate());
    // Titles
    if (dto.getTitleIds() != null && !dto.getTitleIds().isEmpty()) {
      Set<Title> titles = new java.util.HashSet<>();
      for (Long titleId : dto.getTitleIds()) {
        titleRepository.findById(titleId).ifPresent(titles::add);
      }
      segment.setTitles(titles);
    }
    return segment;
  }

  /**
   * Converts a Segment entity to a SegmentDTO.
   *
   * @param segment The Segment entity to convert.
   * @return The corresponding SegmentDTO.
   */
  public SegmentDTO toDto(@NonNull Segment segment) {
    SegmentDTO dto = new SegmentDTO();
    dto.setExternalId(segment.getExternalId());
    dto.setName(segment.getNarration()); // Assuming narration is used as name for DTO
    dto.setShowName(segment.getShow().getName());
    dto.setShowExternalId(segment.getShow().getExternalId());
    dto.setParticipantNames(
        segment.getParticipants().stream()
            .map(p -> p.getWrestler().getName())
            .collect(java.util.stream.Collectors.toList()));
    dto.setWinnerNames(
        segment.getWinners().stream()
            .map(Wrestler::getName)
            .collect(java.util.stream.Collectors.toList()));
    dto.setSegmentTypeName(segment.getSegmentType().getName());
    dto.setSegmentDate(segment.getSegmentDate());
    dto.setNarration(segment.getNarration());
    dto.setTitleIds(
        segment.getTitles().stream()
            .map(Title::getId)
            .collect(java.util.stream.Collectors.toList()));
    dto.setSegmentOrder(segment.getSegmentOrder());
    dto.setMainEvent(segment.isMainEvent());
    return dto;
  }

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @return The created Segment
   */
  public Segment createSegment(
      @NonNull Show show, @NonNull SegmentType matchType, @NonNull Instant matchDate) {
    return createSegment(show, matchType, matchDate, Set.of());
  }

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @param titles The titles contested in this segment
   * @return The created Segment
   */
  public Segment createSegment(
      @NonNull Show show,
      @NonNull SegmentType matchType,
      @NonNull Instant matchDate,
      @NonNull Set<Title> titles) {

    Segment match = new Segment();
    match.setShow(show);
    match.setSegmentType(matchType);
    match.setSegmentDate(matchDate);
    match.setIsTitleSegment(!titles.isEmpty());
    match.setTitles(titles);

    Segment saved = segmentRepository.save(match);
    log.info("Created match with ID: {} for show: {}", saved.getId(), show.getName());
    return saved;
  }

  /**
   * Updates an existing segment based on DTO information.
   *
   * @param id The ID of the segment to update.
   * @param dto The SegmentDTO containing updated information.
   * @return The updated Segment.
   * @throws IllegalArgumentException if the segment with the given ID is not found.
   */
  public Segment updateSegment(@NonNull Long id, @NonNull SegmentDTO dto) {
    return segmentRepository
        .findById(id)
        .map(
            existingSegment -> {
              existingSegment.setNarration(dto.getNarration());
              existingSegment.setSegmentDate(dto.getSegmentDate());
              existingSegment.setIsTitleSegment(!dto.getTitleIds().isEmpty());

              // Update titles
              Set<Title> newTitles = new java.util.HashSet<>();
              if (dto.getTitleIds() != null) {
                for (Long titleId : dto.getTitleIds()) {
                  titleRepository.findById(titleId).ifPresent(newTitles::add);
                }
              }
              existingSegment.setTitles(newTitles);

              // TODO: Handle participants and segment type updates if needed from DTO

              return segmentRepository.save(existingSegment);
            })
        .orElseThrow(() -> new IllegalArgumentException("Segment not found with ID: " + id));
  }

  /**
   * Updates an existing segment.
   *
   * @param segment The segment to update
   * @return The updated Segment
   */
  public Segment updateSegment(@NonNull Segment segment) {
    return segmentRepository.save(segment);
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
  public Page<Segment> getSegmentsByWrestlerParticipation(
      @NonNull Wrestler wrestler, @NonNull Pageable pageable) {
    return segmentRepository.findByWrestlerParticipation(wrestler, pageable);
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
   * Gets all matches where a wrestler participated within a specific season.
   *
   * @param wrestler The wrestler to search for
   * @param season The season to filter by
   * @return List of Segment objects where the wrestler participated in the given season
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsByWrestlerAndSeason(
      @NonNull Wrestler wrestler, @NonNull Season season) {
    return segmentRepository.findByWrestlerParticipationAndSeason(wrestler, season);
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

  /**
   * Gets all matches where a wrestler participated with the show eagerly fetched.
   *
   * @param wrestler The wrestler to search for
   * @return List of Segment objects where the wrestler participated
   */
  @Transactional(readOnly = true)
  public List<Segment> getSegmentsByWrestlerParticipationWithShow(@NonNull Wrestler wrestler) {
    return segmentRepository.findByWrestlerParticipationWithShow(wrestler);
  }
}
