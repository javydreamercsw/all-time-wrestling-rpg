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

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to card rows (seed-update review feature). Cards are
 * the one wired domain whose seed display name is NOT unique — 80 of the catalog's 281 distinct
 * names ship in more than one set ("German Suplex" in seven) — so the entity is resolved through
 * the decision row's {@code entity_key}, reminted exactly the way detection minted it ({@code
 * card:SET#number} via {@link CardSync#entityKeyFor}); the {@code entity_display_name} stays the
 * exact seed name for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CardSeedApplier implements SeedDomainApplier {

  private final CardRepository cardRepository;
  private final DeckCardRepository deckCardRepository;

  public CardSeedApplier(
      final CardRepository cardRepository, final DeckCardRepository deckCardRepository) {
    this.cardRepository = cardRepository;
    this.deckCardRepository = deckCardRepository;
  }

  @Override
  public String domainKey() {
    return CardSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findCard(decision)
        .ifPresentOrElse(
            card -> stamp(card, decision),
            () ->
                log.warn(
                    "Seed decision {}: card '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              applyFieldValue(card, decision);
              stamp(card, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: card '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              card.setSeedFingerprint(null);
              cardRepository.save(card);
            },
            () ->
                log.warn(
                    "Seed decision {}: card '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (wrestler decks holding
    // the card). The fail-safe direction re-checks at delete time: a deck-held card is kept as
    // custom data rather than blowing the FK mid-apply.
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              if (deckCardRepository.findByCard(card).isEmpty()) {
                cardRepository.delete(card);
              } else {
                log.warn(
                    "Seed decision {}: card '{}' is still held by wrestler decks — the row is kept"
                        + " as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: card '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; it is matched against every row's reminted key exactly
    // the way detection minted it (the set#number key is the unique business identity).
    return findCardByKey(entityKey)
        .map(card -> !deckCardRepository.findByCard(card).isEmpty())
        .orElse(false);
  }

  /**
   * Resolves the decision's card by reminting its entity key — the set#number seed key is the
   * unique identity, immune to the cross-set display-name collisions (see class doc).
   */
  private Optional<Card> findCard(SeedUpdateDecision decision) {
    return findCardByKey(decision.getEntityKey());
  }

  private Optional<Card> findCardByKey(String entityKey) {
    return cardRepository.findAll().stream()
        .filter(c -> entityKey.equals(CardSync.entityKeyFor(CardSync.seedKeyOf(c))))
        .findFirst();
  }

  /**
   * Writes the decision's incoming seed value to the matching seed-owned field and stamps the row's
   * fingerprint with the decision's {@code new_fingerprint} (the apply-time stamp obligation).
   */
  private void applyFieldValue(Card card, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "name" -> card.setName(value);
      case "damage" -> card.setDamage(value == null ? null : Integer.valueOf(value));
      case "stamina" -> card.setStamina(value == null ? null : Integer.valueOf(value));
      case "target" -> card.setTarget(value == null ? null : Integer.valueOf(value));
      case "momentum" -> card.setMomentum(value == null ? null : Integer.valueOf(value));
      case "type" -> card.setType(value);
      case "signature" -> card.setSignature(value == null ? null : Boolean.valueOf(value));
      case "finisher" -> card.setFinisher(value == null ? null : Boolean.valueOf(value));
      case "taunt" -> card.setTaunt(value == null ? null : Boolean.valueOf(value));
      case "recover" -> card.setRecover(value == null ? null : Boolean.valueOf(value));
      case "pin" -> card.setPin(value == null ? null : Boolean.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Card field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    cardRepository.save(card);
  }

  /**
   * Stamps the row's seed fingerprint with the decision's detection-time {@code new_fingerprint}.
   */
  private void stamp(Card card, SeedUpdateDecision decision) {
    card.setSeedFingerprint(decision.getNewFingerprint());
    cardRepository.save(card);
  }
}
