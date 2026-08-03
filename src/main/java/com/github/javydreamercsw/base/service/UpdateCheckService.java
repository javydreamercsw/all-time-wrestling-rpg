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
package com.github.javydreamercsw.base.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.event.UpdateAvailableEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "atw.update-check.enabled", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class UpdateCheckService {

  private static final String RELEASES_API =
      "https://api.github.com/repos/javydreamercsw/all-time-wrestling-rpg/releases/latest";

  static String releasesApiUrl() {
    String override = System.getProperty("atw.update-check.releases-api");
    return override != null ? override : RELEASES_API;
  }

  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final Optional<BuildProperties> buildProperties;

  @EventListener(ApplicationReadyEvent.class)
  @Async
  public void checkForUpdates() {
    try {
      String currentVersion = buildProperties.map(BuildProperties::getVersion).orElse(null);
      if (currentVersion == null || currentVersion.endsWith("-SNAPSHOT")) {
        log.debug("Skipping update check for SNAPSHOT build.");
        return;
      }

      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(releasesApiUrl()))
              .header("Accept", "application/vnd.github+json")
              .header("User-Agent", "all-time-wrestling-rpg/" + currentVersion)
              .timeout(Duration.ofSeconds(15))
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.warn("Update check returned HTTP {}", response.statusCode());
        return;
      }

      JsonNode json = objectMapper.readTree(response.body());
      String tagName = json.path("tag_name").asText("");
      String htmlUrl = json.path("html_url").asText("");

      if (tagName.isBlank()) {
        return;
      }

      String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

      if (isNewer(latestVersion, currentVersion)) {
        log.info("Update available: {} (current: {})", latestVersion, currentVersion);
        eventPublisher.publishEvent(new UpdateAvailableEvent(this, latestVersion, htmlUrl));
      } else {
        log.debug("Application is up to date ({})", currentVersion);
      }
    } catch (Exception e) {
      log.warn("Update check failed: {}", e.getMessage());
    }
  }

  /** Returns true if {@code candidate} is strictly newer than {@code current} using semver. */
  static boolean isNewer(final String candidate, final String current) {
    try {
      int[] c = parseSemver(candidate);
      int[] r = parseSemver(current);
      for (int i = 0; i < 3; i++) {
        if (c[i] != r[i]) {
          return c[i] > r[i];
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private static int[] parseSemver(final String version) {
    // Strip any pre-release suffix (e.g. "2.5.0-RC1" → [2, 5, 0])
    String numeric = version.replaceAll("[^0-9.].*$", "").replaceAll("^\\.+|\\.+$", "");
    if (numeric.isBlank()) {
      throw new IllegalArgumentException("Unparseable version: " + version);
    }
    String[] parts = numeric.split("\\.");
    int[] result = new int[3];
    for (int i = 0; i < 3 && i < parts.length; i++) {
      result[i] = parts[i].isBlank() ? 0 : Integer.parseInt(parts[i]);
    }
    return result;
  }
}
