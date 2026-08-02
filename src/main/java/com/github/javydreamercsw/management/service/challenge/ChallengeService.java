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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeService {

  private final ObjectMapper objectMapper;
  private final ResourcePatternResolver resourcePatternResolver;

  private final Path contentDir = resolveContentDir();
  private List<ChallengeDTO> challenges = Collections.emptyList();

  @PostConstruct
  public void init() {
    loadChallenges();
  }

  public synchronized void reload() {
    loadChallenges();
  }

  public Path getContentDir() {
    return contentDir;
  }

  public void loadChallenges() {
    Map<String, ChallengeDTO> byId = new LinkedHashMap<>();

    // Classpath first (bundled challenges)
    try {
      Resource[] files = resourcePatternResolver.getResources("classpath*:challenges/**/*.json");
      for (Resource resource : files) {
        log.debug("Loading challenges from classpath: {}", resource.getFilename());
        try (InputStream is = resource.getInputStream()) {
          List<ChallengeDTO> batch =
              objectMapper.readValue(is, new TypeReference<List<ChallengeDTO>>() {});
          batch.forEach(c -> byId.put(c.getId(), c));
        } catch (IOException e) {
          log.error("Error loading challenges from {}", resource.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning classpath for challenge files", e);
    }

    // Filesystem second — downloaded content overrides classpath on ID conflict
    if (Files.isDirectory(contentDir)) {
      try {
        Files.walk(contentDir)
            .filter(p -> p.toString().endsWith(".json"))
            .filter(p -> !p.startsWith(contentDir.resolve("images")))
            .forEach(
                p -> {
                  log.debug("Loading challenges from filesystem: {}", p);
                  try (InputStream is = Files.newInputStream(p)) {
                    List<ChallengeDTO> batch =
                        objectMapper.readValue(is, new TypeReference<List<ChallengeDTO>>() {});
                    batch.forEach(c -> byId.put(c.getId(), c));
                  } catch (IOException e) {
                    log.error("Error loading challenges from {}", p, e);
                  }
                });
      } catch (IOException e) {
        log.error("Error scanning content dir for challenge files", e);
      }
    }

    challenges = Collections.unmodifiableList(new ArrayList<>(byId.values()));
    log.debug("Loaded {} challenge(s).", challenges.size());
  }

  static Path resolveContentDir() {
    String os = System.getProperty("os.name", "").toLowerCase();
    String home = System.getProperty("user.home");
    Path base;
    if (os.contains("mac")) {
      base = Path.of(home, "Library", "Application Support", "ATW");
    } else if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      base = Path.of(appData != null ? appData : home, "ATW");
    } else {
      String xdg = System.getenv("XDG_DATA_HOME");
      base = Path.of(xdg != null ? xdg : home + "/.local/share", "atw");
    }
    return base.resolve("challenges");
  }

  public List<ChallengeDTO> getAllChallenges() {
    return challenges;
  }

  /** Returns only challenges where {@code active = true}. */
  public List<ChallengeDTO> getActiveChallenges() {
    return challenges.stream().filter(ChallengeDTO::isActive).toList();
  }

  /** Returns only official challenges (expansionCode != "CUSTOM"). */
  public List<ChallengeDTO> getOfficialChallenges() {
    return challenges.stream()
        .filter(ChallengeDTO::isActive)
        .filter(c -> !"CUSTOM".equals(c.getExpansionCode()))
        .toList();
  }

  /** Returns only custom / community challenges (expansionCode == "CUSTOM"). */
  public List<ChallengeDTO> getCustomChallenges() {
    return challenges.stream()
        .filter(ChallengeDTO::isActive)
        .filter(c -> "CUSTOM".equals(c.getExpansionCode()))
        .toList();
  }

  public Optional<ChallengeDTO> getChallenge(@NonNull final String id) {
    return challenges.stream().filter(c -> c.getId().equals(id)).findFirst();
  }
}
