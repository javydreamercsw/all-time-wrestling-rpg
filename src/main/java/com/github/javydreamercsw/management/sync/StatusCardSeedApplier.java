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

import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to status card rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code status_card:KEY} via {@link StatusCardSync#entityKeyFor}) — the key
 * is the domain's stable identity and the level names are seed-owned content (collision targets),
 * so name-based lookup can never resolve a collision; the {@code entity_display_name} stays the
 * exact seed key for the review UI.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class StatusCardSeedApplier implements SeedDomainApplier {

  private final StatusCardRepository statusCardRepository;
  private final WrestlerStatusRepository wrestlerStatusRepository;

  public StatusCardSeedApplier(
      final StatusCardRepository statusCardRepository,
      final WrestlerStatusRepository wrestlerStatusRepository) {
    this.statusCardRepository = statusCardRepository;
    this.wrestlerStatusRepository = wrestlerStatusRepository;
  }

  @Override
  public String domainKey() {
    return StatusCardSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findStatusCard(decision)
        .ifPresentOrElse(
            card -> stamp(card, decision),
            () ->
                log.warn(
                    "Seed decision {}: status card '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findStatusCard(decision)
        .ifPresentOrElse(
            card -> {
              applyFieldValue(card, decision);
              stamp(card, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: status card '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findStatusCard(decision)
        .ifPresentOrElse(
            card -> {
              card.setSeedFingerprint(null);
              statusCardRepository.save(card);
            },
            () ->
                log.warn(
                    "Seed decision {}: status card '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (wrestler statuses on the
    // card). The fail-safe direction re-checks at delete time: a card still applied to a wrestler
    // is kept as custom data rather than blowing the NOT NULL FK mid-apply.
    findStatusCard(decision)
        .ifPresentOrElse(
            card -> {
              if (hasStatuses(card)) {
                log.warn(
                    "Seed decision {}: status card '{}' is applied to wrestlers — the row is kept"
                        + " as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                statusCardRepository.delete(card);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: status card '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findCardByKey(entityKey).map(this::hasStatuses).orElse(false);
  }

  /**
   * Resolves the decision's status card by reminting its entity key — the key survives any admin
   * rename of the seed-owned name fields (see class doc).
   */
  private Optional<StatusCard> findStatusCard(SeedUpdateDecision decision) {
    return findCardByKey(decision.getEntityKey());
  }

  private Optional<StatusCard> findCardByKey(String entityKey) {
    return statusCardRepository.findAll().stream()
        .filter(card -> entityKey.equals(StatusCardSync.entityKeyFor(card.getKey())))
        .findFirst();
  }

  /** Whether any wrestler status row applies this card (the domain's real persisted FK). */
  private boolean hasStatuses(StatusCard card) {
    return wrestlerStatusRepository.existsByStatusCard(card);
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(StatusCard card, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "level1Name" -> card.setLevel1Name(value);
      case "level2Name" -> card.setLevel2Name(value);
      case "description" -> card.setDescription(value);
      case "positive" -> card.setPositive(Boolean.parseBoolean(value));
      case "level1Effect" -> card.setLevel1Effect(value);
      case "level2Effect" -> card.setLevel2Effect(value);
      case "flipUpCondition" -> card.setFlipUpCondition(value);
      case "flipDownCondition" -> card.setFlipDownCondition(value);
      case "discardCondition" -> card.setDiscardCondition(value);
      default ->
          throw new IllegalStateException(
              "Status card field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    statusCardRepository.save(card);
  }

  private void stamp(StatusCard card, SeedUpdateDecision decision) {
    card.setSeedFingerprint(decision.getNewFingerprint());
    statusCardRepository.save(card);
  }
}
