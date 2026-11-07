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
package com.github.javydreamercsw.management.sync.review;

import com.github.javydreamercsw.management.sync.SeedSyncSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared driver for the version-change detection pass of the seed-update review feature: one row's
 * seed-owned fields go through {@link SeedCollisionDetector#classify} and the parked collisions
 * land in {@link SeedUpdateReviewService} as PENDING decisions.
 *
 * <h2>Per-field stored-seed values on a row-level stamp</h2>
 *
 * Rows carry only a row-level {@code seed_fingerprint}; no per-field previous-seed snapshot is
 * stored. This driver reconstructs the previous release's per-field seed values without a schema
 * change, in three steps (each cheaper check first):
 *
 * <ol>
 *   <li><b>Clean row</b> — the row's current fields re-hash to its stored stamp: the row is
 *       byte-identical to the previous seed write, so {@code storedSeedValue} for every field is
 *       the row's current value. The classifier then reads every changed field as tier-1 {@code
 *       SILENT_UPDATE} and every untouched field as tier-3 {@code NO_OP}.
 *   <li><b>Single-edit row</b> — the row is dirty, but substituting the incoming seed's value for
 *       exactly one field (each field tried in turn) reproduces the stored stamp: the administrator
 *       edited that one field and the seed did not change it, while every other field's current
 *       value IS attested as the previous seed's value. Reconstructed this way, the classifier
 *       reads the admin's field as tier-3 {@code PRESERVE_EDIT} (or tier-2 when a later release
 *       changes the seed there — the keep-mine re-collision contract), and every seed-changed
 *       untouched field as tier-1.
 *   <li><b>Fail-safe</b> — neither reconstruction matches (multi-field admin edits, or the same
 *       field edited AND seed-changed): per-field provenance is unknowable from a row-level stamp,
 *       so every field carries {@code null} stored values and classifies fail-safe — fields whose
 *       current value equals the incoming seed read as {@code NO_OP}, everything else parks as
 *       tier-2 {@code PARK_DECISION} (pinned by {@code
 *       SeedCollisionDetectorTest.stampedRowWithNullStoredValuesParksEverythingFailSafe} — null
 *       stored values on a stamped row park, they never silently update). The fail-safe direction
 *       is deliberate: an unresolvable row is asked about, never silently overwritten.
 * </ol>
 *
 * <p>Restarts of the same version re-run the pass harmlessly: {@code parkFieldConflict} dedupes on
 * the {@code (domain, entity, field, PENDING)} coordinate and only refreshes the incoming snapshot.
 */
@Slf4j
public final class SeedReviewDetectionSupport {

  private SeedReviewDetectionSupport() {}

  /**
   * Classifies one row's seed-owned fields and parks the collisions.
   *
   * @param domainKey seed domain (e.g. {@code "wrestler"})
   * @param entityKey stable business key (e.g. {@code "wrestler:bret-hart"})
   * @param displayName human-readable entity name for the review UI
   * @param rowHasStoredStamp whether the row carries a non-blank {@code seed_fingerprint}
   * @param storedStamp the row's stored {@code seed_fingerprint} (ignored when {@code
   *     rowHasStoredStamp} is false)
   * @param observations the row's seed-owned field observations; {@code storedSeedValue} is
   *     ignored, {@code currentValue} and {@code newSeedValue} are required
   * @param rowHashFn re-hashes an ordered list of field values (same order and serialization as the
   *     row's canonical fingerprint) — used for the clean-row and single-edit reconstructions
   * @param newFingerprint the incoming seed's fingerprint for the row (manifest hash, or the
   *     content-computed hash when the manifest lacks the row)
   * @param reviewService the service the parked collisions are reported to
   * @return the detection outcome: the tier-1 field names (the caller applies those values and
   *     re-stamps the row with {@code newFingerprint}) and whether per-field provenance was
   *     reconstructed exactly (a fail-safe row keeps its stale stamp); tier-2 fields are parked
   *     here, tier-3 fields need no action
   */
  public static DetectionResult detectRow(
      String domainKey,
      String entityKey,
      String displayName,
      boolean rowHasStoredStamp,
      String storedStamp,
      List<SeedCollisionDetector.FieldObservation> observations,
      Function<List<String>, String> rowHashFn,
      String newFingerprint,
      SeedUpdateReviewService reviewService) {
    if (newFingerprint == null || newFingerprint.isBlank()) {
      throw new IllegalArgumentException("newFingerprint is required for seed review detection");
    }
    List<SeedCollisionDetector.FieldObservation> effective =
        rowHasStoredStamp
            ? resolveStoredValues(storedStamp, observations, rowHashFn)
            : observations;
    boolean provenanceExact =
        !rowHasStoredStamp
            || rowHashFn
                .apply(
                    effective.stream()
                        .map(SeedCollisionDetector.FieldObservation::storedSeedValue)
                        .toList())
                .equals(storedStamp);
    List<SeedCollisionDetector.FieldCollision> collisions =
        SeedCollisionDetector.classify(rowHasStoredStamp, effective);
    List<String> silentUpdates = new ArrayList<>();
    for (SeedCollisionDetector.FieldObservation o : effective) {
      if (SeedCollisionDetector.disposition(rowHasStoredStamp, o)
          == SeedCollisionDetector.FieldDisposition.SILENT_UPDATE) {
        silentUpdates.add(o.fieldName());
      }
    }
    for (SeedCollisionDetector.FieldCollision collision : collisions) {
      reviewService.parkFieldConflict(
          domainKey,
          entityKey,
          displayName,
          collision.fieldName(),
          collision.currentValue(),
          collision.newValue(),
          newFingerprint);
    }
    return new DetectionResult(silentUpdates, provenanceExact);
  }

