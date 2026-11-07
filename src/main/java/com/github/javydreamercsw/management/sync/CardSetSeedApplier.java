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

import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to card set rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_display_name} — the exact seed name
 * captured at detection time (set names are unique; slugs would be lossy: "Oda Tsurugi" downcases
 * to "Oda Tsurugi" harmlessly but "Sgt. Slaughter" re-spaces to "Sgt. Slaughter" only by luck — the
 * display name is used verbatim, no slug reversal).
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CardSetSeedApplier implements SeedDomainApplier {

  private final CardSetRepository cardSetRepository;
  private final CardRepository cardRepository;

  public CardSetSeedApplier(
      final CardSetRepository cardSetRepository, final CardRepository cardRepository) {
    this.cardSetRepository = cardSetRepository;
    this.cardRepository = cardRepository;
  }

  @Override
  public String domainKey() {
    return CardSetSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    cardSetRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            cardSet -> stamp(cardSet, decision),
            () ->
                log.warn(
                    "Seed decision {}: card set '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    cardSetRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            cardSet -> {
              applyFieldValue(cardSet, decision);
              stamp(cardSet, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: card set '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    cardSetRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            cardSet -> {
              cardSet.setSeedFingerprint(null);
              cardSetRepository.save(cardSet);
            },
            () ->
                log.warn(
                    "Seed decision {}: card set '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (cards referencing the
    // set). The fail-safe direction re-checks at delete time: a set with cards is kept as custom
    // data rather than blowing the FK mid-apply.
    cardSetRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            cardSet -> {
              if (cardRepository.findBySet(cardSet).isEmpty()) {
                cardSetRepository.delete(cardSet);
              } else {
                log.warn(
                    "Seed decision {}: card set '{}' still has cards — the row is kept as custom"
                        + " data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: card set '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous (existing hyphens in a
    // set code are indistinguishable from slug-converted spaces), so match the key exactly the
    // way detection minted it.
    return cardSetRepository.findAll().stream()
        .filter(cs -> entityKey.equals(CardSetSync.entityKeyFor(cs.getCode())))
        .findFirst()
        .map(cardSet -> !cardRepository.findBySet(cardSet).isEmpty())
        .orElse(false);
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(CardSet cardSet, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "name" -> cardSet.setName(value);
      default ->
          throw new IllegalStateException(
              "Card set field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    cardSetRepository.save(cardSet);
  }

  private void stamp(CardSet cardSet, SeedUpdateDecision decision) {
    cardSet.setSeedFingerprint(decision.getNewFingerprint());
    cardSetRepository.save(cardSet);
  }
}
