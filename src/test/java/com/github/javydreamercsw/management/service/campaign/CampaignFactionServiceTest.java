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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignFactionServiceTest {

  @Mock private FactionService factionService;
  @InjectMocks private CampaignFactionService campaignFactionService;

  @Test
  void testJoinFaction() {
    Wrestler player = new Wrestler();
    player.setId(1L);
    Campaign campaign = new Campaign();
    campaign.setWrestler(player);

    Faction faction = new Faction();
    faction.setId(10L);
    faction.setName("The NWO");

    when(factionService.addMemberToFaction(10L, 1L)).thenReturn(Optional.of(faction));

    campaignFactionService.joinFaction(campaign, faction);

    verify(factionService).addMemberToFaction(10L, 1L);
  }

  @Test
  void testBetrayFaction() {
    Wrestler player = new Wrestler();
    player.setId(1L);
    Campaign campaign = new Campaign();
    campaign.setWrestler(player);

    Faction faction = new Faction();
    faction.setId(10L);
    player.setFaction(faction);

    when(factionService.removeMemberFromFaction(any(), any(), any()))
        .thenReturn(Optional.of(faction));

    campaignFactionService.betrayFaction(campaign);

    verify(factionService).removeMemberFromFaction(10L, 1L, "Betrayal in Campaign");
  }
}
