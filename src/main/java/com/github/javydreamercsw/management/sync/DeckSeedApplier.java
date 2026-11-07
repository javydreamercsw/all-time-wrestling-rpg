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
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to deck rows (seed-update review feature). The deck
 * is resolved through its owning wrestler — the decision's {@code entity_display_name} is the
 * wrestler's exact seed name (decks.json is keyed by wrestler; slugs would be lossy: "Sgt.
 * Slaughter" and "Rob Van Dam" do not survive a slug round-trip, and the repository lookups are
 * case-sensitive). The only seed-owned state is the card composition, so {@code USE_NEW} writes the
 * decision's captured seed composition back onto the deck.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's composition, so a later
 * seed change to the deck re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class DeckSeedApplier implements SeedDomainApplier {

  private final DeckService deckService;
  private final WrestlerRepository wrestlerRepository;
  private final CardService cardService;

  public DeckSeedApplier(
      final DeckService deckService,
      final WrestlerRepository wrestlerRepository,
      final CardService cardService) {
    this.deckService = deckService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardService = cardService;
  }

  @Override
  public String domainKey() {
    return DeckSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findDeck(decision)
        .ifPresentOrElse(
            deck -> stamp(deck, decision),
            () ->
                log.warn(
                    "Seed decision {}: deck of '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findDeck(decision)
        .ifPresentOrElse(
            deck -> {
              applyFieldValue(deck, decision);
              stamp(deck, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: deck of '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the composition, mark it no longer seed-owned: clear the row's stamp.
    findDeck(decision)
        .ifPresentOrElse(
            deck -> {
              deck.setSeedFingerprint(null);
              deckService.saveAll(List.of(deck));
            },
            () ->
                log.warn(
                    "Seed decision {}: deck of '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // A deck is the wrestler's gameplay content; the only FK pointing at it is its owner's
    // wrestler row, and removing a wrestler's deck destroys gameplay state. The fail-safe
    // direction keeps the row as custom data (nothing in the database changes).
    log.warn(
        "Seed decision {}: deck of '{}' cannot be removed by seed review (decks are gameplay"
            + " content) — the row is kept as custom data",
        decision.getId(),
        decision.getEntityDisplayName());
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it. A deck is always owned by its wrestler — removal is refused.
    return deckService.findAllWithCards().stream()
        .filter(
            d ->
                d.getWrestler() != null
                    && entityKey.equals(DeckSync.entityKeyFor(d.getWrestler().getName())))
        .findFirst()
        .isPresent();
  }

  /** Resolves the decision's deck by its owning wrestler's exact seed name. */
  private Optional<Deck> findDeck(SeedUpdateDecision decision) {
    Optional<Wrestler> wrestler =
        wrestlerRepository.findAll().stream()
            .filter(w -> w.getName().equalsIgnoreCase(decision.getEntityDisplayName()))
            .findFirst();
    if (wrestler.isEmpty()) {
      return Optional.empty();
    }
    List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler.get());
    return byWrestler.isEmpty() ? Optional.empty() : Optional.of(byWrestler.getFirst());
  }

  /**
   * Writes the decision's captured seed composition onto the deck (the one seed-owned field) and
   * stamps the row's fingerprint with the decision's {@code new_fingerprint}.
   */
  private void applyFieldValue(Deck deck, SeedUpdateDecision decision) {
    applyComposition(deck, decision.getNewValue());
  }

  /**
   * Parses the decision's canonical composition string ({@code SET#number xN} lines, as captured at
   * detection time) back into DeckCard entries. Unresolvable entries (card no longer in the
   * catalog) are skipped, mirroring the seed write path.
   */
  private void applyComposition(Deck deck, String composition) {
    if (composition == null || composition.isBlank()) {
      return;
    }
    Map<String, Card> catalog = new HashMap<>();
    Map<String, Integer> amounts = new HashMap<>();
    for (String line : composition.split("\n")) {
      int x = line.lastIndexOf('x');
      int hash = line.indexOf('#');
      if (x <= 0 || hash <= 0) {
        log.warn("Seed decision apply: skipping malformed composition entry '{}'", line);
        continue;
      }
      String setCode = line.substring(0, hash);
      String number = line.substring(hash + 1, x);
      int amount = Integer.parseInt(line.substring(x + 1));
      Card card = cardService.findByNumberAndSet(Integer.valueOf(number), setCode).orElse(null);
      if (card == null) {
        log.warn(
            "Seed decision apply: composition entry '{}' no longer resolves to a catalog card —"
                + " skipped",
            line);
        continue;
      }
      catalog.put(setCode + "#" + number, card);
      amounts.merge(setCode + "#" + number, amount, Integer::sum);
    }
    deck.getCards().clear();
    for (Map.Entry<String, Integer> entry : amounts.entrySet()) {
      Card card = catalog.get(entry.getKey());
      DeckCard dc = new DeckCard();
      dc.setDeck(deck);
      dc.setCard(card);
      dc.setSet(card.getSet());
      dc.setAmount(entry.getValue());
      deck.getCards().add(dc);
    }
  }

  private void stamp(Deck deck, SeedUpdateDecision decision) {
    deck.setSeedFingerprint(decision.getNewFingerprint());
    deckService.saveAll(List.of(deck));
  }
}
