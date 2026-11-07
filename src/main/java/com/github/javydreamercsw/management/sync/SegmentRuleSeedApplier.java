/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to segment rule rows (seed-update review feature).
 * Writes go through {@code SegmentRuleService.saveRule} (cache-evicting, admin-contextual); entity
 * keys are {@code segment_rule:<slug>}.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class SegmentRuleSeedApplier implements SeedDomainApplier {

  private final SegmentRuleRepository segmentRuleRepository;
  private final SegmentRepository segmentRepository;

  public SegmentRuleSeedApplier(
      final SegmentRuleRepository segmentRuleRepository,
      final SegmentRepository segmentRepository) {
    this.segmentRuleRepository = segmentRuleRepository;
    this.segmentRepository = segmentRepository;
  }

  @Override
  public String domainKey() {
    return SegmentRuleSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findRule(decision).ifPresent(rule -> stamp(rule, decision));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findRule(decision)
        .ifPresent(
            rule -> {
              applyFieldValue(rule, decision);
              stamp(rule, decision);
            });
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findRule(decision)
        .ifPresent(
            rule -> {
              rule.setSeedFingerprint(null);
              segmentRuleRepository.save(rule);
            });
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    findRule(decision).ifPresent(segmentRuleRepository::delete);
  }

  @Override
  public boolean hasDependents(String entityKey) {
    return findRuleByKey(entityKey)
        .map(rule -> !segmentRepository.findBySegmentRulesContaining(rule).isEmpty())
        .orElse(false);
  }

  private Optional<SegmentRule> findRule(SeedUpdateDecision decision) {
    return findRuleByKey(decision.getEntityKey());
  }

  private Optional<SegmentRule> findRuleByKey(String entityKey) {
    return segmentRuleRepository.findByName(SegmentRuleSeedApplier.entityName(entityKey));
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(SegmentRule rule, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> rule.setDescription(value);
      case "requiresHighHeat" -> rule.setRequiresHighHeat(Boolean.parseBoolean(value));
      case "noDq" -> rule.setNoDq(Boolean.parseBoolean(value));
      case "allowsRefereeStopage" -> rule.setAllowsRefereeStopage(Boolean.parseBoolean(value));
      case "bumpAddition" ->
          rule.setBumpAddition(
              value == null
                  ? null
                  : com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition
                      .valueOf(value));
      case "expansionCode" -> rule.setExpansionCode(value);
      default ->
          throw new IllegalStateException(
              "Segment rule field '"
                  + decision.getFieldName()
                  + "' is not a seed-owned field (guideHash is guide-managed, not directly"
                  + " writable)");
    }
    segmentRuleRepository.save(rule);
  }

  private void stamp(SegmentRule rule, SeedUpdateDecision decision) {
    rule.setSeedFingerprint(decision.getNewFingerprint());
    segmentRuleRepository.save(rule);
  }

  /** Reverses {@code SegmentRuleSync#entityKey}: {@code segment_rule:cage} → {@code Cage}. */
  static String entityName(String entityKey) {
    String slug = entityKey.substring(entityKey.indexOf(':') + 1);
    StringBuilder name = new StringBuilder();
    for (String word : slug.split("-")) {
      if (word.isEmpty()) {
        continue;
      }
      if (name.length() > 0) {
        name.append(' ');
      }
      name.append(Character.toUpperCase(word.charAt(0)) + word.substring(1));
    }
    return name.toString();
  }
}
