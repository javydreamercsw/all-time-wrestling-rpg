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
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link GenerateSeedFingerprintManifest}. The classpath guard is exercised as a pure
 * function over resolved {@link Resource} arrays — a polluting fixture is simulated by handing the
 * guard a resource whose URL carries the {@code test-classes} marker, no filesystem seeding needed.
 * The end-to-end generation runs through the same in-process boot the Maven generation profile
 * invokes via {@code main()} ({@code run(Path)}), against a fresh random-uuid in-memory H2
 * database, so the FULL-mode seed pipeline runs exactly as it does in production generation.
 */
class GenerateSeedFingerprintManifestTest {

  private static final ObjectMapper MAPPER = new JacksonConfig().objectMapper();

  @Test
  void writesManifestForEmptyDb() throws Exception {
    Path output = Files.createTempFile("seed-fingerprints-test-", ".json");
    Files.deleteIfExists(output); // the generator must create the file itself

    int exitCode = GenerateSeedFingerprintManifest.run(output);

    assertEquals(0, exitCode, "generation must succeed against an empty test database");
    assertTrue(Files.exists(output), "manifest file must be written");
    JsonNode root = MAPPER.readTree(Files.readString(output));

    assertNotNull(root.path("generatedAt").asText(null), "generatedAt must be present");
    Instant.parse(root.path("generatedAt").asText()); // parseable

    JsonNode domains = root.path("domains");
    assertEquals(11, domains.size(), "all 11 manifest domains must be present");
    List<String> expectedDomains =
        List.of(
            "achievement",
            "campaign_ability_card",
            "card",
            "card_set",
            "championship",
            "deck",
            "npc",
            "ringside_action",
            "segment_rule",
            "segment_type",
            "wrestler");
    for (String domain : expectedDomains) {
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

    String violation = GenerateSeedFingerprintManifest.findTestClasspathViolation(domains);

    assertNotNull(violation, "the guard must reject test-classes URLs");
    assertTrue(violation.contains("test-classes"), "the violation must name test-classes");
    assertTrue(violation.contains("wrestler"), "the violation must name the polluted domain");

    // And the clean map stays clean.
    Map<String, List<Resource>> clean =
        Map.of("wrestler", List.of(new ClassPathResource("npcs.json")));
    assertNull(GenerateSeedFingerprintManifest.findTestClasspathViolation(clean));
  }
}
