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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for chapter progression service methods. Verifies that getAvailableNextChapters
 * and advanceToChapter behave correctly against the real H2 database and the loaded
 * campaign_chapters.json.
 */
@Transactional
class CampaignProgressionServiceIT extends AbstractMockUserIntegrationTest {

  @Autowired private CampaignService campaignService;
  @Autowired private CampaignProgressionService campaignProgressionService;
  @Autowired private CampaignRepository campaignRepository;

  private Campaign campaign;

  @BeforeEach
  void setup() {
    Account account = createTestAccount("prog_it_player", RoleName.PLAYER);
    Wrestler wrestler = createTestWrestler("Progression IT Wrestler");
    wrestler.setAccount(account);
    wrestlerRepository.saveAndFlush(wrestler);
    login(account);
    campaign = campaignService.startCampaign(wrestler);
  }

  @Test
  void getAvailableNextChapters_returnsSuccessorChaptersForBeginning() {
    assertThat(campaign.getState().getCurrentChapterId()).isEqualTo("beginning");

    List<CampaignChapterDTO> available =
        campaignProgressionService.getAvailableNextChapters(campaign);

    assertThat(available).isNotEmpty();
    assertThat(available)
        .extracting(CampaignChapterDTO::getId)
        .containsAnyOf("tournament", "tag_team");
  }

  @Test
  void getAvailableNextChapters_doesNotModifyPersistedState() {
    List<CampaignChapterDTO> available =
        campaignProgressionService.getAvailableNextChapters(campaign);
    assumeTrue(!available.isEmpty(), "Need at least one available next chapter");

    // The "beginning" chapter should NOT appear in completedChapterIds after the read-only call
    Campaign refreshed = campaignRepository.findById(campaign.getId()).orElseThrow();
    assertThat(refreshed.getState().getCompletedChapterIds()).doesNotContain("beginning");
  }

  @Test
  void advanceToChapter_persistsTransitionAndResetsCounters() {
    List<CampaignChapterDTO> available =
        campaignProgressionService.getAvailableNextChapters(campaign);
    assumeTrue(!available.isEmpty(), "Need at least one available next chapter");
    String nextId = available.get(0).getId();

    Optional<String> result = campaignProgressionService.advanceToChapter(campaign, nextId);

    assertThat(result).contains(nextId);
    Campaign refreshed = campaignRepository.findById(campaign.getId()).orElseThrow();
    assertThat(refreshed.getState().getCurrentChapterId()).isEqualTo(nextId);
    assertThat(refreshed.getState().getMatchesPlayed()).isZero();
    assertThat(refreshed.getState().getWins()).isZero();
    assertThat(refreshed.getState().getCurrentEncounterId()).isNull();
    assertThat(refreshed.getState().getCompletedChapterIds()).contains("beginning");
  }
}
