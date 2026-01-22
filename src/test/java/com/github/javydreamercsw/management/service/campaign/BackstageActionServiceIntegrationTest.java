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
        CampaignState.builder()
            .campaign(campaign)
            .currentChapterId("ch1_beginning")
            .skillTokens(0)
            .build();

    campaign.setState(state);
    campaignRepository.save(campaign);

    // Success case: 100 dice guarantees success
    BackstageActionService.ActionOutcome outcome =
        backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 100);
    assertThat(outcome.successes()).isGreaterThan(0);
    assertThat(outcome.description().toLowerCase()).contains("successful");
    assertThat(campaign.getState().getSkillTokens()).isGreaterThan(0);

    // Failure case: 0 dice should never succeed
    Campaign campaign2 =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();
    CampaignState state2 =
        CampaignState.builder()
            .campaign(campaign2)
            .currentChapterId("ch1_beginning")
            .skillTokens(0)
            .build();
    campaign2.setState(state2);
    campaignRepository.save(campaign2);
    BackstageActionService.ActionOutcome failOutcome =
        backstageActionService.performAction(campaign2, BackstageActionType.TRAINING, 0);
    assertThat(failOutcome.successes()).isEqualTo(0);
    assertThat(failOutcome.description().toLowerCase()).contains("fail");
    assertThat(campaign2.getState().getSkillTokens()).isEqualTo(0);

    // Edge case: negative dice (should be treated as 0 or throw)
    Campaign campaign3 =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();
    CampaignState state3 =
        CampaignState.builder()
            .campaign(campaign3)
            .currentChapterId("ch1_beginning")
            .skillTokens(0)
            .build();
    campaign3.setState(state3);
    campaignRepository.save(campaign3);
    BackstageActionService.ActionOutcome negativeOutcome =
        backstageActionService.performAction(campaign3, BackstageActionType.TRAINING, -5);
    assertThat(negativeOutcome.successes()).isEqualTo(0);
    assertThat(negativeOutcome.description().toLowerCase()).contains("fail");
    assertThat(campaign3.getState().getSkillTokens()).isEqualTo(0);

    // Repeated action: skill tokens should increment
    Campaign campaign4 =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();
    CampaignState state4 =
        CampaignState.builder()
            .campaign(campaign4)
            .currentChapterId("ch1_beginning")
            .skillTokens(0)
            .build();
    campaign4.setState(state4);
    campaignRepository.save(campaign4);
    BackstageActionService.ActionOutcome first =
        backstageActionService.performAction(campaign4, BackstageActionType.TRAINING, 100);
    // Reload campaign4 to get updated state
    campaign4 = campaignRepository.findById(campaign4.getId()).orElseThrow();
    assertThat(campaign4.getState().getSkillTokens()).isEqualTo(first.successes());
    BackstageActionService.ActionOutcome second =
        backstageActionService.performAction(campaign4, BackstageActionType.TRAINING, 100);
    campaign4 = campaignRepository.findById(campaign4.getId()).orElseThrow();
    assertThat(first.successes()).isGreaterThanOrEqualTo(0);
    assertThat(second.successes()).isGreaterThanOrEqualTo(0);
    assertThat(campaign4.getState().getSkillTokens())
        .isEqualTo(first.successes() + second.successes());
  }
}
