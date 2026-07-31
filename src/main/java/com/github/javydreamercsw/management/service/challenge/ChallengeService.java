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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  private List<ChallengeDTO> challenges = Collections.emptyList();

  @PostConstruct
  public void init() {
    loadChallenges();
  }

  public void loadChallenges() {
    List<ChallengeDTO> loaded = new ArrayList<>();
    try {
      Resource[] files = resourcePatternResolver.getResources("classpath*:challenges/**/*.json");
      for (Resource resource : files) {
        log.debug("Loading challenges from: {}", resource.getFilename());
        try (InputStream is = resource.getInputStream()) {
          loaded.addAll(objectMapper.readValue(is, new TypeReference<List<ChallengeDTO>>() {}));
        } catch (IOException e) {
          log.error("Error loading challenges from {}", resource.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning classpath for challenge files", e);
    }
    challenges = Collections.unmodifiableList(loaded);
    log.debug("Loaded {} challenge(s).", challenges.size());
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
