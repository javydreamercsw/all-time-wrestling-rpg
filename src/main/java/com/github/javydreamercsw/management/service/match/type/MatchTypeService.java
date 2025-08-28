package com.github.javydreamercsw.management.service.match.type;

import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing match types in the ATW RPG system. Provides business logic for creating,
 * retrieving, and managing match types that define the structure and rules of wrestling matches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatchTypeService {

  private final MatchTypeRepository matchTypeRepository;
  private final Clock clock;

  /**
   * Get paginated list of match types.
   *
   * @param pageable Pagination information
   * @return List of match types
   */
  public List<MatchType> list(Pageable pageable) {
    return matchTypeRepository.findAllBy(pageable).toList();
  }

  /**
   * Get total count of match types.
   *
   * @return Total count
   */
  public long count() {
    return matchTypeRepository.count();
  }

  /**
   * Save a match type.
   *
   * @param matchType The match type to save
   * @return The saved match type
   */
  public MatchType save(@NonNull MatchType matchType) {
    matchType.setCreationDate(clock.instant());
    return matchTypeRepository.saveAndFlush(matchType);
  }

  /**
   * Get all match types.
   *
   * @return List of all match types
   */
  public List<MatchType> findAll() {
    return matchTypeRepository.findAll();
  }

  /**
   * Find a match type by name.
   *
   * @param name The name of the match type
   * @return Optional containing the match type if found
   */
  public Optional<MatchType> findByName(@NonNull String name) {
    return matchTypeRepository.findByName(name);
  }

  /**
   * Check if a match type exists by name.
   *
   * @param name The name to check
   * @return true if a match type with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return matchTypeRepository.existsByName(name);
  }

  /**
   * Create or update a match type from external data.
   *
   * @param name Name of the match type
   * @param description Description of the match type
   * @return The created or updated match type
   */
  @Transactional
  public MatchType createOrUpdateMatchType(@NonNull String name, String description) {
    Optional<MatchType> existingOpt = matchTypeRepository.findByName(name);

    MatchType matchType;
    if (existingOpt.isPresent()) {
      matchType = existingOpt.get();
      log.debug("Updating existing match type: {}", name);
    } else {
      matchType = new MatchType();
      matchType.setCreationDate(clock.instant());
      log.info("Creating new match type: {}", name);
    }

    matchType.setName(name);
    matchType.setDescription(description);

    return matchTypeRepository.save(matchType);
  }

  /**
   * Find match type by ID.
   *
   * @param id The ID of the match type
   * @return Optional containing the match type if found
   */
  public Optional<MatchType> findById(@NonNull Long id) {
    return matchTypeRepository.findById(id);
  }
}
