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

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that CampaignService operations triggered by a player (ROLE_PLAYER) do not throw
 * AuthorizationDeniedException. ShowService.createShow requires ADMIN/BOOKER/SYSTEM; the service
 * must internally elevate privileges via runAsAdmin so that players can advance their campaigns.
 */
@Transactional
class CampaignServicePlayerRoleIT extends AbstractMockUserIntegrationTest {

  @Autowired private CampaignService campaignService;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;

  private Campaign campaign;
  private Wrestler opponent;

  @BeforeEach
  void setup() {
    Account playerAccount = createTestAccount("player_role_it", RoleName.PLAYER);
    Wrestler player = createTestWrestler("IT Player");
    player.setAccount(playerAccount);
    wrestlerRepository.saveAndFlush(player);

    opponent = createTestWrestler("IT Opponent");
    wrestlerRepository.saveAndFlush(opponent);

    campaign = campaignService.startCampaign(player);

    wrestlerAlignmentRepository.save(
        WrestlerAlignment.builder()
            .wrestler(player)
            .campaign(campaign)
            .alignmentType(AlignmentType.NEUTRAL)
            .level(1)
            .build());

    login(playerAccount);
  }

  @Test
  void createMatchForEncounter_asPlayer_doesNotThrowAccessDenied() {
    assertThatNoException()
        .isThrownBy(
            () ->
                campaignService.createMatchForEncounter(
                    campaign, opponent.getName(), "Test match", "One on One"));
  }
}
