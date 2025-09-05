package com.github.javydreamercsw.management.service.show.template;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing show templates in the ATW RPG system. Provides business logic for creating,
 * retrieving, and managing show templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ShowTemplateService {

  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private Clock clock;

  /**
   * Get paginated list of show templates.
   *
   * @param pageable Pagination information
   * @return List of show templates
   */
  public List<ShowTemplate> list(Pageable pageable) {
    return showTemplateRepository.findAllBy(pageable).toList();
  }

  /**
   * Get total count of show templates.
   *
   * @return Total count
   */
  public long count() {
    return showTemplateRepository.count();
  }

  /**
   * Save a show template.
   *
   * @param showTemplate The show template to save
   * @return The saved show template
   */
  public ShowTemplate save(@NonNull ShowTemplate showTemplate) {
    showTemplate.setCreationDate(clock.instant());
    return showTemplateRepository.saveAndFlush(showTemplate);
  }

  /**
   * Get all show templates.
   *
   * @return List of all show templates
   */
  public List<ShowTemplate> findAll() {
    return showTemplateRepository.findAllWithShowType();
  }

  /**
   * Find a show template by name.
   *
   * @param name The name of the show template
   * @return Optional containing the show template if found
   */
  public Optional<ShowTemplate> findByName(@NonNull String name) {
    return showTemplateRepository.findByName(name);
  }

  /**
   * Check if a show template exists by name.
   *
   * @param name The name to check
   * @return true if a show template with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return showTemplateRepository.existsByName(name);
  }

  /**
   * Find show template by ID.
   *
   * @param id The ID of the show template
   * @return Optional containing the show template if found
   */
  public Optional<ShowTemplate> findById(@NonNull Long id) {
    return showTemplateRepository.findById(id);
  }

  /**
   * Get all Premium Live Event templates.
   *
   * @return List of PLE templates
   */
  public List<ShowTemplate> getPremiumLiveEventTemplates() {
    return showTemplateRepository.findPremiumLiveEventTemplates();
  }

  /**
   * Get all Weekly show templates.
   *
   * @return List of weekly show templates
   */
  public List<ShowTemplate> getWeeklyShowTemplates() {
    return showTemplateRepository.findWeeklyShowTemplates();
  }

  /**
   * Find templates by show type name.
   *
   * @param showTypeName The name of the show type
   * @return List of templates for the specified show type
   */
  public List<ShowTemplate> findByShowTypeName(@NonNull String showTypeName) {
    return showTemplateRepository.findByShowTypeName(showTypeName);
  }

  /**
   * Create or update a show template from external data.
   *
   * @param name Name of the show template
   * @param description Description of the show template
   * @param showTypeName Name of the show type
   * @param notionUrl URL to the Notion page for this template
   * @return The created or updated show template
   */
  @Transactional
  public ShowTemplate createOrUpdateTemplate(
      @NonNull String name, String description, @NonNull String showTypeName, String notionUrl) {

    // Find or create show type
    Optional<ShowType> showTypeOpt = showTypeRepository.findByName(showTypeName);
    if (showTypeOpt.isEmpty()) {
      log.warn("Show type not found: {}", showTypeName);
      return null;
    }

    ShowType showType = showTypeOpt.get();
    Optional<ShowTemplate> existingOpt = showTemplateRepository.findByName(name);

    ShowTemplate template;
    if (existingOpt.isPresent()) {
      template = existingOpt.get();
      log.debug("Updating existing show template: {}", name);
    } else {
      template = new ShowTemplate();
      template.setCreationDate(clock.instant());
      log.info("Creating new show template: {}", name);
    }

    template.setName(name);
    template.setDescription(description);
    template.setShowType(showType);
    template.setNotionUrl(notionUrl);

    return showTemplateRepository.save(template);
  }

  /**
   * Get paginated list of all show templates.
   *
   * @param pageable Pagination information
   * @return Page of show templates
   */
  public Page<ShowTemplate> getAllTemplates(Pageable pageable) {
    return showTemplateRepository.findAll(pageable);
  }

  /**
   * Get show template by ID.
   *
   * @param id The ID of the show template
   * @return Optional containing the show template if found
   */
  public Optional<ShowTemplate> getTemplateById(@NonNull Long id) {
    return showTemplateRepository.findById(id);
  }

  /**
   * Get templates by show type name.
   *
   * @param showTypeName The name of the show type
   * @return List of templates for the specified show type
   */
  public List<ShowTemplate> getTemplatesByShowType(@NonNull String showTypeName) {
    return showTemplateRepository.findByShowTypeName(showTypeName);
  }

  /**
   * Update an existing show template.
   *
   * @param id The ID of the show template to update
   * @param name New name for the show template
   * @param description New description for the show template
   * @param showTypeName New show type name
   * @param notionUrl New Notion URL
   * @return Optional containing the updated show template if found
   */
  @Transactional
  public Optional<ShowTemplate> updateTemplate(
      @NonNull Long id,
      @NonNull String name,
      String description,
      @NonNull String showTypeName,
      String notionUrl) {

    Optional<ShowTemplate> templateOpt = showTemplateRepository.findById(id);
    if (templateOpt.isEmpty()) {
      log.warn("Show template not found with ID: {}", id);
      return Optional.empty();
    }

    // Find show type
    Optional<ShowType> showTypeOpt = showTypeRepository.findByName(showTypeName);
    if (showTypeOpt.isEmpty()) {
      log.warn("Show type not found: {}", showTypeName);
      return Optional.empty();
    }

    ShowTemplate template = templateOpt.get();
    template.setName(name);
    template.setDescription(description);
    template.setShowType(showTypeOpt.get());
    template.setNotionUrl(notionUrl);

    ShowTemplate savedTemplate = showTemplateRepository.save(template);
    log.info("Updated show template: {}", name);
    return Optional.of(savedTemplate);
  }

  /**
   * Delete a show template by ID.
   *
   * @param id The ID of the show template to delete
   * @return true if the template was deleted, false if not found
   */
  @Transactional
  public boolean deleteTemplate(@NonNull Long id) {
    if (showTemplateRepository.existsById(id)) {
      showTemplateRepository.deleteById(id);
      log.info("Deleted show template with ID: {}", id);
      return true;
    } else {
      log.warn("Show template not found with ID: {}", id);
      return false;
    }
  }
}
