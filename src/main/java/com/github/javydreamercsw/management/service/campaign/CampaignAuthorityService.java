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
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.npc.NpcType;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CampaignAuthorityService {

  private final NpcRepository npcRepository;
  private final CampaignStateRepository campaignStateRepository;

  /**
   * Gets all active NPCs that can act as authority figures (Owner, Manager).
   *
   * @return List of authority NPCs.
   */
  public List<Npc> getAuthorityNPCs() {
    return npcRepository.findAll().stream()
        .filter(
            npc ->
                NpcType.OWNER.getName().equalsIgnoreCase(npc.getNpcType())
                    || NpcType.MANAGER.getName().equalsIgnoreCase(npc.getNpcType()))
        .toList();
  }

  /**
   * Applies "Unfair Match" modifiers to the player's next match.
   *
   * @param campaign The active campaign.
   * @param playerHealthPenalty Penalty applied to player health.
   * @param opponentHealthPenalty Penalty applied to opponent health (negative for bonus).
   */
  public void applyUnfairMatchModifiers(
      @NonNull Campaign campaign, int playerHealthPenalty, int opponentHealthPenalty) {
    CampaignState state = campaign.getState();
    log.info(
        "Applying Authority penalties to wrestler {}: Player Health Penalty {}, Opponent Health"
            + " Penalty {}",
        campaign.getWrestler().getName(),
        playerHealthPenalty,
        opponentHealthPenalty);

    state.setHealthPenalty(state.getHealthPenalty() + playerHealthPenalty);
    state.setOpponentHealthPenalty(state.getOpponentHealthPenalty() + opponentHealthPenalty);

    campaignStateRepository.save(state);
  }
}
