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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.JacksonConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for {@link SeedProvenanceManifest}. All parsing goes through the production {@link
 * JacksonConfig} mapper — the loader must behave identically under the real ObjectMapper
 * configuration, never a bare {@code new ObjectMapper()}.
 */
class SeedProvenanceManifestTest {

  private static final String FINGERPRINT_A = "a1b2c3d4e5f60718".repeat(4);
  private static final String FINGERPRINT_B = "0123456789abcdef".repeat(4);

  private final ObjectMapper mapper = new JacksonConfig().objectMapper();

  @Test
  void loadsCommittedManifest() {
    // The committed manifest is the real classpath:seed-provenance.json the generator writes and
    // SeedProvenanceManifestEnforcementTest keeps fresh; shape assertions stay coarse here (the
    // enforcement test pins exact values) so this unit test never churns on seed changes.
    SeedProvenanceManifest manifest = SeedProvenanceManifest.load(mapper);
    assertEquals(
        SeedDomains.STAMPING_DOMAINS,
        manifest.domains().keySet(),
        "committed manifest must cover exactly the stamping domains");
    for (Map.Entry<String, Map<String, String>> domain : manifest.domains().entrySet()) {
      assertFalse(
          domain.getValue().isEmpty(), "domain '" + domain.getKey() + "' must not be empty");
      for (String fingerprint : domain.getValue().values()) {
        assertTrue(
            fingerprint.matches("[0-9a-f]{64}"),
            "fingerprint for '" + domain.getKey() + "' must be 64-char lowercase hex");
      }
    }
  }

  @Test
  void absentResourceLogsWarnAndReturnsEmpty() {
    // A classpath path that does not exist: absence must degrade to an empty manifest with a
    // WARN — never an exception.
    Logger logger = (Logger) LoggerFactory.getLogger(SeedProvenanceManifest.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      SeedProvenanceManifest manifest =
          SeedProvenanceManifest.load(
              mapper, new ClassPathResource("seed-manifest-fixtures/absent-provenance.json"));
      assertTrue(manifest.domains().isEmpty());
      assertTrue(manifest.domain("wrestler").isEmpty());
      assertTrue(
          appender.list.stream().anyMatch(event -> event.getLevel().toString().equals("WARN")),
          "absent resource must log a WARN");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void corruptJsonLogsWarnAndReturnsEmpty() {
    Logger logger = (Logger) LoggerFactory.getLogger(SeedProvenanceManifest.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      // Syntactically broken JSON.
      assertTrue(load("{ not json at all ").domains().isEmpty());
      // Well-formed JSON, wrong shape: a domain value is not an object.
      assertTrue(load("{\"domains\": {\"wrestler\": \"not-an-object\"}}").domains().isEmpty());
      assertTrue(
          appender.list.stream().anyMatch(event -> event.getLevel().toString().equals("WARN")),
          "corrupt JSON must log a WARN");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void domainsPreserveDocumentOrder() {
    // Insertion order, deliberately non-alphabetical: later tasks iterate domains() to produce
    // deterministic diffs, so document order must survive parsing end-to-end (LinkedHashMap,
    // never Map.copyOf, which discards insertion order).
    SeedProvenanceManifest manifest = load(documentOrderJson());
    assertEquals(
        List.of("wrestler", "card_set", "achievement", "npc"),
        List.copyOf(manifest.domains().keySet()));
    // Entry order inside a domain must preserve document order too.
    assertEquals(
        List.of("Jeff Hardy", "Bianca Belair"),
        List.copyOf(manifest.domain("wrestler").orElseThrow().keySet()));
  }

  @Test
  void wrongShapeRootLogsWarningAndReturnsEmpty() {
    Logger logger = (Logger) LoggerFactory.getLogger(SeedProvenanceManifest.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      SeedProvenanceManifest manifest = load("{\"generatedAt\": \"2026-11-06T12:00:00Z\"}");
      assertTrue(manifest.domains().isEmpty());
      assertTrue(
          appender.list.stream()
              .anyMatch(
                  event ->
                      event.getLevel().toString().equals("WARN")
                          && event.getFormattedMessage().contains("domains")),
          "wrong-shape root must log a WARN mentioning the missing domains object");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void domainReturnsEmptyOptionalForUnknownDomain() {
    SeedProvenanceManifest manifest =
        SeedProvenanceManifest.of(Map.of("wrestler", Map.of("Jeff Hardy", FINGERPRINT_A)));
    assertTrue(manifest.domain("segment_type").isEmpty());
    assertFalse(manifest.domain("wrestler").isEmpty());
  }

  @Test
  void emptyFactoryReturnsEmptyManifest() {
    SeedProvenanceManifest manifest = SeedProvenanceManifest.empty();
    assertNotNull(manifest);
    assertTrue(manifest.domains().isEmpty());
    assertTrue(manifest.domain("wrestler").isEmpty());
  }

  @Test
  void publicLoadReadsClasspathResourceLeniently() {
    // Production entry point: reads RESOURCE_PATH (seed-provenance.json) from the classpath. The
    // committed manifest exists (see SeedProvenanceManifestEnforcementTest), so this parses it and
    // must stay non-null, never throwing.
    SeedProvenanceManifest manifest = assertDoesNotThrow(() -> SeedProvenanceManifest.load(mapper));
    assertNotNull(manifest);
    assertNotNull(manifest.domains());
  }

  private String documentOrderJson() {
    return """
    {
      "generatedAt": "2026-11-06T12:00:00Z",
      "domains": {
        "wrestler": {
          "Jeff Hardy": "%s",
          "Bianca Belair": "%s"
        },
        "card_set": { "BASE_GAME": "%s" },
        "achievement": { "first_show": "%s" },
        "npc": { "Ringside Medic": "%s" }
      }
    }
    """
        .formatted(FINGERPRINT_A, FINGERPRINT_B, FINGERPRINT_B, FINGERPRINT_A, FINGERPRINT_A);
  }

  private SeedProvenanceManifest load(String json) {
    return SeedProvenanceManifest.load(
        mapper, new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
  }
}
