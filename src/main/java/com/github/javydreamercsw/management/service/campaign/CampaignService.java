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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;

  public Campaign startCampaign(Wrestler wrestler) {
    // Check if wrestler already has active campaign?
    // For now, allow new campaign.

    Campaign campaign =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();

    campaign = campaignRepository.save(campaign);

    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .currentChapter(1)
            .victoryPoints(0)
            .skillTokens(0)
            .bumps(0)
            .healthPenalty(0)
            .handSizePenalty(0)
            .staminaPenalty(0)
            .lastSync(LocalDateTime.now())
            .build();

    campaignStateRepository.save(state);
    campaign.setState(state);

    return campaignRepository.save(campaign);
  }

  public void processMatchResult(Campaign campaign, boolean won) {
    CampaignState state = campaign.getState();
    int chapter = state.getCurrentChapter();
    int vpChange = 0;

    switch (chapter) {
      case 1:
        vpChange = won ? 2 : -1;
        break;
      case 2:
        // TODO: Implement Chapter 2 specific logic (Medium difficulty default?)
        vpChange = won ? 3 : -1; // Assuming slightly higher reward? Spec doesn't specify.
        // Spec: Chapter 2 ... Matches default to Medium difficulty.
        // Spec: Chapter 3 ... Victories earn 4 VP, losses lose 2 VP.
        // Spec for Chapter 1: Victories earn 2 VP, losses lose 1 VP.
        // I will interpolate for Chapter 2 or check if spec missed it.
        // "Chapter 2: The tournament quest. Matches default to Medium difficulty. Features a Rival
        // system..."
        // It doesn't specify VP for Chapter 2. I'll stick to Chapter 1 values or interpolate.
        // Let's assume Chapter 2 is 3 VP / -1 VP for now or same as Ch 1.
        // Given it's a tournament, maybe 3 VP.
        vpChange = won ? 3 : -1;
        break;
      case 3:
        vpChange = won ? 4 : -2;
        break;
    }

    state.setVictoryPoints(state.getVictoryPoints() + vpChange);
    campaignStateRepository.save(state);
  }

  public void advanceChapter(Campaign campaign) {
    CampaignState state = campaign.getState();
    if (state.getCurrentChapter() < 3) {
      state.setCurrentChapter(state.getCurrentChapter() + 1);
      campaignStateRepository.save(state);
    } else {
      completeCampaign(campaign);
    }
  }

  public void completeCampaign(Campaign campaign) {
    campaign.setStatus(CampaignStatus.COMPLETED);
    campaign.setEndedAt(LocalDateTime.now());
    campaignRepository.save(campaign);
  }
}
