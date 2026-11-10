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
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to segment rule rows (seed-update review feature).
 * The entity is resolved through the decision row's {@code entity_display_name} — the exact seed
 * name captured at detection time (slugs are lossy: "No DQ" de-cases to "No Dq", "Free-For-All"
 * re-hyphenates to "Free For All", and the repository lookups are case-sensitive). Writes go
 * through {@code SegmentRuleService.saveRule} (cache-evicting, admin-contextual).
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 *
 * <p>{@code guideHash} decisions resolve as {@code USE_NEW} by reloading the guide payload from
 * {@code segment_rules.json} (beads ATW-md1i) — the decision's value carries only the hash. Rows
 * with no seed entry fall back to warn-and-stamp so the apply batch never rolls back (matching
 * {@link SegmentTypeSeedApplier}).
 */
@Slf4j
@Component
public class SegmentRuleSeedApplier implements SeedDomainApplier {

  private final SegmentRuleRepository segmentRuleRepository;
  private final SegmentRepository segmentRepository;
  private final SegmentRuleSync segmentRuleSync;

  public SegmentRuleSeedApplier(
      final SegmentRuleRepository segmentRuleRepository,
      final SegmentRepository segmentRepository,
      final SegmentRuleSync segmentRuleSync) {
    this.segmentRuleRepository = segmentRuleRepository;
    this.segmentRepository = segmentRepository;
    this.segmentRuleSync = segmentRuleSync;
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
    // The SPI passes the entity key only; a slug round-trip is ambiguous (existing hyphens in a
    // seed name are indistinguishable from slug-converted spaces), so match the key exactly the
    // way detection minted it.
    return segmentRuleRepository.findAll().stream()
        .filter(r -> entityKey.equals(SegmentRuleSync.entityKeyFor(r.getName())))
        .findFirst()
        .map(rule -> !segmentRepository.findBySegmentRulesContaining(rule).isEmpty())
        .orElse(false);
  }

  /** Resolves the decision's rule by its exact seed name (slugs are lossy — see class doc). */
  private Optional<SegmentRule> findRule(SeedUpdateDecision decision) {
    return resolve(decision.getEntityDisplayName());
  }

  /**
   * Matches a rule by its exact name, case-insensitively. The display name is used verbatim — no
   * slug-derived re-spacing (which would corrupt names like "Free-For-All").
   */
  private Optional<SegmentRule> resolve(String name) {
    return segmentRuleRepository.findAll().stream()
        .filter(r -> r.getName().equalsIgnoreCase(name))
        .findFirst();
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
          rule.setBumpAddition(value == null ? null : BumpAddition.valueOf(value));
      case "expansionCode" -> rule.setExpansionCode(value);
      case "guideHash" -> {
        // beads ATW-md1i: the decision's value carries the payload's HASH, not the guide — reload
        // the real payload from segment_rules.json and write guide + hash exactly the way the
        // sync's seed write does. Without a seed entry (admin-created row) there is nothing to
        // reload: warn-and-continue keeps the batch alive (the choice is recorded, row stamped).
        SegmentRulePlayGuide seedGuide =
            segmentRuleSync.reloadSeedGuide(decision.getEntityDisplayName()).orElse(null);
        if (seedGuide == null) {
          log.warn(
              "Seed decision {}: segment rule '{}' has no seed guide payload to reload —"
                  + " guideHash applied as stamp only (the hash is not the guide)",
              decision.getId(),
              decision.getEntityDisplayName());
        } else {
          rule.setGuide(seedGuide);
          rule.setGuideHash(segmentRuleSync.guideHashForGuide(seedGuide));
        }
      }
      default ->
          throw new IllegalStateException(
              "Segment rule field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    segmentRuleRepository.save(rule);
  }

  private void stamp(SegmentRule rule, SeedUpdateDecision decision) {
    rule.setSeedFingerprint(decision.getNewFingerprint());
    segmentRuleRepository.save(rule);
  }
}
