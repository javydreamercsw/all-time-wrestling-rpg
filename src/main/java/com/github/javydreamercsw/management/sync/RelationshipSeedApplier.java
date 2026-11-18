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

import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to wrestler relationship rows (seed-update review
 * feature). The entity is resolved through the decision row's {@code entity_key}, reminted exactly
 * the way detection minted it ({@code relationship:W1-slug<TYPE>W2-slug} via {@link
 * RelationshipSync#entityKeyFor}) — the pair plus type is the domain's stable identity, and
 * key-based reminting keeps resolution rename-proof if seed naming ever moves; the {@code
 * entity_display_name} stays the exact seed name for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class RelationshipSeedApplier implements SeedDomainApplier {

  private final WrestlerRelationshipRepository relationshipRepository;

  public RelationshipSeedApplier(final WrestlerRelationshipRepository relationshipRepository) {
    this.relationshipRepository = relationshipRepository;
  }

  @Override
  public String domainKey() {
    return RelationshipSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findRelationship(decision)
        .ifPresentOrElse(
            relationship -> stamp(relationship, decision),
            () ->
                log.warn(
                    "Seed decision {}: wrestler relationship '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findRelationship(decision)
        .ifPresentOrElse(
            relationship -> {
              applyFieldValue(relationship, decision);
              stamp(relationship, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler relationship '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findRelationship(decision)
        .ifPresentOrElse(
            relationship -> {
              relationship.setSeedFingerprint(null);
              relationshipRepository.save(relationship);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler relationship '{}' not found — nothing to mark"
                        + " custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // A relationship is its own gameplay edge; nothing else in the database holds a FK to it
    // (verified against the entity model — Wrestler's inverse sides map the same rows, they do
    // not reference them independently). The removal deletes the edge.
    findRelationship(decision)
        .ifPresentOrElse(
            relationshipRepository::delete,
            () ->
                log.warn(
                    "Seed decision {}: wrestler relationship '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; the key round-trips exactly the way detection minted it
    // (the pair is stored in seed order on the row, so the key always re-derives). Relationships
    // are leaf edges — no FKs point at them.
    return false;
  }

  /**
   * Resolves the decision's relationship by reminting its entity key from the row's persisted pair
   * and type.
   */
  private Optional<WrestlerRelationship> findRelationship(SeedUpdateDecision decision) {
    return findRelationshipByKey(decision.getEntityKey());
  }

  private Optional<WrestlerRelationship> findRelationshipByKey(String entityKey) {
    return relationshipRepository.findAllWithWrestlers().stream()
        .filter(
            r ->
                entityKey.equals(
                    RelationshipSync.entityKeyFor(
                        r.getWrestler1().getName(), r.getWrestler2().getName(), r.getType())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(WrestlerRelationship relationship, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "level" -> relationship.setLevel(value == null ? null : Integer.valueOf(value));
      case "isStoryline" -> relationship.setIsStoryline(Boolean.parseBoolean(value));
      case "notes" -> relationship.setNotes(value);
      default ->
          throw new IllegalStateException(
              "Wrestler relationship field '"
                  + decision.getFieldName()
                  + "' is not a seed-owned field");
    }
    relationshipRepository.save(relationship);
  }

  private void stamp(WrestlerRelationship relationship, SeedUpdateDecision decision) {
    relationship.setSeedFingerprint(decision.getNewFingerprint());
    relationshipRepository.save(relationship);
  }
}
