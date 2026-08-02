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
package com.github.javydreamercsw.management.service.challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeUpdateService {

  private static final String DEFAULT_MANIFEST_URL =
      "https://javydreamercsw.github.io/all-time-wrestling-rpg/challenges/manifest.json";

  @Value("${atw.challenges.manifest-url:" + DEFAULT_MANIFEST_URL + "}")
  private String manifestUrl;

  private final ChallengeService challengeService;
  private final ObjectMapper objectMapper;

  @Getter private Instant lastChecked;

  public record UpdateResult(int downloaded, int skipped, String message) {}

  @Scheduled(
      initialDelayString = "${atw.challenges.check-initial-delay:30000}",
      fixedRateString = "${atw.challenges.check-interval:3600000}")
  public void scheduledCheck() {
    log.info("Scheduled challenge update check starting");
    UpdateResult result = checkAndApply();
    log.info("Scheduled challenge update check complete: {}", result.message());
  }

  public UpdateResult checkAndApply() {
    Path contentDir = challengeService.getContentDir();
    try {
      Files.createDirectories(contentDir);
      Files.createDirectories(contentDir.resolve("images"));
    } catch (IOException e) {
      log.error("Cannot create content directory", e);
      return new UpdateResult(0, 0, "Error: cannot create content directory.");
    }

    ChallengeContentManifest manifest = fetchManifest();
    if (manifest == null) {
      return new UpdateResult(0, 0, "Could not reach the content server. Check your network.");
    }

    int downloaded = 0;
    int skipped = 0;

    for (ChallengeContentManifest.ChallengePackage pkg : manifest.getPackages()) {
      byte[] json = fetchBytes(pkg.getJsonUrl());
      if (json == null) {
        log.warn("Skipping package {} — could not download JSON", pkg.getId());
        skipped++;
        continue;
      }
      try {
        Files.write(contentDir.resolve(pkg.getId() + ".json"), json);
        downloaded++;
      } catch (IOException e) {
        log.error("Error saving package {}", pkg.getId(), e);
        skipped++;
        continue;
      }

      for (ChallengeContentManifest.ImageEntry img : pkg.getImages()) {
        Path imgDest = contentDir.resolve("images").resolve(img.getName());
        if (Files.exists(imgDest)) {
          continue;
        }
        byte[] bytes = fetchBytes(img.getUrl());
        if (bytes != null) {
          try {
            Files.write(imgDest, bytes);
          } catch (IOException e) {
            log.warn("Could not save image {}", img.getName(), e);
          }
        }
      }
    }

    lastChecked = Instant.now();
    if (downloaded > 0) {
      challengeService.reload();
    }

    String msg = downloaded > 0 ? downloaded + " package(s) updated." : "Already up to date.";
    return new UpdateResult(downloaded, skipped, msg);
  }

  private ChallengeContentManifest fetchManifest() {
    try {
      byte[] bytes = fetchBytes(manifestUrl);
      if (bytes == null) {
        return null;
      }
      return objectMapper.readValue(bytes, ChallengeContentManifest.class);
    } catch (Exception e) {
      log.error("Error fetching/parsing manifest from {}", manifestUrl, e);
      return null;
    }
  }

  private byte[] fetchBytes(final String url) {
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("User-Agent", "all-time-wrestling-rpg")
              .timeout(Duration.ofSeconds(30))
              .GET()
              .build();
      HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (resp.statusCode() != 200) {
        log.warn("HTTP {} fetching {}", resp.statusCode(), url);
        return null;
      }
      return resp.body();
    } catch (Exception e) {
      log.error("Error fetching {}", url, e);
      return null;
    }
  }
}
