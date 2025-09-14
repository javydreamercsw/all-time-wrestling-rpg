package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing segment rules in the ATW RPG system. Provides business logic for creating,
 * retrieving, and managing segment rules that can be applied to wrestling matches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SegmentRuleService {

  @Autowired private SegmentRuleRepository segmentRuleRepository;

  /**
   * Get all active segment rules.
   *
   * @return List of active segment rules
   */
  public List<SegmentRule> getAllActiveRules() {
    return segmentRuleRepository.findByIsActiveTrue();
  }

  /**
   * Find a segment rule by name.
   *
   * @param name The name of the segment rule
   * @return Optional containing the segment rule if found
   */
  public Optional<SegmentRule> findByName(String name) {
    return segmentRuleRepository.findByName(name);
  }

  /**
   * Get segment rules suitable for high-heat rivalries.
   *
   * @return List of segment rules appropriate for intense rivalries
   */
  public List<SegmentRule> getHighHeatRules() {
    return segmentRuleRepository.findSuitableForHighHeat();
  }

  /**
   * Get standard segment rules (not requiring high heat).
   *
   * @return List of standard segment rules
   */
  public List<SegmentRule> getStandardRules() {
    return segmentRuleRepository.findStandardRules();
  }

  /**
   * Create a new segment rule.
   *
   * @param name The name of the segment rule
   * @param description Optional description of the rule
   * @param requiresHighHeat Whether this rule requires high heat to be used
   * @return The created segment rule
   */
  @Transactional
  public SegmentRule createRule(
      @NonNull String name, @NonNull String description, boolean requiresHighHeat) {
    if (segmentRuleRepository.existsByName(name)) {
      throw new IllegalArgumentException("Segment rule with name '" + name + "' already exists");
    }

    SegmentRule rule = new SegmentRule();
    rule.setName(name);
    rule.setDescription(description);
    rule.setRequiresHighHeat(requiresHighHeat);
    rule.setIsActive(true);

    SegmentRule savedRule = segmentRuleRepository.save(rule);
    log.info("Created new segment rule: {}", savedRule.getName());
    return savedRule;
  }

  /**
   * Update an existing segment rule.
   *
   * @param id The ID of the segment rule to update
   * @param name New name (optional)
   * @param description New description (optional)
   * @param requiresHighHeat New high heat requirement (optional)
   * @param isActive New active status (optional)
   * @return The updated segment rule
   */
  @Transactional
  public SegmentRule updateRule(
      @NonNull Long id,
      @NonNull String name,
      @NonNull String description,
      @NonNull Boolean requiresHighHeat,
      @NonNull Boolean isActive) {
    SegmentRule rule =
        segmentRuleRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Segment rule not found with ID: " + id));

    if (!name.equals(rule.getName())) {
      if (segmentRuleRepository.existsByName(name)) {
        throw new IllegalArgumentException("Segment rule with name '" + name + "' already exists");
      }
      rule.setName(name);
    }

    rule.setDescription(description);

    rule.setRequiresHighHeat(requiresHighHeat);

    rule.setIsActive(isActive);

    SegmentRule savedRule = segmentRuleRepository.save(rule);
    log.info("Updated segment rule: {}", savedRule.getName());
    return savedRule;
  }

  /**
   * Deactivate a segment rule (soft delete).
   *
   * @param id The ID of the segment rule to deactivate
   */
  @Transactional
  public void deactivateRule(@NonNull Long id) {
    SegmentRule rule =
        segmentRuleRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Segment rule not found with ID: " + id));

    rule.setIsActive(false);
    segmentRuleRepository.save(rule);
    log.info("Deactivated segment rule: {}", rule.getName());
  }

  /**
   * Check if a segment rule exists by name.
   *
   * @param name The name to check
   * @return true if a rule with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return segmentRuleRepository.existsByName(name);
  }

  /**
   * Get a segment rule by ID.
   *
   * @param id The ID of the segment rule
   * @return Optional containing the segment rule if found
   */
  public Optional<SegmentRule> findById(@NonNull Long id) {
    return segmentRuleRepository.findById(id);
  }

  /**
   * Get all segment rules (including inactive ones).
   *
   * @return List of all segment rules
   */
  public List<SegmentRule> getAllRules() {
    return segmentRuleRepository.findAll();
  }

  /**
   * Create or update a segment rule from external data.
   *
   * @param name Name of the segment rule
   * @param description Description of the segment rule
   * @param requiresHighHeat Whether this rule requires high heat
   * @return The created or updated segment rule
   */
  @Transactional
  public SegmentRule createOrUpdateRule(
      @NonNull String name, String description, boolean requiresHighHeat) {
    Optional<SegmentRule> existingOpt = segmentRuleRepository.findByName(name);

    SegmentRule segmentRule;
    if (existingOpt.isPresent()) {
      segmentRule = existingOpt.get();
      log.debug("Updating existing segment rule: {}", name);
    } else {
      segmentRule = new SegmentRule();
      log.info("Creating new segment rule: {}", name);
    }

    segmentRule.setName(name);
    segmentRule.setDescription(description);
    segmentRule.setRequiresHighHeat(requiresHighHeat);
    segmentRule.setIsActive(true);

    return segmentRuleRepository.save(segmentRule);
  }

  public List<SegmentRule> findAll() {
    return segmentRuleRepository.findAll();
  }
}
