package com.github.javydreamercsw.management.domain.show.template;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ShowTemplate entities. Provides data access operations for show
 * templates.
 */
@Repository
public interface ShowTemplateRepository extends JpaRepository<ShowTemplate, Long> {

  /**
   * Find a show template by name.
   *
   * @param name The name of the template
   * @return Optional containing the template if found
   */
  Optional<ShowTemplate> findByName(String name);

  /**
   * Check if a show template exists by name.
   *
   * @param name The name to check
   * @return true if a template with this name exists
   */
  boolean existsByName(String name);

  /**
   * Find all show templates with pagination.
   *
   * @param pageable Pagination information
   * @return Page of show templates
   */
  Page<ShowTemplate> findAllBy(Pageable pageable);

  /**
   * Find all templates for Premium Live Events (PLEs).
   *
   * @return List of PLE templates
   */
  @Query("SELECT st FROM ShowTemplate st WHERE st.showType.name = 'Premium Live Event (PLE)'")
  List<ShowTemplate> findPremiumLiveEventTemplates();

  /**
   * Find all templates for Weekly shows.
   *
   * @return List of weekly show templates
   */
  @Query("SELECT st FROM ShowTemplate st WHERE st.showType.name = 'Weekly'")
  List<ShowTemplate> findWeeklyShowTemplates();

  /**
   * Find templates by show type name.
   *
   * @param showTypeName The name of the show type
   * @return List of templates for the specified show type
   */
  @Query("SELECT st FROM ShowTemplate st WHERE st.showType.name = :showTypeName")
  List<ShowTemplate> findByShowTypeName(@Param("showTypeName") String showTypeName);

  /**
   * Find all show templates with ShowType eagerly loaded.
   *
   * @return List of show templates with show types
   */
  @Query("SELECT st FROM ShowTemplate st JOIN FETCH st.showType")
  List<ShowTemplate> findAllWithShowType();
}
