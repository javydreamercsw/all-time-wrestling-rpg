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
package com.github.javydreamercsw.management.service.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterCriteriaDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterPointDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CampaignChapterService {

  private final ObjectMapper objectMapper;
  private final FeatureDataService featureDataService;
  private final ExpansionService expansionService;
  private final ResourcePatternResolver resourcePatternResolver;
  private List<CampaignChapterDTO> chapters = Collections.emptyList();

  @Autowired
  public CampaignChapterService(
      @NonNull final ObjectMapper objectMapper,
      final FeatureDataService featureDataService,
      final ExpansionService expansionService,
      @NonNull final ResourcePatternResolver resourcePatternResolver) {
    this.objectMapper = objectMapper;
    this.featureDataService = featureDataService;
    this.expansionService = expansionService;
    this.resourcePatternResolver = resourcePatternResolver;
  }

  @PostConstruct
  public void init() {
    loadChapters();
  }

  public void loadChapters() {
    List<CampaignChapterDTO> merged = new ArrayList<>();

    // 1. Load folder-based campaigns first: campaigns/*/_chapter.json with encounters/ subfolder
    // (folder-based chapters like "beginning" must appear before flat-file chapters so that
    // startCampaign picks them first when multiple chapters are available at VP=0)
    try {
      Resource[] chapterFiles =
          resourcePatternResolver.getResources("classpath*:campaigns/*/_chapter.json");
      for (Resource chapterResource : chapterFiles) {
        log.debug("Loading folder-based campaign chapter: {}", chapterResource.getURL());
        try (InputStream is = chapterResource.getInputStream()) {
          CampaignChapterDTO chapter = objectMapper.readValue(is, CampaignChapterDTO.class);
          String encounterPattern =
              chapterResource.getURL().toString().replace("_chapter.json", "encounters/*.json");
          Resource[] encounterFiles = resourcePatternResolver.getResources(encounterPattern);
          Arrays.sort(encounterFiles, Comparator.comparing(Resource::getFilename));
          for (Resource enc : encounterFiles) {
            log.debug("  Loading encounters from: {}", enc.getFilename());
            try (InputStream eis = enc.getInputStream()) {
              List<StaticEncounterDTO> encounters =
                  objectMapper.readValue(eis, new TypeReference<>() {});
              chapter.getStaticEncounters().addAll(encounters);
            } catch (IOException e) {
              log.error("Error loading encounters from {}", enc.getFilename(), e);
            }
          }
          merged.add(chapter);
        } catch (IOException e) {
          log.error("Error loading folder-based chapter from {}", chapterResource.getURL(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning campaigns/ for folder-based chapters", e);
    }

    // 2. Load flat array files: campaigns/*.json
    try {
      Resource[] flatFiles = resourcePatternResolver.getResources("classpath*:campaigns/*.json");
      for (Resource r : flatFiles) {
        log.debug("Loading campaign chapters from file: {}", r.getFilename());
        try (InputStream is = r.getInputStream()) {
          merged.addAll(objectMapper.readValue(is, new TypeReference<>() {}));
        } catch (IOException e) {
          log.error("Error loading campaign chapters from {}", r.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error scanning campaigns/ for flat chapter files", e);
    }

    if (merged.isEmpty()) {
      log.warn("No campaign chapters loaded from campaigns/");
    }
    chapters = Collections.unmodifiableList(merged);
    log.debug("Loaded {} campaign chapters total.", chapters.size());
  }

  public List<CampaignChapterDTO> getAllChapters() {
    return Collections.unmodifiableList(chapters);
  }

  public Optional<CampaignChapterDTO> getChapter(@NonNull final String id) {
    return chapters.stream().filter(c -> c.getId().equals(id)).findFirst();
  }

  /**
   * Finds all chapters that the player is currently eligible to enter.
   *
   * @param state The current campaign state.
   * @return List of eligible chapters.
   */
  public List<CampaignChapterDTO> findAvailableChapters(@NonNull final CampaignState state) {
    return findAvailableChapters(state, null);
  }

  /**
   * Finds all chapters the player is currently eligible to enter, additionally filtering out
   * chapters whose {@code allowedWrestlerNames} list does not include {@code wrestlerName}.
   *
   * @param state The current campaign state.
   * @param wrestlerName The active wrestler's name, or null to skip the wrestler restriction check.
   */
  public List<CampaignChapterDTO> findAvailableChapters(
      @NonNull final CampaignState state, final String wrestlerName) {
    return chapters.stream()
        .filter(c -> !state.getCompletedChapterIds().contains(c.getId())) // Not already completed
        // Exclude "beginning" chapter if any other chapter has been completed.
        // This prevents re-entering the tutorial chapter after progression.
        .filter(
            c ->
                !"beginning".equals(c.getId())
                    || !state.getCompletedChapterIds().stream()
                        .anyMatch(id -> !"beginning".equals(id)))
        .filter(c -> isAnyPointActive(c.getEntryPoints(), state, true))
        .filter(c -> allExpansionsEnabled(c.getRequiredExpansions()))
        .filter(
            c ->
                c.getAllowedWrestlerNames().isEmpty()
                    || wrestlerName == null
                    || c.getAllowedWrestlerNames().contains(wrestlerName))
        .toList();
  }

  /**
   * Checks if the current chapter is ready to be exited.
   *
   * @param state The current campaign state.
   * @return true if any exit point is active.
   */
  public boolean isChapterComplete(@NonNull final CampaignState state) {
    if (state.getCurrentChapterId() == null) {
      return false;
    }

    return getChapter(state.getCurrentChapterId())
        .map(c -> isAnyPointActive(c.getExitPoints(), state, false))
        .orElse(false);
  }

  public Optional<ChapterPointDTO> getActivePoint(
      @NonNull final List<ChapterPointDTO> points, @NonNull final CampaignState state) {
    return points.stream().filter(p -> areAllCriteriaMet(p.getCriteria(), state)).findFirst();
  }

  public boolean allExpansionsEnabled(final List<String> required) {
    if (required == null || required.isEmpty()) {
      return true;
    }
    return required.stream().allMatch(expansionService::isExpansionEnabled);
  }

  boolean isAnyPointActive(
      @NonNull final List<ChapterPointDTO> points,
      @NonNull final CampaignState state,
      final boolean defaultWhenEmpty) {
    if (points.isEmpty()) {
      return defaultWhenEmpty;
    }
    return points.stream().anyMatch(p -> areAllCriteriaMet(p.getCriteria(), state));
  }

  private boolean areAllCriteriaMet(
      @NonNull final List<ChapterCriteriaDTO> criteriaList, @NonNull final CampaignState state) {
    if (criteriaList.isEmpty()) {
      return true;
    }

    return criteriaList.stream().allMatch(c -> isCriteriaMet(c, state));
  }

  boolean isCriteriaMet(
      @NonNull final ChapterCriteriaDTO criteria, @NonNull final CampaignState state) {
    // Check Victory Points
    if (criteria.getMinVictoryPoints() != null
        && state.getVictoryPoints() < criteria.getMinVictoryPoints()) {
      return false;
    }
    if (criteria.getMaxVictoryPoints() != null
        && state.getVictoryPoints() > criteria.getMaxVictoryPoints()) {
      return false;
    }

    // Check Matches Played
    if (criteria.getMinMatchesPlayed() != null
        && state.getMatchesPlayed() < criteria.getMinMatchesPlayed()) {
      return false;
    }

    // Check Wins
    if (criteria.getMinWins() != null && state.getWins() < criteria.getMinWins()) {
      return false;
    }

    // Check Tournament Status
    java.util.Map<String, Object> featureData = featureDataService.getFeatureData(state);

    if (criteria.getTournamentWinner() != null) {
      boolean isWinner = Boolean.TRUE.equals(featureData.get("tournamentWinner"));
      if (isWinner != criteria.getTournamentWinner()) {
        return false;
      }
    }

    if (criteria.getFailedToQualify() != null) {
      boolean failed = Boolean.TRUE.equals(featureData.get("failedToQualify"));
      if (failed != criteria.getFailedToQualify()) {
        return false;
      }
    }

    if (criteria.getWonFinale() != null) {
      boolean wonFinale = Boolean.TRUE.equals(featureData.get("wonFinale"));
      if (wonFinale != criteria.getWonFinale()) {
        return false;
      }
    }

    // Check Championship Status
    if (criteria.getIsChampion() != null) {
      boolean holdsTitle =
          state.getCampaign().getWrestler().getReigns().stream()
              .anyMatch(TitleReign::isCurrentReign);
      if (holdsTitle != criteria.getIsChampion()) {
        return false;
      }
    }

    // Check Faction Membership
    if (criteria.getHasFaction() != null) {
      Wrestler wrestler = state.getCampaign().getWrestler();
      Long universeId =
          state.getCampaign().getUniverse() != null
              ? state.getCampaign().getUniverse().getId()
              : 1L;
      boolean inFaction =
          wrestler
              .getState(universeId)
              .map(com.github.javydreamercsw.management.domain.wrestler.WrestlerState::getFaction)
              .isPresent();
      if (inFaction != criteria.getHasFaction()) {
        return false;
      }
    }

    // Check Alignment
    if (criteria.getRequiredAlignmentType() != null) {
      WrestlerAlignment alignment = state.getCampaign().getWrestler().getAlignment();
      if (alignment == null
          || !alignment.getAlignmentType().name().equals(criteria.getRequiredAlignmentType())) {
        return false;
      }
      if (criteria.getMinAlignmentLevel() != null
          && alignment.getLevel() < criteria.getMinAlignmentLevel()) {
        return false;
      }
    }

    // Check Completed Chapters
    if (criteria.getRequiredCompletedChapterIds() != null
        && !new HashSet<>(state.getCompletedChapterIds())
            .containsAll(criteria.getRequiredCompletedChapterIds())) {
      return false;
    }

    // Groovy custom script — runs only when all built-in checks have already passed
    String script = criteria.getCustomEvaluationScript();
    if (script != null && !script.isBlank()) {
      return evaluateGroovyScript(script, state);
    }

    return true;
  }

  boolean evaluateGroovyScript(@NonNull final String script, @NonNull final CampaignState state) {
    try {
      Binding binding = new Binding();
      binding.setVariable("state", state);
      Object result = new GroovyShell(binding).evaluate(script);
      return Boolean.TRUE.equals(result);
    } catch (Exception e) {
      log.warn("Groovy script evaluation failed — treating as false: {}", e.getMessage());
      return false;
    }
  }
}
