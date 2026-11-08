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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.DeckCardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(150)
public class DeckSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "deck";

  private final SeedSyncSupport syncSupport;
  private final DeckService deckService;
  private final WrestlerRepository wrestlerRepository;
  private final CardSetRepository cardSetRepository;
  private final CardService cardService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public DeckSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final DeckService deckService,
      final WrestlerRepository wrestlerRepository,
      final CardSetRepository cardSetRepository,
      final CardService cardService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.deckService = deckService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardSetRepository = cardSetRepository;
    this.cardService = cardService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && deckService.count() > 0) {
      topUpMissingDecks();
      if (syncSupport.isVerify()) {
        reconcileVerifiedDecks();
      }
      return;
    }
    loadDecks(true);
  }

  /**
   * Skip-gate branch: decks exist and the {@code skip-if-not-empty} flag protects them from
   * seed-driven updates. Still create decks for wrestlers that have none, so wrestlers added in
   * later releases get their seeded deck on flag-protected installs. Existing decks are never
   * modified.
   */
  private void topUpMissingDecks() {
    List<Deck> missing = loadDecks(false);
    if (!missing.isEmpty()) {
      log.info("Deck top-up: creating {} missing wrestler deck(s).", missing.size());
      deckService.saveAll(missing);
    }
  }

  /**
   * Loads decks from {@code decks.json}.
   *
   * @param updateExisting when true, updates existing decks to match the seed file (full sync);
   *     when false, only wrestlers without any deck get one built from the seed and existing decks
   *     are left untouched.
   * @return Decks that were created or need saving.
   */
  private List<Deck> loadDecks(boolean updateExisting) {
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (!resource.exists()) {
      log.warn("Decks file not found: {}", resource.getPath());
      return List.of();
    }
    log.debug("Loading decks from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<DeckDTO> decksFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      List<Deck> newDecks = new ArrayList<>();
      Map<String, Wrestler> wrestlers =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      Map<Long, CardSet> setCache = new HashMap<>();
      cardSetRepository.findAll().forEach(cs -> setCache.put(cs.getId(), cs));

      List<Deck> decksToSave = new ArrayList<>();
      for (DeckDTO deckDTO : decksFromFile) {
        Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
        if (wrestler == null) {
          continue;
        }

        List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler);
        Deck deck;
        if (byWrestler.isEmpty()) {
          deck = deckService.createDeck(wrestler);
          newDecks.add(deck);
        } else if (!updateExisting) {
          // Top-up mode: never modify an existing deck.
          continue;
        } else {
          deck = byWrestler.getFirst();
        }

        deck.getCards()
            .forEach(
                dc -> {
                  if (dc.getSet() != null && dc.getSet().getId() != null) {
                    dc.setSet(setCache.getOrDefault(dc.getSet().getId(), dc.getSet()));
                  }
                  if (dc.getCard() != null
                      && dc.getCard().getSet() != null
                      && dc.getCard().getSet().getId() != null) {
                    dc.getCard()
                        .setSet(
                            setCache.getOrDefault(
                                dc.getCard().getSet().getId(), dc.getCard().getSet()));
                  }
                });

        Set<DeckCard> cardsToRemove = new HashSet<>(deck.getCards());
        Map<String, Integer> cardKeyToAmount = new HashMap<>();
        Map<String, Card> cardKeyToCard = new HashMap<>();

        for (DeckCardDTO cardDTO : deckDTO.getCards()) {
          Card card =
              cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
          if (card == null) {
            log.warn(
                "Card not found: {} in set {} from deck {}",
                cardDTO.getNumber(),
                cardDTO.getSet(),
                wrestler.getName());
            continue;
          }

          CardSet canonicalSet =
              setCache.computeIfAbsent(card.getSet().getId(), id -> card.getSet());
          card.setSet(canonicalSet);

          String key = card.getSet().getName() + "-" + card.getId();
          cardKeyToAmount.merge(key, cardDTO.getAmount(), Integer::sum);
          cardKeyToCard.putIfAbsent(key, card);
        }

        boolean changed = false;
        for (var entry : cardKeyToAmount.entrySet()) {
          Card card = cardKeyToCard.get(entry.getKey());
          int amount = entry.getValue();

          Optional<DeckCard> existingDeckCardOpt =
              deck.getCards().stream()
                  .filter(dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
                  .findFirst();

          if (existingDeckCardOpt.isPresent()) {
            DeckCard existingDeckCard = existingDeckCardOpt.get();
            if (existingDeckCard.getAmount() != amount) {
              existingDeckCard.setAmount(amount);
              changed = true;
            }
            cardsToRemove.remove(existingDeckCard);
          } else {
            DeckCard newDeckCard = new DeckCard();
            newDeckCard.setCard(card);
            newDeckCard.setSet(card.getSet());
            newDeckCard.setAmount(amount);
            newDeckCard.setDeck(deck);
            deck.getCards().add(newDeckCard);
            changed = true;
          }
        }

        if (updateExisting && !cardsToRemove.isEmpty()) {
          deck.getCards().removeAll(cardsToRemove);
          changed = true;
        }

        // Newly created decks are seed-written by definition: stamp their composition.
        if (newDecks.contains(deck) || changed) {
          stampCompositionFingerprint(deck);
        }

        if (changed) {
          decksToSave.add(deck);
        }
      }
      if (updateExisting && !decksToSave.isEmpty()) {
        deckService.saveAll(decksToSave);
      }
      log.debug(
          "Deck loading completed - {} decks processed, {} updated",
          decksFromFile.size(),
          decksToSave.size());
      return decksToSave;
    } catch (IOException e) {
      log.error("Error loading decks from file", e);
      return List.of();
    }
  }

  /**
   * Canonical composition string for a deck's fingerprint: one {@code set#number xN} entry per
   * DeckCard, sorted, newline-joined.
   */
  private static String composition(Deck deck) {
    return deck.getCards().stream()
        .filter(dc -> dc.getSet() != null && dc.getCard() != null)
        .map(dc -> dc.getSet().getCode() + "#" + dc.getCard().getNumber() + "x" + dc.getAmount())
        .sorted()
        .collect(Collectors.joining("\n"));
  }

  /**
   * Fingerprint over a deck row's current composition, in canonical form. Public for the
   * seed-provenance manifest generator, which re-hashes fresh seed rows through the owning sync's
   * own method.
   */
  public String compositionFingerprint(Deck deck) {
    return syncSupport.fingerprint(composition(deck));
  }

  /** Stamps a deck with the fingerprint of its current composition. */
  private void stampCompositionFingerprint(Deck deck) {
    deck.setSeedFingerprint(compositionFingerprint(deck));
  }

  /**
   * Verify mode: reconcile the composition of existing decks. The stored fingerprint is the hash of
   * the composition as the last seed write left it; re-hashing the deck's current composition
   * detects hand edits. A deck that still hashes to its stamp is clean and adopts seed composition
   * changes; a deck whose composition drifted is user-edited — warned and preserved. Wrestlers with
   * no seed entry are never considered.
   */
  private void reconcileVerifiedDecks() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<DeckDTO> decksFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Wrestler> wrestlers =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      List<Deck> toSave = new ArrayList<>();
      for (DeckDTO deckDTO : decksFromFile) {
        Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
        if (wrestler == null) {
          continue;
        }
        List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler);
        if (byWrestler.isEmpty()) {
          continue; // no deck yet; top-up (which runs first) creates it
        }
        Deck deck = byWrestler.getFirst();
        String current = compositionFingerprint(deck);
        if (SeedSyncSupport.isBlankFingerprint(deck.getSeedFingerprint())) {
          // Legacy deck: adopt only if its composition already equals the seed's.
          if (compositionMatchesSeed(deck, deckDTO)) {
            deck.setSeedFingerprint(current);
            toSave.add(deck);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: deck of '{}' has a hand-edited composition (legacy row, no "
                    + "fingerprint); preserving user values.",
                wrestler.getName());
            preserved++;
          }
        } else if (current.equals(deck.getSeedFingerprint())) {
          if (compositionMatchesSeed(deck, deckDTO)) {
            continue; // already in sync
          }
          applySeedComposition(deck, deckDTO);
          stampCompositionFingerprint(deck);
          toSave.add(deck);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: deck of '{}' was modified after seeding (fingerprint mismatch); "
                  + "preserving user values.",
              wrestler.getName());
          preserved++;
        }
      }
      if (!toSave.isEmpty()) {
        deckService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("deck", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during deck verify reconcile", e);
    }
  }

  /** Resolves the seed composition entries to cards and compares them with the deck's. */
  private boolean compositionMatchesSeed(Deck deck, DeckDTO deckDTO) {
    return seedCompositionEquals(deck.getCards(), deckDTO);
  }

  /**
   * Resolves each seed card entry ({@code set}, {@code number}, {@code amount}) to a Card and
   * compares the resolved multiset with the given deck cards. Unresolvable seed entries (card not
   * in the catalog) are ignored, mirroring the seed write path.
   */
  private boolean seedCompositionEquals(Set<DeckCard> deckCards, DeckDTO deckDTO) {
    Map<String, Integer> seedAmounts = new LinkedHashMap<>();
    for (DeckCardDTO cardDTO : deckDTO.getCards()) {
      Card card =
          cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
      if (card == null) {
        continue;
      }
      String key = card.getSet().getCode() + "#" + card.getNumber();
      seedAmounts.merge(key, cardDTO.getAmount(), Integer::sum);
    }
    Map<String, Integer> deckAmounts = new LinkedHashMap<>();
    for (DeckCard dc : deckCards) {
      if (dc.getSet() == null || dc.getCard() == null) {
        continue;
      }
      String key = dc.getSet().getCode() + "#" + dc.getCard().getNumber();
      deckAmounts.merge(key, dc.getAmount(), Integer::sum);
    }
    return seedAmounts.equals(deckAmounts);
  }

  /** Reconciles the deck's composition to the seed: add missing, adjust amounts, remove extra. */
  private void applySeedComposition(Deck deck, DeckDTO deckDTO) {
    Set<DeckCard> toRemove = new HashSet<>(deck.getCards());
    Map<String, Card> resolved = new HashMap<>();
    Map<String, Integer> amounts = new HashMap<>();
    for (DeckCardDTO cardDTO : deckDTO.getCards()) {
      Card card =
          cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
      if (card == null) {
        log.warn(
            "Card not found: {} in set {} from deck {}",
            cardDTO.getNumber(),
            cardDTO.getSet(),
            deckDTO.getWrestler());
        continue;
      }
      String key = card.getSet().getCode() + "#" + card.getNumber();
      resolved.putIfAbsent(key, card);
      amounts.merge(key, cardDTO.getAmount(), Integer::sum);
    }
    boolean changed = false;
    for (var entry : amounts.entrySet()) {
      String key = entry.getKey();
      Card card = resolved.get(key);
      int amount = entry.getValue();
      Optional<DeckCard> existing =
          deck.getCards().stream()
              .filter(dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
              .findFirst();
      if (existing.isPresent()) {
        DeckCard dc = existing.get();
        if (dc.getAmount() != amount) {
          dc.setAmount(amount);
          changed = true;
        }
        toRemove.remove(dc);
      } else {
        DeckCard newDeckCard = new DeckCard();
        newDeckCard.setCard(card);
        newDeckCard.setSet(card.getSet());
        newDeckCard.setAmount(amount);
        newDeckCard.setDeck(deck);
        deck.getCards().add(newDeckCard);
        changed = true;
      }
    }
    if (!toRemove.isEmpty()) {
      deck.getCards().removeAll(toRemove);
      changed = true;
    }
    if (changed) {
      log.debug("Reconciled deck composition for {}", deckDTO.getWrestler());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing decks through
   * {@link SeedCollisionDetector} instead of reconciling directly — tier-1 fields update silently
   * and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay untouched. Only
   * meaningful in PROTECT/VERIFY mode with existing rows. The domain's only seed-owned state is the
   * card composition (single-blob row, Task 5's per-row parking contract): the one observed field
   * is the sorted composition string.
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<DeckDTO> decksFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Wrestler> wrestlers =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      List<Deck> toSave = new ArrayList<>();
      for (DeckDTO deckDTO : decksFromFile) {
        Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
        if (wrestler == null) {
          continue; // user-created wrestlers have no seed match; missing decks are top-up's job
        }
        List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler);
        if (byWrestler.isEmpty()) {
          continue; // no deck yet; top-up (which runs first) creates it
        }
        Deck deck = byWrestler.getFirst();
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest,
                DOMAIN_KEY,
                deckDTO.getWrestler(),
                seedCompositionFingerprint(deckDTO));
        boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(deck.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(deck, deckDTO);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(deckDTO.getWrestler()),
                deckDTO.getWrestler(),
                rowHasStoredStamp,
                deck.getSeedFingerprint(),
                observations,
                this::deckValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the composition field, reconciled to the seed in place on the
          // loaded deck (the service's save path is shared with verify mode's reconcile).
          applySeedComposition(deck, deckDTO);
          toSave.add(deck);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(deck.getSeedFingerprint())) {
          deck.setSeedFingerprint(stamp);
          if (!toSave.contains(deck)) {
            toSave.add(deck);
          }
        }
      }
      if (!toSave.isEmpty()) {
        deckService.saveAll(toSave);
      }
      log.info(
          "Seed review detection (deck) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during deck seed-review detection", e);
    }
  }

  /** The detection-time per-field observation for one deck row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Deck deck, DeckDTO deckDTO) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    // The one seed-owned field: the deck's resolved seed composition. The DB side is the row's
    // current composition in canonical form; the seed side is the seed's resolvable entries in the
    // same canonical form (both render as the sorted "SET#number xN" multiset).
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "composition", null, canonicalComposition(deck.getCards()), seedComposition(deckDTO)));
    return obs;
  }

  /** Canonical composition string for a set of deck cards (mirrors {@link #composition(Deck)}). */
  private static String canonicalComposition(Set<DeckCard> deckCards) {
    return deckCards.stream()
        .filter(dc -> dc.getSet() != null && dc.getCard() != null)
        .map(dc -> dc.getSet().getCode() + "#" + dc.getCard().getNumber() + "x" + dc.getAmount())
        .sorted()
        .collect(Collectors.joining("\n"));
  }

  /** The seed's resolvable composition entries, in the same canonical form as the DB side. */
  private String seedComposition(DeckDTO deckDTO) {
    Map<String, Integer> seedAmounts = new LinkedHashMap<>();
    for (DeckCardDTO cardDTO : deckDTO.getCards()) {
      Card card =
          cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
      if (card == null) {
        continue; // unresolvable entries are ignored, mirroring the seed write path
      }
      seedAmounts.merge(
          card.getSet().getCode() + "#" + card.getNumber(), cardDTO.getAmount(), Integer::sum);
    }
    return seedAmounts.entrySet().stream()
        .map(e -> e.getKey() + "x" + e.getValue())
        .sorted()
        .collect(Collectors.joining("\n"));
  }

  /** Fingerprint the incoming seed composition would produce, in canonical form. */
  private String seedCompositionFingerprint(DeckDTO deckDTO) {
    return syncSupport.fingerprint(seedComposition(deckDTO));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #compositionFingerprint(Deck)} exactly.
   */
  private String deckValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.getFirst());
  }

  private static String entityKey(String wrestlerName) {
    return entityKeyFor(wrestlerName);
  }

  /**
   * The canonical entity key for a deck's owning wrestler name — shared with {@code
   * DeckSeedApplier} so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String wrestlerName) {
    return "deck:" + wrestlerName.toLowerCase().replace(' ', '-');
  }
}
