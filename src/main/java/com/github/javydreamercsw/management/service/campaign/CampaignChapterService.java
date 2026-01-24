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
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterCriteriaDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterPointDTO;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CampaignChapterService {

  private final ObjectMapper objectMapper;
  private List<CampaignChapterDTO> chapters = Collections.emptyList();

  @Autowired
  public CampaignChapterService(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void init() {
    loadChapters();
  }

  public void loadChapters() {
    try (InputStream is = getClass().getResourceAsStream("/campaign_chapters.json")) {
      if (is == null) {
        log.error("campaign_chapters.json not found in resources.");
        return;
      }
      chapters = objectMapper.readValue(is, new TypeReference<>() {});
      log.info("Loaded {} campaign chapters.", chapters.size());
    } catch (IOException e) {
      log.error("Error loading campaign chapters from JSON", e);
    }
  }

  public List<CampaignChapterDTO> getAllChapters() {
    return Collections.unmodifiableList(chapters);
  }

  public Optional<CampaignChapterDTO> getChapter(@NonNull String id) {
    return chapters.stream().filter(c -> c.getId().equals(id)).findFirst();
  }

  /**
   * Finds all chapters that the player is currently eligible to enter.
   *
   * @param state The current campaign state.
   * @return List of eligible chapters.
   */
  public List<CampaignChapterDTO> findAvailableChapters(@NonNull CampaignState state) {
    return chapters.stream()
        .filter(c -> !state.getCompletedChapterIds().contains(c.getId())) // Not already completed
        .filter(c -> isAnyPointActive(c.getEntryPoints(), state))
        .toList();
  }

  /**
   * Checks if the current chapter is ready to be exited.
   *
   * @param state The current campaign state.
   * @return true if any exit point is active.
   */
  public boolean isChapterComplete(@NonNull CampaignState state) {
    if (state.getCurrentChapterId() == null) return false;

    return getChapter(state.getCurrentChapterId())
        .map(c -> isAnyPointActive(c.getExitPoints(), state))
        .orElse(false);
  }

  private boolean isAnyPointActive(
      @NonNull List<ChapterPointDTO> points, @NonNull CampaignState state) {
    if (points.isEmpty()) {
      // If no points are defined, we might want a default behavior.
      // For entry: maybe only the first chapter?
      // For now, assume if no points are defined, it's not active unless it's an intro.
      return false;
    }
    return points.stream().anyMatch(p -> areAllCriteriaMet(p.getCriteria(), state));
  }

  private boolean areAllCriteriaMet(
      @NonNull List<ChapterCriteriaDTO> criteriaList, @NonNull CampaignState state) {
    if (criteriaList.isEmpty()) return true;

    return criteriaList.stream().allMatch(c -> isCriteriaMet(c, state));
  }

  private boolean isCriteriaMet(
      @NonNull ChapterCriteriaDTO criteria, @NonNull CampaignState state) {
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
    if (criteria.getTournamentWinner() != null
        && state.isTournamentWinner() != criteria.getTournamentWinner()) {
      return false;
    }

    if (criteria.getFailedToQualify() != null
        && state.isFailedToQualify() != criteria.getFailedToQualify()) {
      return false;
    }

    if (criteria.getWonFinale() != null && state.isWonFinale() != criteria.getWonFinale()) {
      return false;
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
      boolean inFaction = state.getCampaign().getWrestler().getFaction() != null;
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
    return criteria.getRequiredCompletedChapterIds() == null
        || new HashSet<>(state.getCompletedChapterIds())
            .containsAll(criteria.getRequiredCompletedChapterIds());

    // TODO: Implement customEvaluationScript logic using Groovy
  }
}
