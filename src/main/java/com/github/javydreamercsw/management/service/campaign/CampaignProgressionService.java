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

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles campaign phase progression: advancing chapters, completing campaigns, and initializing
 * chapter-specific state. Extracted from {@link CampaignService} to reduce its scope.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignProgressionService {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;
  private final CampaignChapterService chapterService;
  private final TournamentService tournamentService;
  private final TitleRepository titleRepository;
  private final TeamRepository teamRepository;
  private final TitleService titleService;
  private final StorylineDirectorService storylineDirectorService;
  private final WrestlerStatusService wrestlerStatusService;
  private final FeatureDataService featureDataService;

  // Field-injected with @Lazy to break the circular dependency with CampaignService
  @org.springframework.beans.factory.annotation.Autowired
  @org.springframework.context.annotation.Lazy
  private CampaignService campaignService;

  private final Random random = new Random();

  private static final String KEY_FINALS_PHASE = "finalsPhase";
  private static final String KEY_PARTNER_ID = "partnerId";
  private static final String KEY_RECRUITING_PARTNER = "recruitingPartner";

  /**
   * Returns the chapters the player could enter after the current one completes, without modifying
   * any persisted state. Use this to present a choice when more than one chapter qualifies.
   */
  @Transactional(readOnly = true)
  public List<CampaignChapterDTO> getAvailableNextChapters(@NonNull final Campaign campaignParam) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    campaign.getWrestler().getReigns().size();
    CampaignState state = campaign.getState();
    String oldId = state.getCurrentChapterId();

    // Temporarily mark the current chapter as completed so entry criteria that require it
    // resolve correctly. readOnly = true prevents Hibernate from flushing this change.
    boolean added = oldId != null && state.getCompletedChapterIds().add(oldId);
    List<CampaignChapterDTO> result =
        chapterService.findAvailableChapters(state, campaign.getWrestler().getName());
    if (added) {
      state.getCompletedChapterIds().remove(oldId);
    }
    return result;
  }

  /**
   * Transitions the campaign to a specific next chapter chosen by the player (or auto-selected when
   * only one option exists). Marks the current chapter complete, awards exit-point status cards,
   * resets per-chapter counters, and runs chapter-specific initialization.
   */
  public Optional<String> advanceToChapter(
      @NonNull final Campaign campaignParam, @NonNull final String targetChapterId) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    campaign.getWrestler().getReigns().size();

    CampaignChapterDTO nextChapter =
        chapterService
            .getChapter(targetChapterId)
            .orElseThrow(
                () -> new IllegalArgumentException("Chapter not found: " + targetChapterId));

    CampaignState state = campaign.getState();
    String oldId = state.getCurrentChapterId();

    if (oldId != null) {
      state.getCompletedChapterIds().add(oldId);
      chapterService
          .getChapter(oldId)
          .ifPresent(
              oldChapter ->
                  chapterService
                      .getActivePoint(oldChapter.getExitPoints(), state)
                      .ifPresent(
                          point -> {
                            if (point.getStatusCardRewards() != null) {
                              point
                                  .getStatusCardRewards()
                                  .forEach(
                                      key ->
                                          wrestlerStatusService.assignStatus(
                                              campaign.getWrestler().getId(), key));
                            }
                          }));
    }

    state.setCurrentChapterId(nextChapter.getId());
    state.setCurrentEncounterId(null);
    state.setMatchesPlayed(0);
    state.setWins(0);
    state.setLosses(0);

    chapterService
        .getActivePoint(nextChapter.getEntryPoints(), state)
        .ifPresent(
            point -> {
              if (point.getStatusCardRewards() != null) {
                point
                    .getStatusCardRewards()
                    .forEach(
                        key ->
                            wrestlerStatusService.assignStatus(
                                campaign.getWrestler().getId(), key));
              }
            });

    if (nextChapter.isTournament()) {
      featureDataService.setFeatureValue(state, KEY_FINALS_PHASE, true);
      tournamentService.initializeTournament(campaign);
    } else {
      featureDataService.setFeatureValue(state, KEY_FINALS_PHASE, false);
    }

    if (nextChapter.isTagTeam()) {
      initializeTagTeamChapter(campaign);
    }

    campaignStateRepository.save(state);
    log.info("Advanced to chapter: {}", nextChapter.getId());
    return Optional.of(nextChapter.getId());
  }

  public Optional<String> advanceChapter(@NonNull final Campaign campaignParam) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    campaign.getWrestler().getReigns().size();

    CampaignChapterDTO contextChapter = campaignService.getCurrentChapter(campaign).orElse(null);

    List<CampaignChapterDTO> available = getAvailableNextChapters(campaignParam);
    if (!available.isEmpty()) {
      return advanceToChapter(campaignParam, available.get(0).getId());
    } else {
      CampaignState state = campaign.getState();
      String oldId = state.getCurrentChapterId();
      if (oldId != null) {
        state.getCompletedChapterIds().add(oldId);
        chapterService
            .getChapter(oldId)
            .ifPresent(
                oldChapter ->
                    chapterService
                        .getActivePoint(oldChapter.getExitPoints(), state)
                        .ifPresent(
                            point -> {
                              if (point.getStatusCardRewards() != null) {
                                point
                                    .getStatusCardRewards()
                                    .forEach(
                                        key ->
                                            wrestlerStatusService.assignStatus(
                                                campaign.getWrestler().getId(), key));
                              }
                            }));
      }
      log.info("No predefined next chapter found. Initializing AI-driven storyline.");
      if (state.getActiveStoryline() != null) {
        storylineDirectorService.abandonStoryline(state.getActiveStoryline());
      }
      CampaignStoryline newStoryline =
          storylineDirectorService.initializeStoryline(campaign, contextChapter);
      state.setActiveStoryline(newStoryline);
      state.setCurrentChapterId(newStoryline.getTitle());
      campaignStateRepository.save(state);
      return Optional.of(newStoryline.getTitle());
    }
  }

  public void completeCampaign(@NonNull Campaign campaign) {
    campaign.setStatus(CampaignStatus.COMPLETED);
    campaign.setEndedAt(LocalDateTime.now());
    campaignRepository.save(campaign);
  }

  @Transactional(readOnly = true)
  public boolean isChapterComplete(@NonNull final Campaign campaignParam) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
    campaign.getWrestler().getReigns().size();
    return chapterService.isChapterComplete(campaign.getState());
  }

  void initializeTagTeamChapter(@NonNull Campaign campaign) {
    titleRepository
        .findByName("ATW Tag Team")
        .ifPresent(
            title -> {
              if (title.isVacant()) {
                List<Team> teams = teamRepository.findAll();
                teams.removeIf(
                    t ->
                        t.getWrestler1().equals(campaign.getWrestler())
                            || t.getWrestler2().equals(campaign.getWrestler()));
                if (!teams.isEmpty()) {
                  Team newChamps = teams.get(random.nextInt(teams.size()));
                  List<Wrestler> champs =
                      List.of(newChamps.getWrestler1(), newChamps.getWrestler2());
                  titleService.awardTitleTo(title, champs);
                  log.info(
                      "Initialized Tag Team Chapter: Awarded vacant title to {}",
                      newChamps.getName());
                }
              }
            });

    featureDataService.setFeatureValue(campaign.getState(), KEY_PARTNER_ID, null);
    featureDataService.setFeatureValue(campaign.getState(), KEY_RECRUITING_PARTNER, true);
  }
}
