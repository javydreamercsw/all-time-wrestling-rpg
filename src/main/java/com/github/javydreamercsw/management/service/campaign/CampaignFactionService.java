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
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CampaignFactionService {

  private final FactionService factionService;

  /**
   * Logic for player joining an existing faction during a campaign.
   *
   * @param campaign The active campaign.
   * @param faction The faction to join.
   * @return The updated faction.
   */
  public Optional<Faction> joinFaction(@NonNull Campaign campaign, @NonNull Faction faction) {
    Wrestler player = campaign.getWrestler();
    log.info("Wrestler {} joining faction {} during campaign", player.getName(), faction.getName());
    return factionService.addMemberToFaction(faction.getId(), player.getId());
  }

  /**
   * Logic for player betraying/leaving their current faction during a campaign.
   *
   * @param campaign The active campaign.
   * @return The updated faction (now without the player).
   */
  public Optional<Faction> betrayFaction(@NonNull Campaign campaign) {
    Wrestler player = campaign.getWrestler();
    Faction currentFaction = player.getFaction();

    if (currentFaction == null) {
      log.warn("Wrestler {} is not in a faction to betray", player.getName());
      return Optional.empty();
    }

    log.info(
        "Wrestler {} betraying faction {} during campaign",
        player.getName(),
        currentFaction.getName());
    return factionService.removeMemberFromFaction(
        currentFaction.getId(), player.getId(), "Betrayal in Campaign");
  }

  /**
   * Logic for player recruiting an NPC to their faction during a campaign.
   *
   * @param campaign The active campaign.
   * @param recruit The wrestler to recruit.
   * @return The updated faction.
   */
  public Optional<Faction> recruitToFaction(@NonNull Campaign campaign, @NonNull Wrestler recruit) {
    Wrestler player = campaign.getWrestler();
    Faction currentFaction = player.getFaction();

    if (currentFaction == null) {
      log.warn("Player {} must be in a faction to recruit others", player.getName());
      return Optional.empty();
    }

    log.info(
        "Player {} recruiting {} to faction {} during campaign",
        player.getName(),
        recruit.getName(),
        currentFaction.getName());
    return factionService.addMemberToFaction(currentFaction.getId(), recruit.getId());
  }
}
