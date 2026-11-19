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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Immutable view over the committed seed-provenance manifest ({@code seed-provenance.json}): a
 * per-domain map of seed key → expected fingerprint, used by the seed-update review feature to
 * detect seed-data collisions on version-upgrade boots.
 *
 * <p>JSON shape:
 *
 * <pre>{@code
 * {
 *   "generatedAt": "<ISO-8601>",
 *   "domains": {
 *     "<domain>": { "<key>": "<64-hex>" }
 *   }
 * }
 * }</pre>
 *
 * <p>Loading is lenient: a missing or malformed manifest never breaks startup — {@link #load} logs
 * a WARN and returns an empty manifest, and seed-review detection simply falls back to treating
 * every row as seed-owned (slow but correct).
 *
 * <p>Domain and entry maps preserve the manifest's document (insertion) order, so iteration over
 * {@link #domains()} is deterministic across loads.
 */
@Slf4j
public final class SeedProvenanceManifest {

  /** Classpath location of the committed manifest. */
  public static final String RESOURCE_PATH = "seed-provenance.json";

  private static final SeedProvenanceManifest EMPTY = new SeedProvenanceManifest(Map.of());

  private final Map<String, Map<String, String>> domains;

  private SeedProvenanceManifest(Map<String, Map<String, String>> domains) {
    // Defensive copy over a LinkedHashMap to preserve the manifest's document order
    // (Map.copyOf would discard insertion order).
    this.domains = Collections.unmodifiableMap(new LinkedHashMap<>(domains));
  }

  /**
   * @return an empty manifest (no domains, no entries)
   */
  public static SeedProvenanceManifest empty() {
    return EMPTY;
  }

  /**
   * @return the manifest domains: domain key → (seed key → fingerprint)
   */
  public Map<String, Map<String, String>> domains() {
    return domains;
  }

  /**
   * @param domainKey domain key (e.g. {@code "wrestler"})
   * @return the domain's seed key → fingerprint map, or empty when the domain is unknown/absent
   */
  public Optional<Map<String, String>> domain(String domainKey) {
    return Optional.ofNullable(domains.get(domainKey));
  }

  /**
   * Loads the committed manifest from {@link #RESOURCE_PATH} on the classpath.
   *
   * @param mapper the production {@link ObjectMapper} (see {@code JacksonConfig})
   * @return the parsed manifest, or an empty manifest when the resource is absent/unparseable —
   *     never throws
   */
  public static SeedProvenanceManifest load(ObjectMapper mapper) {
    ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
    if (!resource.exists()) {
      log.warn(
          "Seed provenance manifest not found on classpath ({}); seed-review detection will "
              + "run without it.",
          RESOURCE_PATH);
      return EMPTY;
    }
    return load(mapper, resource);
  }

  /**
   * Lenient parse of a manifest resource. Absent or malformed input yields an empty manifest and a
   * WARN log — never an exception.
   *
   * @param mapper the production {@link ObjectMapper} (see {@code JacksonConfig})
   * @param resource the manifest resource to parse
   * @return the parsed manifest, or an empty manifest on any failure — never throws
   */
  public static SeedProvenanceManifest load(ObjectMapper mapper, Resource resource) {
    if (resource == null) {
      log.warn("Seed provenance manifest resource is null; returning an empty manifest.");
      return EMPTY;
    }
    // Resource.exists()/getDescription() go inside the try: a misbehaving Resource must not
    // break the never-throws contract.
    try (InputStream in = resource.getInputStream()) {
      if (!resource.exists()) {
        log.warn(
            "Seed provenance manifest resource missing ({}); returning an empty manifest.",
            resource.getDescription());
        return EMPTY;
      }
      return parse(mapper.readTree(in));
    } catch (IOException e) {
      log.warn(
          "Failed to read seed provenance manifest from {}; returning an empty manifest.",
          resource.getDescription(),
          e);
      return EMPTY;
    } catch (RuntimeException e) {
      log.warn(
          "Failed to read or parse seed provenance manifest from {}; returning an empty"
              + " manifest.",
          resource.getDescription(),
          e);
      return EMPTY;
    }
  }

  /**
   * Builds a manifest from an in-memory domain map.
   *
   * @param domains domain key → (seed key → fingerprint)
   * @return a manifest holding a defensive copy of the given domains
   */
  public static SeedProvenanceManifest of(Map<String, Map<String, String>> domains) {
    return new SeedProvenanceManifest(domains == null ? Map.of() : domains);
  }

  private static SeedProvenanceManifest parse(JsonNode root) {
    if (root == null || !root.isObject()) {
      log.warn("Seed provenance manifest root is not a JSON object; returning an empty manifest.");
      return EMPTY;
    }
    JsonNode domainsNode = root.path("domains");
    if (!domainsNode.isObject()) {
      log.warn("Seed provenance manifest has no 'domains' object; returning an empty manifest.");
      return EMPTY;
    }
    Map<String, Map<String, String>> parsed = new LinkedHashMap<>();
    domainsNode
        .properties()
        .forEach(
            domainEntry -> {
              String domainKey = domainEntry.getKey();
              JsonNode entriesNode = domainEntry.getValue();
              if (!entriesNode.isObject()) {
                log.warn(
                    "Seed provenance manifest domain '{}' is not an object; ignoring it.",
                    domainKey);
                return;
              }
              Map<String, String> entries = new LinkedHashMap<>();
              entriesNode
                  .properties()
                  .forEach(
                      keyEntry -> {
                        JsonNode valueNode = keyEntry.getValue();
                        if (valueNode.isTextual()) {
                          entries.put(keyEntry.getKey(), valueNode.asText());
                        } else {
                          log.warn(
                              "Seed provenance manifest entry '{}.{}' is not a string; ignoring"
                                  + " it.",
                              domainKey,
                              keyEntry.getKey());
                        }
                      });
              parsed.put(domainKey, Collections.unmodifiableMap(entries));
            });
    return new SeedProvenanceManifest(parsed);
  }
}
