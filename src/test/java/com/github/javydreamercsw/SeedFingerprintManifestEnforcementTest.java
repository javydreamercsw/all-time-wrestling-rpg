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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.JacksonConfig;
import com.github.javydreamercsw.management.sync.GenerateSeedFingerprintManifest;
import com.github.javydreamercsw.management.sync.SeedDomains;
import com.github.javydreamercsw.management.sync.SeedFingerprintManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

/**
 * CI gate mirroring {@link ReleasedMigrationChecksumTest}: fails {@code mvn test} when the
 * committed {@code seed-fingerprints.json} no longer describes what the seed pipeline actually
 * writes.
 *
 * <p>The manifest drives verify-mode seed syncs' fast path (see {@code SeedFastPathChecker}): a
 * stale manifest either permanently disables the fast path (slower syncs) or, worse, lets verify
 * mode "reconcile" against fingerprints that no longer reflect the seed data. So whenever a seed
 * resource changes, the manifest must be regenerated and re-committed in the same change — this
 * test enforces that, exactly as the migration checksum gate enforces frozen migration scripts.
 *
 * <p>{@link #manifest_matchesFreshSyncOutput} boots the real application context on an empty
 * in-memory H2 database (test profile) via {@link GenerateSeedFingerprintManifest#run(Path)} — the
 * same in-process entry point {@code scripts/generate-seed-fingerprints.sh} invokes — then
 * deep-compares every domain of the fresh output against the committed manifest. The comparison
 * ignores {@code generatedAt} (wall-clock by design; every domain's entries must match exactly).
 */
class SeedFingerprintManifestEnforcementTest {

  private static final ObjectMapper MAPPER = new JacksonConfig().objectMapper();

  private static final String FINGERPRINT_COMMITTED = "a1b2c3d4e5f60718".repeat(4);
  private static final String FINGERPRINT_FRESH = "0123456789abcdef".repeat(4);

  @Test
  void manifest_matchesFreshSyncOutput(@TempDir Path tempDir) throws Exception {
    Path freshFile = tempDir.resolve("seed-fingerprints-fresh.json");
    int exitCode = GenerateSeedFingerprintManifest.run(freshFile);
    assertEquals(
        0,
        exitCode,
        "fresh seed fingerprint generation must succeed against an empty test database");
    assertTrue(Files.exists(freshFile), "fresh manifest file must be written");

    SeedFingerprintManifest committed = loadCommittedManifest();
    SeedFingerprintManifest fresh =
        SeedFingerprintManifest.load(MAPPER, new FileSystemResource(freshFile));

    List<String> violations = findDomainMismatches(fresh.domains(), committed.domains());
    if (!violations.isEmpty()) {
      fail(failureMessage(violations));
    }
  }

  @Test
  void manifest_coversEveryStampingDomain() {
    SeedFingerprintManifest committed = loadCommittedManifest();

    List<String> missingOrEmpty =
        SeedDomains.STAMPING_DOMAINS.stream()
            .filter(domain -> committed.domain(domain).map(Map::isEmpty).orElse(true))
            .toList();

    if (!missingOrEmpty.isEmpty()) {
      fail(
          "Committed seed fingerprint manifest does not cover every stamping domain ("
              + missingOrEmpty.size()
              + " missing or empty):\n\n"
              + String.join("\n", missingOrEmpty)
              + "\n\nTo fix: run scripts/generate-seed-fingerprints.sh and commit the regenerated"
              + " src/main/resources/seed-fingerprints.json.");
    }
  }

  /**
   * Pins the failure mode of {@link #manifest_matchesFreshSyncOutput} without paying for a context
   * boot: a corrupted manifest value must produce a violation naming the mismatched domain, the
   * seed key, and both fingerprint values, and the failure message must point at the regeneration
   * script.
   */
  @Test
  void mismatchReportNamesDomainKeyAndRegenScript() {
    Map<String, Map<String, String>> fresh =
        Map.of("segment_type", Map.of("Promo", FINGERPRINT_FRESH));
    Map<String, Map<String, String>> committed =
        Map.of("segment_type", Map.of("Promo", FINGERPRINT_COMMITTED));

    List<String> violations = findDomainMismatches(fresh, committed);

    assertEquals(1, violations.size());
    String violation = violations.getFirst();
    assertTrue(violation.contains("segment_type"), "must name the mismatched domain");
    assertTrue(violation.contains("Promo"), "must name the mismatched seed key");
    assertTrue(violation.contains(FINGERPRINT_COMMITTED), "must show the committed value");
    assertTrue(violation.contains(FINGERPRINT_FRESH), "must show the fresh value");

    String message = failureMessage(violations);
    assertTrue(message.contains("scripts/generate-seed-fingerprints.sh"), "must name the script");
    assertTrue(
        message.contains("src/main/resources/seed-fingerprints.json"), "must name the artifact");
  }

