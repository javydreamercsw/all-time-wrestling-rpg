package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.management.domain.show.match.rule.MatchRule;
import com.github.javydreamercsw.management.domain.show.match.rule.MatchRuleRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing match rules in the ATW RPG system. Provides business logic for creating,
 * retrieving, and managing match rules that can be applied to wrestling matches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchRuleService {

  @Autowired private MatchRuleRepository matchRuleRepository;

  /**
   * Get all active match rules.
   *
   * @return List of active match rules
   */
  public List<MatchRule> getAllActiveRules() {
    return matchRuleRepository.findByIsActiveTrue();
  }

  /**
   * Find a match rule by name.
   *
   * @param name The name of the match rule
   * @return Optional containing the match rule if found
   */
  public Optional<MatchRule> findByName(String name) {
    return matchRuleRepository.findByName(name);
  }

  /**
   * Get match rules suitable for high-heat rivalries.
   *
   * @return List of match rules appropriate for intense rivalries
   */
  public List<MatchRule> getHighHeatRules() {
    return matchRuleRepository.findSuitableForHighHeat();
  }

  /**
   * Get standard match rules (not requiring high heat).
   *
   * @return List of standard match rules
   */
  public List<MatchRule> getStandardRules() {
    return matchRuleRepository.findStandardRules();
  }

  /**
   * Create a new match rule.
   *
   * @param name The name of the match rule
   * @param description Optional description of the rule
   * @param requiresHighHeat Whether this rule requires high heat to be used
   * @return The created match rule
   */
  @Transactional
  public MatchRule createRule(
      @NonNull String name, @NonNull String description, boolean requiresHighHeat) {
    if (matchRuleRepository.existsByName(name)) {
      throw new IllegalArgumentException("Match rule with name '" + name + "' already exists");
    }

    MatchRule rule = new MatchRule();
    rule.setName(name);
    rule.setDescription(description);
    rule.setRequiresHighHeat(requiresHighHeat);
    rule.setIsActive(true);

    MatchRule savedRule = matchRuleRepository.save(rule);
    log.info("Created new match rule: {}", savedRule.getName());
    return savedRule;
  }

  /**
   * Update an existing match rule.
   *
   * @param id The ID of the match rule to update
   * @param name New name (optional)
   * @param description New description (optional)
   * @param requiresHighHeat New high heat requirement (optional)
   * @param isActive New active status (optional)
   * @return The updated match rule
   */
  @Transactional
  public MatchRule updateRule(
      @NonNull Long id,
      @NonNull String name,
      @NonNull String description,
      @NonNull Boolean requiresHighHeat,
      @NonNull Boolean isActive) {
    MatchRule rule =
        matchRuleRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match rule not found with ID: " + id));

    if (!name.equals(rule.getName())) {
      if (matchRuleRepository.existsByName(name)) {
        throw new IllegalArgumentException("Match rule with name '" + name + "' already exists");
      }
      rule.setName(name);
    }

    rule.setDescription(description);

    rule.setRequiresHighHeat(requiresHighHeat);

    rule.setIsActive(isActive);

    MatchRule savedRule = matchRuleRepository.save(rule);
    log.info("Updated match rule: {}", savedRule.getName());
    return savedRule;
  }

  /**
   * Deactivate a match rule (soft delete).
   *
   * @param id The ID of the match rule to deactivate
   */
  @Transactional
  public void deactivateRule(@NonNull Long id) {
    MatchRule rule =
        matchRuleRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match rule not found with ID: " + id));

    rule.setIsActive(false);
    matchRuleRepository.save(rule);
    log.info("Deactivated match rule: {}", rule.getName());
  }

  /**
   * Check if a match rule exists by name.
   *
   * @param name The name to check
   * @return true if a rule with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return matchRuleRepository.existsByName(name);
  }

  /**
   * Get a match rule by ID.
   *
   * @param id The ID of the match rule
   * @return Optional containing the match rule if found
   */
  public Optional<MatchRule> findById(@NonNull Long id) {
    return matchRuleRepository.findById(id);
  }

  /**
   * Get all match rules (including inactive ones).
   *
   * @return List of all match rules
   */
  public List<MatchRule> getAllRules() {
    return matchRuleRepository.findAll();
  }

  /**
   * Create or update a match rule from external data.
   *
   * @param name Name of the match rule
   * @param description Description of the match rule
   * @param requiresHighHeat Whether this rule requires high heat
   * @return The created or updated match rule
   */
  @Transactional
  public MatchRule createOrUpdateRule(
      @NonNull String name, String description, boolean requiresHighHeat) {
    Optional<MatchRule> existingOpt = matchRuleRepository.findByName(name);

    MatchRule matchRule;
    if (existingOpt.isPresent()) {
      matchRule = existingOpt.get();
      log.debug("Updating existing match rule: {}", name);
    } else {
      matchRule = new MatchRule();
      log.info("Creating new match rule: {}", name);
    }

    matchRule.setName(name);
    matchRule.setDescription(description);
    matchRule.setRequiresHighHeat(requiresHighHeat);
    matchRule.setIsActive(true);

    return matchRuleRepository.save(matchRule);
  }
}
