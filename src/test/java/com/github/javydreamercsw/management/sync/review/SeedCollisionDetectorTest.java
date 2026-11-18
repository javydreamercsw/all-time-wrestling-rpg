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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector.FieldCollision;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector.FieldDisposition;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector.FieldObservation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the per-field seed collision classifier (seed-update review feature, Task 5).
 *
 * <p>The cases pin the three-way per-field contract: unstamped rows decide on a direct content
 * compare against the incoming seed (first-upgrade); stamped rows decide on the stored-vs-current
 * (admin edit?) and stored-vs-new (seed changed too?) axes. The keep-mine case simulates the
 * post-Apply state the spec mandates: a field decided KEEP_MINE is stamped with the reviewed
 * release's seed value while the content stays the administrator's, so a later seed change re-parks
 * it instead of silently overwriting it.
 */
class SeedCollisionDetectorTest {

  @Test
  void unstampedRowMatchingNewSeedStampsSilently() {
    // Pre-provenance row whose content already matches the incoming release: nothing to decide,
    // the caller stamps the row silently.
    List<FieldObservation> observations =
        List.of(
            observation("alignment", null, "Face", "Face"),
            observation("tier", null, "Rookie", "Rookie"));

    assertTrue(SeedCollisionDetector.classify(false, observations).isEmpty());
    for (FieldObservation o : observations) {
      assertEquals(FieldDisposition.NO_OP, SeedCollisionDetector.disposition(false, o));
    }
  }

  @Test
  void unstampedRowDivergentFieldParksDecision() {
    // Pre-provenance row where one field drifted from the incoming seed: exactly that field is
    // parked with (current, new) values; the matching fields are absent.
    List<FieldObservation> observations =
        List.of(
            observation("alignment", null, "Face", "Face"),
            observation("finisher", null, "Legend Driver", "Diamond Cutter"),
            observation("tier", null, "Rookie", "Rookie"));

    assertEquals(
        List.of(new FieldCollision("finisher", "Legend Driver", "Diamond Cutter")),
        SeedCollisionDetector.classify(false, observations));
    assertEquals(
        FieldDisposition.PARK_DECISION,
        SeedCollisionDetector.disposition(false, observations.get(1)));
    assertEquals(
        FieldDisposition.NO_OP, SeedCollisionDetector.disposition(false, observations.get(0)));
    assertEquals(
        FieldDisposition.NO_OP, SeedCollisionDetector.disposition(false, observations.get(2)));
  }

  @Test
  void stampedRowSeedChangedFieldUpdatesSilently() {
    // Stamped row, field untouched by administrators (stored == current), seed moved on: tier-1
    // silent update — the caller applies the new value and re-stamps, no decision is parked.
    FieldObservation o = observation("alignment", "Face", "Face", "Heel");

    assertEquals(FieldDisposition.SILENT_UPDATE, SeedCollisionDetector.disposition(true, o));
    assertTrue(SeedCollisionDetector.classify(true, List.of(o)).isEmpty());
  }

  @Test
  void stampedRowAdminEditedAndSeedChangedParksCollision() {
    // Stamped row where the administrator edited the field away from the stored seed value and
    // the new seed changed the field too: a genuine tier-2 collision carrying (current, new).
    FieldObservation o =
        observation("finisher", "Diamond Cutter", "Legend Driver", "Face Cruncher");

    assertEquals(FieldDisposition.PARK_DECISION, SeedCollisionDetector.disposition(true, o));
    assertEquals(
        List.of(new FieldCollision("finisher", "Legend Driver", "Face Cruncher")),
        SeedCollisionDetector.classify(true, List.of(o)));
  }

  @Test
  void stampedRowAdminEditedSeedUnchangedPreservesSilently() {
    // Stamped row where the administrator edited the field but the seed still declares the stored
    // value: tier-3 — the edit is preserved silently, nothing parked, nothing applied.
    FieldObservation o =
        observation("finisher", "Diamond Cutter", "Legend Driver", "Diamond Cutter");

    assertEquals(FieldDisposition.PRESERVE_EDIT, SeedCollisionDetector.disposition(true, o));
    assertTrue(SeedCollisionDetector.classify(true, List.of(o)).isEmpty());
  }

