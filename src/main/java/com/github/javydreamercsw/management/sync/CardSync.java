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
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(80)
public class CardSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "card";

  private final SeedSyncSupport syncSupport;
  private final CardService cardService;
  private final CardSetService cardSetService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CardService cardService,
      final CardSetService cardSetService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.cardService = cardService;
    this.cardSetService = cardSetService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && cardService.count() > 0) {
      topUpMissingCards();
      if (syncSupport.isVerify()) {
        reconcileVerifiedCards();
      }
      return;
    }
    loadCards(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still insert seed entries that don't exist yet, so card
   * sets added in later releases reach flag-protected installs. Existing rows are never re-saved.
   */
  private void topUpMissingCards() {
    List<Card> missing = loadCards(false);
    if (!missing.isEmpty()) {
      log.info("Card top-up: inserting {} missing seed card(s).", missing.size());
      cardService.saveAll(missing);
    }
  }

  /**
   * Loads cards from {@code classpath*:cards/*.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only entries missing from the database are returned and existing rows are left
   *     untouched.
   * @return Cards that were inserted or need saving.
   */
  private List<Card> loadCards(boolean updateExisting) {
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, CardSet> setCache = new HashMap<>();
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));

      List<Card> allMissing = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        log.debug("Loading cards from file: {}", resource.getFilename());
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          List<Card> fileToSave = new ArrayList<>();
          for (CardDTO dto : cardsFromFile) {
            CardSet set = setCache.get(dto.getSet());
            if (set == null) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                log.warn(
                    "CardSet with code {} not found for card {}. Skipping card.",
                    dto.getSet(),
                    dto.getName());
                continue;
              }
              set = setOpt.get();
              setCache.put(dto.getSet(), set);
            }

            final String key = dto.getSet() + "#" + dto.getNumber();
            Card card = existing.get(key);
            if (card == null) {
              Card newCard = fromDto(dto, set);
              newCard.setSeedFingerprint(cardFingerprint(dto));
              fileToSave.add(newCard);
              allMissing.add(newCard);
            } else if (updateExisting) {
              applyDto(card, dto, set);
              card.setSeedFingerprint(cardFingerprint(card));
              fileToSave.add(card);
            }
          }
          if (updateExisting && !fileToSave.isEmpty()) {
            cardService.saveAll(fileToSave);
          }
          log.debug(
              "Processed {} cards from {} ({} new)",
              cardsFromFile.size(),
              resource.getFilename(),
              fileToSave.size());
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
      return allMissing;
    } catch (IOException e) {
      log.error("Error resolving card resources", e);
      return List.of();
    }
  }

  private static Card fromDto(@NonNull CardDTO dto, @NonNull CardSet set) {
    Card card = new Card();
    applyDto(card, dto, set);
    return card;
  }

  /** Fingerprint over a card's seed-owned fields in canonical order. */
  private String cardFingerprint(CardDTO dto) {
    return syncSupport.fingerprint(
        dto.getName(),
        dto.getDamage(),
        dto.getStamina(),
        dto.getTarget(),
        dto.getMomentum(),
        dto.getType(),
        dto.isSignature(),
        dto.isFinisher(),
        dto.isTaunt(),
        dto.isRecover(),
        dto.isPin());
  }

  /**
   * Fingerprint over a card row's current seed-owned fields, in the same canonical order. Public
   * for the seed-provenance manifest generator, which re-hashes fresh seed rows through the owning
   * sync's own method.
   */
  public String cardFingerprint(Card card) {
    return syncSupport.fingerprint(
        card.getName(),
        card.getDamage(),
        card.getStamina(),
        card.getTarget(),
        card.getMomentum(),
        card.getType(),
        card.getSignature(),
        card.getFinisher(),
        card.getTaunt(),
        card.getRecover(),
        card.getPin());
  }

  private static void applyDto(@NonNull Card card, @NonNull CardDTO dto, @NonNull CardSet set) {
    card.setName(dto.getName());
    card.setDamage(dto.getDamage());
    card.setFinisher(dto.isFinisher());
    card.setSignature(dto.isSignature());
    card.setStamina(dto.getStamina());
    card.setMomentum(dto.getMomentum());
    card.setTarget(dto.getTarget());
    card.setNumber(dto.getNumber());
    card.setSet(set);
    card.setType(dto.getType());
    card.setTaunt(dto.isTaunt());
    card.setPin(dto.isPin());
    card.setRecover(dto.isRecover());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing cards keyed by set#number. The stored
   * fingerprint is the hash of the row's seed-owned fields as the last seed write left them;
   * re-hashing the row's current fields detects hand edits. A row that still hashes to its stored
   * fingerprint is clean and adopts seed changes; a row whose fields drifted is user-edited —
   * warned and preserved. Cards in user-created sets have no seed match and are never touched.
   */
  private void reconcileVerifiedCards() {
    long start = System.currentTimeMillis();
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));
      Map<String, CardSet> setCache = new HashMap<>();
      int reconciled = 0;
      int preserved = 0;
      List<Card> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          for (CardDTO dto : cardsFromFile) {
            CardSet set = setCache.get(dto.getSet());
            if (set == null) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                continue;
              }
              set = setOpt.get();
              setCache.put(dto.getSet(), set);
            }
            String key = dto.getSet() + "#" + dto.getNumber();
            Card card = existing.get(key);
            if (card == null) {
              continue; // user-created rows have no seed match and are never touched
            }
            String current = cardFingerprint(card);
            if (SeedSyncSupport.isBlankFingerprint(card.getSeedFingerprint())) {
              if (matchesSeed(card, dto, set)) {
                card.setSeedFingerprint(current);
                toSave.add(card);
                reconciled++;
              } else {
                log.warn(
                    "Seed verify: card {}#{} has hand-edited seed-owned fields "
                        + "(legacy row, no fingerprint); preserving user values.",
                    dto.getSet(),
                    dto.getNumber());
                preserved++;
              }
            } else if (current.equals(card.getSeedFingerprint())) {
              if (matchesSeed(card, dto, set)) {
                continue; // already in sync
              }
              applyDto(card, dto, set);
              card.setSeedFingerprint(cardFingerprint(card));
              toSave.add(card);
              reconciled++;
            } else {
              log.warn(
                  "Seed verify: card {}#{} was modified after seeding (fingerprint mismatch); "
                      + "preserving user values.",
                  dto.getSet(),
                  dto.getNumber());
              preserved++;
            }
          }
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
      if (!toSave.isEmpty()) {
        cardService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("card", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving card resources for verify reconcile", e);
    }
  }

  private boolean matchesSeed(Card card, CardDTO dto, CardSet set) {
    return Objects.equals(card.getName(), dto.getName())
        && Objects.equals(card.getDamage(), dto.getDamage())
        && Objects.equals(card.getStamina(), dto.getStamina())
        && Objects.equals(card.getTarget(), dto.getTarget())
        && Objects.equals(card.getMomentum(), dto.getMomentum())
        && Objects.equals(card.getType(), dto.getType())
        && card.getSignature() == dto.isSignature()
        && card.getFinisher() == dto.isFinisher()
        && card.getTaunt() == dto.isTaunt()
        && card.getRecover() == dto.isRecover()
        && card.getPin() == dto.isPin()
        && card.getSet().getId().equals(set.getId());
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing cards
   * field-by-field through {@link SeedCollisionDetector} instead of reconciling directly — tier-1
   * fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields
   * stay untouched. Only meaningful in PROTECT/VERIFY mode with existing rows.
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, CardSet> setCache = new HashMap<>();
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));
      List<Card> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          for (CardDTO dto : cardsFromFile) {
            CardSet set = setCache.get(dto.getSet());
            if (set == null) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                continue; // card's set is missing; not a seeded row in this install
              }
              set = setOpt.get();
              setCache.put(dto.getSet(), set);
            }
            Card card = existing.get(dto.getSet() + "#" + dto.getNumber());
            if (card == null) {
              continue; // user-created rows have no seed match; missing rows are top-up's job
            }
            String newFingerprint =
                SeedReviewDetectionSupport.incomingFingerprint(
                    provenanceManifest, DOMAIN_KEY, seedKey(dto), cardFingerprint(dto));
            boolean rowHasStoredStamp =
                SeedReviewDetectionSupport.hasStamp(card.getSeedFingerprint());
            List<SeedCollisionDetector.FieldObservation> observations =
                fieldObservations(card, dto);
            SeedReviewDetectionSupport.DetectionResult result =
                SeedReviewDetectionSupport.detectRow(
                    DOMAIN_KEY,
                    entityKey(seedKey(dto)),
                    dto.getName(),
                    rowHasStoredStamp,
                    card.getSeedFingerprint(),
                    observations,
                    this::cardValueListFingerprint,
                    newFingerprint,
                    seedReviewService);
            String stamp =
                SeedReviewDetectionSupport.stampForResolvedRow(
                    result.provenanceExact(), newFingerprint);
            if (!result.silentUpdateFields().isEmpty()) {
              // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
              // the same row must never be touched (per-field granularity, spec "Tier 2
              // granularity").
              applySilentSeedFields(card, dto, result.silentUpdateFields());
              toSave.add(card);
            }
            // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
            // comparing after the set reads the NEW value and the save is silently skipped forever
            // (the row never converges).
            if (stamp != null && !stamp.equals(card.getSeedFingerprint())) {
              card.setSeedFingerprint(stamp);
              if (!toSave.contains(card)) {
                toSave.add(card);
              }
            }
          }
        } catch (IOException e) {
          log.error("Error during card seed-review detection", e);
        }
      }
      if (!toSave.isEmpty()) {
        cardService.saveAll(toSave);
      }
      log.info(
          "Seed review detection (card) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving card resources for seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one card row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(Card card, CardDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation("name", null, card.getName(), dto.getName()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "damage", null, str(card.getDamage()), String.valueOf(dto.getDamage())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "stamina", null, str(card.getStamina()), String.valueOf(dto.getStamina())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "target", null, str(card.getTarget()), String.valueOf(dto.getTarget())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "momentum", null, str(card.getMomentum()), String.valueOf(dto.getMomentum())));
    obs.add(
        new SeedCollisionDetector.FieldObservation("type", null, card.getType(), dto.getType()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "signature", null, str(card.getSignature()), String.valueOf(dto.isSignature())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "finisher", null, str(card.getFinisher()), String.valueOf(dto.isFinisher())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "taunt", null, str(card.getTaunt()), String.valueOf(dto.isTaunt())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "recover", null, str(card.getRecover()), String.valueOf(dto.isRecover())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "pin", null, str(card.getPin()), String.valueOf(dto.isPin())));
    return obs;
  }

  private static String str(Object value) {
    return value == null ? null : value.toString();
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #cardFingerprint(Card)} exactly (both are {@code toString}-based).
   */
  private String cardValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.toArray());
  }

  private static String seedKey(CardDTO dto) {
    return dto.getSet() + "#" + dto.getNumber();
  }

  private static String entityKey(String seedKey) {
    return entityKeyFor(seedKey);
  }

  /**
   * The canonical entity key for a card seed key ({@code SET#number}) — shared with {@code
   * CardSeedApplier} so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String seedKey) {
    return "card:" + seedKey.toLowerCase();
  }

  /**
   * The canonical seed key for a card row ({@code SET#number}) — shared with {@code
   * CardSeedApplier} so apply-side resolution remints keys exactly as detection minted them.
   */
  static String seedKeyOf(Card card) {
    return card.getSet().getCode() + "#" + card.getNumber();
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@code applyDto(Card, CardDTO, CardSet)}.
   */
  private void applySilentSeedFields(Card card, CardDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "name" -> card.setName(dto.getName());
        case "damage" -> card.setDamage(dto.getDamage());
        case "stamina" -> card.setStamina(dto.getStamina());
        case "target" -> card.setTarget(dto.getTarget());
        case "momentum" -> card.setMomentum(dto.getMomentum());
        case "type" -> card.setType(dto.getType());
        case "signature" -> card.setSignature(dto.isSignature());
        case "finisher" -> card.setFinisher(dto.isFinisher());
        case "taunt" -> card.setTaunt(dto.isTaunt());
        case "recover" -> card.setRecover(dto.isRecover());
        case "pin" -> card.setPin(dto.isPin());
        default ->
            throw new IllegalStateException("Unknown silent-update card field '" + field + "'");
      }
    }
  }
}
