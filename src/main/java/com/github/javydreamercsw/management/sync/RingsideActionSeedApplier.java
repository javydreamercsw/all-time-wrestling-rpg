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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionTypeRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to ringside action rows (seed-update review feature).
 * The entity is resolved through the decision row's {@code entity_display_name} — the exact seed
 * name captured at detection time (slugs are lossy and the repository lookups are case-sensitive).
 *
 * <p>Ringside actions have no persisted dependents — segments hold the referee NPC, not the action;
 * decks hold cards — so removals delete directly.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class RingsideActionSeedApplier implements SeedDomainApplier {

  private final RingsideActionRepository ringsideActionRepository;
  private final RingsideActionTypeRepository ringsideActionTypeRepository;

  public RingsideActionSeedApplier(
      final RingsideActionRepository ringsideActionRepository,
      final RingsideActionTypeRepository ringsideActionTypeRepository) {
    this.ringsideActionRepository = ringsideActionRepository;
    this.ringsideActionTypeRepository = ringsideActionTypeRepository;
  }

  @Override
  public String domainKey() {
    return RingsideActionSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    ringsideActionRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            action -> stamp(action, decision),
            () ->
                log.warn(
                    "Seed decision {}: ringside action '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    ringsideActionRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            action -> {
              applyFieldValue(action, decision);
              stamp(action, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: ringside action '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    ringsideActionRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            action -> {
              action.setSeedFingerprint(null);
              ringsideActionRepository.save(action);
            },
            () ->
                log.warn(
                    "Seed decision {}: ringside action '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    ringsideActionRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            ringsideActionRepository::delete,
            () ->
                log.warn(
                    "Seed decision {}: ringside action '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it. No persisted FK points at a ringside action (segments hold the
    // referee NPC, not the action; decks hold cards) — removal is never dependent-blocked.
    return false;
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(RingsideAction action, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "type" ->
          ringsideActionTypeRepository
              .findByName(value)
              .ifPresentOrElse(
                  action::setType,
                  () ->
                      log.warn(
                          "Seed decision {}: ringside action type '{}' not found — type not"
                              + " changed",
                          decision.getId(),
                          value));
      case "description" -> action.setDescription(value);
      case "impact" -> action.setImpact(value == null ? 0 : Integer.parseInt(value));
      case "risk" -> action.setRisk(value == null ? 0 : Integer.parseInt(value));
      case "alignment" ->
          action.setAlignment(value == null ? AlignmentType.NEUTRAL : AlignmentType.valueOf(value));
      case "expansionCode" -> action.setExpansionCode(value);
      default ->
          throw new IllegalStateException(
              "Ringside action field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    ringsideActionRepository.save(action);
  }

  private void stamp(RingsideAction action, SeedUpdateDecision decision) {
    action.setSeedFingerprint(decision.getNewFingerprint());
    ringsideActionRepository.save(action);
  }
}
