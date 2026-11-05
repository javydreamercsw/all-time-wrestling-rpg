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

import java.util.Map;

/**
 * Decides whether verify-mode seed sync can take the fast path for a single domain: skip
 * reconciliation entirely because the database is provably in its seed state.
 *
 * <p>The decision is a <em>three-way</em> match between the committed {@link
 * SeedFingerprintManifest}, the fingerprint stored on each DB row, and the fingerprint recomputed
 * from the row's current fields:
 *
 * <ul>
 *   <li><strong>manifest ↔ stored</strong> — the row's stored fingerprint still equals the value
 *       the manifest recorded when the seed was generated (no one edited the stored value);
 *   <li><strong>stored ↔ recomputed</strong> — the row's current fields still hash to the stored
 *       fingerprint (no one edited the row's data either).
 * </ul>
 *
 * <p>Any mismatch — or a missing manifest domain, an empty domain, or a manifest entry with no DB
 * row — means the fast path cannot prove safety and the caller must fall back to the legacy (slow
 * but correct) reconcile path.
 */
public final class SeedFastPathChecker {

  private SeedFastPathChecker() {}

  /**
   * Returns whether the seed fast path applies to the given domain: every manifest entry has a DB
   * row whose stored AND recomputed fingerprint both equal the manifest value.
   *
   * <p>The caller computes {@code dbKeysToRecomputed} by re-hashing the current seed-owned fields
   * of each DB row (in the entity's canonical field order, via {@link
   * SeedSyncSupport#fingerprint(Object...)}); it is not the persisted value. Both DB maps must be
   * keyed identically (seed key → fingerprint) and have equal key sets; the caller is responsible
   * for that invariant. Extra DB rows not covered by the manifest are ignored — non-seed data never
   * blocks the fast path.
   *
   * @param manifest the committed seed-fingerprint manifest
   * @param domainKey the sync domain to check (e.g. {@code "wrestler"})
   * @param dbKeysToStored seed key → fingerprint stored on the DB row
   * @param dbKeysToRecomputed seed key → fingerprint re-hashed from the row's current fields
   * @return {@code true} iff the manifest has the domain non-empty and every manifest entry matches
   *     a DB row on both stored and recomputed fingerprints
   */
  public static boolean fastPathApplies(
      SeedFingerprintManifest manifest,
      String domainKey,
      Map<String, String> dbKeysToStored,
      Map<String, String> dbKeysToRecomputed) {
    if (manifest == null || domainKey == null) {
      return false;
    }
    return manifest
        .domain(domainKey)
        .filter(entries -> !entries.isEmpty())
        .map(
            entries ->
                entries.entrySet().stream()
                    .allMatch(
                        entry -> {
                          String stored = dbKeysToStored.get(entry.getKey());
                          String recomputed = dbKeysToRecomputed.get(entry.getKey());
                          return stored != null
                              && stored.equals(entry.getValue())
                              && recomputed != null
                              && recomputed.equals(entry.getValue());
                        }))
        .orElse(false);
  }
}
