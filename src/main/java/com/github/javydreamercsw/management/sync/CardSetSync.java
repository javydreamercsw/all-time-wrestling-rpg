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
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(70)
public class CardSetSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "card_set";

  private final SeedSyncSupport syncSupport;
  private final CardSetService cardSetService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CardSetSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CardSetService cardSetService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.cardSetService = cardSetService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && cardSetService.count() > 0) {
      topUpMissingCardSets();
      if (syncSupport.isVerify()) {
        reconcileVerifiedCardSets();
      }
      return;
    }
    loadCardSets(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still insert seed entries that don't exist yet, so
   * catalogs added in later releases reach flag-protected installs. Existing rows are never
   * re-saved.
   */
  private void topUpMissingCardSets() {
    List<CardSet> missing = loadCardSets(false);
    if (!missing.isEmpty()) {
      log.info("Card set top-up: inserting {} missing seed card set(s).", missing.size());
      cardSetService.saveAll(missing);
    }
  }

  /**
   * Loads card sets from {@code sets.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only entries missing from the database are returned and existing rows are left
   *     untouched.
   * @return Card sets that were inserted or need saving.
   */
  private List<CardSet> loadCardSets(boolean updateExisting) {
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (!resource.exists()) {
      log.warn("Card sets file not found: {}", resource.getPath());
      return List.of();
    }
    log.debug("Loading card sets from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<CardSet> setsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      List<CardSet> toSave = new ArrayList<>();
      for (CardSet c : setsFromFile) {
        Optional<CardSet> existingOpt = cardSetService.findBySetCode(c.getCode());
        if (existingOpt.isPresent()) {
          if (updateExisting) {
            CardSet existing = existingOpt.get();
            existing.setName(c.getName());
            existing.setSeedFingerprint(syncSupport.fingerprint(existing.getName()));
            toSave.add(existing);
          }
        } else {
          c.setSeedFingerprint(syncSupport.fingerprint(c.getName()));
          toSave.add(c);
        }
      }
      if (!toSave.isEmpty() && updateExisting) {
        cardSetService.saveAll(toSave);
      }
      log.debug("Card sets loading completed - {} sets processed", setsFromFile.size());
      return toSave;
    } catch (IOException e) {
      log.error("Error loading card sets from file", e);
      return List.of();
    }
  }

  /**
   * Verify mode: reconcile seed-owned fields (name) of existing card sets. The stored fingerprint
   * is the hash of the row's name as the last seed write left it; re-hashing the row's current name
   * detects hand edits. A row that still hashes to its stored fingerprint is clean and adopts seed
   * changes; a row whose name drifted is user-edited — warned and preserved. User-created sets
   * (CUSTOM etc.) have no seed match and are never touched.
   */
  private void reconcileVerifiedCardSets() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CardSet> setsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      int reconciled = 0;
      int preserved = 0;
      List<CardSet> toSave = new ArrayList<>();
      for (CardSet c : setsFromFile) {
        Optional<CardSet> existingOpt = cardSetService.findBySetCode(c.getCode());
        if (existingOpt.isEmpty()) {
          continue; // user-created rows have no seed match and are never touched
        }
        CardSet existing = existingOpt.get();
        String current = syncSupport.fingerprint(existing.getName());
        if (SeedSyncSupport.isBlankFingerprint(existing.getSeedFingerprint())) {
          if (c.getName().equals(existing.getName())) {
            existing.setSeedFingerprint(current);
            toSave.add(existing);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: card set '{}' has hand-edited seed-owned fields "
                    + "(legacy row, no fingerprint); preserving user values.",
                existing.getCode());
            preserved++;
          }
        } else if (current.equals(existing.getSeedFingerprint())) {
          if (c.getName().equals(existing.getName())) {
            continue; // already in sync
          }
          existing.setName(c.getName());
          // Stamp the NEW name's hash — the pre-rename hash would poison the next pass.
          existing.setSeedFingerprint(syncSupport.fingerprint(existing.getName()));
          toSave.add(existing);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: card set '{}' was modified after seeding (fingerprint mismatch); "
                  + "preserving user values.",
              existing.getCode());
          preserved++;
        }
      }
      if (!toSave.isEmpty()) {
        cardSetService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("card set", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during card set verify reconcile", e);
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing card sets
   * field-by-field through {@link SeedCollisionDetector} instead of reconciling directly — tier-1
   * fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields
   * stay untouched. Only meaningful in PROTECT/VERIFY mode with existing rows. The domain's only
   * seed-owned field is the display name (single-blob row, Task 5's per-row parking contract).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CardSet> setsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      List<CardSet> toSave = new ArrayList<>();
      for (CardSet c : setsFromFile) {
        Optional<CardSet> existingOpt = cardSetService.findBySetCode(c.getCode());
        if (existingOpt.isEmpty()) {
          continue; // user-created rows have no seed match; missing rows are top-up's job
        }
        CardSet existing = existingOpt.get();
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, c.getCode(), syncSupport.fingerprint(c.getName()));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(existing, c);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(c.getCode()),
                c.getName(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::cardSetValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin rename on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(existing, c, result.silentUpdateFields());
          toSave.add(existing);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(existing.getSeedFingerprint())) {
          existing.setSeedFingerprint(stamp);
          if (!toSave.contains(existing)) {
            toSave.add(existing);
          }
        }
      }
      if (!toSave.isEmpty()) {
        cardSetService.saveAll(toSave);
      }
      log.info(
          "Seed review detection (card set) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during card set seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one card set row against its seed row. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      CardSet existing, CardSet seed) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "name", null, existing.getName(), seed.getName()));
    return obs;
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors the sync's name-only stamp exactly.
   */
  private String cardSetValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.getFirst());
  }

  private static String entityKey(String code) {
    return entityKeyFor(code);
  }

  /**
   * The canonical entity key for a card-set code — shared with {@code CardSetSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String code) {
    return "card_set:" + code.toLowerCase();
  }

  /**
   * Writes ONLY the named silent-update fields from the seed row onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries. The
   * fingerprint is NOT touched here: the caller's stamp block adopts the incoming fingerprint (the
   * manifest hash, or the new name's content hash), which is the re-stamp the rename needs.
   */
  private void applySilentSeedFields(CardSet existing, CardSet seed, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "name" -> existing.setName(seed.getName());
        default ->
            throw new IllegalStateException("Unknown silent-update card set field '" + field + "'");
      }
    }
  }
}
