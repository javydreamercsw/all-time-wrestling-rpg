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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.JacksonConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link GenerateSeedProvenanceManifest}. The classpath guard is exercised as a pure
 * function over resolved {@link Resource} arrays against a synthetic project layout in
 * {@code @TempDir} — no {@code target/} manipulation needed. The end-to-end generation runs through
 * the same in-process boot the Maven generation profile invokes via {@code main()} ({@code
 * run(Path)}), against a fresh random-uuid in-memory H2 database, so the FULL-mode seed pipeline
 * runs exactly as it does in production generation.
 */
class GenerateSeedProvenanceManifestTest {

  private static final ObjectMapper MAPPER = new JacksonConfig().objectMapper();

  @Test
  void writesManifestForEmptyDb() throws Exception {
    Path output = Files.createTempFile("seed-provenance-test-", ".json");
    Files.deleteIfExists(output); // the generator must create the file itself

    int exitCode = GenerateSeedProvenanceManifest.run(output);

    assertEquals(0, exitCode, "generation must succeed against an empty test database");
    assertTrue(Files.exists(output), "manifest file must be written");
    JsonNode root = MAPPER.readTree(Files.readString(output));

    assertNotNull(root.path("generatedAt").asText(null), "generatedAt must be present");
    Instant.parse(root.path("generatedAt").asText()); // parseable

    JsonNode domains = root.path("domains");
    assertEquals(
        SeedDomains.STAMPING_DOMAINS,
        domains.properties().stream().map(Map.Entry::getKey).collect(Collectors.toSet()),
        "all stamping manifest domains must be present");
    for (String domain : SeedDomains.STAMPING_DOMAINS) {
      JsonNode entries = domains.path(domain);
      assertTrue(entries.isObject(), "domain '%s' must be a JSON object".formatted(domain));
      assertTrue(
          entries.properties().iterator().hasNext(),
          "domain '%s' must be non-empty".formatted(domain));
      entries
          .properties()
          .forEach(
              entry -> {
                String value = entry.getValue().asText();
                assertTrue(
                    value.length() == 64 && HexFormat.of().parseHex(value).length == 32,
                    "fingerprint '%s.%s' must be 64 hex characters"
                        .formatted(domain, entry.getKey()));
              });
    }
  }

  @Test
  void failsWhenTestClasspathResourcesPresent() {
    // A resource whose URL carries the test-classpath marker — exactly what the guard must
    // reject. ByteArrayResource always exists(); the URL override simulates a fixture resolving
    // into target/test-classes without needing one on disk.
    Resource polluting =
        new ByteArrayResource(new byte[0]) {
          @Override
          public String getDescription() {
            return "URL [file:.../target/test-classes/wrestlers-extra.json]";
          }

          @Override
          public URL getURL() throws IOException {
            return URI.create("file:///tmp/test-classes/wrestlers-extra.json").toURL();
          }
        };
    Map<String, List<Resource>> domains =
        Map.of("wrestler", List.of(new ClassPathResource("npcs.json"), polluting));

    String violation = GenerateSeedProvenanceManifest.findSeedResourceViolation(domains);

    assertNotNull(violation, "the guard must reject test-classes URLs");
    assertTrue(violation.contains("test-classes"), "the violation must name test-classes");
    assertTrue(violation.contains("wrestler"), "the violation must name the polluted domain");

    // And the clean map stays clean.
    Map<String, List<Resource>> clean =
        Map.of("wrestler", List.of(new ClassPathResource("npcs.json")));
    assertNull(GenerateSeedProvenanceManifest.findSeedResourceViolation(clean));
  }

  /**
   * The Jeff Hardy failure mode (review fix round 1): a stale build-output leftover in {@code
   * target/classes} with no committed {@code src/main/resources} counterpart. {@code mvn
   * test-compile} does not purge {@code target/classes} resources, so a seed file from another
   * branch survives there and would otherwise be seeded into the manifest as if it were committed
   * seed data. The guard must fail generation.
   */
  @Test
  void failsWhenTargetClassesResourceHasNoCommittedCounterpart(@TempDir Path tempDir)
      throws Exception {
    Path projectDir = syntheticProject(tempDir, "project-stale");
    // Stale leftover: exists in the synthetic target/classes only.
    Path staleTarget = projectDir.resolve("target/classes/wrestlers/JEFF_HARDY.json");
    Files.createDirectories(staleTarget.getParent());
    Files.writeString(staleTarget, "{}");

    String violation =
        GenerateSeedProvenanceManifest.findSeedResourceViolation(
            Map.of("wrestler", List.of(new FileSystemResource(staleTarget))), projectDir);

    assertNotNull(violation, "the guard must reject stale target/classes resources");
    assertTrue(violation.contains("wrestler"), "the violation must name the polluted domain");
    assertTrue(violation.contains("JEFF_HARDY.json"), "the violation must name the stale file");
    assertTrue(
        violation.contains("no committed"), "the violation must name the missing counterpart");
  }

  /**
   * A target/classes resource that has drifted from its committed counterpart (same path, changed
   * content) is equally stale — seeding it would record fingerprints for data no committed source
   * produces.
   */
  @Test
  void failsWhenTargetClassesResourceDiffersFromCommittedCounterpart(@TempDir Path tempDir)
      throws Exception {
    Path projectDir = syntheticProject(tempDir, "project-drift");
    Path srcFile = projectDir.resolve("src/main/resources/npcs.json");
    Path targetFile = projectDir.resolve("target/classes/npcs.json");
    Files.createDirectories(srcFile.getParent());
    Files.createDirectories(targetFile.getParent());
    Files.writeString(srcFile, "[{\"name\":\"Committed\"}]");
    Files.writeString(targetFile, "[{\"name\":\"Drifted\"}]");

    String violation =
        GenerateSeedProvenanceManifest.findSeedResourceViolation(
            Map.of("npc", List.of(new FileSystemResource(targetFile))), projectDir);

    assertNotNull(violation, "the guard must reject diverged target/classes resources");
    assertTrue(violation.contains("npc"), "the violation must name the polluted domain");
    assertTrue(
        violation.contains("differs from its committed"),
        "the violation must name the content divergence");
  }

  /** A target/classes resource byte-identical to its committed counterpart is clean. */
  @Test
  void passesWhenTargetClassesResourceMatchesCommittedCounterpart(@TempDir Path tempDir)
      throws Exception {
    Path projectDir = syntheticProject(tempDir, "project-clean");
    Path srcFile = projectDir.resolve("src/main/resources/npcs.json");
    Path targetFile = projectDir.resolve("target/classes/npcs.json");
    Files.createDirectories(srcFile.getParent());
    Files.createDirectories(targetFile.getParent());
    Files.writeString(srcFile, "[{\"name\":\"Committed\"}]");
    Files.writeString(targetFile, "[{\"name\":\"Committed\"}]");

    String violation =
        GenerateSeedProvenanceManifest.findSeedResourceViolation(
            Map.of("npc", List.of(new FileSystemResource(targetFile))), projectDir);

    assertNull(violation, "a byte-identical build copy must pass the guard");
  }

  /**
   * Creates the skeleton of a synthetic project layout: {@code <name>/target/classes} and {@code
   * <name>/src/main/resources} directories (both empty; individual tests populate them).
   */
  private static Path syntheticProject(Path tempDir, String name) throws IOException {
    Path projectDir = tempDir.resolve(name);
    Files.createDirectories(projectDir.resolve("target/classes"));
    Files.createDirectories(projectDir.resolve("src/main/resources"));
    return projectDir;
  }
}
