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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CampaignTagTeamChapterTest extends AbstractMockUserIntegrationTest {

  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;

  @Autowired private WrestlerRepository wrestlerRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private TitleRepository titleRepository;
  @Autowired private ObjectMapper objectMapper;

  private Campaign campaign;

  @org.junit.jupiter.api.BeforeEach
  public void setup() {
    // Ensure "ATW Tag Team" title exists
    if (titleRepository.findByName("ATW Tag Team").isEmpty()) {
      Title tagTitle = new Title();
      tagTitle.setName("ATW Tag Team");
      tagTitle.setTier(WrestlerTier.MIDCARDER);
      tagTitle.setGender(Gender.MALE);
      tagTitle.setChampionshipType(ChampionshipType.TEAM);
      tagTitle.setIsActive(true);
      titleRepository.save(tagTitle);
    }

    Wrestler player = wrestlerRepository.findAll().get(0); // Use first available wrestler

    // Ensure at least one other team exists for title awarding
    if (wrestlerRepository.count() < 3) {
      Wrestler w2 = new Wrestler();
      w2.setName("Team Member 1");
      w2.setGender(Gender.MALE);
      w2.setTier(WrestlerTier.MIDCARDER);
      wrestlerRepository.save(w2);

      Wrestler w3 = new Wrestler();
      w3.setName("Team Member 2");
      w3.setGender(Gender.MALE);
      w3.setTier(WrestlerTier.MIDCARDER);
      wrestlerRepository.save(w3);

      Team team = new Team();
      team.setName("Test Team");
      team.setWrestler1(w2);
      team.setWrestler2(w3);
      team.setStatus(TeamStatus.ACTIVE);
      teamRepository.save(team);
    } else {
      // If wrestlers exist, ensure at least one team exists that doesn't have the player
      if (teamRepository.count() == 0) {
        List<Wrestler> others =
            wrestlerRepository.findAll().stream().filter(w -> !w.equals(player)).toList();
        if (others.size() >= 2) {
          Team team = new Team();
          team.setName("Test Team");
          team.setWrestler1(others.get(0));
          team.setWrestler2(others.get(1));
          team.setStatus(TeamStatus.ACTIVE);
          teamRepository.save(team);
        }
      }
    }

    campaign = campaignService.startCampaign(player);
  }

  @Test
  public void testFailChapter1AndEnterTagChapter() throws JsonProcessingException {
    CampaignState state = campaign.getState();
    // Simulate Chapter 1 Failure
    // Criteria: minMatchesPlayed: 5, maxVictoryPoints: 4
    state.setMatchesPlayed(5);
    state.setVictoryPoints(3); // Less than 4
    state.setWins(1);
    campaignStateRepository.save(state);

    // Advance Chapter
    // Should detect "Sent to Development" exit from Ch1
    // And "Sent to Development" entry for Ch2 Tag Team
    campaignService.advanceChapter(campaign);

    // Reload

    campaign = campaignRepository.findById(campaign.getId()).get();

    state = campaign.getState();

    assertThat(state.getCurrentChapterId()).isEqualTo("tag_team");

    // Verify Tag Team Chapter Initialization

    // 1. Tag Team Title should be awarded (was vacant)
    Title tagTitle = titleRepository.findByName("ATW Tag Team").get();
    assertThat(tagTitle.isVacant()).isFalse();
    assertThat(tagTitle.getCurrentChampions()).isNotEmpty();

    // 2. Partner ID should be null (recruiting)
    Map<String, Object> features =
        objectMapper.readValue(state.getFeatureData(), new TypeReference<>() {});
    assertThat(features.get("partnerId")).isNull();
    assertThat(features.get("recruitingPartner")).isEqualTo(true);
  }
}
