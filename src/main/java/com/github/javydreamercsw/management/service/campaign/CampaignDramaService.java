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
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignDramaService {

  private final DramaEventService dramaEventService;
  private final WrestlerRepository wrestlerRepository;
  private final Random random;

  /**
   * Trigger a Rival event for Chapter 2.
   *
   * @param campaign The campaign.
   * @return The created DramaEvent.
   */
  public Optional<DramaEvent> triggerRivalEvent(Campaign campaign) {
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

    return dramaEventService.createDramaEvent(
        player.getId(),
        rival.getId(),
        DramaEventType.CAMPAIGN_RIVAL,
        DramaEventSeverity.NEGATIVE, // Rivals usually mean trouble
        title,
        description);
  }

  /**
   * Trigger an Outsider event for Chapter 3.
   *
   * @param campaign The campaign.
   * @return The created DramaEvent.
   */
  public Optional<DramaEvent> triggerOutsiderEvent(Campaign campaign) {
    Wrestler player = campaign.getWrestler();

    // Find a suitable outsider (someone not in the same faction, or high tier?)
    // For now, just a random opponent
    Wrestler outsider = findRival(player); // Reuse logic for now
    if (outsider == null) {
      return Optional.empty();
    }

    String title = "The Outsider Arrives: " + outsider.getName();
    String description =
        "A mysterious outsider, " + outsider.getName() + ", has arrived to challenge you.";

    return dramaEventService.createDramaEvent(
        player.getId(),
        outsider.getId(),
        DramaEventType.CAMPAIGN_OUTSIDER,
        DramaEventSeverity.MAJOR, // High stakes
        title,
        description);
  }

  private Wrestler findRival(Wrestler player) {
    List<Long> allIds = wrestlerRepository.findAllIds();
    // Filter out player
    List<Long> opponentIds = allIds.stream().filter(id -> !id.equals(player.getId())).toList();

    if (opponentIds.isEmpty()) return null;

    Long randomId = opponentIds.get(random.nextInt(opponentIds.size()));
    return wrestlerRepository.findById(randomId).orElse(null);
  }
}
