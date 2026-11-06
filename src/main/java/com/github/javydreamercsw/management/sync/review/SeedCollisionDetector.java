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

import java.util.List;
import java.util.Objects;

/**
 * Pure per-field classifier for seed-data collisions (seed-update review feature). Decides, field
 * by field, whether a value updates silently or parks as a pending {@link SeedUpdateDecision} for
 * administrator review — the semantic heart of the detection pass.
 *
 * <h2>The three-way per-field contract</h2>
 *
 * Every seed-owned field of an entity is observed as a {@link FieldObservation}: the value the
 * previous release's seed wrote for that field ({@code storedSeedValue}), the value the row holds
 * now ({@code currentValue}), and the value the incoming release's seed declares ({@code
 * newSeedValue}). {@link #disposition(boolean, FieldObservation)} maps one observation onto one of
 * four outcomes:
 *
 * <ul>
 *   <li><b>Unstamped row</b> ({@code rowHasStoredStamp == false}) — there is no recorded baseline,
 *       so {@code storedSeedValue} is ignored and the decision is a direct content compare: {@code
 *       currentValue == newSeedValue} is {@link FieldDisposition#NO_OP} (the caller stamps the row
 *       silently); any other value is a first-upgrade collision and parks ({@link
 *       FieldDisposition#PARK_DECISION}).
 *   <li><b>Stamped row, untouched field</b> ({@code storedSeedValue == currentValue}): the seed
 *       changed it ({@code storedSeedValue != newSeedValue}) → {@link
 *       FieldDisposition#SILENT_UPDATE} (tier 1 — the caller applies the new value and re-stamps);
 *       all three agree → {@link FieldDisposition#NO_OP}.
 *   <li><b>Stamped row, administrator-edited field</b> ({@code storedSeedValue != currentValue}):
 *       the seed changed the field too → genuine collision, {@link FieldDisposition#PARK_DECISION}
 *       (tier 2); the seed still declares the stored value → the edit is respected, {@link
 *       FieldDisposition#PRESERVE_EDIT} (tier 3).
 * </ul>
 *
 * <h2>Stamp and stored-value semantics</h2>
 *
 * {@code rowHasStoredStamp} is a <b>row-level</b> property: it reflects the row's {@code
 * seed_fingerprint} stamp (blank means legacy, pre-provenance — see {@link
 * com.github.javydreamercsw.management.sync.SeedSyncSupport#isBlankFingerprint}), not any per-field
 * property of the observations. When it is {@code true}, {@code storedSeedValue} must carry the
 * previous release's per-field seed value; {@code null} is legitimate for a single field (the
 * previous seed omitted that field), but passing {@code null} for <b>every</b> field of a stamped
 * row parks all of them ({@code null != currentValue} and {@code null != newSeedValue} read as an
 * administrator edit plus a seed change) — fail-safe, but wrong. When it is {@code false}, {@code
 * storedSeedValue} is ignored entirely.
 *
 * <p>{@link #classify(boolean, List)} returns only the {@link FieldDisposition#PARK_DECISION}
 * fields as {@link FieldCollision}s carrying the {@code (currentValue, newSeedValue)} pair the
 * caller passes to {@link SeedUpdateReviewService#parkFieldConflict}. Silent outcomes are
 * deliberately absent from the list: applying tier-1 updates and stamping no-op rows is the
 * caller's job.
 *
 * <h2>Keep-mine stamping</h2>
 *
 * The keep-mine contract falls out of the stamped-row branch. When an administrator resolves a
 * collision with {@code KEEP_MINE}, the caller keeps the administrator's content but stamps the row
 * with the reviewed release's seed value — so the next detection pass observes {@code
 * storedSeedValue == reviewedSeedValue != currentValue}. With the same seed that is {@link
 * FieldDisposition#PRESERVE_EDIT} (the kept edit survives restarts); when a later release changes
 * the seed again, the field becomes {@link FieldDisposition#PARK_DECISION} — it re-collides and is
 * re-offered for review instead of being silently overwritten.
 *
 * <h2>What this class does not do</h2>
 *
 * Removal detection is <b>not</b> a field diff — it needs DB lookups and is the caller's
 * computation: an entity is a removal candidate when its key is present in the previous release's
 * provenance manifest (or otherwise known to be seed-owned) but absent from the new seed's
 * entity-key set. {@link #isRemoval(boolean)} pins that caller-side contract.
 *
 * <p>This is a pure static utility — no Spring wiring. PR 4's per-domain adapters translate each
 * sync's entity fields into {@link FieldObservation}s and feed the parked collisions plus the
 * silent updates back to the service.
 */