  /**
   * Loads the committed manifest from the classpath — the exact resource production verify-mode
   * syncs reconcile against.
   */
  private static SeedFingerprintManifest loadCommittedManifest() {
    ClassPathResource resource = new ClassPathResource(SeedFingerprintManifest.RESOURCE_PATH);
    if (!resource.exists()) {
      fail(
          "Required file not found on the classpath: "
              + SeedFingerprintManifest.RESOURCE_PATH
              + "\nRun scripts/generate-seed-fingerprints.sh to create it."
              + " This file must be committed alongside the sync classes.");
    }
    SeedFingerprintManifest manifest = SeedFingerprintManifest.load(MAPPER, resource);
    if (manifest.domains().isEmpty()) {
      fail(
          "Committed seed fingerprint manifest ("
              + SeedFingerprintManifest.RESOURCE_PATH
              + ") is empty or unparseable.\n"
              + "Run scripts/generate-seed-fingerprints.sh to regenerate it.");
    }
    return manifest;
  }

  /**
   * Pure per-domain deep comparison of the fresh sync output against the committed manifest.
   *
   * @param fresh domain key → (seed key → fingerprint) from a fresh full sync
   * @param committed domain key → (seed key → fingerprint) from the committed manifest
   * @return one human-readable violation per difference, deterministic order; empty when the two
   *     domain maps are deep-equal
   */
  static List<String> findDomainMismatches(
      Map<String, Map<String, String>> fresh, Map<String, Map<String, String>> committed) {
    List<String> violations = new ArrayList<>();
    Set<String> domains = new TreeSet<>();
    domains.addAll(fresh.keySet());
    domains.addAll(committed.keySet());
    for (String domain : domains) {
      if (!fresh.containsKey(domain)) {
        violations.add(
            domain
                + ": the committed manifest lists this domain but the fresh sync output produced"
                + " no entries for it (sync removed or no longer stamping).");
        continue;
      }
      if (!committed.containsKey(domain)) {
        violations.add(
            domain
                + ": the fresh sync output stamps this domain but the committed manifest has no"
                + " entry for it.");
        continue;
      }
      Map<String, String> committedEntries = committed.get(domain);
      Map<String, String> freshEntries = fresh.get(domain);
      Set<String> keys = new TreeSet<>();
      keys.addAll(committedEntries.keySet());
      keys.addAll(freshEntries.keySet());
      for (String key : keys) {
        String committedValue = committedEntries.get(key);
        String freshValue = freshEntries.get(key);
        if (freshValue == null) {
          violations.add(
              domain
                  + ": committed manifest entry '"
                  + key
                  + "' has no counterpart in the fresh sync output (seed removed or key"
                  + " changed).");
        } else if (committedValue == null) {
          violations.add(
              domain
                  + ": fresh sync output entry '"
                  + key
                  + "' is missing from the committed manifest.");
        } else if (!committedValue.equals(freshValue)) {
          violations.add(
              domain
                  + ": fingerprint mismatch for '"
                  + key
                  + "'."
                  + "\n    committed: "
                  + committedValue
                  + "\n    fresh:     "
                  + freshValue);
        }
      }
    }
    return violations;
  }

  /** Failure message in the style of {@link ReleasedMigrationChecksumTest}. */
  private static String failureMessage(List<String> violations) {
    return "Committed seed fingerprint manifest is stale ("
        + violations.size()
        + " mismatch(es)):\n\n"
        + String.join("\n\n", violations)
        + "\n\nTo fix: run scripts/generate-seed-fingerprints.sh and commit the regenerated"
        + " src/main/resources/seed-fingerprints.json.";
  }
}