  @Test
  void keepMineStampPreventsNextReleaseOverwrite() {
    // Boot 1 (release 1): the seed declares "Diamond Cutter", the row holds the administrator's
    // "Legend Driver", and the collision is resolved with KEEP_MINE — the content stays
    // "Legend Driver" while the row is stamped with release 1's seed value ("Diamond Cutter").
    //
    // Boot 2 (release 2): the seed changes the field to "Face Cruncher". Because the keep-mine
    // decision stamped the row, the classifier sees stamped + stored ("Diamond Cutter") !=
    // current ("Legend Driver") + stored != new ("Face Cruncher") -> PARK_DECISION: the kept
    // value is re-offered for review instead of being silently overwritten.
    FieldObservation boot2 =
        observation("finisher", "Diamond Cutter", "Legend Driver", "Face Cruncher");
    assertEquals(FieldDisposition.PARK_DECISION, SeedCollisionDetector.disposition(true, boot2));
    assertEquals(
        List.of(new FieldCollision("finisher", "Legend Driver", "Face Cruncher")),
        SeedCollisionDetector.classify(true, List.of(boot2)));

    // Convergence: if the administrator instead picks USE_NEW at boot 2, the content moves to the
    // release-2 value under the release-2 stamp — boot 3 is a no-op, so the loop terminates.
    FieldObservation boot3 =
        observation("finisher", "Face Cruncher", "Face Cruncher", "Face Cruncher");
    assertEquals(FieldDisposition.NO_OP, SeedCollisionDetector.disposition(true, boot3));
  }

  @Test
  void stampedRowAllValuesEqualIsNoOp() {
    // All three values agree (stored == current == new): nothing changed anywhere.
    FieldObservation o = observation("alignment", "Face", "Face", "Face");

    assertEquals(FieldDisposition.NO_OP, SeedCollisionDetector.disposition(true, o));
    assertTrue(SeedCollisionDetector.classify(true, List.of(o)).isEmpty());
  }

  @Test
  void stampedRowWithNullStoredValuesParksEverythingFailSafe() {
    // Misuse guard for the stamped-row contract: a stamped row must carry the previous release's
    // per-field seed values. Passing null for every stored value is wrong — null reads as both an
    // administrator edit (stored != current) and a seed change (stored != new) — so EVERY field
    // with content parks, fail-safe but noisy: nothing is ever silently updated, everything is
    // re-offered for review instead.
    List<FieldObservation> observations =
        List.of(
            observation("finisher", null, "Legend Driver", "Face Cruncher"),
            observation("alignment", null, "Face", "Face"));

    assertEquals(
        FieldDisposition.PARK_DECISION,
        SeedCollisionDetector.disposition(true, observations.get(0)));
    assertEquals(
        FieldDisposition.PARK_DECISION,
        SeedCollisionDetector.disposition(true, observations.get(1)));
    assertEquals(
        List.of(
            new FieldCollision("finisher", "Legend Driver", "Face Cruncher"),
            new FieldCollision("alignment", "Face", "Face")),
        SeedCollisionDetector.classify(true, observations));
  }

  @Test
  void stampedRowNullStoredMatchingValuesIsNoOp() {
    // Corner of the stamped-row contract: the previous seed omitted the field (null stored) and
    // the field is absent from the row and from the incoming seed too (current == new == null) —
    // nothing differs anywhere, so the classifier no-ops. Note the contrast with a NON-null
    // matching (current, new) pair under null stored: that parks (see
    // stampedRowWithNullStoredValuesParksEverythingFailSafe) because null stored matches neither.
    FieldObservation o = observation("nickname", null, null, null);

    assertEquals(FieldDisposition.NO_OP, SeedCollisionDetector.disposition(true, o));
    assertTrue(SeedCollisionDetector.classify(true, List.of(o)).isEmpty());
  }

  @Test
  void removalDetectedWhenSeedDropsEntity() {
    // Removal detection is the caller's two-step computation: first confirm from the provenance
    // manifest (or the row's own stamp) that the entity key was previously seed-owned, then check
    // the incoming seed. Only the second half lives here — an entity absent from the new seed is
    // a removal candidate; one still declared is not.
    assertFalse(SeedCollisionDetector.isRemoval(true));
    assertTrue(SeedCollisionDetector.isRemoval(false));
  }

  private static FieldObservation observation(
      String fieldName, String storedSeedValue, String currentValue, String newSeedValue) {
    return new FieldObservation(fieldName, storedSeedValue, currentValue, newSeedValue);
  }
}