public final class SeedCollisionDetector {

  private SeedCollisionDetector() {}

  /**
   * One seed-owned field of one entity, observed at detection time.
   *
   * @param fieldName the seed-owned field's name
   * @param storedSeedValue what the previous release's seed wrote for this field; {@code null} is
   *     legitimate when the previous seed omitted the field, and the value is ignored altogether
   *     when the row is unstamped ({@code rowHasStoredStamp == false}) — it is never a marker for
   *     "unstamped", which is a row-level property (see the class doc)
   * @param currentValue what the row holds now
   * @param newSeedValue what the incoming release's seed declares
   */
  public record FieldObservation(
      String fieldName, String storedSeedValue, String currentValue, String newSeedValue) {}

  /**
   * A field that parks as a pending review decision.
   *
   * @param fieldName the colliding field
   * @param currentValue the database's current value at detection time
   * @param newValue the incoming seed value at detection time
   */
  public record FieldCollision(String fieldName, String currentValue, String newValue) {}

  /** The four per-field outcomes of the three-way comparison. */
  public enum FieldDisposition {
    /** Seed changed the field and nobody edited it — the caller applies the new value silently. */
    SILENT_UPDATE,
    /** Genuine collision — the caller parks a pending decision for administrator review. */
    PARK_DECISION,
    /** Nothing to do — the caller just (re-)stamps the row. */
    NO_OP,
    /** Administrator edit with an unchanged seed — preserved silently, never overwritten. */
    PRESERVE_EDIT
  }

  /**
   * Classifies one field observation.
   *
   * @param rowHasStoredStamp whether the row carries a stored seed stamp (false for pre-provenance
   *     rows — their {@code storedSeedValue} is ignored)
   * @param observation the field's three-way observation
   * @return the field's disposition
   */
  public static FieldDisposition disposition(
      boolean rowHasStoredStamp, FieldObservation observation) {
    if (!rowHasStoredStamp) {
      // No baseline recorded: first-upgrade content compare against the incoming seed only.
      return Objects.equals(observation.currentValue(), observation.newSeedValue())
          ? FieldDisposition.NO_OP
          : FieldDisposition.PARK_DECISION;
    }
    boolean adminEdited =
        !Objects.equals(observation.storedSeedValue(), observation.currentValue());
    boolean seedChanged =
        !Objects.equals(observation.storedSeedValue(), observation.newSeedValue());
    if (!adminEdited) {
      return seedChanged ? FieldDisposition.SILENT_UPDATE : FieldDisposition.NO_OP;
    }
    return seedChanged ? FieldDisposition.PARK_DECISION : FieldDisposition.PRESERVE_EDIT;
  }

  /**
   * Classifies a row's seed-owned fields and returns only the ones that park as pending review
   * decisions (tier-2 collisions), each carrying the {@code (currentValue, newValue)} pair for
   * {@link SeedUpdateReviewService#parkFieldConflict}. Silent-update, no-op and preserved-edit
   * fields are not returned — acting on those is the caller's job.
   *
   * @param rowHasStoredStamp whether the row carries a stored seed stamp
   * @param observations the row's seed-owned field observations
   * @return the colliding fields, in the given order
   */
  public static List<FieldCollision> classify(
      boolean rowHasStoredStamp, List<FieldObservation> observations) {
    return observations.stream()
        .filter(o -> disposition(rowHasStoredStamp, o) == FieldDisposition.PARK_DECISION)
        .map(o -> new FieldCollision(o.fieldName(), o.currentValue(), o.newSeedValue()))
        .toList();
  }

  /**
   * Pins the removal contract: an entity absent from the incoming seed is a removal candidate.
   * Whether the key was previously seed-owned (the other half of the test) is the caller's
   * manifest/entity-key-set comparison, not a field diff.
   *
   * @param seedContainsEntity whether the incoming seed declares the entity key
   * @return true when the seed dropping the entity makes it a removal candidate
   */
  public static boolean isRemoval(boolean seedContainsEntity) {
    return !seedContainsEntity;
  }
}
