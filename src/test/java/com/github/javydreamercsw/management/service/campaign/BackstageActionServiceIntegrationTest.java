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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@WithMockUser(roles = "BOOKER")
class BackstageActionServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BackstageActionService backstageActionService;
  @Autowired private CampaignRepository campaignRepository;

  @Test
  @Transactional
  void testTrainingAction() {
    Wrestler wrestler = createTestWrestler("Campaigner");
    wrestler = wrestlerRepository.save(wrestler);

    Campaign campaign =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();

    CampaignState state =
        CampaignState.builder().campaign(campaign).currentChapter(1).skillTokens(0).build();

    campaign.setState(state);
    campaignRepository.save(campaign);

    // Act - Drive = 5 dice. Approx 83% chance of at least one success.
    // Since we can't easily mock Random inside the @Service in an Integration Test without complex
    // setup,
    // we'll run it and check if state *might* change, or loop to ensure success?
    // Or we can rely on the probability.
    // Alternatively, we can assume that with enough dice, we get a success.

    // Let's rely on the result outcome string for verification in this simple pass.
    BackstageActionService.ActionOutcome outcome =
        backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 100);
    // 100 dice guarantees success basically.

    // Assert
    assertThat(outcome.successes()).isGreaterThan(0);
    assertThat(outcome.description()).contains("successful");
    assertThat(campaign.getState().getSkillTokens()).isEqualTo(1);
  }
}
