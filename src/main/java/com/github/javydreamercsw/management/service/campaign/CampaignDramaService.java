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

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignDramaService {

  private final DramaEventService dramaEventService;
  private final WrestlerRepository wrestlerRepository;
  private final CampaignService campaignService;
  private final UniverseContextService universeContextService;
  private final FeatureDataService featureDataService;
  private final Random random;

  /**
   * Check for and trigger story events based on campaign state.
   *
   * @param campaign The campaign to check.
   * @return Optional triggered event.
   */
  public Optional<DramaEvent> checkForStoryEvents(@NonNull final Campaign campaign) {
    CampaignState state = campaign.getState();
    Wrestler player = campaign.getWrestler();

    CampaignChapterDTO chapter = campaignService.getCurrentChapter(campaign).orElse(null);
    if (chapter == null) {
      return Optional.empty();
    }

    // Tournament Chapter: Rivalry
    if (chapter.isTournament()) {
      // Check if player has active rivalry.
      // For now, if no active rivalry, trigger one.
      if (player.getActiveRivalries().isEmpty()) {
        log.info("Triggering Tournament Rival Event for campaign {}", campaign.getId());
        return triggerRivalEvent(campaign);
      }
    }

    // Chapter 3: Outsider — fires at most once per campaign state
    if ("ch3_outsider".equals(state.getCurrentChapterId())) {
      boolean alreadyTriggered =
          featureDataService.getFeatureValue(state, "outsiderEventTriggered", Boolean.class, false);
      if (!alreadyTriggered && random.nextDouble() < 0.2) {
        log.info("Triggering Chapter 3 Outsider Event for campaign {}", campaign.getId());
        Optional<DramaEvent> event = triggerOutsiderEvent(campaign);
        if (event.isPresent()) {
          featureDataService.setFeatureValue(state, "outsiderEventTriggered", true);
        }
        return event;
      }
    }

    return Optional.empty();
  }

  /**
   * Trigger a Rival event for Chapter 2.
   *
   * @param campaign The campaign.
   * @return The created DramaEvent.
   */
  public Optional<DramaEvent> triggerRivalEvent(@NonNull final Campaign campaign) {
    Wrestler player = campaign.getWrestler();

    // Find a suitable rival
    Wrestler rival = findRival(player);
    if (rival == null) {
      log.warn("No suitable rival found for campaign {}", campaign.getId());
      return Optional.empty();
    }

    String title = "Rival Encounter: " + rival.getName();
    String description =
        "You encounter your rival " + rival.getName() + " backstage. Tensions are high.";

    Long universeId =
        campaign.getUniverse() != null
            ? campaign.getUniverse().getId()
            : universeContextService.getCurrentUniverseId();

    return dramaEventService.createDramaEvent(
        player.getId(),
        rival.getId(),
        DramaEventType.CAMPAIGN_RIVAL,
        DramaEventSeverity.NEGATIVE, // Rivals usually mean trouble
        title,
        description,
        universeId);
  }

  /**
   * Trigger an Outsider event for Chapter 3.
   *
   * @param campaign The campaign.
   * @return The created DramaEvent.
   */
  public Optional<DramaEvent> triggerOutsiderEvent(@NonNull final Campaign campaign) {
    Wrestler player = campaign.getWrestler();

    Wrestler outsider = findOutsider(player);
    if (outsider == null) {
      return Optional.empty();
    }

    String title = "The Outsider Arrives: " + outsider.getName();
    String description =
        "A mysterious outsider, " + outsider.getName() + ", has arrived to challenge you.";

    Long universeId =
        campaign.getUniverse() != null
            ? campaign.getUniverse().getId()
            : universeContextService.getCurrentUniverseId();

    return dramaEventService.createDramaEvent(
        player.getId(),
        outsider.getId(),
        DramaEventType.CAMPAIGN_OUTSIDER,
        DramaEventSeverity.MAJOR, // High stakes
        title,
        description,
        universeId);
  }

  private Wrestler findOutsider(@NonNull final Wrestler player) {
    // Prefer wrestlers not in the same faction and with a higher tier than the player.
    // Falls back to any active wrestler excluding the player if no strong candidate exists.
    WrestlerTier playerTier =
        player.getDefaultState().map(s -> s.getTier()).orElse(WrestlerTier.ROOKIE);

    Object playerFaction = player.getDefaultState().map(s -> s.getFaction()).orElse(null);

    List<Wrestler> candidates =
        wrestlerRepository.findAll().stream()
            .filter(w -> !w.getId().equals(player.getId()))
            .filter(
                w -> {
                  Object wFaction = w.getDefaultState().map(s -> s.getFaction()).orElse(null);
                  return playerFaction == null || !playerFaction.equals(wFaction);
                })
            .sorted(
                Comparator.comparingInt(
                        (Wrestler w) ->
                            w.getDefaultState()
                                .map(s -> s.getTier())
                                .orElse(WrestlerTier.ROOKIE)
                                .ordinal())
                    .reversed())
            .toList();

    // Prefer a candidate at least one tier above the player
    return candidates.stream()
        .filter(
            w ->
                w.getDefaultState().map(s -> s.getTier()).orElse(WrestlerTier.ROOKIE).ordinal()
                    > playerTier.ordinal())
        .findFirst()
        .or(() -> candidates.stream().findFirst())
        .orElse(null);
  }

  private Wrestler findRival(@NonNull final Wrestler player) {
    return wrestlerRepository.findRandomExcluding(player.getId(), PageRequest.of(0, 1)).stream()
        .findFirst()
        .orElse(null);
  }
}