  /** Outcome of one row's detection: tier-1 field names plus provenance confidence. */
  public record DetectionResult(List<String> silentUpdateFields, boolean provenanceExact) {}

  /**
   * Reconstructs per-field stored values for a stamped row (see the class doc's three steps).
   *
   * @return observations carrying reconstructed {@code storedSeedValue}s — exact for clean and
   *     single-edit rows, all-null (fail-safe) otherwise
   */
  private static List<SeedCollisionDetector.FieldObservation> resolveStoredValues(
      String storedStamp,
      List<SeedCollisionDetector.FieldObservation> observations,
      Function<List<String>, String> rowHashFn) {
    // Step 1 — clean row: current values ARE the previous seed's values.
    List<String> currentValues = observations.stream().map(o -> o.currentValue()).toList();
    if (rowHashFn.apply(currentValues).equals(storedStamp)) {
      return withStored(observations, currentValues);
    }
    // Step 2 — single-edit row: one field's current value replaced by the NEW SEED's value
    // reproduces the stored stamp → the admin edited that field (the seed did not change it, so
    // the stamp still holds the seed's value there) and every other field's current value is
    // attested as the previous seed's value. The CANDIDATE list is the stored-value vector: for
    // the edited field it restores the seed's value (not the admin's).
    for (int i = 0; i < observations.size(); i++) {
      List<String> candidate = new ArrayList<>(currentValues);
      candidate.set(i, observations.get(i).newSeedValue());
      if (rowHashFn.apply(candidate).equals(storedStamp)) {
        log.debug(
            "Seed review: single admin edit localized to field '{}'",
            observations.get(i).fieldName());
        return withStored(observations, candidate);
      }
    }
    // Step 3 — fail-safe: provenance unknowable for a multi-edited row. Fields where the DB
    // already equals the incoming seed need no decision (stored = current → NO_OP); fields that
    // DIFFER get null stored values, which the classifier reads as admin-edit + seed-change →
    // PARK_DECISION. The fail-safe direction is deliberate: an unresolvable row is asked about,
    // never silently overwritten.
    log.debug("Seed review: row provenance unresolvable — parking every differing field fail-safe");
    List<String> failSafe = new ArrayList<>(observations.size());
    for (SeedCollisionDetector.FieldObservation o : observations) {
      failSafe.add(
          java.util.Objects.equals(o.currentValue(), o.newSeedValue()) ? o.currentValue() : null);
    }
    return withStored(observations, failSafe);
  }

  private static List<SeedCollisionDetector.FieldObservation> withStored(
      List<SeedCollisionDetector.FieldObservation> observations, List<String> storedValues) {
    List<SeedCollisionDetector.FieldObservation> rebuilt = new ArrayList<>(observations.size());
    for (int i = 0; i < observations.size(); i++) {
      SeedCollisionDetector.FieldObservation o = observations.get(i);
      rebuilt.add(
          new SeedCollisionDetector.FieldObservation(
              o.fieldName(), storedValues.get(i), o.currentValue(), o.newSeedValue()));
    }
    return rebuilt;
  }

  private static List<String> nullList(int size) {
    List<String> values = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      values.add(null);
    }
    return values;
  }

  /**
   * Resolves the incoming seed's fingerprint for one row: the committed manifest's hash when the
   * row's key is present there, otherwise the row's content-computed hash (first-upgrade
   * content-compare fallback — the manifest is never a runtime hard dependency).
   *
   * @param manifest the loaded {@code seed-provenance.json} (never {@code null})
   * @param domainKey seed domain
   * @param seedKey the row's manifest key (usually the business name)
   * @param contentFingerprint the row's fingerprint recomputed from incoming seed values, in the
   *     owning sync's canonical field order
   * @return the manifest hash, or {@code contentFingerprint} when the manifest has no entry
   */
  public static String incomingFingerprint(
      SeedProvenanceManifest manifest,
      String domainKey,
      String seedKey,
      String contentFingerprint) {
    Map<String, String> entries = manifest.domain(domainKey).orElse(Map.of());
    return entries.getOrDefault(seedKey, contentFingerprint);
  }

  /**
   * Re-stamps a stamped row after the version-change pass decided its fate, per the {@link
   * SeedDomainApplier} stamp contract: tier-1 (values applied) and tier-3 (no-op) rows adopt the
   * incoming fingerprint so the next boot's three-way compare sees a clean baseline; a fail-safe
   * row (tier-2 parked, provenance unresolvable) keeps its stale stamp so it re-collides after
   * resolution.
   *
   * @param provenanceExact whether per-field provenance was reconstructed exactly for this row
   *     (clean or single-edit)
   * @return the fingerprint to stamp ({@code newFingerprint}), or {@code null} when the row must
   *     keep its existing stamp
   */
  public static String stampForResolvedRow(boolean provenanceExact, String newFingerprint) {
    return provenanceExact ? newFingerprint : null;
  }

  /**
   * @return true when the fingerprint is present and non-blank (see {@link SeedSyncSupport}).
   */
  public static boolean hasStamp(String fingerprint) {
    return !SeedSyncSupport.isBlankFingerprint(fingerprint);
  }
}
