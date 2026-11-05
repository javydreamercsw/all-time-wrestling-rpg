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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Immutable view over the committed seed-fingerprint manifest ({@code seed-fingerprints.json}): a
 * per-domain map of seed key → expected fingerprint.
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
 * a WARN and returns an empty manifest, and verify-mode syncs simply fall back to the legacy
 * reconcile path (slow but correct).
 */
@Slf4j
public final class SeedFingerprintManifest {

  /** Classpath location of the committed manifest. */
  public static final String RESOURCE_PATH = "seed-fingerprints.json";

  private static final SeedFingerprintManifest EMPTY = new SeedFingerprintManifest(Map.of());

  private final Map<String, Map<String, String>> domains;

  private SeedFingerprintManifest(Map<String, Map<String, String>> domains) {
    // Defensive copy with stable iteration order.
    this.domains = Map.copyOf(new LinkedHashMap<>(domains));
  }

  /**
   * @return the manifest domains: domain key → (seed key → fingerprint).
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
  public static SeedFingerprintManifest load(ObjectMapper mapper) {
    ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
    if (!resource.exists()) {
      log.warn(
          "Seed fingerprint manifest not found on classpath ({}); verify mode will reconcile "
              + "without the fast path.",
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
  public static SeedFingerprintManifest load(ObjectMapper mapper, Resource resource) {
    if (resource == null || !resource.exists()) {
      log.warn(
          "Seed fingerprint manifest resource missing ({}); returning an empty manifest.",
          resource == null ? "null" : resource.getDescription());
      return EMPTY;
    }
    try (InputStream in = resource.getInputStream()) {
      return parse(mapper.readTree(in));
    } catch (IOException e) {
      log.warn(
          "Failed to read seed fingerprint manifest from {}; returning an empty manifest.",
          resource.getDescription(),
          e);
      return EMPTY;
    } catch (RuntimeException e) {
      log.warn(
          "Failed to parse seed fingerprint manifest from {}; returning an empty manifest.",
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
  public static SeedFingerprintManifest of(Map<String, Map<String, String>> domains) {
    return new SeedFingerprintManifest(domains == null ? Map.of() : domains);
  }

  private static SeedFingerprintManifest parse(JsonNode root) {
    if (root == null || !root.isObject()) {
      return EMPTY;
    }
    JsonNode domainsNode = root.path("domains");
    if (!domainsNode.isObject()) {
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
                    "Seed fingerprint manifest domain '{}' is not an object; ignoring it.",
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
                              "Seed fingerprint manifest entry '{}.{}' is not a string; ignoring"
                                  + " it.",
                              domainKey,
                              keyEntry.getKey());
                        }
                      });
              parsed.put(domainKey, Map.copyOf(entries));
            });
    return new SeedFingerprintManifest(parsed);
  }
}
