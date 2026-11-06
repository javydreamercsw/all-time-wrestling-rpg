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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SeedFastPathChecker}: the three-way (manifest ↔ stored ↔ recomputed)
 * decision that lets verify-mode syncs skip per-domain reconciliation.
 */
class SeedFastPathCheckerTest {

  private static final String FP_A = "a1b2c3d4e5f60718".repeat(4);
  private static final String FP_B = "0123456789abcdef".repeat(4);
  private static final String FP_C = "fedcba9876543210".repeat(4);

  @Test
  void appliesWhenAllThreeWayMatch() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(
            Map.of("wrestler", Map.of("Jeff Hardy", FP_A, "Bianca Belair", FP_B)));
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A, "Bianca Belair", FP_B);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A, "Bianca Belair", FP_B);
    assertTrue(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }

  @Test
  void fallsBackWhenStoredDiffers() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(Map.of("wrestler", Map.of("Jeff Hardy", FP_A)));
    // Stored fingerprint no longer matches the committed manifest value.
    Map<String, String> stored = Map.of("Jeff Hardy", FP_C);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A);
    assertFalse(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }

  @Test
  void fallsBackWhenRecomputedDiffers() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(Map.of("wrestler", Map.of("Jeff Hardy", FP_A)));
    // Stored still matches the manifest, but the live row fields no longer hash to it — the row
    // was edited outside the seeder.
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_C);
    assertFalse(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }

  @Test
  void fallsBackWhenRowMissing() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(
            Map.of("wrestler", Map.of("Jeff Hardy", FP_A, "Bianca Belair", FP_B)));
    // "Bianca Belair" exists in the manifest but has no DB row.
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A);
    assertFalse(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }

  @Test
  void fallsBackWhenManifestDomainMissing() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(Map.of("wrestler", Map.of("Jeff Hardy", FP_A)));
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A);
    assertFalse(SeedFastPathChecker.fastPathApplies(manifest, "segment_type", stored, recomputed));
  }

  @Test
  void ignoresExtraDbRows() {
    SeedFingerprintManifest manifest =
        SeedFingerprintManifest.of(Map.of("wrestler", Map.of("Jeff Hardy", FP_A)));
    // The DB holds rows the manifest doesn't cover (e.g. non-seed or player-created data);
    // extras must not block the fast path.
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A, "Local Guy", FP_C);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A, "Local Guy", FP_C);
    assertTrue(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }

  @Test
  void fallsBackWhenManifestEmpty() {
    // The domain exists in the manifest but carries no entries — nothing to verify against.
    SeedFingerprintManifest manifest = SeedFingerprintManifest.of(Map.of("wrestler", Map.of()));
    Map<String, String> stored = Map.of("Jeff Hardy", FP_A);
    Map<String, String> recomputed = Map.of("Jeff Hardy", FP_A);
    assertFalse(SeedFastPathChecker.fastPathApplies(manifest, "wrestler", stored, recomputed));
  }
}
