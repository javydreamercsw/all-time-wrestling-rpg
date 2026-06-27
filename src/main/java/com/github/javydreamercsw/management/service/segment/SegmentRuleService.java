/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.segment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
  @Autowired private ExpansionService expansionService;
  @Autowired private UniverseContextService universeContextService;
  @Autowired private UniverseSettingsService universeSettingsService;
  @Autowired private ObjectMapper objectMapper;

  private Set<String> enabledExpansionCodes() {
    return universeContextService
        .getCurrentUniverse()
        .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
        .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
  }

  /**
   * Find a segment rule by name.
   *
   * @param name The name of the segment rule
   * @return Optional containing the segment rule if found
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      key = "#name")
  public Optional<SegmentRule> findByName(final String name) {
    return segmentRuleRepository.findByName(name);
  }

  /**
   * Get segment rules suitable for high-heat rivalries.
   *
   * @return List of segment rules appropriate for intense rivalries
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      key = "'highHeat'")
  public List<SegmentRule> getHighHeatRules() {
    Set<String> enabled = enabledExpansionCodes();
    return segmentRuleRepository.findSuitableForHighHeat().stream()
        .filter(r -> r.getExpansionCode() == null || enabled.contains(r.getExpansionCode()))
        .collect(Collectors.toList());
  }

  /**
   * Get standard segment rules (not requiring high heat).
   *
   * @return List of standard segment rules
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      key = "'standard'")
  public List<SegmentRule> getStandardRules() {
    Set<String> enabled = enabledExpansionCodes();
    return segmentRuleRepository.findStandardRules().stream()
        .filter(r -> r.getExpansionCode() == null || enabled.contains(r.getExpansionCode()))
        .collect(Collectors.toList());
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
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      allEntries = true)
  public SegmentRule createRule(
      @NonNull final String name,
      @NonNull final String description,
      final boolean requiresHighHeat) {
    if (segmentRuleRepository.existsByName(name)) {
      throw new IllegalArgumentException("Segment rule with name '" + name + "' already exists");
    }

    SegmentRule rule = new SegmentRule();
    rule.setName(name);
    rule.setDescription(description);
    rule.setRequiresHighHeat(requiresHighHeat);

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
   * @return The updated segment rule
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      allEntries = true)
  public SegmentRule updateRule(
      @NonNull final Long id,
      @NonNull final String name,
      @NonNull final String description,
      @NonNull final Boolean requiresHighHeat) {
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

    SegmentRule savedRule = segmentRuleRepository.save(rule);
    log.info("Updated segment rule: {}", savedRule.getName());
    return savedRule;
  }

  /**
   * Check if a segment rule exists by name.
   *
   * @param name The name to check
   * @return true if a rule with this name exists
   */
  @PreAuthorize("isAuthenticated()")
  public boolean existsByName(@NonNull final String name) {
    return segmentRuleRepository.existsByName(name);
  }

  /**
   * Get a segment rule by ID.
   *
   * @param id The ID of the segment rule
   * @return Optional containing the segment rule if found
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      key = "#id")
  public Optional<SegmentRule> findById(@NonNull final Long id) {
    return segmentRuleRepository.findById(id);
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
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      allEntries = true)
  public SegmentRule createOrUpdateRule(
      @NonNull final String name,
      final String description,
      final boolean requiresHighHeat,
      final boolean noDq,
      final BumpAddition bumpAddition) {
    return createOrUpdateRule(
        name, description, requiresHighHeat, noDq, bumpAddition, "BASE_GAME", null);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      allEntries = true)
  public SegmentRule createOrUpdateRule(
      @NonNull final String name,
      final String description,
      final boolean requiresHighHeat,
      final boolean noDq,
      final BumpAddition bumpAddition,
      final String expansionCode) {
    return createOrUpdateRule(
        name, description, requiresHighHeat, noDq, bumpAddition, expansionCode, null);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      key = "'all'")
  public List<SegmentRule> findAll() {
    Set<String> enabled = enabledExpansionCodes();
    Map<String, Integer> priorities = expansionService.buildPriorityMap();
    return segmentRuleRepository.findAll().stream()
        .filter(r -> r.getExpansionCode() == null || enabled.contains(r.getExpansionCode()))
        .collect(
            Collectors.toMap(
                SegmentRule::getName,
                r -> r,
                (a, b) ->
                    priorities.getOrDefault(a.getExpansionCode(), 0)
                            >= priorities.getOrDefault(b.getExpansionCode(), 0)
                        ? a
                        : b))
        .values()
        .stream()
        .sorted(Comparator.comparing(SegmentRule::getName))
        .collect(Collectors.toList());
  }

  public long count() {
    return segmentRuleRepository.count();
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_RULES_CACHE,
      allEntries = true)
  public SegmentRule createOrUpdateRule(
      @NonNull final String name,
      final String description,
      final boolean requiresHighHeat,
      final boolean noDq,
      final BumpAddition bumpAddition,
      final String expansionCode,
      final SegmentRulePlayGuide guide) {
    Optional<SegmentRule> existingOpt = segmentRuleRepository.findByName(name);
    String incomingHash = computeGuideHash(guide);

    if (existingOpt.isPresent()) {
      SegmentRule sr = existingOpt.get();
      boolean guideChanged = !java.util.Objects.equals(sr.getGuideHash(), incomingHash);
      if (java.util.Objects.equals(sr.getDescription(), description)
          && sr.getRequiresHighHeat() == requiresHighHeat
          && sr.getNoDq() == noDq
          && sr.getBumpAddition() == bumpAddition
          && java.util.Objects.equals(sr.getExpansionCode(), expansionCode)
          && !guideChanged) {
        return sr;
      }
      sr.setDescription(description);
      sr.setRequiresHighHeat(requiresHighHeat);
      sr.setNoDq(noDq);
      sr.setBumpAddition(bumpAddition);
      sr.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");
      if (guideChanged) {
        sr.setGuide(guide);
        sr.setGuideHash(incomingHash);
      }
      log.debug("Updating existing segment rule: {}", name);
      return segmentRuleRepository.save(sr);
    }

    SegmentRule segmentRule = new SegmentRule();
    log.debug("Creating new segment rule: {}", name);
    segmentRule.setName(name);
    segmentRule.setDescription(description);
    segmentRule.setRequiresHighHeat(requiresHighHeat);
    segmentRule.setNoDq(noDq);
    segmentRule.setBumpAddition(bumpAddition);
    segmentRule.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");
    segmentRule.setGuide(guide);
    segmentRule.setGuideHash(incomingHash);
    return segmentRuleRepository.save(segmentRule);
  }

  private String computeGuideHash(final SegmentRulePlayGuide guide) {
    if (guide == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(guide);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("Failed to compute guide hash for segment rule", e);
      return null;
    }
  }
}
